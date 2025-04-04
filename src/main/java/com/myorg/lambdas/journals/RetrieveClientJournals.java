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

import java.util.*;

/**
 * Lambda to retrieve all journals for a client, sorted by timestamp (latest first).
 */
@Slf4j
public class RetrieveClientJournals implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received event: {}", event);
            String clientId = event.getPathParameters() != null ? event.getPathParameters().get("clientId") : null;
            if (clientId == null || clientId.isEmpty()) {
                log.warn("Missing required parameter: clientId");
                return response.withStatusCode(400).withBody("{\"success\": false, \"message\": \"Missing required parameter: clientId\"}");
            }
            log.info("Querying journals for clientId: {}", clientId);
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":clientId", AttributeValue.builder().s(clientId).build());

            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(CollectionNames.JOURNALS)
                    .indexName("client-timestamp-index")
                    .keyConditionExpression("clientId = :clientId")
                    .expressionAttributeValues(expressionValues)
                    .scanIndexForward(false)
                    .build();

            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            List<Map<String, Object>> journals = new ArrayList<>();
/**
 * The below code is used to convert Dynamo Db item to into a Map.
 */
            for (Map<String, AttributeValue> item : queryResponse.items()) {
                Map<String, Object> journal = new HashMap<>();
                for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                    AttributeValue value = entry.getValue();
                    if (value.s() != null) journal.put(entry.getKey(), value.s());
                    else if (value.n() != null) journal.put(entry.getKey(), Integer.parseInt(value.n()));
                    else if (value.bool() != null) journal.put(entry.getKey(), value.bool());
                    else journal.put(entry.getKey(), null);
                }
                journals.add(journal);
            }

            log.info("Total journals retrieved: {}", journals.size());
            return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(new Response(true, "Journals retrieved successfully", journals)));

        } catch (Exception e) {
            log.error("Error while retrieving journals: ", e);
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
