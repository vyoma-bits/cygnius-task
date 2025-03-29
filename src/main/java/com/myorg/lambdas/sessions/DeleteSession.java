package com.myorg.lambdas.sessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.myorg.CollectionNames;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DeleteSession implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        log.info("Received delete request event: {}", event);

        try {
            String sessionId = event.getPathParameters() != null ? event.getPathParameters().get("sessionId") : null;
            String therapistId = event.getQueryStringParameters() != null ? event.getQueryStringParameters().get("therapistId") : null;
            if (sessionId == null || therapistId == null) {
                log.warn("Missing required parameters: sessionId or therapistId. sessionId={}, therapistId={}", sessionId, therapistId);
                return response.withStatusCode(400).withBody("{\"error\": \"Missing sessionId or therapistId\"}");
            }
            log.info("Attempting to delete session. sessionId={}, therapistId={}", sessionId, therapistId);
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("session_id", AttributeValue.builder().s(sessionId).build());
            key.put("therapist_id", AttributeValue.builder().s(therapistId).build());
            DeleteItemRequest deleteItemRequest = DeleteItemRequest.builder()
                    .tableName(CollectionNames.SESSIONS)
                    .key(key)
                    .build();
            dynamoDbClient.deleteItem(deleteItemRequest);
            log.info("Session deleted successfully. sessionId={}, therapistId={}", sessionId, therapistId);
            return response.withStatusCode(200).withBody("{\"message\": \"Session deleted successfully\"}");
        } catch (Exception e) {
            log.error("Error while deleting session: ", e);
            return response.withStatusCode(500).withBody("{\"error\": \"Internal server error: " + e.getMessage() + "\"}");
        }
    }
}
