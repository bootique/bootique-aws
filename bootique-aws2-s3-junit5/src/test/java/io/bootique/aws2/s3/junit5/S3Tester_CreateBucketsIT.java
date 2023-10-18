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
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
public class S3Tester_CreateBucketsIT extends BaseAwsTest {

    @BQTestTool
    static final S3Tester s3 = S3Tester
            .create()
            .createBuckets("bt1", "bt2");

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(aws.moduleWithTestAws())
            .module(s3.moduleWithTestS3())
            .createRuntime();

    @Test
    public void test() throws IOException {
        S3Client s3 = app.getInstance(S3ClientFactory.class).client();

        s3.putObject(b -> b.bucket("bt1").key("b1k1"), RequestBody.fromBytes("b1v1".getBytes()));
        s3.putObject(b -> b.bucket("bt2").key("b2k1"), RequestBody.fromBytes("b2v1".getBytes()));

        byte[] b1Bytes = s3.getObject(b -> b.bucket("bt1").key("b1k1")).readAllBytes();
        assertEquals("b1v1", new String(b1Bytes, StandardCharsets.UTF_8));

        byte[] b2Bytes = s3.getObject(b -> b.bucket("bt2").key("b2k1")).readAllBytes();
        assertEquals("b2v1", new String(b2Bytes, StandardCharsets.UTF_8));
    }
}
