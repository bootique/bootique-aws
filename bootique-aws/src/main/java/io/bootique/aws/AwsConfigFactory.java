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

package io.bootique.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.aws.credentials.OrderedCredentialsProvider;

import java.util.Comparator;
import java.util.Set;

@BQConfig
public class AwsConfigFactory {

    private String accessKey;
    private String secretKey;
    private String defaultRegion;

    @BQConfigProperty("Sets AWS account credentials 'accessKey'")
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @BQConfigProperty("AWS account credentials 'secretKey'")
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @BQConfigProperty("Optional default region to use for AWS calls. Ignored if 'serviceEndpoint' " +
            "is set (in which case 'signingRegion' property is used to mirror AWS conventions")
    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    public AwsConfig createConfig(Set<OrderedCredentialsProvider> credentialsProviders) {
        return new AwsConfig(createDefaultRegion(), createCredentialsProvider(credentialsProviders));
    }

    protected AWSCredentialsProvider createCredentialsProvider(Set<OrderedCredentialsProvider> altCredentialsProviders) {
        AWSCredentialsProvider provider1 = createBootiqueCredentialsProvider();

        // Bootique configuration of the credentials takes precedence over other preconfigured strategies
        if (provider1 != null) {
            return provider1;
        }

        AWSCredentialsProvider provider2 = createCredentailsProviderChain(altCredentialsProviders);
        if (provider2 != null) {
            return provider2;
        }

        throw new IllegalStateException("No accessKey/secretKey configured, and no alternative credentials providers specified");
    }

    protected AWSCredentialsProvider createBootiqueCredentialsProvider() {

        if (accessKey == null && secretKey == null) {
            return null;
        }

        if (accessKey != null && secretKey != null) {
            return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
        }

        // partial configuration...
        if (accessKey == null) {
            throw new IllegalStateException("'secretKey' is set, but 'accessKey' is not");
        } else {
            throw new IllegalStateException("'accessKey' is set, but 'secretKey' is not");
        }
    }

    protected AWSCredentialsProvider createCredentailsProviderChain(Set<OrderedCredentialsProvider> credentialsProviders) {

        if (credentialsProviders.isEmpty()) {
            return null;
        }

        if (credentialsProviders.size() == 1) {
            return credentialsProviders.iterator().next().getProvider();
        }

        AWSCredentialsProvider[] sorted = credentialsProviders.stream()
                .sorted(Comparator.comparing(OrderedCredentialsProvider::getOrder))
                .map(OrderedCredentialsProvider::getProvider)
                .toArray(AWSCredentialsProvider[]::new);

        return new AWSCredentialsProviderChain(sorted);
    }

    protected Regions createDefaultRegion() {
        return defaultRegion != null ? Regions.fromName(defaultRegion) : null;
    }
}
