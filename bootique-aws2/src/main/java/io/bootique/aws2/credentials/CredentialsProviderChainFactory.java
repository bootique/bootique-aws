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
package io.bootique.aws2.credentials;

import io.bootique.di.Injector;
import io.bootique.di.Key;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProviderChain;

import java.util.Comparator;
import java.util.Set;

// not exposing this strategy in YAML. Only used internally as a fallback..
public class CredentialsProviderChainFactory implements AwsCredentialsProviderFactory {

    @Override
    public AwsCredentialsProvider create(Injector injector) {
        Set<OrderedCredentialsProvider> diProviders = injector.getInstance(Key.getSetOf(OrderedCredentialsProvider.class));

        AwsCredentialsProvider provider = createCredentialsProviderChain(diProviders);
        if (provider == null) {
            throw new IllegalStateException(
                    "No credentials providers available via injection, " +
                            "and no explicit configuration was provided (profile or access/secret key");
        }

        return provider;
    }

    protected AwsCredentialsProvider createCredentialsProviderChain(Set<OrderedCredentialsProvider> diProviders) {

        if (diProviders.isEmpty()) {
            return null;
        }

        if (diProviders.size() == 1) {
            return diProviders.iterator().next().getProvider();
        }

        AwsCredentialsProvider[] sorted = diProviders.stream()
                .sorted(Comparator.comparing(OrderedCredentialsProvider::getOrder))
                .map(OrderedCredentialsProvider::getProvider)
                .toArray(AwsCredentialsProvider[]::new);

        return AwsCredentialsProviderChain.of(sorted);
    }
}
