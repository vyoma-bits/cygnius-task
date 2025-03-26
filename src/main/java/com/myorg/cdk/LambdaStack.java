package com.myorg.cdk;

import com.myorg.utils.LambdaFactory;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.Effect;
import software.amazon.awscdk.services.iam.ManagedPolicy;
import software.amazon.awscdk.services.iam.PolicyStatement;
import software.amazon.awscdk.services.iam.Role;
import software.amazon.awscdk.services.iam.ServicePrincipal;
import software.amazon.awscdk.services.lambda.Function;
import software.constructs.Construct;

import java.util.List;

public class LambdaStack extends Stack {
    private final Function messagesLambda;
//    private final Function therapyLambda;
//    private final Function mappingLambda;

    public LambdaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id,props);
        Role lambdaRole = Role.Builder.create(this, "LambdaDynamoDBRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole")))
                .build();

        // Attach a policy allowing Lambda to write to any table
        lambdaRole.addToPolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("dynamodb:PutItem", "dynamodb:GetItem", "dynamodb:UpdateItem", "dynamodb:DeleteItem"))
                .resources(List.of("arn:aws:dynamodb:ap-south-1:897729110707:table/*")) // Wildcard allows all tables
                .build());

        // ✅ Create Lambda functions using LambdaFactory
        messagesLambda = LambdaFactory.createLambda(this, "MessagesLambda",
                "com.myorg.lambdas.messages.CreateMessage::handleRequest",
                "target/aws-cdk-final-0.1.jar",lambdaRole);

//
//        therapyLambda = LambdaFactory.createLambda(this, "TherapyLambda",
//                "com.myorg.therapy.TherapyHandler::handleRequest",
//                "target/therapy-lambda.jar");
//
//        mappingLambda = LambdaFactory.createLambda(this, "MappingLambda",
//                "com.myorg.mappings.MappingHandler::handleRequest",
//                "target/mapping-lambda.jar");
    }

    public Function getMessagesLambda() { return messagesLambda; }
//    public Function getTherapyLambda() { return therapyLambda; }
//    public Function getMappingLambda() { return mappingLambda; }
}