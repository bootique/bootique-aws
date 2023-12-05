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
import io.bootique.BQModule;
import io.bootique.ModuleCrate;
import io.bootique.aws2.AwsConfig;
import io.bootique.aws2.secrets.transformer.RDSToHikariDataSourceTransformer;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Binder;
import io.bootique.di.Provides;
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient;

import javax.inject.Singleton;

/**
 * @since 3.0
 */
public class AwsSecretsModule implements BQModule {

    public static AwsSecretsManagerExtender extend(Binder binder) {
        return new AwsSecretsManagerExtender(binder);
    }

    private static final String CONFIG_PREFIX = "awssecrets";

    @Override
    public ModuleCrate crate() {
        return ModuleCrate.of(this)
                .description("Provides integration with AWS Secrets client v2.")
                .config(CONFIG_PREFIX, AwsSecretsFactory.class)
                .build();
    }

    @Override
    public void configure(Binder binder) {

        AwsSecretsModule.extend(binder)
                .initAllExtensions()
                .addTransformer("rds-to-hikari-datasource", RDSToHikariDataSourceTransformer.class);

        BQCoreModule.extend(binder).addConfigLoader(AwsSecretsConfigurationLoader.class);
    }

    @Singleton
    @Provides
    SecretsManagerClient provideSecretsManager(ConfigurationFactory configFactory, AwsConfig config) {
        return configFactory.config(AwsSecretsFactory.class, CONFIG_PREFIX).createSecretsManager(config);
    }
}
