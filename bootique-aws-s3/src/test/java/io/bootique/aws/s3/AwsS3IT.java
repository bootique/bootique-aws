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
package io.bootique.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectSummary;
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@BQTest
public class AwsS3IT {

    static DockerImageName localstackImage = DockerImageName.parse("localstack/localstack:4.0.3");

    @Container
    static final LocalStackContainer localstack = new LocalStackContainer(localstackImage)
            .withServices(LocalStackContainer.Service.S3);

    @BQApp(skipRun = true)
    final BQRuntime app = Bootique.app()
            .autoLoadModules()
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.accessKey", localstack.getAccessKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.aws.secretKey", localstack.getSecretKey()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awss3.signingRegion", localstack.getRegion()))
            .module(b -> BQCoreModule.extend(b).setProperty("bq.awss3.serviceEndpoint", localstack.getEndpointOverride(LocalStackContainer.Service.S3).toString()))
            .createRuntime();

    @Test
    public void bucketOperations() throws IOException {
        AmazonS3 s3 = app.getInstance(AmazonS3.class);

        // create bucket
        Bucket bucket = s3.createBucket("test-bucket");
        assertEquals("test-bucket", bucket.getName());

        // put bucket
        s3.putObject(bucket.getName(), "f1", "dummy content");

        // list bucket
        ListObjectsV2Result list = s3.listObjectsV2(bucket.getName());
        assertEquals(1, list.getKeyCount());
        S3ObjectSummary summary = list.getObjectSummaries().get(0);
        assertEquals(bucket.getName(), summary.getBucketName());
        assertEquals("f1", summary.getKey());

        // get object
        S3Object o = s3.getObject(bucket.getName(), "f1");
        try (BufferedReader in = new BufferedReader(new InputStreamReader(o.getObjectContent()))) {
            String content = in.lines().collect(Collectors.joining(""));
            assertEquals("dummy content", content);
        }

        // delete object
        s3.deleteObject(bucket.getName(), "f1");

        // delete bucket
        s3.deleteBucket(bucket.getName());
    }
}
