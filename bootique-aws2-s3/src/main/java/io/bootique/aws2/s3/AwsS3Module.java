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

import io.bootique.ConfigModule;
import io.bootique.aws2.AwsConfig;
import io.bootique.config.ConfigurationFactory;
import io.bootique.di.Injector;
import io.bootique.di.Provides;

import javax.inject.Singleton;

public class AwsS3Module extends ConfigModule {

    @Provides
    @Singleton
    S3ClientFactory provideS3ClientFactory(ConfigurationFactory configFactory, AwsConfig config, Injector injector) {
        return config(S3ClientFactoryFactory.class, configFactory).create(config, injector);
    }
}
