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
package io.bootique.aws2.s3;

import io.bootique.BQCoreModule;
import io.bootique.BQRuntime;
import io.bootique.Bootique;
import io.bootique.junit5.BQApp;
import io.bootique.junit5.BQTest;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@BQTest
public class AwsS3IT {

    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:0.11.3");

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.S3);

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.accessKey", localstack.getAccessKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.secretKey", localstack.getSecretKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.defaultRegion", localstack.getRegion()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awss3.endpointOverride", localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString()))
            .createRuntime();

    @Test
    public void testBucketOperations() throws IOException {
        S3Client s3 = app.getInstance(S3Client.class);

        // create bucket
        s3.createBucket(b -> b.bucket("test-bucket"));

        // put bucket ,
        s3.putObject(b -> b.bucket("test-bucket").key("f1"), RequestBody.fromString("dummy content"));

        // list bucket
        ListObjectsV2Response list = s3.listObjectsV2(b -> b.bucket("test-bucket"));
        assertEquals(1, list.contents().size());
        S3Object s3object = list.contents().get(0);
        assertEquals("f1", s3object.key());

        // get object
        ResponseInputStream<GetObjectResponse> objectData = s3.getObject(b -> b.bucket("test-bucket").key("f1"));

        try (BufferedReader in = new BufferedReader(new InputStreamReader(objectData))) {
            String content = in.lines().collect(Collectors.joining(""));
            assertEquals("dummy content", content);
        }

        // delete object
        s3.deleteObject(b -> b.bucket("test-bucket").key("f1"));

        // delete bucket
        s3.deleteBucket(b -> b.bucket("test-bucket"));
    }
}
