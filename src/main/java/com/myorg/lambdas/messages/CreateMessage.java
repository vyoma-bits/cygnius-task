package com.myorg.lambdas.messages;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.time.Instant;
import java.util.Map;

public class CreateMessage implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            JsonNode body = objectMapper.readTree(event.getBody());
            if (!body.has("sender") || !body.has("receiver") || !body.has("content")) {
                return response.withStatusCode(400).withBody("{\"error\": \"Missing required fields\"}");
            }
            String sender = body.get("sender").asText();
            String receiver = body.get("receiver").asText();
            String content = body.get("content").asText();
            String timestamp = Instant.now().toString();
            String messageId = receiver + "#" + sender + "#1234";
            Map<String, AttributeValue> item = Map.of(
                    "message_id", AttributeValue.builder().s(messageId).build(),
                    "sender", AttributeValue.builder().s(sender).build(),
                    "receiver", AttributeValue.builder().s(receiver).build(),
                    "timestamp", AttributeValue.builder().s(timestamp).build(),
                    "content", AttributeValue.builder().s(content).build()
            );
            dynamoDbClient.putItem(PutItemRequest.builder().tableName(CollectionNames.MESSAGES).item(item).build());
            return response.withStatusCode(201).withBody("{\"message\": \"Message created successfully\", \"message_id\": \"" + messageId + "\"}");
        } catch (Exception e) {
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }
}
