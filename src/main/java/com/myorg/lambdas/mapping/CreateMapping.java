package com.myorg.lambdas.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import com.myorg.Messages.Mapping;
import com.myorg.Messages.Response;
import com.myorg.Messages.enums.JournalAccessStatus;
import com.myorg.Messages.enums.MappingStatus;
import lombok.extern.java.Log;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log
public class CreateMapping implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received CreateMapping request: " + event.getBody());
            Mapping request = objectMapper.readValue(event.getBody(), Mapping.class);
            if (request.getClientId() == null || request.getTherapistId() == null) {
                log.warning("Missing required fields: clientId and therapistId are mandatory.");
                Response errorResponse = new Response(false, "ClientId and TherapistId are required", null);
                return response.withStatusCode(400).withBody(objectMapper.writeValueAsString(errorResponse));
            }

            String mappingId = UUID.randomUUID().toString();
            long createdAtTimestamp = Instant.now().toEpochMilli();
            MappingStatus mappingStatus = request.getMappingStatus() != null ? request.getMappingStatus() : MappingStatus.NOT_CONNECTED;
            JournalAccessStatus journalAccessStatus = request.getJournalAccessStatus() != null ? request.getJournalAccessStatus() : JournalAccessStatus.DENIED;
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("mappingId", AttributeValue.builder().s(request.getClientId() + "#" + request.getTherapistId()).build());
            item.put("clientId", AttributeValue.builder().s(request.getClientId()).build());
            item.put("therapistId", AttributeValue.builder().s(request.getTherapistId()).build());
            item.put("mappingStatus", AttributeValue.builder().s(mappingStatus.name()).build());
            item.put("journalAccessStatus", AttributeValue.builder().s(journalAccessStatus.name()).build());
            item.put("createdAt", AttributeValue.builder().n(String.valueOf(createdAtTimestamp)).build());
            log.info("Storing mapping in DynamoDB: " + item);
            dynamoDbClient.putItem(PutItemRequest.builder()
                    .tableName(CollectionNames.MAPPINGS)
                    .item(item)
                    .build());

            log.info("Mapping successfully stored with ID: " + mappingId);
            Mapping mappingResponse = Mapping.builder()
                    .mappingId(mappingId)
                    .clientId(request.getClientId())
                    .therapistId(request.getTherapistId())
                    .mappingStatus(mappingStatus)
                    .journalAccessStatus(journalAccessStatus)
                    .createdAt(createdAtTimestamp)
                    .build();

            Response successResponse = new Response(true, "Mapping created successfully", mappingResponse);
            return response.withStatusCode(201).withBody(objectMapper.writeValueAsString(successResponse));

        } catch (Exception e) {
            log.severe("Error occurred while creating mapping: " + e.getMessage());
            Response errorResponse = new Response(false, "Internal Server Error", null);
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
