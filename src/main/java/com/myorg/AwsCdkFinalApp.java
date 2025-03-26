package com.myorg;

import com.myorg.cdk.ApiGatewayStack;
import com.myorg.cdk.DynamoDbStack;
import com.myorg.cdk.LambdaStack;
import software.amazon.awscdk.App;
import software.amazon.awscdk.StackProps;

public class AwsCdkFinalApp {
    public static void main(final String[] args) {
        App app = new App();
        LambdaStack lambdaStack = new LambdaStack(app, "LambdaStack", StackProps.builder().build());
        new AwsCdkFinalStack(app, "AwsCdkFinalStack", StackProps.builder()

                // If you don't specify 'env', this stack will be environment-agnostic.
                // Account/Region-dependent features and context lookups will not work,
                // but a single synthesized template can be deployed anywhere.

                // Uncomment the next block to specialize this stack for the AWS Account
                // and Region that are implied by the current CLI configuration.
                /*
                .env(Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build())
                */

                // Uncomment the next block if you know exactly what Account and Region you
                // want to deploy the stack to.
                /*
                .env(Environment.builder()
                        .account("123456789012")
                        .region("us-east-1")
                        .build())
                */

                // For more information, see https://docs.aws.amazon.com/cdk/latest/guide/environments.html
                .build());
        new DynamoDbStack(app, "DynamoDbStack", StackProps.builder().build());

        new ApiGatewayStack(app, "ApiGatewayStack",lambdaStack.getMessagesLambda() );
        app.synth();
    }
}

