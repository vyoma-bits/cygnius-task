package com.myorg.lambdas.requests;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.Map;
public class UpdateRequestStatus implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            String requestId = event.getQueryStringParameters().get("requestId");
            String isApprovedStr = event.getQueryStringParameters().get("isApproved");
            String createdAt = event.getQueryStringParameters().get("createdAt");

            if (requestId == null || isApprovedStr == null) {
                return response.withStatusCode(400).withBody("{\"error\":\"Missing requestId or isApproved in query params\"}");
            }
            boolean isApproved = Boolean.parseBoolean(isApprovedStr);
            Map<String, AttributeValue> key = Map.of(
                    "request_id", AttributeValue.builder().s(requestId).build(),
                    "created_at", AttributeValue.builder().s(createdAt).build()
            );
            Map<String, String> expressionAttributeNames = Map.of("#isApproved", "isApproved");
            Map<String, AttributeValue> expressionAttributeValues = Map.of(":val", AttributeValue.builder().bool(isApproved).build());
            UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                    .tableName(CollectionNames.REQUESTS)
                    .key(key)
                    .updateExpression("SET #isApproved = :val")
                    .expressionAttributeNames(expressionAttributeNames)
                    .expressionAttributeValues(expressionAttributeValues)
                    .build();
            dynamoDbClient.updateItem(updateRequest);
            return response.withStatusCode(200).withBody("{\"message\":\"Request status updated successfully\"}");
        } catch (Exception e) {
            return response.withStatusCode(500).withBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }
}
