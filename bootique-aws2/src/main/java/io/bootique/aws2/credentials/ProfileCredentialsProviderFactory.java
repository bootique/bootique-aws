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

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.di.Injector;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.ProfileCredentialsProvider;

/**
 * @since 3.0
 */
@JsonTypeName("profile")
@BQConfig("Configures credentials from the AWS profile stored in ~/.aws/credentials. " +
        "If no profile name is specified, looks for the profile with the name 'default'.")
public class ProfileCredentialsProviderFactory implements AwsCredentialsProviderFactory {

    private static final String DEFAULT_PROFILE_NAME = "default";

    private String profile;

    @BQConfigProperty("Sets the name of the local AWS profile used to resolve credentials")
    public void setProfile(String profile) {
        this.profile = profile;
    }

    @Override
    public AwsCredentialsProvider create(Injector injector) {
        String profile = this.profile != null ? this.profile : DEFAULT_PROFILE_NAME;
        return ProfileCredentialsProvider.create(profile);
    }
}
