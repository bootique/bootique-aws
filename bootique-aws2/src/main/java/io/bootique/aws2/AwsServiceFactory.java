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
package io.bootique.aws2;

import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import software.amazon.awssdk.awscore.client.builder.AwsClientBuilder;

import java.net.URI;

/**
 * A common superclass of factories for various AWS services such as S3, etc. Used by other "bootique-aws-*" modules or
 * custom modules that require to map some AWS service.
 *
 * @since 3.0
 */
@BQConfig
public abstract class AwsServiceFactory {

    private URI endpointOverride;

    @BQConfigProperty("Specific service endpoint, overriding the default endpoint derived from the configuration region. Useful local tests.")
    public void setEndpointOverride(URI endpointOverride) {
        this.endpointOverride = endpointOverride;
    }

    /**
     * Configures common parts of each AWS client such as the default region and custom service endpoint.
     */
    protected <Builder extends AwsClientBuilder<Builder, Service>, Service> Builder configure(Builder builder, AwsConfig config) {
        builder.credentialsProvider(config.getCredentialsProvider());

        // use service-specific endpoint config if set explicitly, otherwise use region from the common config
        if (endpointOverride != null) {
            builder.endpointOverride(endpointOverride);
        }

        config.getDefaultRegion().ifPresent(builder::region);
        return builder;
    }
}
