<!--
  Licensed to ObjectStyle LLC under one
  or more contributor license agreements.  See the NOTICE file
  distributed with this work for additional information
  regarding copyright ownership.  The ObjectStyle LLC licenses
  this file to you under the Apache License, Version 2.0 (the
  "License"); you may not use this file except in compliance
  with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing,
  software distributed under the License is distributed on an
  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
  KIND, either express or implied.  See the License for the
  specific language governing permissions and limitations
  under the License.
  -->

[![Build Status](https://travis-ci.org/bootique/bootique-aws.svg)](https://travis-ci.org/bootique/bootique-aws)
[![Maven Central](https://img.shields.io/maven-central/v/io.bootique.aws/bootique-aws.svg?colorB=brightgreen)](https://search.maven.org/artifact/io.bootique.aws/bootique-aws)

# bootique-aws

Helps to build Bootique apps that consume AWS services and/or are deployed in AWS environment (such as AWS Java Lambdas). 
Integrates [Amazon Java SDK 1.x](https://aws.amazon.com/sdk-for-java/) libraries with Bootique and allows to seamlessly 
merge data from AWS Secret Manager into Bootique app configuration.

## Getting Started

To be able to call any AWS services, an app will need to be provided with a set of AWS credentials and a default region. 
This can be done either via a Bootique config or one of the standard AWS client library strategies. Let's look at the 
first option - Bootique config. Start by importing `bootique-aws` dependency:
```xml
<dependency>
	<groupId>io.bootique.aws</groupId>
	<artifactId>bootique-aws</artifactId>
</dependency>
```
Generate access credentials in the AWS console, and configure the app similar to this:
```yaml
aws:
  accessKey: AKINXC5IHNPO255OW4EW
  secretKey: N8RX3nvEjlOfB3Fmp+KPVAV+4wbLSQCUL9+tkEA+
  defaultRegion: us-east-2
```
To use a specific AWS service, you will need to import a corresponding module, that will create an injectable
client singleton for such service and will automatically use the credentials above. E.g. for S3 this might look like
this:
```xml
<dependency>
	<groupId>io.bootique.aws</groupId>
	<artifactId>bootique-aws-s3</artifactId>
</dependency>
```

```java
@Inject
private AmazonS3 s3Client;
```

_If Bootique doesn't yet provide a module to call your favorite AWS service, you can easily write your own.
Refer to "bootique-aws-s3" source code for a good example. And don't forget to ping us, so we make it available in Bootique._

AWS client library has its own 
[credential provider chain](https://docs.aws.amazon.com/sdk-for-java/v1/developer-guide/credentials.html) that attempts
to load credentials from various sources (env vars, system properties, profile files, etc.). It is disabled by default
in Bootique to avoid unexpected interactions between the app and the environment (e.g. a misconfigured unit test 
messing up a production cluster). But it can be turned on explicitly as follows:

```java
// turn on the entire default provider chain
AwsModule.extend(binder).addAwsCredentialsProviderChain();
```

```java
// turn on one or more credential providers individually
AwsModule.extend(binder)
        .addEnvCredentialsProvider(1)
        .addProfileCredentialsProvider(2);
```
Note that Bootique configuration will still take precedence over these providers, and only if the configuration is
missing, the providers would be invoked.

## AWS EC2 and ECS

You don't need an explicit `accessKey` / `secretKey` configuration when running on EC2 or ECS, as these environments 
provide a built-in metadata service to look up credentials. To enable no-config deployment in EC2 or ECS, add the 
following credentials provider:
```java
AwsModule.extend(binder).addEC2ContainerCredentialsProvider();
```

## AWS Lambdas

Minimal footprint and quick startup time of Bootique makes it a perfect technology for writing 
[AWS Lambdas in Java](https://docs.aws.amazon.com/lambda/latest/dg/lambda-java.html). Instead of a "main" class in a 
normal app, you would implement a Lambda "handler" with Bootique runtime included in it:
```java
public class MyHandler implements RequestHandler<Object, String> {

    private static BQRuntime runtime = Bootique.app()
            .autoLoadModules()
            .createRuntime();

    @Override
    public String handleRequest(Object o, Context context) {
        // do something
    }
}
```
The main difference with a standalone app is that there's no CLI, and you only "create" a BQRuntime, but do not
"run" a command. You'd usually make the `runtime` static to speed up responses from warmed-up lambda instances.
Within the `handle*` method you'd obtain Bootique objects by calling `runtime.getInstance(MyType.class)` instead of
injection. Otherwise, all the Bootique APIs and practices should work unchanged.

Since Lambda environment already includes credentials to access the rest of AWS as shell variables, you don't need an
explicit `accessKey` / `secretKey` configuration. Instead, add an extra line to add a Lambda-friendly credentials 
provider:
```java
private static BQRuntime runtime = Bootique.app()
    .autoLoadModules()
     // this will pick up credentials from the environment vars
    .module(b -> AwsModule.extend(b).addLambdaCredentialsProvider())
    .createRuntime();
```

## AWS Secret Manager as a Source of App Configuration

Often parts of the app configuration (especially various passwords) are stored as "secrets" in the 
[AWS Secrets Manager](https://aws.amazon.com/secrets-manager/). WHen you query a Secrets Manager, secrets are returned 
as simple JSON objects. Bootique provides a way to load and merge them into the main app configuration tree. To work 
with the Secrets Manager you will need the following dependency:

```xml
<dependency>
	<groupId>io.bootique.aws</groupId>
	<artifactId>bootique-aws-secrets</artifactId>
</dependency>
```
And then you'd list any secrets that should be included in the app configuration:
```yaml
awssecrets:
  secrets:
    secret1:
      awsName: "mysecret" # Either a human-readable AWS name of a secret or an AWS ARN
      mergePath: "myapp.subconfig" # Where in a config tree to place the loaded secret
      jsonTransformer: "mytransformer" # Optional. 
         # A symbolic name of a class implementing AwsJsonTransformer that would 
         # transform the secret's JSON into a form compatible with the app config.
```
If secret field names match exactly your target configuration properties, you won't need a transformer. Otherwise, 
write a class implementing `AwsJsonTransformer` and register it like this:
```java
AwsSecretsModule.extend(binder).addTransformer("mytransformer", MyTransformer.class);
```
Bootique strives to provide built-in transformers for the known secret formats. Here is an example showing how to 
load a standard RDS connection secret (that has a predefined format) and transform it to a `bootique-jdbc` Hikari 
DataSource configuration:

```yaml
awssecrets:
  secrets:
    secret1: 
      awsName: "myRDSSecret"
      mergePath: "jdbc.mydb"
      jsonTransformer: "rds-to-hikari-datasource" # This transformer is provided by 
          # Bootique out of the box and will transform a standard RDS connection secret
          # into a Hikari config with "jdbcUrl", "username" and "password" keys.
```

## Custom Endpoints and Testing

TODO