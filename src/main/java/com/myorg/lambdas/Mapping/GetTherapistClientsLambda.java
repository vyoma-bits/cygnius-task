package com.myorg.lambdas.Mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import java.util.Map;

public class GetTherapistClientsLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final String TABLE_NAME = "Therapists";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            String therapistId = event.getPathParameters().get("therapistId");
            if (therapistId == null) {
                return response.withStatusCode(400).withBody("{\"error\": \"therapistId is required\"}");
            }

            GetItemRequest request = GetItemRequest.builder()
                    .tableName(TABLE_NAME)
                    .key(Map.of("therapist_id", AttributeValue.builder().s(therapistId).build()))
                    .build();

            var result = dynamoDbClient.getItem(request);
            if (!result.hasItem()) {
                return response.withStatusCode(404).withBody("{\"error\": \"Therapist not found\"}");
            }

            var clientIds = result.item().get("client_ids");
            return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(Map.of("client_ids", clientIds.s())));
        } catch (Exception e) {
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
