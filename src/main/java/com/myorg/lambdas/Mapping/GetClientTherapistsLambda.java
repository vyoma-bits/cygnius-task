package com.myorg.lambdas.Mapping;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.Messages.TherapistResponse;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;

import java.util.*;

public class GetClientTherapistsLambda implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {

    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private static final String CLIENTS_TABLE = "Clients";
    private static final String THERAPISTS_TABLE = "Therapists";
    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();

        try {
            // Get client ID from path parameters
            String clientId = event.getPathParameters() != null ? event.getPathParameters().get("clientId") : null;
            if (clientId == null) {
                return response.withStatusCode(400).withBody("{\"error\": \"clientId is required\"}");
            }

            // Get therapist IDs associated with the client
            GetItemRequest clientRequest = GetItemRequest.builder()
                    .tableName(CLIENTS_TABLE)
                    .key(Map.of("client_id", AttributeValue.builder().s(clientId).build()))
                    .build();

            var clientResult = dynamoDbClient.getItem(clientRequest);
            if (!clientResult.hasItem()) {
                return response.withStatusCode(404).withBody("{\"error\": \"Client not found\"}");
            }

            var therapistIdsAttr = clientResult.item().get("therapist_ids");
            if (therapistIdsAttr == null || therapistIdsAttr.l().isEmpty()) {
                return response.withStatusCode(200).withBody("{\"therapists\": []}");
            }

            // Fetch therapist details
            List<TherapistResponse> therapists = getTherapistDetails(therapistIdsAttr.l());

            return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(Map.of("therapists", therapists)));

        } catch (Exception e) {
            return response.withStatusCode(500).withBody("{\"error\": \"" + e.getMessage() + "\"}");
        }
    }

    private List<TherapistResponse> getTherapistDetails(List<AttributeValue> therapistIds) {
        List<TherapistResponse> therapists = new ArrayList<>();

        for (AttributeValue therapistIdAttr : therapistIds) {
            String therapistId = therapistIdAttr.s();

            GetItemRequest therapistRequest = GetItemRequest.builder()
                    .tableName(THERAPISTS_TABLE)
                    .key(Map.of("therapist_id", AttributeValue.builder().s(therapistId).build()))
                    .build();

            var therapistResult = dynamoDbClient.getItem(therapistRequest);
            if (therapistResult.hasItem()) {
                var item = therapistResult.item();
                therapists.add(new TherapistResponse(
                        Integer.parseInt(item.get("therapist_id").n()),
                        item.get("email").s(),
                        item.get("first_name").s(),
                        item.get("last_name").s(),
                        item.get("specialization").s(),
                        item.get("address").s(),
                        item.get("created_at").s()
                ));
            }
        }
        return therapists;
    }
}
