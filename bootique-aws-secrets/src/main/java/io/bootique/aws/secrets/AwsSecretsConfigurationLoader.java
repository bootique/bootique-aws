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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bootique.aws.AwsConfig;
import io.bootique.aws.AwsConfigFactory;
import io.bootique.aws.credentials.OrderedCredentialsProvider;
import io.bootique.config.ConfigurationFactory;
import io.bootique.config.jackson.JsonConfigurationFactory;
import io.bootique.config.jackson.JsonConfigurationLoader;
import io.bootique.config.jackson.PropertiesConfigurationLoader;
import io.bootique.jackson.JacksonService;
import io.bootique.log.BootLogger;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;

/**
 * @since 2.0.B1
 */
public class AwsSecretsConfigurationLoader implements JsonConfigurationLoader {

    // this ordering places us at the end of the standard loader chain
    public static int ORDER = PropertiesConfigurationLoader.ORDER + 100;

    private final JacksonService jackson;
    private final BootLogger bootLogger;
    private final Set<OrderedCredentialsProvider> credentialsProviders;
    private final Map<String, AwsJsonTransformer> transformers;

    @Inject
    public AwsSecretsConfigurationLoader(
            JacksonService jackson,
            BootLogger bootLogger,
            Set<OrderedCredentialsProvider> credentialsProviders,
            Map<String, AwsJsonTransformer> transformers) {

        this.jackson = jackson;
        this.bootLogger = bootLogger;
        this.credentialsProviders = credentialsProviders;
        this.transformers = transformers;
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

        ObjectMapper jsonMapper = jackson.newObjectMapper();

        ConfigurationFactory configFactory = new JsonConfigurationFactory(mutableInput, jsonMapper);
        AwsSecretsFactory secretsFactory = configFactory.config(AwsSecretsFactory.class, "awssecrets");

        if (secretsFactory.isEmpty()) {
            return mutableInput;
        }

        AwsConfig config = configFactory.config(AwsConfigFactory.class, "aws").createConfig(credentialsProviders);
        return secretsFactory.updateConfiguration(mutableInput, config, jsonMapper, transformers, bootLogger);
    }
}
