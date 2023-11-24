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
import com.amazonaws.services.secretsmanager.model.GetSecretValueRequest;
import com.amazonaws.services.secretsmanager.model.GetSecretValueResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeType;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.config.jackson.merger.InPlacePropertiesMerger;
import io.bootique.log.BootLogger;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;

/**
 * AWS SecretsManager secret ARN or name.
 *
 * @since 2.0
 * @deprecated in favor of AWS v2 API
 */
@Deprecated(since = "3.0", forRemoval = true)
@BQConfig
public class AwsSecretFactory {

    private String awsName;
    private String mergePath;
    private String jsonTransformer;

    @BQConfigProperty("Secret ARN or name within AWS SecretsManager")
    public AwsSecretFactory setAwsName(String awsName) {
        this.awsName = awsName;
        return this;
    }

    @BQConfigProperty("App configuration path to merge secret values to. E.g. 'jdbc.mydb'")
    public AwsSecretFactory setMergePath(String mergePath) {
        this.mergePath = mergePath;
        return this;
    }

    @BQConfigProperty("A optional name of a registered transformer to process JSON coming from AWS Secrets " +
            "Manager to a format appropriate for the target Bootique config")
    public AwsSecretFactory setJsonTransformer(String jsonTransformer) {
        this.jsonTransformer = jsonTransformer;
        return this;
    }

    public JsonNode updateConfiguration(
            BootLogger bootLogger,
            AWSSecretsManager secretsManager,
            ObjectMapper jsonMapper,
            Map<String, AwsJsonTransformer> transformers,
            JsonNode mutableInput) {

        bootLogger.trace(() -> "Loading config for path '" + mergePath + "' and AWS secret '" + awsName + "'");

        String secret = readSecret(secretsManager);
        JsonNode parsed = parseSecret(jsonMapper, secret);
        JsonNode normalized = normalizeSecret(transformers, parsed);
        return mergeSecret(mutableInput, normalized);
    }

    protected String readSecret(AWSSecretsManager secretsManager) {
        Objects.requireNonNull(awsName, "No secret 'awsName' specified. Must be either a name or an ARN");
        GetSecretValueRequest request = new GetSecretValueRequest().withSecretId(awsName);

        // TODO: handle AWS exceptions... E.g. a user-friendly message for ResourceNotFoundException?
        GetSecretValueResult response = secretsManager.getSecretValue(request);
        return response.getSecretString();
    }

    protected JsonNode parseSecret(ObjectMapper jsonMapper, String secret) {
        JsonNode parsed;
        try {
            parsed = jsonMapper.readTree(secret);
        } catch (IOException e) {
            throw new RuntimeException("Error parsing AWS secret '" + awsName + "' to JSON", e);
        }

        if (parsed.getNodeType() != JsonNodeType.OBJECT) {
            throw new RuntimeException("AWS secret '" + awsName + "' is not a JSON object: " + parsed.getNodeType());
        }

        return parsed;
    }

    protected JsonNode normalizeSecret(Map<String, AwsJsonTransformer> transformers, JsonNode secret) {
        if (jsonTransformer == null) {
            return secret;
        }

        AwsJsonTransformer transformer = transformers.get(jsonTransformer);
        if (transformer == null) {
            throw new RuntimeException("Unknown or invalid 'jsonTransformer': "
                    + jsonTransformer
                    + ". Known transformers: "
                    + transformers.keySet());
        }

        return transformer.fromSecret(secret);
    }

    protected JsonNode mergeSecret(JsonNode mutableInput, JsonNode secret) {

        // secret is a 1-level deep JSON map. We can represent it as a property map and use InPlacePropertiesMerger
        String prefix = mergePath != null && mergePath.length() > 0 ? mergePath + "." : "";
        Map<String, String> properties = new HashMap<>();
        Iterator<Map.Entry<String, JsonNode>> it = secret.fields();
        while (it.hasNext()) {
            Map.Entry<String, JsonNode> f = it.next();
            properties.put(prefix + f.getKey(), f.getValue().asText());
        }

        // TODO: This is suboptimal. Since the base path is shared by all properties, we can optimize merging by
        //  finding the target sub-node, and storing the values there directly
        return new InPlacePropertiesMerger(properties).apply(mutableInput);
    }
}
