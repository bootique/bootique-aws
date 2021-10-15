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

import io.bootique.ModuleExtender;
import io.bootique.aws2.credentials.OrderedCredentialsProvider;
import io.bootique.di.Binder;
import io.bootique.di.SetBuilder;
import software.amazon.awssdk.auth.credentials.*;

/**
 * @since 2.0.B1
 */
public class AwsModuleExtender extends ModuleExtender<AwsModuleExtender> {

    public static final int DEFAULT_CREDENTIALS_PROVIDER_ORDER = 10;
    public static final int LAMBDA_CREDENTIALS_PROVIDER_ORDER = DEFAULT_CREDENTIALS_PROVIDER_ORDER + 10;
    public static final int INSTANCE_CREDENTIALS_PROVIDER_ORDER = LAMBDA_CREDENTIALS_PROVIDER_ORDER + 10;
    public static final int CONTAINER_CREDENTIALS_PROVIDER_ORDER = INSTANCE_CREDENTIALS_PROVIDER_ORDER + 10;

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
        return addCredentialsProvider(DefaultCredentialsProvider.create(), DEFAULT_CREDENTIALS_PROVIDER_ORDER);
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
     * Registers a credentials provider that looks up credentials via EC2 instance metadata service.
     *
     * @see #INSTANCE_CREDENTIALS_PROVIDER_ORDER
     */
    public AwsModuleExtender addInstanceCredentialsProvider() {
        return addInstanceCredentialsProvider(INSTANCE_CREDENTIALS_PROVIDER_ORDER);
    }

    /**
     * Registers a credentials provider that looks up credentials in AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" or
     * "AWS_CONTAINER_CREDENTIALS_FULL_URI" variables in the container's (ECS) environment.
     *
     * @see #CONTAINER_CREDENTIALS_PROVIDER_ORDER
     */
    public AwsModuleExtender addContainerCredentialsProvider() {
        return addContainerCredentialsProvider(CONTAINER_CREDENTIALS_PROVIDER_ORDER);
    }

    /**
     * Registers a credentials provider, that looks up credentials using standard AWS env variables: AWS_ACCESS_KEY_ID,
     * AWS_ACCESS_KEY, AWS_SECRET_KEY, AWS_SECRET_ACCESS_KEY.
     */
    public AwsModuleExtender addEnvVarsCredentialsProvider(int order) {
        return addCredentialsProvider(EnvironmentVariableCredentialsProvider.create(), order);
    }

    public AwsModuleExtender addProfileCredentialsProvider(int order) {
        return addCredentialsProvider(ProfileCredentialsProvider.create(), order);
    }

    public AwsModuleExtender addSystemPropertiesCredentialsProvider(int order) {
        return addCredentialsProvider(SystemPropertyCredentialsProvider.create(), order);
    }

    /**
     * Registers a credentials provider that looks up credentials via EC2 instance metadata service.
     */
    public AwsModuleExtender addInstanceCredentialsProvider(int order) {
        return addCredentialsProvider(InstanceProfileCredentialsProvider.create(), order);
    }

    /**
     * Registers a credentials provider that looks up credentials in AWS_CONTAINER_CREDENTIALS_RELATIVE_URI" or
     * "AWS_CONTAINER_CREDENTIALS_FULL_URI" variables in the container's (ECS) environment.
     */
    public AwsModuleExtender addContainerCredentialsProvider(int order) {
        return addCredentialsProvider(ContainerCredentialsProvider.builder().build(), order);
    }

    public AwsModuleExtender addCredentialsProvider(AwsCredentialsProvider provider, int order) {
        contributeOrderedCredentialsProviders().addInstance(new OrderedCredentialsProvider(provider, order));
        return this;
    }

    protected SetBuilder<OrderedCredentialsProvider> contributeOrderedCredentialsProviders() {
        return orderedCredentialsProviders != null ? orderedCredentialsProviders : (orderedCredentialsProviders = newSet(OrderedCredentialsProvider.class));
    }
}
