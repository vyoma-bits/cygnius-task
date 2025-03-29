package com.myorg.lambdas.sessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import lombok.extern.java.Log;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import com.myorg.Messages.Session;
import com.myorg.Messages.Response;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log
public class CreateSession implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received Session Creation request: " + event.getBody());
            Session sessionRequest = objectMapper.readValue(event.getBody(), Session.class);
            if (sessionRequest.getTherapistId() == null || sessionRequest.getSessionDate() == null || sessionRequest.getStartTime() == null || sessionRequest.getEndTime() == null) {
                log.warning("Missing required fields in request");
                Response errorResponse = new Response(false, "Fields are missing", null);
                return response.withStatusCode(400).withBody(objectMapper.writeValueAsString(errorResponse));
            }
            String sessionId = UUID.randomUUID().toString();
            log.info("Generated session ID: " + sessionId);
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("session_id", AttributeValue.builder().s(sessionId).build());
            item.put("therapist_id", AttributeValue.builder().s(sessionRequest.getTherapistId()).build());
            item.put("sessionDate", AttributeValue.builder().s(sessionRequest.getSessionDate()).build());
            item.put("sessionStartTime", AttributeValue.builder().s(sessionRequest.getStartTime()).build());
            item.put("sessionEndTime", AttributeValue.builder().s(sessionRequest.getEndTime()).build());
            item.put("privateNotes", AttributeValue.builder().s(sessionRequest.getPrivateNotes() != null ? sessionRequest.getPrivateNotes() : "").build());
            item.put("sharedNotes", AttributeValue.builder().s(sessionRequest.getSharedNotes() != null ? sessionRequest.getSharedNotes() : "").build());
            item.put("created_at", AttributeValue.builder().s(String.valueOf(Instant.now().toEpochMilli())).build());
            item.put("isBooked", AttributeValue.builder().bool(false).build());
            item.put("isCompleted", AttributeValue.builder().bool(false).build());
            item.put("sessionDate#sessionStartTime",AttributeValue.builder().s(sessionRequest.getSessionDate()+"#"+sessionRequest.getStartTime()).build());
            dynamoDbClient.putItem(PutItemRequest.builder().tableName(CollectionNames.SESSIONS).item(item).build());
            log.info("Session saved to DynamoDB successfully with session ID: " + sessionId);
            Session session = Session.builder()
                    .sessionId(sessionId)
                    .therapistId(sessionRequest.getTherapistId())
                    .privateNotes(sessionRequest.getPrivateNotes())
                    .sharedNotes(sessionRequest.getSharedNotes())
                    .sessionDate(sessionRequest.getSessionDate())
                    .startTime(sessionRequest.getStartTime())
                    .endTime(sessionRequest.getEndTime())
                    .build();

            Response successResponse = new Response(true, "Session created successfully", session);
            return response.withStatusCode(201).withBody(objectMapper.writeValueAsString(successResponse));
        } catch (Exception e) {
            log.severe("Error occurred while creating session: " + e.getMessage());
            Response errorResponse = new Response(false, "Internal Server Error", null);
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
