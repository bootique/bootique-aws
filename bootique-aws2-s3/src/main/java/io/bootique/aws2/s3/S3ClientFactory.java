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
package io.bootique.aws2.s3;

import io.bootique.aws2.AwsConfig;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3ClientBuilder;

import java.net.URI;
import java.util.Objects;

/**
 * An injectable object that allows to create {@link S3Client} instances with minimal configuration.
 *
 * @since 3.0
 */
public class S3ClientFactory {

    private final AwsConfig config;
    private final URI endpointOverride;

    public S3ClientFactory(AwsConfig config, URI endpointOverride) {
        this.config = config;
        this.endpointOverride = endpointOverride;
    }

    public S3Client newClient() {
        return newBuilder().build();
    }

    public Builder newBuilder() {
        return new Builder(config, endpointOverride);
    }

    public static class Builder {

        private final AwsConfig config;
        private Region region;
        private URI endpointOverride;

        protected Builder(AwsConfig config, URI endpointOverride) {
            this.config = config;
            this.endpointOverride = endpointOverride;
        }

        public Builder endpointOverride(String endpointOverride) {
            Objects.requireNonNull(endpointOverride);
            return endpointOverride(URI.create(endpointOverride));
        }

        public Builder endpointOverride(URI endpointOverride) {
            this.endpointOverride = Objects.requireNonNull(endpointOverride);
            return this;
        }

        public Builder region(String region) {
            return region(Region.of(region));
        }

        public Builder region(Region region) {
            this.region = Objects.requireNonNull(region);
            return this;
        }

        public S3Client build() {
            S3ClientBuilder awsBuilder = S3Client.builder();

            awsBuilder.credentialsProvider(config.getCredentialsProvider());

            // use service-specific endpoint config if set explicitly, otherwise use region from the common config
            if (endpointOverride != null) {
                awsBuilder.endpointOverride(endpointOverride);
            }

            Region region = this.region != null ? this.region : config.getDefaultRegion().orElse(null);
            if (region != null) {
                awsBuilder.region(region);
            }

            return awsBuilder.build();
        }
    }
}
