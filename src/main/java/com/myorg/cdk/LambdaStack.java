package com.myorg.cdk;

import com.myorg.utils.LambdaFactory;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Function;
import software.constructs.Construct;

import java.util.List;

public class LambdaStack extends Stack {

    private final Function messagesLambda;
    private final Function messageHistoryLambda;
    private final Function createSessionLambda;
    private final Function updateSessionLambda;
    private final Function deleteSessionLambda;
    private final Function listSessionsLambda;
    private final Function readSessionLambda;
    private final Function createRequestLambda;
    private final Function updateRequestStatusLambda;

    public LambdaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        PolicyStatement cloudWatchPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("logs:CreateLogGroup", "logs:CreateLogStream", "logs:PutLogEvents"))
                .resources(List.of("*"))
                .build();

        PolicyStatement dynamoFullPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "dynamodb:PutItem",
                        "dynamodb:GetItem",
                        "dynamodb:UpdateItem",
                        "dynamodb:DeleteItem",
                        "dynamodb:Query",
                        "dynamodb:Scan",
                        "dynamodb:BatchWriteItem",
                        "dynamodb:BatchGetItem"
                ))
                .resources(List.of("arn:aws:dynamodb:ap-south-1:897729110707:table/*", "arn:aws:dynamodb:ap-south-1:897729110707:table/*/index/*"))
                .build();

        Role lambdaRole = Role.Builder.create(this, "LambdaRole")
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole")))
                .build();
        lambdaRole.addToPolicy(cloudWatchPolicy);
        lambdaRole.addToPolicy(dynamoFullPolicy);

        //  Lambdas for Messages
        messagesLambda = LambdaFactory.createLambda(this, "MessagesLambda",
                "com.myorg.lambdas.messages.CreateMessage::handleRequest",
                "target/aws-cdk-final-0.1.jar", lambdaRole);

        messageHistoryLambda = LambdaFactory.createLambda(this, "MessageHistoryLambda",
                "com.myorg.lambdas.messages.GetMessageHistory::handleRequest",
                "target/aws-cdk-final-0.1.jar", lambdaRole);

        //  Lambdas for Sessions
        createSessionLambda = LambdaFactory.createLambda(this, "CreateSessionLambda",
                "com.myorg.lambdas.sessions.CreateSession::handleRequest",
                "target/aws-cdk-final-0.1.jar", lambdaRole);

        updateSessionLambda = LambdaFactory.createLambda(this, "UpdateSessionLambda",
                "com.myorg.lambdas.sessions.UpdateSession::handleRequest",
                "target/aws-cdk-final-0.1.jar", lambdaRole);

        deleteSessionLambda = LambdaFactory.createLambda(this, "DeleteSessionLambda",
                "com.myorg.lambdas.sessions.DeleteSession::handleRequest",
                "target/aws-cdk-final-0.1.jar", lambdaRole);

        listSessionsLambda = LambdaFactory.createLambda(this, "ListSessionsLambda",
                "com.myorg.lambdas.sessions.ListSessions::handleRequest",
                "target/aws-cdk-final-0.1.jar", lambdaRole);

        readSessionLambda = LambdaFactory.createLambda(this, "ReadSessionLambda",
                "com.myorg.lambdas.sessions.ReadSession::handleRequest",
                "target/aws-cdk-final-0.1.jar", lambdaRole);

        //  Lambdas for Requests
        createRequestLambda = LambdaFactory.createLambda(this, "CreateRequestLambda",
                "com.myorg.lambdas.requests.CreateRequest::handleRequest",
                "target/aws-cdk-final-0.1.jar", lambdaRole);

        updateRequestStatusLambda = LambdaFactory.createLambda(this, "UpdateRequestStatusLambda",
                "com.myorg.lambdas.requests.UpdateRequestStatus::handleRequest",
                "target/aws-cdk-final-0.1.jar", lambdaRole);
    }

    //  Getters
    public Function getMessagesLambda() { return messagesLambda; }
    public Function getMessageHistoryLambda() { return messageHistoryLambda; }
    public Function getCreateSessionLambda() { return createSessionLambda; }
    public Function getUpdateSessionLambda() { return updateSessionLambda; }
    public Function getDeleteSessionLambda() { return deleteSessionLambda; }
    public Function getListSessionsLambda() { return listSessionsLambda; }
    public Function getReadSessionLambda() { return readSessionLambda; }
    public Function getCreateRequestLambda() { return createRequestLambda; }
    public Function getUpdateRequestStatusLambda() { return updateRequestStatusLambda; }
}
