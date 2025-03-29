package com.myorg.lambdas.sessions;

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
public class ReadSession implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received event: {}", event);
            String sessionId = event.getQueryStringParameters() != null ? event.getQueryStringParameters().get("sessionId") : null;

            if (sessionId == null || sessionId.isEmpty()) {
                log.warn("Missing required parameter: sessionId");
                return response.withStatusCode(400).withBody(objectMapper.writeValueAsString(new Response(false, "Missing required parameter: sessionId", null)));
            }

            log.info("Querying for sessionId: {}", sessionId);
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":sessionId", AttributeValue.builder().s(sessionId).build());

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(CollectionNames.SESSIONS)
                    .keyConditionExpression("session_id = :sessionId")
                    .expressionAttributeValues(expressionValues)
                    .build();

            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            List<Map<String, AttributeValue>> items = queryResponse.items();

            log.info("Query result count: {}", items.size());

            if (items.isEmpty()) {
                log.warn("Session not found for sessionId: {}", sessionId);
                return response.withStatusCode(404).withBody(objectMapper.writeValueAsString(new Response(false, "Session not found", null)));
            }

            Map<String, Object> sessionData = convertDynamoItemToMap(items.get(0));

            log.info("Session fetched successfully for sessionId: {}", sessionId);
            return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(new Response(true, "Session fetched successfully", sessionData)));

        } catch (Exception e) {
            log.error("Error while reading session: ", e);
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
