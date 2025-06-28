package com.myorg.lambdas.journals;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import com.myorg.Messages.Journal;
import com.myorg.Messages.Response;
import lombok.extern.java.Log;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log
public class CreateJournal implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received Journal Creation request: " + event.getBody());
            Journal journalRequest = objectMapper.readValue(event.getBody(), Journal.class);
            if ( journalRequest.getJournalGroupId() == null ||journalRequest.getClientId() == null || journalRequest.getEmotion() == null ||
                    journalRequest.getFeeling() == null || journalRequest.getIntensity() == null) {
                log.warning("Missing required fields in journal request");
                Response errorResponse = new Response(false, "Required fields are missing", null);
                return response.withStatusCode(400).withBody(objectMapper.writeValueAsString(errorResponse));
            }
            String journalId = UUID.randomUUID().toString();
            log.info("Generated journal ID: " + journalId);
            String timestamp = String.valueOf(Instant.now().toEpochMilli());
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("journalGroupId", AttributeValue.builder().s(journalRequest.getJournalGroupId()).build());
            item.put("journalId", AttributeValue.builder().s(journalId).build());
            item.put("clientId", AttributeValue.builder().s(journalRequest.getClientId()).build());
            item.put("emotion", AttributeValue.builder().s(journalRequest.getEmotion()).build());
            item.put("feeling", AttributeValue.builder().s(journalRequest.getFeeling()).build());
            item.put("intensity", AttributeValue.builder().n(journalRequest.getIntensity().toString()).build());
            item.put("notes", AttributeValue.builder().s(journalRequest.getNotes() != null ? journalRequest.getNotes() : "").build());
            item.put("timestamp", AttributeValue.builder().s(timestamp).build());
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(CollectionNames.JOURNALS)
                    .item(item)
                    .build());

            log.info("Journal saved to DynamoDB successfully with journal ID: " + journalId);
            Journal journalResponse = Journal.builder()
                    .journalId(journalId)
                    .clientId(journalRequest.getClientId())
                    .emotion(journalRequest.getEmotion())
                    .feeling(journalRequest.getFeeling())
                    .intensity(journalRequest.getIntensity())
                    .notes(journalRequest.getNotes())
                    .timestamp(timestamp)
                    .build();

            Response successResponse = new Response(true, "Journal created successfully", journalResponse);
            return response.withStatusCode(201).withBody(objectMapper.writeValueAsString(successResponse));

        } catch (Exception e) {
            log.severe("Error occurred while creating journal: " + e.getMessage());
            Response errorResponse = new Response(false, "Internal Server Error", null);
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}