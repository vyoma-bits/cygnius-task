package com.myorg.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.Function;
import software.constructs.Construct;

public class ApiGatewayStack extends Stack {
    public ApiGatewayStack(final Construct scope, final String id, Function messagesLambda) {
        super(scope, id);

        // ✅ 1. Create API Gateway
        RestApi api = RestApi.Builder.create(this, "MessagesApi")
                .restApiName("Messages API")
                .description("API Gateway for Messages Lambda")
                .build();

        // ✅ 2. Create API Resource (e.g., /messages)
        Resource messagesResource = api.getRoot().addResource("messages");

        // ✅ 3. Integrate with Lambda
        LambdaIntegration messagesIntegration = LambdaIntegration.Builder.create(messagesLambda).build();

        // ✅ 4. Add HTTP Methods
        messagesResource.addMethod("POST", messagesIntegration);  // Create message
    }
}
