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
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class ListMapping implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received event: {}", event);
            Map<String, String> pathParams = event.getPathParameters();
            String mappingId = pathParams != null ? pathParams.get("mappingId") : null;

            if (mappingId == null || mappingId.isEmpty()) {
                log.warn("Missing required parameter: mappingId");
                return response.withStatusCode(400).withBody(
                        objectMapper.writeValueAsString(new Response(false, "Missing required parameter: mappingId", null))
                );
            }
            Map<String, String> queryParams = event.getQueryStringParameters();
            Long createdAt = queryParams != null && queryParams.get("createdAt") != null
                    ? Long.parseLong(queryParams.get("createdAt"))
                    : null;

            log.info("Querying for mappingId: {}", mappingId);
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":mappingId", AttributeValue.builder().s(mappingId).build());
            expressionValues.put(":createdAt", AttributeValue.builder().n(String.valueOf(createdAt)).build());
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(CollectionNames.MAPPINGS)
                    .keyConditionExpression("mappingId = :mappingId AND createdAt = :createdAt") // Ensure both attributes are used
                    .expressionAttributeValues(expressionValues)
                    .build();
            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            List<Map<String, AttributeValue>> items = queryResponse.items();

            log.info("Query result count: {}", items.size());

            if (items.isEmpty()) {
                log.warn("Mapping not found for mappingId: {}", mappingId);
                return response.withStatusCode(404).withBody(
                        objectMapper.writeValueAsString(new Response(false, "Mapping not found", null))
                );
            }

            Map<String, Object> mappingData = convertDynamoItemToMap(items.get(0));

            log.info("Mapping fetched successfully for mappingId: {}", mappingId);
            return response.withStatusCode(200).withBody(
                    objectMapper.writeValueAsString(new Response(true, "Mapping fetched successfully", mappingData))
            );

        } catch (Exception e) {
            log.error("Error while reading mapping: ", e);
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * This function is used to convert Dynamo Db item to into a Map that could be used for more purposes.
     * @param item
     * @return
     */
    private Map<String, Object> convertDynamoItemToMap(Map<String, AttributeValue> item) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
            AttributeValue value = entry.getValue();
            if (value.s() != null) result.put(entry.getKey(), value.s());
            else if (value.n() != null) result.put(entry.getKey(), value.n());
            else if (value.bool() != null) result.put(entry.getKey(), value.bool());
            else if (value.hasM()) result.put(entry.getKey(), value.m().toString());
            else if (value.hasL()) result.put(entry.getKey(), value.l().toString());
            else result.put(entry.getKey(), null);
        }
        return result;
    }
}
