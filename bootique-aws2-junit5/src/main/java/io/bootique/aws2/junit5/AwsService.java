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
package io.bootique.aws2.junit5;

import org.testcontainers.containers.localstack.LocalStackContainer;

/**
 * Lists AWS services that can be emulated in your tests.
 *
 * @since 3.0
 */
public enum AwsService {

    // Must match LocalStackContainer.Service enum. Will need to be updated  as we upgrade to newer TC with more
    // services. Consistency is guaranteed via AwsServiceTest

    API_GATEWAY,
    EC2,
    KINESIS,
    DYNAMODB,
    DYNAMODB_STREAMS,
    S3,
    FIREHOSE,
    LAMBDA,
    SNS,
    SQS,
    REDSHIFT,
    SES,
    ROUTE53,
    CLOUDFORMATION,
    CLOUDWATCH,
    SSM,
    SECRETSMANAGER,
    STEPFUNCTIONS,
    CLOUDWATCHLOGS,
    STS,
    IAM,
    KMS;

    public LocalStackContainer.Service toLocalstackService() {
        return LocalStackContainer.Service.valueOf(name());
    }
}
