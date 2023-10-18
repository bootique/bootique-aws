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

import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@BQTest
@Disabled("Until #23 is fixed")
public class AwsTesterIT {

    @BQTestTool
    static final AwsTester tester = AwsTester.aws(AwsService.S3, AwsService.EC2);

    @BQApp(skipRun = true)
    static final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .createRuntime();

    @Test
    public void testS3() throws IOException {
        S3Client s3 = S3Client
                .builder()
                .endpointOverride(tester.getEndpointOverride())
                .credentialsProvider(tester.getCredentialsProvider())
                .region(tester.getRegion())
                .build();

        s3.createBucket(b -> b.bucket("foo"));
        s3.putObject(b -> b.bucket("foo").key("bar"), RequestBody.fromBytes("baz".getBytes()));
        byte[] bytes = s3.getObject(b -> b.bucket("foo").key("bar")).readAllBytes();
        assertEquals("baz", new String(bytes, StandardCharsets.UTF_8));
    }

}
