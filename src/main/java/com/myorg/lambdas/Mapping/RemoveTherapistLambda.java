package com.myorg.lambdas.Mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Map;

public class RemoveTherapistLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final String TABLE_NAME = "Clients";

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            String clientId = event.getPathParameters().get("clientId");
            String therapistId = event.getQueryStringParameters().get("therapistId");

            if (clientId == null || therapistId == null) {
                return response.withStatusCode(400).withBody("{\"error\": \"clientId and therapistId are required\"}");
            }

            UpdateItemRequest request = UpdateItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of("client_id", AttributeValue.builder().s(clientId).build()))
                    .updateExpression("DELETE therapist_ids :therapistId")
                    .expressionAttributeValues(Map.of(":therapistId", AttributeValue.builder().ss(therapistId).build()))
                    .build();

            dynamoDbClient.updateItem(request);
            return response.withStatusCode(200).withBody("{\"message\": \"Therapist removed successfully\"}");
        } catch (Exception e) {
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}