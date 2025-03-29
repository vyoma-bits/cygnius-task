package com.myorg.lambdas.messages;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;

import java.util.*;

@Slf4j
public class GetMessageHistory implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final String INDEX_NAME = "messages-index";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            Map<String, String> params = event.getQueryStringParameters();
            if (params == null || !params.containsKey("sender") || !params.containsKey("receiver")) {
                return response.withStatusCode(400).withBody("{\"error\": \"Missing required parameters\"}");
            }
            String senderId = params.get("sender");
            String receiverId = params.get("receiver");
            List<Map<String, AttributeValue>> result = queryMessages(senderId, receiverId);
            List<Map<String, String>> simpleResult = new ArrayList<>();
            for (Map<String, AttributeValue> item : result) {
                Map<String, String> simpleItem = new HashMap<>();
                for (Map.Entry<String, AttributeValue> entry : item.entrySet()) {
                    simpleItem.put(entry.getKey(), entry.getValue().s());
                }
                simpleResult.add(simpleItem);
            }
            String jsonResponse = objectMapper.writeValueAsString(simpleResult);
            return response.withStatusCode(200).withBody(jsonResponse);

        } catch (Exception e) {
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private List<Map<String, AttributeValue>> queryMessages(String sender, String receiver) {
        String prefix = receiver + "#" + sender;
        QueryRequest request = QueryRequest.builder()
                .tableName(CollectionNames.MESSAGES)
                .indexName(INDEX_NAME)
                .keyConditionExpression("sender = :sender AND begins_with(message_id, :prefix)")
                .expressionAttributeValues(Map.of(
                        ":sender", AttributeValue.builder().s(sender).build(),
                        ":prefix", AttributeValue.builder().s(prefix).build()
                ))
                .build();
        QueryResponse response = dynamoDbClient.query(request);
        return response.items();
    }
}