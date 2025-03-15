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
import io.bootique.aws2.s3.S3ClientFactory;
import io.bootique.aws2.s3.S3ClientFactoryFactory;
import io.bootique.di.Injector;
import io.bootique.di.Key;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

@BQConfig
@JsonTypeName("awss3test")
public class S3TestFactory extends S3ClientFactoryFactory {

    static final Logger LOGGER = LoggerFactory.getLogger(S3TestFactory.class);

    private final Injector injector;

    @Inject
    public S3TestFactory(AwsConfig config, Injector injector) {
        super(config);
        this.injector = injector;
    }

    @Override
    public S3ClientFactory create() {
        Key<AwsTester> awsTesterKey = Key.get(AwsTester.class);
        if (!injector.hasProvider(awsTesterKey)) {
            LOGGER.warn("Using S3Tester without AwsTester. This will likely result in an invalid configuration");
            return super.create();
        }

        AwsTester tester = injector.getInstance(awsTesterKey);
        setEndpointOverride(tester.getEndpointOverride());
        S3ClientFactory factory = super.create();

        // run client startup callbacks
        Key<S3Tester> s3TesterKey = Key.get(S3Tester.class);
        if (!injector.hasProvider(s3TesterKey)) {
            LOGGER.warn("No S3Tester in the environment. Skipping tester initialization");
            return factory;
        }

        S3Tester s3Tester = injector.getInstance(s3TesterKey);
        s3Tester.onS3ClientFactoryInit(factory);

        return factory;
    }
}
