package com.myorg;

import com.myorg.cdk.ApiGatewayStack;
import com.myorg.cdk.DynamoDbStack;
import com.myorg.cdk.LambdaStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class AwsCdkFinalApp {
    public static void main(final String[] args) {
        App app = new App();

        //  Deploy Lambda Functions
        LambdaStack lambdaStack = new LambdaStack(app, "LambdaStack", StackProps.builder().build());

        // . Deploy DynamoDB Tables
        new DynamoDbStack(app, "DynamoDbStack", StackProps.builder().build());

        //  Deploy API Gateway
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


        app.synth();
    }
}
