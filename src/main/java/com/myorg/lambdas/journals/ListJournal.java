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
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This lambda reads a particualr session details
 */
@Slf4j
public class ListJournal implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received event: {}", event);
            String journalId = event.getQueryStringParameters() != null ? event.getQueryStringParameters().get("journalId") : null;
            if (journalId == null || journalId.isEmpty()) {
                log.warn("Missing required parameter: sessionId");
                return response.withStatusCode(400).withBody(objectMapper.writeValueAsString(new Response(false, "Missing required parameter: journalId", null)));
            }
            log.info("Querying for sessionId: {}", journalId);
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":journalId", AttributeValue.builder().s(journalId).build());
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(CollectionNames.JOURNALS)
                    .keyConditionExpression("journalId = :journalId")
                    .expressionAttributeValues(expressionValues)
                    .build();
            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            List<Map<String, AttributeValue>> items = queryResponse.items();
            log.info("Query result count: {}", items.size());

            if (items.isEmpty()) {
                log.warn("journal not found for journalId: {}", journalId);
                return response.withStatusCode(404).withBody(objectMapper.writeValueAsString(new Response(false, "journal not found", null)));
            }
            Map<String, Object> sessionData = convertDynamoItemToMap(items.get(0));
            log.info("journal fetched successfully for journalId: {}", journalId);
            return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(new Response(true, "journal fetched successfully", sessionData)));

        } catch (Exception e) {
            log.error("Error while reading session: ", e);
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    /**
     * This function is used to convert Dynamo Db item to into a Map.
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
