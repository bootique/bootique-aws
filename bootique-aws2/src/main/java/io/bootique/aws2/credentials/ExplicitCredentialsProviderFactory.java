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
package io.bootique.aws2.credentials;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.bootique.annotation.BQConfig;
import io.bootique.annotation.BQConfigProperty;
import io.bootique.di.Injector;
import software.amazon.awssdk.auth.credentials.*;

/**
 * @since 3.0
 */
@JsonTypeName("explicit")
@BQConfig("Configures 'access key', 'secret key' and an optional 'session token' that serve as credentials to access AWS services.")
public class ExplicitCredentialsProviderFactory implements AwsCredentialsProviderFactory {

    private String accessKey;
    private String secretKey;
    private String sessionToken;

    @BQConfigProperty("Sets AWS access key credential")
    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    @BQConfigProperty("AWS secret key credential")
    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    @BQConfigProperty("AWS session token credential. It is optional, and is only used with 'temporary' credentials")
    public void setSessionToken(String sessionToken) {
        this.sessionToken = sessionToken;
    }

    @Override
    public AwsCredentialsProvider create(Injector injector) {

        if (accessKey == null && secretKey == null) {
            throw new NullPointerException("'accessKey' and 'secretKey' are not set");
        } else if (accessKey == null) {
            throw new NullPointerException("'accessKey' is not set");
        } else if (secretKey == null) {
            throw new NullPointerException("'secretKey' is not set");
        }

        AwsCredentials credentials = sessionToken != null
                ? AwsSessionCredentials.create(accessKey, secretKey, sessionToken)
                : AwsBasicCredentials.create(accessKey, secretKey);

        return StaticCredentialsProvider.create(credentials);
    }
}
