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

package io.bootique.aws2;

import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.*;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class AwsModuleIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Test
    @DisplayName("Credentials provider based on BQ config with access and secret keys")
    public void testAwsConfig_ExplicitCredentialsProvider() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .property("bq.aws.credentials.accessKey", "xyz")
                .property("bq.aws.credentials.secretKey", "abc")
                .createRuntime()
                .getInstance(AwsConfig.class);

        AwsCredentials credentials = config.getCredentialsProvider().resolveCredentials();

        assertTrue(credentials instanceof AwsBasicCredentials);
        assertEquals("xyz", credentials.accessKeyId());
        assertEquals("abc", credentials.secretAccessKey());

    }

    @Test
    @DisplayName("Credentials provider based on BQ config with access and secret keys and session token")
    public void testAwsConfig_ExplicitCredentialsProvider_SessionToken() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .property("bq.aws.credentials.accessKey", "xyz")
                .property("bq.aws.credentials.secretKey", "abc")
                .property("bq.aws.credentials.sessionToken", "123")
                .createRuntime()
                .getInstance(AwsConfig.class);

        AwsCredentials credentials = config.getCredentialsProvider().resolveCredentials();

        assertTrue(credentials instanceof AwsSessionCredentials);
        assertEquals("xyz", credentials.accessKeyId());
        assertEquals("abc", credentials.secretAccessKey());
        assertEquals("123", ((AwsSessionCredentials) credentials).sessionToken());
    }

    @Test
    @DisplayName("Credentials provider based on BQ config with profile")
    public void testAwsConfig_ProfileCredentialsProvider() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .property("bq.aws.credentials.type", "profile")
                .property("bq.aws.credentials.profile", "some_profile")
                .createRuntime()
                .getInstance(AwsConfig.class);

        assertTrue(config.getCredentialsProvider() instanceof ProfileCredentialsProvider);
    }

    @Test
    @DisplayName("When no BQ credentials configured, alt provider must take over")
    public void testAwsConfig_DICredentialsProvider() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa", "tcps"), 5))
                .createRuntime()
                .getInstance(AwsConfig.class);

        assertTrue(config.getCredentialsProvider() instanceof TestCredentialsProvider);
        AwsCredentials credentials = config.getCredentialsProvider().resolveCredentials();

        assertNotNull(credentials);
        assertEquals("tcpa", credentials.accessKeyId());
        assertEquals("tcps", credentials.secretAccessKey());
    }

    @Test
    @DisplayName("When no BQ credentials configured, alt provider must take over")
    public void testAwsConfig_DICredentialsProvider_Ordering() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa1", "tcps1"), 5))
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa2", "tcps2"), 2))
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa3", "tcps3"), 6))

                .createRuntime()
                .getInstance(AwsConfig.class);

        assertTrue(config.getCredentialsProvider() instanceof AwsCredentialsProviderChain);
        AwsCredentials credentials = config.getCredentialsProvider().resolveCredentials();

        assertNotNull(credentials);
        assertEquals("tcpa2", credentials.accessKeyId());
        assertEquals("tcps2", credentials.secretAccessKey());
    }

    @Test
    @DisplayName("When BQ credentials are present, alt provider must be ignored")
    public void testAwsConfig_DICredentialsProvider_ConfigProviderWins() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .property("bq.aws.credentials.accessKey", "xyz")
                .property("bq.aws.credentials.secretKey", "abc")
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa", "tcps"), 5))
                .createRuntime()
                .getInstance(AwsConfig.class);

        AwsCredentials credentials = config.getCredentialsProvider().resolveCredentials();

        assertNotNull(credentials);
        assertEquals("xyz", credentials.accessKeyId());
        assertEquals("abc", credentials.secretAccessKey());
    }

    static final class TestCredentialsProvider implements AwsCredentialsProvider {

        private final String accessKey;
        private final String secretKey;

        public TestCredentialsProvider(String accessKey, String secretKey) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
        }

        @Override
        public AwsCredentials resolveCredentials() {
            return AwsBasicCredentials.create(accessKey, secretKey);
        }
    }
}
