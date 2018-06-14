/**
 *    Licensed to the ObjectStyle LLC under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ObjectStyle LLC licenses
 *  this file to you under the Apache License, Version 2.0 (the
 *  “License”); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  “AS IS” BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */

package io.bootique.aws;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSCredentialsProvider;
import io.bootique.test.junit.BQTestFactory;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AwsModuleIT {

    @Rule
    public BQTestFactory testFactory = new BQTestFactory();

    @Test
    public void testInjectableAWSCredentialsProvider() {
        AWSCredentialsProvider provider = testFactory
                .app()
                .autoLoadModules()
                .property("bq.aws.accessKey", "xyz")
                .property("bq.aws.secretKey", "abc")
                .createRuntime()
                .getInstance(AWSCredentialsProvider.class);

        AWSCredentials credentials = provider.getCredentials();

        assertNotNull(credentials);
        assertEquals("xyz", credentials.getAWSAccessKeyId());
        assertEquals("abc", credentials.getAWSSecretKey());
    }
}
