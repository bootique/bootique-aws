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

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.aws2.AwsConfig;
import io.bootique.aws2.junit5.AwsTester;
import io.bootique.aws2.s3.S3Factory;
import io.bootique.di.Injector;
import io.bootique.di.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.services.s3.S3Client;

@BQConfig
@JsonTypeName("awss3test")
public class S3TestFactory extends S3Factory {

    static final Logger LOGGER = LoggerFactory.getLogger(S3TestFactory.class);

    @Override
    public S3Client createS3(AwsConfig config, Injector injector) {
        Key<AwsTester> awsTesterKey = Key.get(AwsTester.class);
        if (!injector.hasProvider(awsTesterKey)) {
            LOGGER.warn("Using S3Tester without AwsTester. This will likely result in an invalid configuration");
            return super.createS3(config, injector);
        }

        AwsTester tester = injector.getInstance(awsTesterKey);
        setEndpointOverride(tester.getEndpointOverride());
        S3Client client = super.createS3(config, injector);

        // run client startup callbacks
        Key<S3Tester> s3TesterKey = Key.get(S3Tester.class);
        if (!injector.hasProvider(s3TesterKey)) {
            LOGGER.warn("No S3Tester in the environment. Skipping tester initialization");
            return client;
        }

        S3Tester s3Tester = injector.getInstance(s3TesterKey);
        s3Tester.onS3ClientInit(client);

        return client;
    }
}
