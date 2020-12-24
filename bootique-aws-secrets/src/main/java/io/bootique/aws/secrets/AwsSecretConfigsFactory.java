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
import com.fasterxml.jackson.databind.ObjectMapper;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.log.BootLogger;

import java.util.List;

/**
 * @since 2.0.B1
 */
@BQConfig
public class AwsSecretConfigsFactory {

    private List<AwsSecretConfigFactory> secrets;

    @BQConfigProperty("A list of AWS secrets that must be loaded and merged into the app configuration")
    public void setSecrets(List<AwsSecretConfigFactory> secrets) {
        this.secrets = secrets;
    }

    public boolean isEmpty() {
        return secrets == null || secrets.isEmpty();
    }

    public JsonNode updateConfiguration(
            BootLogger bootLogger,
            AWSSecretsManager secretsManager,
            ObjectMapper jsonMapper,
            JsonNode mutableInput) {

        if (!isEmpty()) {
            for (AwsSecretConfigFactory configFactory : secrets) {
                mutableInput = configFactory.updateConfiguration(bootLogger, secretsManager, jsonMapper, mutableInput);
            }
        }

        return mutableInput;
    }
}
