package com.myorg.utils;

import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.iam.IRole;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.Code;
import software.constructs.Construct;

public class LambdaFactory {
    /**
     * Creates an AWS Lambda function with the specified handler and JAR path.
     *
     * @param scope CDK Stack
     * @param functionName Name of the Lambda function
     * @param handler Full Java handler path (e.g., "com.myorg.lambda.CreateMessageLambda::handleRequest")
     * @param jarPath Path to the Lambda JAR file
     * @return Created Lambda function
     */
    public static Function createLambda(Construct scope, String functionName, String handler, String jarPath, IRole role) {
        return Function.Builder.create(scope, functionName)
                .runtime(Runtime.JAVA_17)  // ✅ Change to Java 17 if needed
                .code(Code.fromAsset(jarPath)) // ✅ Pass JAR path dynamically
                .role(role)
                .handler(handler)
                .memorySize(512)  // ✅ 512MB RAM
                .timeout(Duration.seconds(10))  // ✅ 10s Timeout
                .build();
    }
}
