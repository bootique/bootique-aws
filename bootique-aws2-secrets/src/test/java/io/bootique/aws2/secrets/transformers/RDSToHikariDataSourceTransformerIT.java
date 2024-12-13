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
package io.bootique.aws2.secrets.transformers;

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
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@BQTest
public class RDSToHikariDataSourceTransformerIT {

    // TODO: unfortunately can't reuse Localstack between the tests, as Testcontainers doesn't provide a GLOBAL scope
    private static final DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:4.0.3");

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

        StaticCredentialsProvider credentialsProvider = StaticCredentialsProvider.create(
                AwsBasicCredentials.create(localstack.getAccessKey(), localstack.getSecretKey()));

        SecretsManagerClient.builder()
                .credentialsProvider(credentialsProvider)
                .region(Region.of(localstack.getRegion()))
                .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.SECRETSMANAGER))
                .build()
                .createSecret(b -> b.secretString(rdsSecret).name("rdsSecret"));
    }

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.credentials.accessKey", localstack.getAccessKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.credentials.secretKey", localstack.getSecretKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.defaultRegion", localstack.getRegion()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.endpointOverride", localstack.getEndpointOverride(LocalStackContainer.Service.SECRETSMANAGER).toString()))

            // base config. Secrets will be merged on top
            .module(b -> BQCoreModule.extend(b).setProperty("bq.a.type", "hikari"))

            // configure RDS Secret
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s1.awsName", "rdsSecret"))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s1.jsonTransformer", "rds-to-hikari-datasource"))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awssecrets.secrets.s1.mergePath", "a"))

            .createRuntime();

    @Test
    public void rdsConfigTranslatedAndMerged() {

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
