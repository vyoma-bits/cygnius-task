package com.myorg.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.services.apigateway.*;
import software.amazon.awscdk.services.lambda.Function;
import software.constructs.Construct;

public class ApiGatewayStack extends Stack {
    public ApiGatewayStack(final Construct scope, final String id,
                           Function messagesLambda, Function messageHistoryLambda,
                           Function createSessionLambda, Function updateSessionLambda,
                           Function deleteSessionLambda, Function listSessionsLambda,
                           Function readSessionLambda,
                           // ✅ Newly added for Requests
                           Function createRequestLambda, Function updateRequestStatusLambda,Function createMappingLambda,
                           Function updateMappingLambda,
                           Function listMappingLambda,
                           Function deleteMappingLambda,
                           Function getRequestDetailsLambda,
                           Function createJournallambda
                           ) {
        super(scope, id);
        RestApi api = RestApi.Builder.create(this, "TherapyApi")
                .restApiName("Therapy API")
                .description("API Gateway for Messages, Sessions and Requests")
                .build();

        Resource messagesResource = api.getRoot().addResource("messages");
        messagesResource.addResource("history").addMethod("GET", LambdaIntegration.Builder.create(messageHistoryLambda).build());
        LambdaIntegration messagesIntegration = LambdaIntegration.Builder.create(messagesLambda).build();
        messagesResource.addMethod("POST", messagesIntegration);

        Resource sessionsResource = api.getRoot().addResource("sessions");
        sessionsResource.addMethod("POST", LambdaIntegration.Builder.create(createSessionLambda).build());
        sessionsResource.addMethod("GET", LambdaIntegration.Builder.create(listSessionsLambda).build());
        Resource sessionByIdResource = sessionsResource.addResource("{sessionId}");
        sessionByIdResource.addMethod("GET", LambdaIntegration.Builder.create(readSessionLambda).build());
        sessionByIdResource.addMethod("PUT", LambdaIntegration.Builder.create(updateSessionLambda).build());
        sessionByIdResource.addMethod("DELETE", LambdaIntegration.Builder.create(deleteSessionLambda).build());


        Resource requestsResource = api.getRoot().addResource("requests");

        requestsResource.addMethod("POST", LambdaIntegration.Builder.create(createRequestLambda).build());

        Resource requestByIdResource = requestsResource.addResource("{requestId}");

        requestByIdResource.addMethod("GET", LambdaIntegration.Builder.create(getRequestDetailsLambda).build());
        requestByIdResource.addMethod("PUT", LambdaIntegration.Builder.create(updateRequestStatusLambda).build());

        Resource mappingsResource = api.getRoot().addResource("mappings");
        mappingsResource.addMethod("POST", LambdaIntegration.Builder.create(createMappingLambda).build());

        Resource mappingByIdResource = mappingsResource.addResource("{mappingId}");
        mappingByIdResource.addMethod("PUT", LambdaIntegration.Builder.create(updateMappingLambda).build());
        mappingByIdResource.addMethod("DELETE", LambdaIntegration.Builder.create(deleteMappingLambda).build());
        mappingByIdResource.addMethod("GET", LambdaIntegration.Builder.create(listMappingLambda).build());

        Resource journalResource = api.getRoot().addResource("journals");
        sessionsResource.addMethod("POST", LambdaIntegration.Builder.create(createJournallambda).build());


    }
}
