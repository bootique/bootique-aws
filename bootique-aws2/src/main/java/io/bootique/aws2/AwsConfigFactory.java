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
import io.bootique.aws2.credentials.AwsCredentialsProviderFactory;
import io.bootique.aws2.credentials.CredentialsProviderChainFactory;
import io.bootique.di.Injector;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.regions.Region;


@BQConfig
public class AwsConfigFactory {

    private String defaultRegion;
    private AwsCredentialsProviderFactory credentials;

    @BQConfigProperty("Optional default region to use for AWS calls. Ignored if 'serviceEndpoint' " +
            "is set (in which case 'signingRegion' property is used to mirror AWS conventions")
    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    @BQConfigProperty
    public void setCredentials(AwsCredentialsProviderFactory credentials) {
        this.credentials = credentials;
    }

    public AwsConfig createConfig(Injector injector) {
        return new AwsConfig(createDefaultRegion(), createCredentialsProvider(injector));
    }

    protected AwsCredentialsProvider createCredentialsProvider(Injector injector) {

        AwsCredentialsProviderFactory factory = this.credentials != null
                ? this.credentials
                : new CredentialsProviderChainFactory();

        return factory.create(injector);
    }

    protected Region createDefaultRegion() {
        return defaultRegion != null ? Region.of(defaultRegion) : null;
    }
}
