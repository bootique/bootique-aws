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

import io.bootique.aws2.s3.S3ClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * @since 3.0.M1
 */
class S3TesterLifecycleManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(S3TesterLifecycleManager.class);

    private final AtomicBoolean attachedToRuntime;
    private List<String> bucketNames;
    private final List<Consumer<S3ClientFactory>> afterBucketsCreated;
    private final List<Consumer<S3ClientFactory>> beforeEachTest;

    private S3ClientFactory clientFactory;
    private boolean withinTestMethod;

    S3TesterLifecycleManager() {
        this.attachedToRuntime = new AtomicBoolean(false);
        this.beforeEachTest = new ArrayList<>();
        this.afterBucketsCreated = new ArrayList<>();
        this.bucketNames = new ArrayList<>();
    }

    void beforeMethod() {

        this.withinTestMethod = true;

        if (isStarted()) {
            beforeEachTest.forEach(c -> c.accept(clientFactory));
        }
    }

    void afterMethod() {
        this.withinTestMethod = false;
    }

    void onS3ClientFactoryInit(S3ClientFactory clientFactory) {
        checkUnused();
        this.clientFactory = clientFactory;

        if (!bucketNames.isEmpty()) {
            S3Client client = clientFactory.newClient();
            bucketNames.forEach(n -> execCreateBucket(client, n));
        }
        afterBucketsCreated.forEach(c -> c.accept(clientFactory));

        // run before callbacks that where skipped previously because the manager was not initialized
        if (withinTestMethod) {
            beforeEachTest.forEach(c -> c.accept(clientFactory));
        }
    }

    private void execCreateBucket(S3Client client, String bucketName) {
        LOGGER.debug("Creating test bucket {}", bucketName);
        client.createBucket(b -> b.bucket(bucketName));
    }

    void createBuckets(String... bucketNames) {
        for (String n : bucketNames) {
            this.bucketNames.add(n);
        }
    }

    void runAfterBucketsCreated(Consumer<S3ClientFactory> callback) {
        afterBucketsCreated.add(callback);
    }

    void runBeforeEachTest(Consumer<S3ClientFactory> callback) {
        beforeEachTest.add(callback);
    }

    private boolean isStarted() {
        return clientFactory != null;
    }

    private void checkUnused() {
        if (!attachedToRuntime.compareAndSet(false, true)) {
            throw new IllegalStateException("S3Tester is already connected to another BQRuntime. " +
                    "To fix this error use one S3Tester per BQRuntime.");
        }
    }
}
