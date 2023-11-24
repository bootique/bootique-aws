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

import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.ProfileCredentialsProvider;
import io.bootique.ModuleExtender;
import io.bootique.aws.credentials.OrderedCredentialsProvider;
import io.bootique.di.Binder;
import io.bootique.di.SetBuilder;

/**
 * @since 2.0
 * @deprecated in favor of AWS v2 API
 */
@Deprecated(since = "3.0", forRemoval = true)
public class AwsModuleExtender extends ModuleExtender<AwsModuleExtender> {

    public static final int DEFAULT_CREDENTIALS_PROVIDER_ORDER = 10;
    public static final int LAMBDA_CREDENTIALS_PROVIDER_ORDER = DEFAULT_CREDENTIALS_PROVIDER_ORDER + 10;
    public static final int EC2_CONTAINER_CREDENTIALS_PROVIDER_ORDER = LAMBDA_CREDENTIALS_PROVIDER_ORDER + 10;

    private SetBuilder<OrderedCredentialsProvider> orderedCredentialsProviders;

    public AwsModuleExtender(Binder binder) {
        super(binder);
    }

    @Override
    public AwsModuleExtender initAllExtensions() {
        contributeOrderedCredentialsProviders();
        return this;
    }

    public AwsModuleExtender addAwsCredentialsProviderChain() {
        return addCredentialsProvider(DefaultAWSCredentialsProviderChain.getInstance(), DEFAULT_CREDENTIALS_PROVIDER_ORDER);
    }

    /**
     * Registers a credentials provider that looks up credentials using env variables, that are present in the AWS
     * Lambda environment. See this AWS
     * <a href="https://docs.aws.amazon.com/lambda/latest/dg/configuration-envvars.html">documentation page</a>.
     *
     * @see #LAMBDA_CREDENTIALS_PROVIDER_ORDER
     */
    public AwsModuleExtender addLambdaCredentialsProvider() {
        return addEnvVarsCredentialsProvider(LAMBDA_CREDENTIALS_PROVIDER_ORDER);
    }

    /**
     * Registers a credentials provider that looks up credentials EC2 container metadata serice.
     *
     * @see #EC2_CONTAINER_CREDENTIALS_PROVIDER_ORDER
     */
    public AwsModuleExtender addEC2ContainerCredentialsProvider() {
        return addEC2ContainerCredentialsProvider(EC2_CONTAINER_CREDENTIALS_PROVIDER_ORDER);
    }

    /**
     * Registers a credentials provider, that looks up credentials using standard AWS env variables: AWS_ACCESS_KEY_ID,
     * AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_SECRET_ACCESS_KEY.
     */
    public AwsModuleExtender addEnvVarsCredentialsProvider(int order) {
        return addCredentialsProvider(new EnvironmentVariableCredentialsProvider(), order);
    }

    public AwsModuleExtender addProfileCredentialsProvider(int order) {
        return addCredentialsProvider(new ProfileCredentialsProvider(), order);
    }

    public AwsModuleExtender addWebIdentityTokenCredentialsProvider(int order) {
        return addCredentialsProvider(WebIdentityTokenCredentialsProvider.create(), order);
    }

    public AwsModuleExtender addSystemPropertiesCredentialsProvider(int order) {
        return addCredentialsProvider(new SystemPropertiesCredentialsProvider(), order);
    }

    public AwsModuleExtender addEC2ContainerCredentialsProvider(int order) {
        return addCredentialsProvider(new EC2ContainerCredentialsProviderWrapper(), order);
    }

    public AwsModuleExtender addCredentialsProvider(AWSCredentialsProvider provider, int order) {
        contributeOrderedCredentialsProviders().addInstance(new OrderedCredentialsProvider(provider, order));
        return this;
    }

    protected SetBuilder<OrderedCredentialsProvider> contributeOrderedCredentialsProviders() {
        return orderedCredentialsProviders != null ? orderedCredentialsProviders : (orderedCredentialsProviders = newSet(OrderedCredentialsProvider.class));
    }
}
