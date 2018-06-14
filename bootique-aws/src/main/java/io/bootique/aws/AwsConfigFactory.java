/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;

import java.util.Objects;
import java.util.Optional;

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

    @BQConfigProperty("Optional default region to use for AWS calls.")
    public void setDefaultRegion(String defaultRegion) {
        this.defaultRegion = defaultRegion;
    }

    public AwsConfig createConfig() {
        Optional<Regions> region = Optional.ofNullable(defaultRegion).map(Regions::fromName);
        return new AwsConfig(region);
    }

    public AWSCredentialsProvider createCredentialsProvider() {
        AWSCredentials credentials = createCredentials();
        return new AWSStaticCredentialsProvider(credentials);
    }

    protected AWSCredentials createCredentials() {
        Objects.requireNonNull(accessKey, "'accessKey' is null");
        Objects.requireNonNull(secretKey, "'secretKey' is null");

        return new BasicAWSCredentials(accessKey, secretKey);
    }

}
