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

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.secretsmanager.AWSSecretsManager;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.amazonaws.services.secretsmanager.model.CreateSecretResult;
import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.config.ConfigurationFactory;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@Testcontainers
@BQTest
public class AwsSecretsConfigurationLoaderIT {

    private static CreateSecretResult SECRET1;
    private static CreateSecretResult SECRET2;

    // TODO: unfortunately can't reuse Localstack between the tests, as Testcontainers doesn't provide a GLOBAL scope
    private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:2.2.0");

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.SECRETSMANAGER);

    @BeforeAll
    static void loadSecrets() {

        String secret1 = "{\"password\":\"y_secret\"}";
        String secret2 = "{\"password\":\"z_secret\", \"user\":\"z_uname\"}";

        AWSSecretsManager sm = AWSSecretsManagerClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(new BasicAWSCredentials(localstack.getAccessKey(), localstack.getSecretKey())))
                .withEndpointConfiguration(localstack.getEndpointConfiguration(LocalStackContainer.Service.SECRETSMANAGER))
                .build();

        SECRET1 = sm.createSecret(new CreateSecretRequest().withSecretString(secret1).withName("secret1"));
        assertNotNull(SECRET1);

        SECRET2 = sm.createSecret(new CreateSecretRequest().withSecretString(secret2).withName("secret2"));
        assertNotNull(SECRET2);
    }

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.accessKey", localstack.getAccessKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.secretKey", localstack.getSecretKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.signingRegion", localstack.getEndpointConfiguration(LocalStackContainer.Service.SECRETSMANAGER).getSigningRegion()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.serviceEndpoint", localstack.getEndpointConfiguration(LocalStackContainer.Service.SECRETSMANAGER).getServiceEndpoint()))

            // load some base config.. Secrets will be merged on top of it
            .module(b -> BQCoreModule.extend(b).setProperty("bq.a.user", "a_uname"))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.a.password", "a_secret"))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.b.c.user", "bc_uname"))

            // configure and enable AwsSecretsConfigurationLoader that we are testing
            // test lookup by both name and ARN
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s1.awsName", SECRET1.getName()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s1.mergePath", "a"))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s2.awsName", SECRET2.getARN()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s2.mergePath", "b.c"))

            .createRuntime();

    @Test
    public void testConfigMerged() {

        UserProfile a = app.getInstance(ConfigurationFactory.class).config(UserProfile.class, "a");
        assertEquals("a_uname", a.user);
        assertEquals("y_secret", a.password);

        UserProfile bc = app.getInstance(ConfigurationFactory.class).config(UserProfile.class, "b.c");
        assertEquals("z_uname", bc.user);
        assertEquals("z_secret", bc.password);
    }

    public static final class UserProfile {
        private String user;
        private String password;

        public void setPassword(String password) {
            this.password = password;
        }

        public void setUser(String user) {
            this.user = user;
        }
    }
}
