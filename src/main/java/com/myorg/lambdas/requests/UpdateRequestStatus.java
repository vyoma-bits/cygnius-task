package com.myorg.lambdas.requests;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.myorg.CollectionNames;
import com.myorg.Messages.Event;
import com.myorg.Messages.Mapping;
import com.myorg.Messages.Response;
import com.myorg.Messages.Session;
import com.myorg.Messages.UpdateRequest;
import com.myorg.Messages.enums.JournalAccessStatus;
import com.myorg.Messages.enums.MappingStatus;
import lombok.extern.java.Log;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.UpdateItemRequest;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This lambda is used to update the request(If the request has been approved, it updates the corresponding request entry )
 */
@Log
public class UpdateRequestStatus implements RequestHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> {
    private final DynamoDbClient dynamoDbClient = DynamoDbClient.create();
    private final LambdaClient lambdaClient = LambdaClient.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SESSION_BOOKING = "SESSION_BOOKING";
    private static final String MAPPING = "MAPPING";
    private static final String JOURNAL_ACCESS = "JOURNAL_ACCESS";
    private final String GET_REQUEST_DETAILS_LAMBDA = System.getenv("GET_REQUEST_DETAILS_LAMBDA");
    private final String CREATE_MAPPING_LAMBDA = System.getenv("CREATE_MAPPING_LAMBDA");
    private final String SESSION_UPDATE_LAMBDA = System.getenv("SESSION_UPDATE_LAMBDA");
    private final String UPDATE_MAPPING_LAMBDA = System.getenv("UPDATE_MAPPING_LAMBDA");

    @Override
    public APIGatewayProxyResponseEvent handleRequest(APIGatewayProxyRequestEvent event, Context context) {
        APIGatewayProxyResponseEvent response = new APIGatewayProxyResponseEvent();
        try {
            log.info("Received request to update request status: " + event.getQueryStringParameters());

            String requestId = event.getPathParameters() != null ? event.getPathParameters().get("requestId") : null;

            String isApprovedStr = event.getQueryStringParameters().get("isApproved");

            if (requestId == null || isApprovedStr == null) {
                return response.withStatusCode(400).withBody("{\"error\":\"Missing requestId or isApproved in query params\"}");
            }

            boolean isApproved = Boolean.parseBoolean(isApprovedStr);
            Event requestDetails = getRequestDetails(requestId);
            if (requestDetails == null) {
                return response.withStatusCode(404).withBody("{\"error\":\"Request not found\"}");
            }
            log.info(requestDetails.toString());
            updateRequestStatus(requestId, isApproved);

            if (isApproved) {
                switch (requestDetails.getType()) {
                    case SESSION_BOOKING:
                        processSessionBooking(requestDetails);
                        break;
                    case MAPPING:
                        processMapping(requestDetails);
                        break;
                    case JOURNAL_ACCESS:
                        processJournalAccess(requestDetails);
                        break;
                    default:
                        return response.withStatusCode(400).withBody("{\"error\":\"Unknown request type\"}");
                }
            }

            Response successResponse = new Response(true, "Request status updated successfully", null);
            return response.withStatusCode(200).withBody(objectMapper.writeValueAsString(successResponse));

        } catch (Exception e) {
            log.severe("Error occurred while updating request status: " + e.getMessage());
            return response.withStatusCode(500).withBody("{\"error\":\"" + e.getMessage() + "\"}");
        }
    }

    private Event getRequestDetails(String requestId) throws Exception {
        try {
            APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
            Map<String, String> pathParameters = new HashMap<>();
            pathParameters.put("requestId", requestId);
            requestEvent.setPathParameters(pathParameters);

            String requestJson = objectMapper.writeValueAsString(requestEvent);

            InvokeRequest invokeRequest = InvokeRequest.builder()
                    .functionName(GET_REQUEST_DETAILS_LAMBDA)
                    .payload(SdkBytes.fromUtf8String(requestJson))
                    .build();

            InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);
            String responseJson = invokeResponse.payload().asUtf8String();

            APIGatewayProxyResponseEvent lambdaResponse = objectMapper.readValue(responseJson, APIGatewayProxyResponseEvent.class);
            Response event = objectMapper.readValue(lambdaResponse.getBody(), Response.class);
            return objectMapper.convertValue(event.getData(), Event.class);
        } catch (Exception e) {
            log.severe("Error invoking GetRequestDetailsLambda: " + e.getMessage());
            throw e;
        }
    }

    private void updateRequestStatus(String requestId, boolean isApproved) {
        Map<String, AttributeValue> key = Map.of("requestId", AttributeValue.builder().s(requestId).build());
        Map<String, String> expressionAttributeNames = Map.of("#isApproved", "isApproved");
        Map<String, AttributeValue> expressionAttributeValues = Map.of(":val", AttributeValue.builder().bool(isApproved).build());

        UpdateItemRequest updateRequest = UpdateItemRequest.builder()
                .tableName(CollectionNames.REQUESTS)
                .key(key)
                .updateExpression("SET #isApproved = :val")
                .expressionAttributeNames(expressionAttributeNames)
                .expressionAttributeValues(expressionAttributeValues)
                .build();

        dynamoDbClient.updateItem(updateRequest);
        log.info("Updated request status for requestId: " + requestId + " to isApproved: " + isApproved);
    }

    private void processSessionBooking(Event requestDetails) throws Exception {
        log.info("Processing SESSION_BOOKING request");

        String sessionId = requestDetails.getMessage();
        String therapistId = requestDetails.getTo();

        if (sessionId == null || therapistId == null) {
            throw new IllegalArgumentException("Missing required parameters");
        }

        UpdateRequest<Session> updateRequest = new UpdateRequest<>();
        Session session = new Session();
        session.setClientId(requestDetails.getFrom());
        updateRequest.setEntity(session);
        updateRequest.setFieldsToUpdate(List.of("clientId"));

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        Map<String, String> pathParameters = new HashMap<>();
        pathParameters.put("sessionId", sessionId);
        requestEvent.setPathParameters(pathParameters);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("therapistId", therapistId);
        requestEvent.setQueryStringParameters(queryParams);
        requestEvent.setBody(objectMapper.writeValueAsString(updateRequest));
        String requestJson = objectMapper.writeValueAsString(requestEvent);
        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(SESSION_UPDATE_LAMBDA)
                .payload(SdkBytes.fromUtf8String(requestJson))
                .build();

        InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);

        if (invokeResponse.functionError() != null) {
            throw new Exception("Error invoking UpdateSessionLambda: " + invokeResponse.functionError());
        }

        log.info("Successfully invoked UpdateSessionLambda");
    }

    private void processMapping(Event requestDetails) throws Exception {
        log.info("Processing MAPPING request");

        String clientId = requestDetails.getFrom();
        String therapistId = requestDetails.getTo();

        if (clientId == null || therapistId == null) {
            throw new IllegalArgumentException("Missing required parameters");
        }
        Mapping mapping = Mapping.builder()
                .clientId(clientId)
                .therapistId(therapistId)
                .mappingStatus(MappingStatus.CONNECTED)
                .journalAccessStatus(JournalAccessStatus.DENIED)
                .build();

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        requestEvent.setBody(objectMapper.writeValueAsString(mapping));
        String requestJson = objectMapper.writeValueAsString(requestEvent);

        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(CREATE_MAPPING_LAMBDA)
                .payload(SdkBytes.fromUtf8String(requestJson))
                .build();

        InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);

        if (invokeResponse.functionError() != null) {
            throw new Exception("Error invoking CreateMappingLambda: " + invokeResponse.functionError());
        }

        log.info("Successfully invoked CreateMappingLambda");
    }
    private void processJournalAccess(Event requestDetails) throws Exception {
        log.info("Processing JOURNAL_ACCESS request");

        String clientId = requestDetails.getFrom();
        String therapistId = requestDetails.getTo();

        if (clientId == null || therapistId == null) {
            throw new IllegalArgumentException("Missing required parameters");
        }

        UpdateRequest<Mapping> updateRequest = new UpdateRequest<>();
        Mapping mapping = new Mapping();
        mapping.setJournalAccessStatus(JournalAccessStatus.GRANTED);
        updateRequest.setEntity(mapping);
        updateRequest.setFieldsToUpdate(List.of("journalAccessStatus"));

        APIGatewayProxyRequestEvent requestEvent = new APIGatewayProxyRequestEvent();
        Map<String, String> pathParameters = new HashMap<>();
        pathParameters.put("mappingId", clientId);
        requestEvent.setPathParameters(pathParameters);
        Map<String, String> queryParams = new HashMap<>();
        queryParams.put("therapistId", therapistId);
        requestEvent.setQueryStringParameters(queryParams);
        requestEvent.setBody(objectMapper.writeValueAsString(updateRequest));
        String requestJson = objectMapper.writeValueAsString(requestEvent);
        InvokeRequest invokeRequest = InvokeRequest.builder()
                .functionName(UPDATE_MAPPING_LAMBDA)
                .payload(SdkBytes.fromUtf8String(requestJson))
                .build();

        InvokeResponse invokeResponse = lambdaClient.invoke(invokeRequest);

        if (invokeResponse.functionError() != null) {
            throw new Exception("Error invoking UpdateMappingLambda: " + invokeResponse.functionError());
        }

        log.info("Successfully invoked UpdateMappingLambda");
    }
}
