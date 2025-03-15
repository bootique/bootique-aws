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
package io.bootique.aws.secrets.transformer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.bootique.aws.secrets.AwsJsonTransformer;
import io.bootique.log.BootLogger;

import jakarta.inject.Inject;

/**
 * @since 2.0
 * @deprecated in favor of AWS v2 API
 */
@Deprecated(since = "3.0", forRemoval = true)
public class RDSToHikariDataSourceTransformer implements AwsJsonTransformer {

    private final BootLogger bootLogger;

    @Inject
    public RDSToHikariDataSourceTransformer(BootLogger bootLogger) {
        this.bootLogger = bootLogger;
    }

    @Override
    public JsonNode fromSecret(JsonNode rdsSecret) {

        ObjectNode hikariConfig = new ObjectNode(new JsonNodeFactory(true));

        JsonNode username = rdsSecret.get("username");
        if (username != null) {
            hikariConfig.put("username", username.asText());
        }

        JsonNode password = rdsSecret.get("password");
        if (password != null) {
            hikariConfig.put("password", password.asText());
        }

        String jdbcUrl = toJdbcUrl(rdsSecret);
        if (jdbcUrl != null) {
            hikariConfig.put("jdbcUrl", jdbcUrl);
        }

        return hikariConfig;
    }

    protected String toJdbcUrl(JsonNode rdsSecret) {

        // generate a URL like jdbc:engine://host:port/dbname
        // Require at least "host" and "engine" keys for the valid URL

        JsonNode engine = rdsSecret.get("engine");
        if (engine == null) {
            bootLogger.stderr("AWS RDS secret must have 'engine' specified");
            return null;
        }

        JsonNode host = rdsSecret.get("host");
        if (host == null) {
            bootLogger.stderr("AWS RDS secret must have 'host' specified");
            return null;
        }

        StringBuilder url = new StringBuilder()
                .append("jdbc:")
                .append(engine.asText())
                .append("://")
                .append(host.asText());

        JsonNode port = rdsSecret.get("port");
        if (port != null) {
            url.append(":").append(port.asText());
        }

        JsonNode db = rdsSecret.get("dbname");
        if (db != null) {
            url.append("/").append(db.asText());
        }

        return url.toString();
    }
}
