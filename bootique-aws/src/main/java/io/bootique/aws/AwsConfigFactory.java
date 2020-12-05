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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.regions.Regions;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

@BQConfig
public class AwsConfigFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(AwsConfigFactory.class);

    private String accessKey;
    private String secretKey;
    private String defaultRegion;
    private String serviceEndpoint;
    private String signingRegion;

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

    /**
     * @since 2.0.B1
     */
    @BQConfigProperty("Optional alternative service endpoint. Useful local tests.")
    public void setServiceEndpoint(String serviceEndpoint) {
        this.serviceEndpoint = serviceEndpoint;
    }

    /**
     * @since 2.0.B1
     */
    @BQConfigProperty("Optional signing region to use with alternative service endpoint. Ignored if 'serviceEndpoint' is not set")
    public void setSigningRegion(String signingRegion) {
        this.signingRegion = signingRegion;
    }

    public AwsConfig createConfig() {
        return new AwsConfig(createDefaultRegion(), createEndpointConfig(), createCredentialsProvider());
    }

    protected AWSCredentialsProvider createCredentialsProvider() {
        AWSCredentials credentials = createCredentials();
        return new AWSStaticCredentialsProvider(credentials);
    }

    protected AWSCredentials createCredentials() {
        Objects.requireNonNull(accessKey, "'accessKey' is null");
        Objects.requireNonNull(secretKey, "'secretKey' is null");

        return new BasicAWSCredentials(accessKey, secretKey);
    }

    protected AwsClientBuilder.EndpointConfiguration createEndpointConfig() {
        return serviceEndpoint != null ? new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, signingRegion) : null;
    }

    protected Regions createDefaultRegion() {

        // It appears AwsClientBuilder can take either a region or an endpoint configuration (or nothing at all),
        // but not both.

        if (serviceEndpoint != null) {

            if (defaultRegion != null) {
                LOGGER.warn("Ignoring 'aws.defaultRegion' property, as 'aws.serviceEndpoint' is set");
            }

            return null;
        }

        return defaultRegion != null ? Regions.fromName(defaultRegion) : null;
    }
}
