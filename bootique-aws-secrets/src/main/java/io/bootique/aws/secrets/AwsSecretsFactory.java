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
import com.amazonaws.services.secretsmanager.AWSSecretsManagerClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.aws.AwsConfig;
import io.bootique.aws.AwsServiceFactory;
import io.bootique.log.BootLogger;

import java.util.Map;

/**
 * The factory is used in two places over the same configuration structure: {@link AwsSecretsConfigurationLoader} to
 * update config, and the {@link AwsSecretsModule} to create injectable AWSSecretsManager.
 *
 * @since 2.0
 * @deprecated in favor of AWS v2 API
 */
@Deprecated(since = "3.0", forRemoval = true)
@BQConfig
public class AwsSecretsFactory extends AwsServiceFactory {

    private Map<String, AwsSecretFactory> secrets;

    @BQConfigProperty("A map of AWS secrets that must be loaded and merged into the app configuration. " +
            "Keys in the map are purely symbolic unique names not tied to the AWS naming. " +
            "They are useful for referencing specific secrets when overriding configuration")
    public void setSecrets(Map<String, AwsSecretFactory> secrets) {
        this.secrets = secrets;
    }

    public boolean isEmpty() {
        return secrets == null || secrets.isEmpty();
    }

    public AWSSecretsManager createSecretsManager(AwsConfig config) {
        return configure(AWSSecretsManagerClientBuilder.standard(), config).build();
    }

    public JsonNode updateConfiguration(
            JsonNode mutableInput,
            AwsConfig config,
            ObjectMapper jsonMapper,
            Map<String, AwsJsonTransformer> transformers,
            BootLogger bootLogger) {

        if (!isEmpty()) {
            // note that we are not using singleton AWSSecretsManager. Since "updateConfiguration" is intended to be
            // called from JsonConfigurationLoader, it needs to create most of its AWS machinery right on the spot.
            AWSSecretsManager secretsManager = createSecretsManager(config);

            // ignoring secret key names... They are used purely for reference when overriding configuration.
            for (AwsSecretFactory configFactory : secrets.values()) {
                mutableInput = configFactory.updateConfiguration(bootLogger, secretsManager, jsonMapper, transformers, mutableInput);
            }
        }

        return mutableInput;
    }
}
