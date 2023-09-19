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
package io.bootique.aws.secrets.transformers;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.amazonaws.services.secretsmanager.model.CreateSecretRequest;
import com.zaxxer.hikari.HikariConfig;
import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.config.ConfigurationFactory;
import io.bootique.jdbc.hikaricp.HikariCPManagedDataSourceFactory;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@BQTest
public class RDSToHikariDataSourceTransformerIT {

    // TODO: unfortunately can't reuse Localstack between the tests, as Testcontainers doesn't provide a GLOBAL scope
    private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:2.2.0");

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.SECRETSMANAGER);

    @BeforeAll
    static void loadSecrets() {

        String rdsSecret = "{\"password\":\"rds_password\", " +
                "\"username\":\"rds_user\"," +
                "\"engine\":\"fakedb\"," +
                "\"host\":\"rdshost\"," +
                "\"port\":\"7890\"," +
                "\"dbname\":\"mydb\"}";

        AWSCredentialsProvider credentialsProvider = new AWSStaticCredentialsProvider(
                new BasicAWSCredentials(localstack.getAccessKey(), localstack.getSecretKey()));

        AwsClientBuilder.EndpointConfiguration configuration = new AwsClientBuilder.EndpointConfiguration(
                localstack.getEndpointOverride(LocalStackContainer.Service.SECRETSMANAGER).toString(),
                localstack.getRegion()
        );

        AWSSecretsManagerClientBuilder.standard()
                .withCredentials(credentialsProvider)
                .withEndpointConfiguration(configuration)
                .build()
                .createSecret(new CreateSecretRequest().withSecretString(rdsSecret).withName("rdsSecret"));
    }

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.accessKey", localstack.getAccessKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.secretKey", localstack.getSecretKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.signingRegion", localstack.getRegion()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.serviceEndpoint", localstack.getEndpointOverride(LocalStackContainer.Service.SECRETSMANAGER).toString()))

            // base config. Secrets will be merged on top
            .module(b -> BQCoreModule.extend(b).setProperty("bq.a.type", "hikari"))

            // configure RDS Secret
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s1.awsName", "rdsSecret"))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s1.jsonTransformer", "rds-to-hikari-datasource"))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s1.mergePath", "a"))

            .createRuntime();

    @Test
    public void testRdsConfigTranslatedAndMerged() {

        HikariConfig config = app.getInstance(ConfigurationFactory.class)
                .config(TestHikariFactory.class, "a")
                .toConfig();

        assertEquals("jdbc:fakedb://rdshost:7890/mydb", config.getJdbcUrl());
        assertEquals("rds_user", config.getUsername());
        assertEquals("rds_password", config.getPassword());
    }

    public static final class TestHikariFactory extends HikariCPManagedDataSourceFactory {
        public HikariConfig toConfig() {
            return super.toConfiguration("dummy");
        }
    }

}
