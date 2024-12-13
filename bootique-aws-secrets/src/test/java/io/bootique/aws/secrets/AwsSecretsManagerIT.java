/*
 * Licensed to ObjectStyle LLC under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ObjectStyle LLC licenses
 * this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package io.bootique.aws.secrets;

import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.PutSecretValueRequest;
import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@BQTest
public class AwsSecretsManagerIT {

    // TODO: unfortunately can't reuse Localstack between the tests, as Testcontainers doesn't provide a GLOBAL scope

    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:4.0.3");

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.SECRETSMANAGER);

    // app can't be static, as it won't be able to access values from localstack
    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.accessKey", localstack.getAccessKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.secretKey", localstack.getSecretKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.signingRegion", localstack.getRegion()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.serviceEndpoint", localstack.getEndpointOverride(LocalStackContainer.Service.SECRETSMANAGER).toString()))
            .createRuntime();

    @Test
    public void secrets() {

        String s1 = "{\"a\":\"top secret\"}";
        String s2 = "{\"a\":\"top secret updated\"}";

        AWSSecretsManager secretsManager = app.getInstance(AWSSecretsManager.class);

        String arn = secretsManager.createSecret(new CreateSecretRequest().withSecretString(s1).withName("s")).getARN();
        assertNotNull(arn);
        assertEquals(s1, secretsManager.getSecretValue(new GetSecretValueRequest().withSecretId(arn)).getSecretString());

        secretsManager.putSecretValue(new PutSecretValueRequest().withSecretString(s2).withSecretId(arn));
        assertEquals(s2, secretsManager.getSecretValue(new GetSecretValueRequest().withSecretId(arn)).getSecretString());
    }
}
