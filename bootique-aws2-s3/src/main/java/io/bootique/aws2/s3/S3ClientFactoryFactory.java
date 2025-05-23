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

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.aws2.AwsConfig;
import io.bootique.config.PolymorphicConfiguration;

import jakarta.inject.Inject;
import java.net.URI;

@BQConfig
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", defaultImpl = S3ClientFactoryFactory.class)
@JsonTypeName("default")
public class S3ClientFactoryFactory implements PolymorphicConfiguration {

    private final AwsConfig config;
    private URI endpointOverride;

    @Inject
    public S3ClientFactoryFactory(AwsConfig config) {
        this.config = config;
    }

    public S3ClientFactory create() {
        return new S3ClientFactory(config, endpointOverride);
    }

    @BQConfigProperty("Specific service endpoint, overriding the default endpoint derived from the configuration region. Useful local tests.")
    public S3ClientFactoryFactory setEndpointOverride(URI endpointOverride) {
        this.endpointOverride = endpointOverride;
        return this;
    }
}
