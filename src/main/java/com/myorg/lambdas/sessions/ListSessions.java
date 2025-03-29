package com.myorg.lambdas.sessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import com.myorg.Messages.Session;
import lombok.extern.java.Log;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.QueryRequest;
import software.amazon.awssdk.services.dynamodb.model.QueryResponse;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import com.myorg.Messages.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Log
public class ListSessions implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final String INDEX_NAME = "therapist-session-time-index";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received ListSessions request: " + event.getQueryStringParameters());
            String therapistId = event.getQueryStringParameters() != null ? event.getQueryStringParameters().get("therapistId") : null;
            if (therapistId == null || therapistId.isEmpty()) {
                log.warning("Missing required parameter: therapistId");
                Response errorResponse = new Response(false, "Missing required parameter: therapistId", null);
                return response.withStatusCode(400).withBody(objectMapper.writeValueAsString(errorResponse));
            }
            log.info("Querying sessions for therapistId: " + therapistId);
            Map<String, AttributeValue> expressionValues = new HashMap<>();
            expressionValues.put(":therapistId", AttributeValue.builder().s(therapistId).build());
            QueryRequest queryRequest = QueryRequest.builder()
                    .tableName(CollectionNames.SESSIONS)
                    .indexName(INDEX_NAME)
                    .keyConditionExpression("therapist_id = :therapistId")
                    .expressionAttributeValues(expressionValues)
                    .scanIndexForward(true)
                    .build();
            QueryResponse queryResponse = dynamoDbClient.query(queryRequest);
            List<Map<String, AttributeValue>> items = queryResponse.items();
            log.info("Fetched " + items.size() + " sessions");
            List<Session> sessions = items.stream().map(item -> Session.builder()
                    .sessionId(item.getOrDefault("session_id", AttributeValue.builder().s("").build()).s())
                    .therapistId(item.getOrDefault("therapist_id", AttributeValue.builder().s("").build()).s())
                    .privateNotes(item.getOrDefault("privateNotes", AttributeValue.builder().s("").build()).s())
                    .sharedNotes(item.getOrDefault("sharedNotes", AttributeValue.builder().s("").build()).s())
                    .sessionDate(item.getOrDefault("sessionDate", AttributeValue.builder().s("").build()).s())
                    .startTime(item.getOrDefault("sessionStartTime", AttributeValue.builder().s("").build()).s())
                    .endTime(item.getOrDefault("sessionEndTime", AttributeValue.builder().s("").build()).s())
                    .clientId(item.getOrDefault("clientId", AttributeValue.builder().s("").build()).s())
                    .build()
            ).collect(Collectors.toList());
            Response successResponse = new Response(true, "Sessions fetched successfully", sessions);
            return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(successResponse));

        } catch (Exception e) {
            log.severe("Error while listing sessions: " + e.getMessage());
            Response errorResponse = new Response(false, "Internal Server Error", null);
            return response.withStatusCode(500).withBody(safeWrite(errorResponse));
        }
    }

    private String safeWrite(Response response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (Exception ex) {
            return "{\"status\": false, \"message\": \"Serialization error\"}";
        }
    }
}
