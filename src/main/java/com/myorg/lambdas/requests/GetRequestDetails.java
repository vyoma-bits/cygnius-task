package com.myorg.lambdas.requests;

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

/**
 * This lambda is used to get the details of the request item
 */
@Slf4j
public class GetRequestDetails implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received event: {}", event);
            String requestId = event.getPathParameters() != null ? event.getPathParameters().get("requestId") : null;


            if (requestId == null || requestId.isEmpty()) {
                log.warn("Missing required parameter: requestId");
                return response.withStatusCode(400).withBody(objectMapper.writeValueAsString(new Response(false, "Missing required parameter: requestId", null)));
            }

            log.info("Querying for requestId: {}", requestId);
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":requestId", AttributeValue.builder().s(requestId).build());

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(CollectionNames.REQUESTS)
                    .keyConditionExpression("requestId = :requestId")
                    .expressionAttributeValues(expressionValues)
                    .build();

            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            List<Map<String, AttributeValue>> items = queryResponse.items();

            log.info("Query result count: {}", items.size());

            if (items.isEmpty()) {
                log.warn("Request not found for requestId: {}", requestId);
                return response.withStatusCode(404).withBody(objectMapper.writeValueAsString(new Response(false, "Request not found", null)));
            }

            Map<String, Object> requestData = convertDynamoItemToMap(items.get(0));

            log.info("Request details fetched successfully for requestId: {}", requestId);
            return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(new Response(true, "Request details fetched successfully", requestData)));

        } catch (Exception e) {
            log.error("Error while fetching request details: ", e);
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

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
