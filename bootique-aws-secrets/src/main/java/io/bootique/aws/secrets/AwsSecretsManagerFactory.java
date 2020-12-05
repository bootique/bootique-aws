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
import io.bootique.aws.AwsConfig;

/**
 * @since 2.0.B1
 */
public class AwsSecretsManagerFactory {

    public AWSSecretsManager create(AwsConfig config) {
        AWSSecretsManagerClientBuilder builder = AWSSecretsManagerClientBuilder.standard()
                // while SecretsManager generally does not require credentials, with access granted based on the
                // caller location, tests with Localstack fail without credentials. So have to set them here...
                .withCredentials(config.getCredentialsProvider());

        config.getDefaultRegion().ifPresent(builder::withRegion);
        config.getEndpointConfiguration().ifPresent(builder::withEndpointConfiguration);

        return builder.build();
    }
}
