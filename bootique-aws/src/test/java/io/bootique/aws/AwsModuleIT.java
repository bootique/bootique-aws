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

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSCredentialsProviderChain;
import com.amazonaws.auth.BasicAWSCredentials;
import io.bootique.junit5.BQTest;
import io.bootique.junit5.BQTestFactory;
import io.bootique.junit5.BQTestTool;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@BQTest
public class AwsModuleIT {

    @BQTestTool
    final BQTestFactory testFactory = new BQTestFactory();

    @Test
    @DisplayName("Credentials provider based on BQ config")
    public void testAwsConfig_BootiqueCredentialsProvider() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .property("bq.aws.accessKey", "xyz")
                .property("bq.aws.secretKey", "abc")
                .createRuntime()
                .getInstance(AwsConfig.class);

        AWSCredentials credentials = config.getCredentialsProvider().getCredentials();

        assertNotNull(credentials);
        assertEquals("xyz", credentials.getAWSAccessKeyId());
        assertEquals("abc", credentials.getAWSSecretKey());
    }

    @Test
    @DisplayName("When no BQ credentials configured, alt provider must take over")
    public void testAwsConfig_AltCredentialsProvider() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa", "tcps"), 5))
                .createRuntime()
                .getInstance(AwsConfig.class);

        assertTrue(config.getCredentialsProvider() instanceof TestCredentialsProvider);
        AWSCredentials credentials = config.getCredentialsProvider().getCredentials();

        assertNotNull(credentials);
        assertEquals("tcpa", credentials.getAWSAccessKeyId());
        assertEquals("tcps", credentials.getAWSSecretKey());
    }

    @Test
    @DisplayName("When no BQ credentials configured, alt provider must take over")
    public void testAwsConfig_AltCredentialsProvider_Ordering() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa1", "tcps1"), 5))
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa2", "tcps2"), 2))
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa3", "tcps3"), 6))

                .createRuntime()
                .getInstance(AwsConfig.class);

        assertTrue(config.getCredentialsProvider() instanceof AWSCredentialsProviderChain);
        AWSCredentials credentials = config.getCredentialsProvider().getCredentials();

        assertNotNull(credentials);
        assertEquals("tcpa2", credentials.getAWSAccessKeyId());
        assertEquals("tcps2", credentials.getAWSSecretKey());
    }

    @Test
    @DisplayName("When BQ credentials are present, alt provider must be ignored")
    public void testAwsConfig_AltCredentialsProvider_BqProviderWins() {
        AwsConfig config = testFactory
                .app()
                .autoLoadModules()
                .property("bq.aws.accessKey", "xyz")
                .property("bq.aws.secretKey", "abc")
                .module(b -> AwsModule.extend(b).addCredentialsProvider(new TestCredentialsProvider("tcpa", "tcps"), 5))
                .createRuntime()
                .getInstance(AwsConfig.class);

        AWSCredentials credentials = config.getCredentialsProvider().getCredentials();

        assertNotNull(credentials);
        assertEquals("xyz", credentials.getAWSAccessKeyId());
        assertEquals("abc", credentials.getAWSSecretKey());
    }

    static final class TestCredentialsProvider implements AWSCredentialsProvider {

        private final String accessKey;
        private final String secretKey;

        public TestCredentialsProvider(String accessKey, String secretKey) {
            this.accessKey = accessKey;
            this.secretKey = secretKey;
        }

        @Override
        public AWSCredentials getCredentials() {
            return new BasicAWSCredentials(accessKey, secretKey);
        }

        @Override
        public void refresh() {
            throw new UnsupportedOperationException("Irrelevant");
        }
    }
}
