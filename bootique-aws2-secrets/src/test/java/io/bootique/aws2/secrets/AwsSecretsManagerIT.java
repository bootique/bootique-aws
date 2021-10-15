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
package io.bootique.aws2.secrets;

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
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@BQTest
public class AwsSecretsManagerIT {

    // TODO: unfortunately can't reuse Localstack between the tests, as Testcontainers doesn't provide a GLOBAL scope

    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.SECRETSMANAGER);

    // app can't be static, as it won't be able to access values from localstack
    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.accessKey", localstack.getAccessKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.secretKey", localstack.getSecretKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.defaultRegion", localstack.getRegion()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.endpointOverride", localstack.getEndpointOverride(LocalStackContainer.Service.SECRETSMANAGER).toString()))
            .createRuntime();

    @Test
    public void testSecrets() {

        String secret = "{\"a\":\"top secret\"}";

        SecretsManagerClient secretsManager = app.getInstance(SecretsManagerClient.class);

        String arn = secretsManager.putSecretValue(b -> b.secretString(secret)).arn();
        assertNotNull(arn);

        String secretRead = secretsManager.getSecretValue(b -> b.secretId(arn)).secretString();
        assertEquals(secret, secretRead);
    }
}
