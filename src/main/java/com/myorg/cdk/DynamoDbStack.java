package com.myorg.cdk;

import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.dynamodb.*;
import software.constructs.Construct;

public class DynamoDbStack extends Stack {
    public DynamoDbStack(final Construct scope, final String id,final StackProps props) {
        super(scope, id,props);

        // 1. Messages Table
        Table messagesTable = Table.Builder.create(this, "Messages")
                .tableName("Messages")
                .partitionKey(Attribute.builder().name("message_id").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("timestamp").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        messagesTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("messages-index")
                .partitionKey(Attribute.builder().name("sender").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("message_id").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        // 2. Clients Table
        Table clientsTable = Table.Builder.create(this, "Clients")
                .tableName("Clients")
                .partitionKey(Attribute.builder().name("client_id").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        clientsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("email-index")
                .partitionKey(Attribute.builder().name("email").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        // 3. Therapists Table
        Table therapistsTable = Table.Builder.create(this, "Therapists")
                .tableName("Therapists")
                .partitionKey(Attribute.builder().name("therapist_id").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        therapistsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("email-index")
                .partitionKey(Attribute.builder().name("email").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        therapistsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("specialization-createdAt-index")
                .partitionKey(Attribute.builder().name("specialization").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("created_at").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        therapistsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("address-index")
                .partitionKey(Attribute.builder().name("address").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("created_at").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        // 4. Journals Table
        Table journalsTable = Table.Builder.create(this, "Journals")
                .tableName("Journals")
                .partitionKey(Attribute.builder().name("journal_id").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("timestamp").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        journalsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("client-timestamp-index")
                .partitionKey(Attribute.builder().name("client_id").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("timestamp").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        journalsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("emotion-intensity-index")
                .partitionKey(Attribute.builder().name("emotion").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("intensity").type(AttributeType.NUMBER).build())
                .projectionType(ProjectionType.ALL)
                .build());

        // 5. Requests Table
        Table requestsTable = Table.Builder.create(this, "Requests")
                .tableName("Requests")
                .partitionKey(Attribute.builder().name("request_id").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("created_at").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        requestsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("from-type-index")
                .partitionKey(Attribute.builder().name("from").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("type").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        requestsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("to-type-index")
                .partitionKey(Attribute.builder().name("to").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("type").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        // 6. Sessions Table
        Table sessionsTable = Table.Builder.create(this, "Sessions")
                .tableName("Sessions")
                .partitionKey(Attribute.builder().name("session_id").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("therapist_id").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        sessionsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("therapist-session-time-index")
                .partitionKey(Attribute.builder().name("therapist_id").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("sessionDate#sessionStartTime").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        sessionsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("client-session-time-index")
                .partitionKey(Attribute.builder().name("client_id").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("sessionDate#sessionStartTime").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        sessionsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("client-therapist-datetime-index")
                .partitionKey(Attribute.builder().name("client_id#therapist_id").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("sessionDate#sessionStartTime").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        sessionsTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("therapist-client-datetime-index")
                .partitionKey(Attribute.builder().name("therapist_id#client_id").type(AttributeType.STRING).build())
                .sortKey(Attribute.builder().name("sessionDate#sessionStartTime").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());

        // 7. Users Table
        Table usersTable = Table.Builder.create(this, "Users")
                .tableName("Users")
                .partitionKey(Attribute.builder().name("user_id").type(AttributeType.STRING).build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        usersTable.addGlobalSecondaryIndex(GlobalSecondaryIndexProps.builder()
                .indexName("email-index")
                .partitionKey(Attribute.builder().name("email").type(AttributeType.STRING).build())
                .projectionType(ProjectionType.ALL)
                .build());
    }
}