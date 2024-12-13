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
package io.bootique.aws2.junit5;

import io.bootique.BQCoreModule;
import io.bootique.BQModule;
import io.bootique.di.Binder;
import io.bootique.junit5.BQTestScope;
import io.bootique.junit5.scope.BQAfterScopeCallback;
import io.bootique.junit5.scope.BQBeforeScopeCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;

import java.net.URI;
import java.util.stream.Stream;

/**
 * A {@link io.bootique.junit5.BQTestTool} that allows to emulate multiple AWS services via Localstack and
 * Testcontainers.
 *
 * @since 3.0
 */
public class AwsTester implements BQBeforeScopeCallback, BQAfterScopeCallback {

    static final String LOCALSTACK_CONTAINER_VERSION = "4.0.3";

    private final LocalStackContainer localstack;

    public static AwsTester aws(AwsService... services) {

        // TODO: dynamically switch to an ARM image?

        DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:" + LOCALSTACK_CONTAINER_VERSION);
        return aws(localstackImage, services);
    }

    public static AwsTester aws(DockerImageName localstackImage, AwsService... services) {

        LocalStackContainer.Service[] tcServices = Stream
                .of(services)
                .map(AwsService::toLocalstackService)
                .toArray(i -> new LocalStackContainer.Service[i]);

        LocalStackContainer container = new LocalStackContainer(localstackImage).withServices(tcServices);

        return new AwsTester(container);
    }

    protected AwsTester(LocalStackContainer localstack) {
        this.localstack = localstack;
    }

    @Override
    public void beforeScope(BQTestScope scope, ExtensionContext context) {
        localstack.start();
    }

    @Override
    public void afterScope(BQTestScope scope, ExtensionContext context) {
        localstack.stop();
    }

    /**
     * Returns a Bootique module that can be used to configure a local AWS services to be used by the test
     * {@link io.bootique.BQRuntime}. This method can be used to initialize one or more BQRuntimes in a test class.
     */
    public BQModule moduleWithTestAws() {
        return this::configure;
    }

    private void configure(Binder binder) {
        BQCoreModule.extend(binder)
                .setProperty("bq.aws.credentials.accessKey", getAccessKey())
                .setProperty("bq.aws.credentials.secretKey", getSecretKey())
                .setProperty("bq.aws.defaultRegion", getRegionName());

        binder.bind(AwsTester.class).toInstance(this);
    }

    public URI getEndpointOverride() {
        // The latest Localstack uses a single port for all services, so we can use any "service" to resolve
        // the end override
        return localstack.getEndpointOverride(LocalStackContainer.Service.EC2);
    }

    public AwsCredentialsProvider getCredentialsProvider() {
        return StaticCredentialsProvider.create(AwsBasicCredentials.create(getAccessKey(), getSecretKey()));
    }

    public Region getRegion() {
        return Region.of(getRegionName());
    }

    public String getRegionName() {
        return localstack.getRegion();
    }

    public String getAccessKey() {
        return localstack.getAccessKey();
    }

    public String getSecretKey() {
        return localstack.getSecretKey();
    }
}
