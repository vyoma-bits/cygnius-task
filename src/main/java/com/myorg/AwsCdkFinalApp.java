package com.myorg;

import com.myorg.cdk.ApiGatewayStack;
import com.myorg.cdk.DynamoDbStack;
import com.myorg.cdk.LambdaStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class AwsCdkFinalApp {
    public static void main(final String[] args) {
        App app = new App();

        // ✅ 1. Deploy Lambda Functions
        LambdaStack lambdaStack = new LambdaStack(app, "LambdaStack", StackProps.builder().build());

        // ✅ 2. Deploy DynamoDB Tables
        new DynamoDbStack(app, "DynamoDbStack", StackProps.builder().build());

        // ✅ 3. Deploy API Gateway, Passing All Required Lambda Functions
        new ApiGatewayStack(app, "ApiGatewayStack",
                lambdaStack.getMessagesLambda(),
                lambdaStack.getMessageHistoryLambda(),
                lambdaStack.getCreateSessionLambda(),
                lambdaStack.getUpdateSessionLambda(),
                lambdaStack.getDeleteSessionLambda(),
                lambdaStack.getListSessionsLambda(),
                lambdaStack.getReadSessionLambda(),
                lambdaStack.getCreateRequestLambda(),
                lambdaStack.getUpdateRequestStatusLambda()
        );

        // ✅ 4. Synthesize the App
        app.synth();
    }
}
