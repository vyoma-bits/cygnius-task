package com.myorg.lambdas.mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import com.myorg.Messages.Mapping;
import com.myorg.Messages.Response;
import com.myorg.Messages.UpdateRequest;
import lombok.extern.java.Log;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Log
public class UpdateMapping implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received event: " + event);

            Map<String, String> pathParams = event.getPathParameters();
            String mappingId = pathParams != null ? pathParams.get("mappingId") : null;

            Map<String, String> queryParams = event.getQueryStringParameters();

            if (mappingId == null) return response.withStatusCode(400).withBody(
                    objectMapper.writeValueAsString(new Response(false, "Missing mappingId parameter", null)));
            log.info("Mapping ID: " + mappingId);

            UpdateRequest<Mapping> updateRequest = objectMapper.readValue(event.getBody(),
                    objectMapper.getTypeFactory().constructParametricType(UpdateRequest.class, Mapping.class));

            List<String> fieldsToUpdate = updateRequest.getFieldsToUpdate();
            Mapping entity = updateRequest.getEntity();
            log.info("Fields to update: " + fieldsToUpdate);
            log.info("Entity values: " + objectMapper.writeValueAsString(entity));

            if (fieldsToUpdate == null || fieldsToUpdate.isEmpty()) {
                return response.withStatusCode(400).withBody(
                        objectMapper.writeValueAsString(new Response(false, "No fields specified to update", null)));
            }

            Map<String, AttributeValue> expressionAttributeValues = new HashMap<>();
            Map<String, String> expressionAttributeNames = new HashMap<>();
            StringBuilder updateExpression = new StringBuilder("SET ");
            int count = 0;

            for (String field : fieldsToUpdate) {
                String attributeName = "#field" + count;
                String attributeValue = ":val" + count;

                switch (field) {
                    case "mappingStatus":
                        expressionAttributeValues.put(attributeValue, AttributeValue.builder().s(entity.getMappingStatus().name()).build());
                        break;
                    case "journalAccessStatus":
                        expressionAttributeValues.put(attributeValue, AttributeValue.builder().s(entity.getJournalAccessStatus().name()).build());
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
            keyMap.put("mappingId", AttributeValue.builder().s(mappingId).build());

            UpdateItemRequest updateRequestDynamo = UpdateItemRequest.builder()
                    .tableName(CollectionNames.MAPPINGS)
                    .key(keyMap)
                    .updateExpression(updateExpression.toString())
                    .expressionAttributeValues(expressionAttributeValues)
                    .expressionAttributeNames(expressionAttributeNames)
                    .build();

            dynamoDbClient.updateItem(updateRequestDynamo);

            return response.withStatusCode(200).withBody(
                    objectMapper.writeValueAsString(new Response(true, "Mapping updated successfully", null))
            );
        } catch (Exception e) {
            log.severe("Error updating session: " + e.getMessage());
            e.printStackTrace();
            try {
                return response.withStatusCode(500).withBody(
                        objectMapper.writeValueAsString(new Response(false, "Internal error", null)));
            } catch (JsonProcessingException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

}
