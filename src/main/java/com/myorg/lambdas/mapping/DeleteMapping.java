package com.myorg.lambdas.mapping;

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
public class DeleteMapping implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received event: {}", event);
            Map<String, String> pathParams = event.getPathParameters();
            String mappingId = pathParams != null ? pathParams.get("mappingId") : null;
            Map<String, String> queryParams = event.getQueryStringParameters();
            Long createdAt = queryParams != null && queryParams.get("createdAt") != null
                    ? Long.parseLong(queryParams.get("createdAt"))
                    : null;

            if (mappingId == null || mappingId.isEmpty()) {
                log.warn("Missing required parameter: mappingId");
                return response.withStatusCode(400).withBody(
                        objectMapper.writeValueAsString(new Response(false, "Missing required parameter: mappingId", null))
                );
            }

            log.info("Deleting mapping with mappingId: {}", mappingId);
            Map<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("mappingId", AttributeValue.builder().s(mappingId).build());
            keyMap.put("createdAt", AttributeValue.builder().n(String.valueOf(createdAt)).build());
            DeleteItemRequest deleteRequest = DeleteItemRequest.builder()
                    .tableName(CollectionNames.MAPPINGS)
                    .key(keyMap)
                    .build();

            DeleteItemResponse deleteResponse = dynamoDbClient.deleteItem(deleteRequest);

            log.info("Mapping deleted successfully for mappingId: {}", mappingId);
            return response.withStatusCode(200).withBody(
                    objectMapper.writeValueAsString(new Response(true, "Mapping deleted successfully", null))
            );

        } catch (Exception e) {
            log.error("Error while deleting mapping: ", e);
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
