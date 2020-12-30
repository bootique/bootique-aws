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

To setup a Bootique app to access any AWS services, start by importing `bootique-aws` dependency:
```xml
<dependency>
	<groupId>io.bootique.aws</groupId>
	<artifactId>bootique-aws</artifactId>
</dependency>
```
Obtain access credentials from AWS, and provide a configuration to the app similar to this:
```yaml
aws:
  accessKey: AKINXC5IHNPO255OW4EW
  secretKey: N8RX3nvEjlOfB3Fmp+KPVAV+4wbLSQCUL9+tkEA+
  defaultRegion: us-east-2
```

To use a specific AWS service (e.g. S3), you will need to import a corresponding module, that will create an injectable
client singleton for such service based on configuration above:

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

This recipe works the same for apps that are deployed within the AWS cloud or anywhere else outside of it.

TODO: other strategies for credentials loading

## AWS Secret Manager as a Source of App Configuration

Often parts of the app configuration (especially various service credentials) are stored in the 
[AWS Secrets Manager](https://aws.amazon.com/secrets-manager/) as "secrets". Secrets are returned as simple JSON objects. 
Bootique provides a way to load and merge them into the main app configuration tree. To work with the Secrets Manager
you will need the following dependency:

```xml
<dependency>
	<groupId>io.bootique.aws</groupId>
	<artifactId>bootique-aws-secrets</artifactId>
</dependency>
```
Once you include it, you'd list the secrets to load and merge with the rest of the app configuration using a config
similar to this:
```yaml
awssecrets:
  secrets:
    - awsName: "mysecret" # Either a human-readable name of a secret or an ARN
      mergePath: "myapp.subconfig" # Where in a config tree to place the loaded secret
      jsonTransformer: "mytransformer" # Optional. 
         # A symbolic name of a class implementing AwsJsonTransformer that would 
         # transform the secret's JSON into a form compatible with the app config.
```
If secret field names exactly match your target configuration properties, you don't need a transformer. Otherwise 
write a class implementing `AwsJsonTransformer` and register it like this:
```java
AwsSecretsModule.extend(binder).addTransformer("mytransformer", MyTransformer.class);
```
Bootique strives to provide built-in transformers for the known secret structures. Here is an example showing how to 
load a standard RDS connection secret (that has a predefined format) and transform it to a `bootique-jdbc` Hikari 
DataSource configuration:

```yaml
awssecrets:
  secrets:
    - awsName: "myRDSSecret"
      mergePath: "jdbc.mydb"
      jsonTransformer: "rds-to-hikari-datasource" # This transformer is provided by 
          # Bootique out of the box and will transform a standard RDS connection secret
          # into a Hikari config with "jdbcUrl", "username" and "password" keys.
```
## AWS Lambdas with Bootique 

Minimal footprint and quick startup time of Bootique makes it perfect for writing AWS Java Lambdas. From the Bootique 
perspective Lambda is just another kind of Java app. Lambda's "handler" class may look like this:
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
The main difference with a stadalone app is that there's no CLI involved, and you only "create" a BQRuntime, but do not 
"run" a command within it. You'd usually make the `runtime` static to speed up processing of warmed-up lambda instances. 
Within the `handle` method you'd obtain Bootique objects by calling `runtime.getInstance(MyType.class)` instead of 
injection. Otherwise all the Bootique APIs and practices should work unchanged. 

Since Lambda environment already includes credentials to access the rest of AWS as shell variables, you can avoid
`accessKey` / `secretKey` configuration. Instead add an extra line when creating a `BQRuntime`:

```java
private static BQRuntime runtime = Bootique.app()
    .autoLoadModules()
     // this will pick up credentials from the environment vars
    .module(b -> AwsModule.extend(b).addLambdaCredentialsProvider())
    .createRuntime();
```

## Custom Endpoints and Testing

TODO