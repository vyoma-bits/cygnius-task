package com.myorg.lambdas.sessions;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import com.myorg.Messages.Response;
import com.myorg.Messages.Session;
import com.myorg.Messages.UpdateRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.java.Log;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

/**
 * This lambda updates the session details
 */
@Log
public class UpdateSession implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received event: " + event);
            Map<String, String> pathParams = event.getPathParameters();
            String sessionId = pathParams != null ? pathParams.get("sessionId") : null;
            Map<String, String> queryParams = event.getQueryStringParameters();
            String therapistId = queryParams != null ? queryParams.get("therapistId") : null;
            if (sessionId == null || therapistId == null) {
                return response.withStatusCode(400).withBody(
                        objectMapper.writeValueAsString(new Response(false, "Missing required parameters", null))
                );
            }
            log.info("Session ID: " + sessionId + ", Therapist ID: " + therapistId);
            UpdateRequest<Session> updateRequest = objectMapper.readValue(event.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(UpdateRequest.class, Session.class));
            List<String> fieldsToUpdate = updateRequest.getFieldsToUpdate();
            Session entity = updateRequest.getEntity();
            if (fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
                return response.withStatusCode(400).withBody(
                        objectMapper.writeValueAsString(new Response(false, "No fields specified for update", null))
                );
            }
            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            Map<String, String> expressionAttributeNames = new HashMap<>();
            StringBuilder updateExpression = new StringBuilder("SET ");
            int count = 0;
            for (String field : fieldsToUpdate) {
                String attributeName = "#field" + count;
                String attributeValue = ":val" + count;

                switch (field) {
                    case "privateNotes":
                        expressionAttributeValues.put(attributeValue, AttributeValue.builder().s(entity.getPrivateNotes()).build());
                        break;
                    case "sharedNotes":
                        expressionAttributeValues.put(attributeValue, AttributeValue.builder().s(entity.getSharedNotes()).build());
                        break;
                    case "sessionDate":
                        expressionAttributeValues.put(attributeValue, AttributeValue.builder().s(entity.getSessionDate()).build());
                        break;
                    case "startTime":
                        expressionAttributeValues.put(attributeValue, AttributeValue.builder().s(entity.getStartTime()).build());
                        break;
                    case "endTime":
                        expressionAttributeValues.put(attributeValue, AttributeValue.builder().s(entity.getEndTime()).build());
                        break;
                    case "clientId":
                        expressionAttributeValues.put(attributeValue, AttributeValue.builder().s(entity.getClientId()).build());
                        break;
                    default:
                        continue;
                }
                expressionAttributeNames.put(attributeName, field);
                if (count > 0) {
                    updateExpression.append(", ");
                }
                updateExpression.append(attributeName).append(" = ").append(attributeValue);
                count++;
            }

            if (expressionAttributeValues.isEmpty()) {
                return response.withStatusCode(400).withBody(
                        objectMapper.writeValueAsString(new Response(false, "No valid fields to update", null))
                );
            }

            Map<String, AttributeValue> keyMap = new HashMap<>();
            keyMap.put("session_id", AttributeValue.builder().s(sessionId).build());
            keyMap.put("therapist_id", AttributeValue.builder().s(therapistId).build());

            UpdateItemRequest updateRequestDynamo = UpdateItemRequest.builder()
                    .tableName(CollectionNames.SESSIONS)
                    .key(keyMap)
                    .updateExpression(updateExpression.toString())
                    .expressionAttributeValues(expressionAttributeValues)
                    .expressionAttributeNames(expressionAttributeNames)
                    .build();

            dynamoDbClient.updateItem(updateRequestDynamo);
            return response.withStatusCode(200).withBody(
                    objectMapper.writeValueAsString(new Response(true, "Session updated successfully", null))
            );
        } catch (Exception e) {
            log.severe("Error updating session: " + e.getMessage());
            e.printStackTrace();
            return response.withStatusCode(500).withBody(errorResponse(e));
        }
    }

    private String errorResponse(Exception e) {
        try {
            return objectMapper.writeValueAsString(new Response(false, "Error updating session", e.getMessage()));
        } catch (Exception ex) {
            return "{\"status\":false,\"message\":\"Internal error\",\"data\":null}";
        }
    }
}