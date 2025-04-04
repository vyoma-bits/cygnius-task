package com.myorg.lambdas.journals;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import com.myorg.Messages.Response;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest;
import software.amazon.awssdk.services.dynamodb.model.DeleteItemResponse;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class DeleteClientJournal implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received event: {}", event);
            String journalId = event.getPathParameters() != null ? event.getPathParameters().get("journalId") : null;
            if (journalId == null || journalId.isEmpty()) {
                log.warn("Missing required parameter: journalId");
                return response.withStatusCode(400).withBody(objectMapper.writeValueAsString(new Response(false, "Missing required parameter: journalId", null)));
            }
            log.info("Deleting journal with ID: {}", journalId);
            Map<String, AttributeValue> key = new HashMap<>();
            key.put("journalId", AttributeValue.builder().s(journalId).build());
            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName(CollectionNames.JOURNALS)
                    .key(key)
                    .build();
            DeleteItemResponse deleteResponse = dynamoDbClient.deleteItem(deleteRequest);
            if (deleteResponse.sdkHttpResponse().isSuccessful()) {
                log.info("Journal deleted successfully: {}", journalId);
                return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(new Response(true, "Journal deleted successfully", null)));
            } else {
                log.error("Failed to delete journal: {}", journalId);
                return response.withStatusCode(500).withBody(objectMapper.writeValueAsString(new Response(false, "Failed to delete journal", null)));
            }

        } catch (Exception e) {
            log.error("Error while deleting journal: ", e);
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
