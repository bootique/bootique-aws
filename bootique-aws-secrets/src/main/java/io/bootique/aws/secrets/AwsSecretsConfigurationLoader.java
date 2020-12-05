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
import com.fasterxml.jackson.databind.JsonNode;
import io.bootique.aws.AwsConfig;
import io.bootique.aws.AwsConfigFactory;
import io.bootique.config.ConfigurationFactory;
import io.bootique.config.jackson.JsonConfigurationFactory;
import io.bootique.config.jackson.JsonConfigurationLoader;
import io.bootique.config.jackson.PropertiesConfigurationLoader;
import io.bootique.jackson.JacksonService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * @since 2.0.B1
 */
public class AwsSecretsConfigurationLoader implements JsonConfigurationLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsSecretsConfigurationLoader.class);

    // this ordering places us at the end of the standard loader chain
    public static int ORDER = PropertiesConfigurationLoader.ORDER + 100;

    private final JacksonService jackson;

    @Inject
    public AwsSecretsConfigurationLoader(JacksonService jackson) {
        this.jackson = jackson;
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public JsonNode updateConfiguration(JsonNode mutableInput) {

        // We can't rely on injectable ConfigurationFactory to initialize the loader, as the loader itself is used for
        // ConfigurationFactory bootstrap. So instead we'll do our own assembly using the same factories that produce
        // injectable singletons.

        ConfigurationFactory configFactory = new JsonConfigurationFactory(mutableInput, jackson.newObjectMapper());
        Map<String, SecretId> secretIds = configFactory.config(AwsSecretConfigsFactory.class, "awssecrets").create();

        if (secretIds.isEmpty()) {
            return mutableInput;
        }

        AwsConfig config = configFactory.config(AwsConfigFactory.class, "aws").createConfig();
        AWSSecretsManager secretsManager = new AwsSecretsManagerFactory().create(config);

        SecretConfigLoader loader = new SecretConfigLoader(secretsManager, mutableInput);
        secretIds.forEach(loader::load);

        return loader.mutableInput;
    }

    static class SecretConfigLoader {

        private final AWSSecretsManager secretsManager;
        private JsonNode mutableInput;

        SecretConfigLoader(AWSSecretsManager secretsManager, JsonNode mutableInput) {
            this.secretsManager = secretsManager;
            this.mutableInput = mutableInput;
        }

        void load(String prefix, SecretId secretId) {

        }
    }

}
