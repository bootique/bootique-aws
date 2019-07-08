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

package io.bootique.aws;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import io.bootique.ConfigModule;
import io.bootique.config.ConfigurationFactory;

public class AwsModule extends ConfigModule {

    @Provides
    @Singleton
    AwsConfigFactory provideConfigFactory(ConfigurationFactory configurationFactory) {
        return config(AwsConfigFactory.class, configurationFactory);
    }

    @Provides
    @Singleton
    AWSCredentialsProvider provideCredentialsProvider(AwsConfigFactory configFactory) {
        return configFactory.createCredentialsProvider();
    }

    @Provides
    @Singleton
    AwsConfig provideConfig(AwsConfigFactory configFactory) {
        return configFactory.createConfig();
    }
}
