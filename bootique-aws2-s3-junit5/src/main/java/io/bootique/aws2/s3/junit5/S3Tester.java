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
package io.bootique.aws2.s3.junit5;

import io.bootique.BQCoreModule;
import io.bootique.di.BQModule;
import io.bootique.di.Binder;
import io.bootique.junit5.BQTestScope;
import io.bootique.junit5.scope.BQAfterMethodCallback;
import io.bootique.junit5.scope.BQBeforeMethodCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.function.Consumer;

/**
 * A {@link io.bootique.junit5.BQTestTool} that allows to manage test S3 buckets within a unit test. Only works with
 * a single test {@link io.bootique.BQRuntime}.
 *
 * @since 3.0.M1
 */
public class S3Tester implements BQBeforeMethodCallback, BQAfterMethodCallback {

    private final S3TesterLifecycleManager lifecycleManager;

    public static S3Tester create() {
        return new S3Tester();
    }

    protected S3Tester() {
        this.lifecycleManager = new S3TesterLifecycleManager();
    }

    @Override
    public void beforeMethod(BQTestScope scope, ExtensionContext context) {
        lifecycleManager.beforeMethod();
    }

    @Override
    public void afterMethod(BQTestScope scope, ExtensionContext context) {
        lifecycleManager.afterMethod();
    }

    void onS3ClientInit(S3Client client) {
        lifecycleManager.onS3ClientInit(client);
    }

    /**
     * Returns a Bootique module that can be used to configure a local S3 service to be used by the test
     * {@link io.bootique.BQRuntime}. This method can be used to initialize one or more BQRuntimes in a test class,
     * so that they can share emulated S3 buckets.
     */
    public BQModule moduleWithTestS3() {
        return this::configure;
    }

    public S3Tester createBuckets(String... bucketNames) {
        lifecycleManager.createBuckets(bucketNames);
        return this;
    }

    public S3Tester runAfterBucketsCreated(Consumer<S3Client> callback) {
        lifecycleManager.runAfterBucketsCreated(callback);
        return this;
    }

    public S3Tester runBeforeEachTest(Consumer<S3Client> callback) {
        lifecycleManager.runBeforeEachTest(callback);
        return this;
    }

    private void configure(Binder binder) {
        binder.bind(S3Tester.class).toInstance(this);
        BQCoreModule.extend(binder).setProperty("bq.awss3.type", "awss3test");
    }
}
