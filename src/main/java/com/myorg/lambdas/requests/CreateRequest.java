package com.myorg.lambdas.requests;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import lombok.extern.java.Log;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import com.myorg.Messages.Response;
import com.myorg.Messages.Event;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Log
public class CreateRequest implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received CreateEventRequest request: " + event.getBody());
            Event request = objectMapper.readValue(event.getBody(), Event.class);
            if (request.getType() == null || request.getFrom() == null || request.getTo() == null || request.getMessage() == null) {
                log.warning("Missing required fields in request");
                Response errorResponse = new Response(false, "Fields are missing", null);
                return response.withStatusCode(400).withBody(objectMapper.writeValueAsString(errorResponse));
            }
            String requestId = UUID.randomUUID().toString();
            String createdAt = String.valueOf(Instant.now().toEpochMilli());
            Map<String, AttributeValue> item = new HashMap<>();
            item.put("request_id", AttributeValue.builder().s(requestId).build());
            item.put("created_at", AttributeValue.builder().s(createdAt).build());
            item.put("type", AttributeValue.builder().s(request.getType()).build());
            item.put("from", AttributeValue.builder().s(request.getFrom()).build());
            item.put("to", AttributeValue.builder().s(request.getTo()).build());
            item.put("message", AttributeValue.builder().s(request.getMessage()).build());
            item.put("isApproved", AttributeValue.builder().bool(false).build());
            log.info("Putting item into DynamoDB: " + item);
            dynamoDbClient.putItem(PutItemRequest.builder().tableName(CollectionNames.REQUESTS).item(item).build());
            log.info("Request saved to DynamoDB successfully with request ID: " + requestId);
            Event requestResponse = Event.builder()
                    .requestId(requestId)
                    .createdAt(createdAt)
                    .type(request.getType())
                    .from(request.getFrom())
                    .to(request.getTo())
                    .message(request.getMessage())
                    .isApproved(false)
                    .build();
            Response successResponse = new Response(true, "Request created successfully", requestResponse);
            return response.withStatusCode(201).withBody(objectMapper.writeValueAsString(successResponse));
        } catch (Exception e) {
            log.severe("Error occurred while creating request: " + e.getMessage());
            Response errorResponse = new Response(false, "Internal Server Error", null);
            try {
                return response.withStatusCode(500).withBody(objectMapper.writeValueAsString(errorResponse));
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
}
