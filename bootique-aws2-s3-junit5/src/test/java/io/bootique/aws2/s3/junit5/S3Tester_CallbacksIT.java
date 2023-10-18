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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.aws2.s3.S3ClientFactory;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
@Disabled("Until #23 is fixed")
public class S3Tester_CallbacksIT extends BaseAwsTest {

    @BQTestTool
    static final S3Tester s3 = S3Tester
            .create()
            .createBuckets("bucket")
            .runAfterBucketsCreated(S3Tester_CallbacksIT::populateOnStart)
            .runBeforeEachTest(S3Tester_CallbacksIT::populateBeforeMethod);

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(aws.moduleWithTestAws())
            .module(s3.moduleWithTestS3())
            .createRuntime();

    static void populateOnStart(S3ClientFactory factory) {
        factory.client().putObject(
                b -> b.bucket("bucket").key("onstart"),
                RequestBody.fromBytes("onstart".getBytes()));
    }

    static void populateBeforeMethod(S3ClientFactory factory) {
        factory.client().putObject(
                b -> b.bucket("bucket").key("beforeMethod"),
                RequestBody.fromBytes("beforeMethod".getBytes()));
    }
    @BeforeAll
    static void checkOnStart() throws IOException {

        S3Client s3 = app.getInstance(S3ClientFactory.class).client();

        Set<String> objects = s3.listObjects(b -> b.bucket("bucket"))
                .contents().stream().map(S3Object::key).collect(Collectors.toSet());

        assertEquals(Set.of("onstart"), objects);

        byte[] b1Bytes = s3.getObject(b -> b.bucket("bucket").key("onstart")).readAllBytes();
        assertEquals("onstart", new String(b1Bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void checkBeforeMethod() throws IOException {
        S3Client s3 = app.getInstance(S3ClientFactory.class).client();

        Set<String> objects = s3.listObjects(b -> b.bucket("bucket"))
                .contents().stream().map(S3Object::key).collect(Collectors.toSet());

        assertEquals(Set.of("onstart", "beforeMethod"), objects);

        byte[] b1Bytes = s3.getObject(b -> b.bucket("bucket").key("beforeMethod")).readAllBytes();
        assertEquals("beforeMethod", new String(b1Bytes, StandardCharsets.UTF_8));
    }
}
