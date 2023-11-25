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
import com.amazonaws.regions.Regions;

import java.util.Objects;
import java.util.Optional;

/**
 * @deprecated in favor of AWS v2 API
 */
@Deprecated(since = "3.0", forRemoval = true)
public class AwsConfig {

    private final Regions defaultRegion;
    private final AWSCredentialsProvider credentialsProvider;

    public AwsConfig(
            Regions defaultRegion,
            AWSCredentialsProvider credentialsProvider) {

        this.defaultRegion = defaultRegion;
        this.credentialsProvider = Objects.requireNonNull(credentialsProvider);
    }

    /**
     * @since 2.0
     */
    public AWSCredentialsProvider getCredentialsProvider() {
        return credentialsProvider;
    }

    public Optional<Regions> getDefaultRegion() {
        return Optional.ofNullable(defaultRegion);
    }
}
