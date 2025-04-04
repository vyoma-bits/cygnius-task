package com.myorg.cdk;

import com.myorg.utils.LambdaFactory;
import lombok.Getter;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.iam.*;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.Permission;
import software.constructs.Construct;

import java.util.List;

@Getter
public class LambdaStack extends Stack {

    private final Function messagesLambda;
    private final Function messageHistoryLambda;
    private final Function createSessionLambda;
    private final Function updateSessionLambda;
    private final Function deleteSessionLambda;
    private final Function listSessionsLambda;
    private final Function readSessionLambda;
    private final Function createRequestLambda;
    private final Function updateRequestStatusLambda;
    private final Function createMappingLambda;
    private final Function updateMappingLambda;
    private final Function listMappingLambda;
    private final Function deleteMappingLambda;
    private final Function getRequestLambda;
    private final Function createJournalLambda;
    private final Function listJournalLambda;
    private final Function deleteClientJournalLambda;
    private final Function retrieveClientJournalsLambda;

    public LambdaStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        // Table and index ARNs
        String sessionTableArn = "arn:aws:dynamodb:ap-south-1:897729110707:table/Sessions";
        List<String> sessionIndexArns = List.of(
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Sessions/index/therapist-session-time-index",
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Sessions/index/client-session-time-index",
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Sessions/index/client-therapist-datetime-index",
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Sessions/index/therapist-client-datetime-index"
        );

        List<String> mappingIndexArns = List.of(
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Mappings/index/therapist-client-index",
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Mappings/index/mapping-status-index",
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Mappings/index/journal-access-index",
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Mappings/index/client-therapists-index"
        );

        List<String> journalIndexArns = List.of(
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Journals/index/client-timestamp-index",
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Journals/index/emotion-intensity-index"
        );

        List<String> messageIndexArns = List.of(
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Messages/index/messages-index"
        );

        List<String> requestIndexArns = List.of(
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Requests/index/from-type-index",
                "arn:aws:dynamodb:ap-south-1:897729110707:table/Requests/index/to-type-index"
        );

        String mappingTableArn = "arn:aws:dynamodb:ap-south-1:897729110707:table/Mappings";
        String journalTableArn = "arn:aws:dynamodb:ap-south-1:897729110707:table/Journals";
        String messageTableArn = "arn:aws:dynamodb:ap-south-1:897729110707:table/Messages";
        String requestTableArn = "arn:aws:dynamodb:ap-south-1:897729110707:table/Requests";

        // IAM Roles
        Role messagesCrudRole = createCrudRole("MessagesCrudRole", messageTableArn);
        Role sessionsCrudRole = createCrudRole("SessionsCrudRole", sessionTableArn);
        Role sessionsReadRole = createReadOnlyRoleWithIndexes("SessionsReadRole", sessionTableArn, sessionIndexArns);
        Role requestsCrudRole = createCrudRole("RequestsCrudRole", requestTableArn);
        Role mappingCrudRole = createCrudRole("MappingCrudRole", mappingTableArn);
        Role mappingReadRole = createReadOnlyRole("MappingReadRole", mappingTableArn);
        Role journalCrudRole = createCrudRole("JournalCrudRole", journalTableArn);
        Role journalReadRole = createReadOnlyRole("JournalReadRole", journalTableArn);

        // Messages Lambdas
        messagesLambda = LambdaFactory.createLambda(this, "MessagesLambda",
                "com.myorg.lambdas.messages.CreateMessage::handleRequest",
                "target/aws-cdk-final-0.1.jar", messagesCrudRole);

        messageHistoryLambda = LambdaFactory.createLambda(this, "MessageHistoryLambda",
                "com.myorg.lambdas.messages.GetMessageHistory::handleRequest",
                "target/aws-cdk-final-0.1.jar", messagesCrudRole);

        // Sessions Lambdas
        createSessionLambda = LambdaFactory.createLambda(this, "CreateSessionLambda",
                "com.myorg.lambdas.sessions.CreateSession::handleRequest",
                "target/aws-cdk-final-0.1.jar", sessionsCrudRole);

        updateSessionLambda = LambdaFactory.createLambda(this, "UpdateSessionLambda",
                "com.myorg.lambdas.sessions.UpdateSession::handleRequest",
                "target/aws-cdk-final-0.1.jar", sessionsCrudRole);

        deleteSessionLambda = LambdaFactory.createLambda(this, "DeleteSessionLambda",
                "com.myorg.lambdas.sessions.DeleteSession::handleRequest",
                "target/aws-cdk-final-0.1.jar", sessionsCrudRole);

        listSessionsLambda = LambdaFactory.createLambda(this, "ListSessionsLambda",
                "com.myorg.lambdas.sessions.ListSessions::handleRequest",
                "target/aws-cdk-final-0.1.jar", sessionsReadRole);

        readSessionLambda = LambdaFactory.createLambda(this, "ReadSessionLambda",
                "com.myorg.lambdas.sessions.ReadSession::handleRequest",
                "target/aws-cdk-final-0.1.jar", sessionsReadRole);

        // Requests Lambdas
        createRequestLambda = LambdaFactory.createLambda(this, "CreateRequestLambda",
                "com.myorg.lambdas.requests.CreateRequest::handleRequest",
                "target/aws-cdk-final-0.1.jar", requestsCrudRole);

        updateRequestStatusLambda = LambdaFactory.createLambda(this, "UpdateRequestStatusLambda",
                "com.myorg.lambdas.requests.UpdateRequestStatus::handleRequest",
                "target/aws-cdk-final-0.1.jar", requestsCrudRole);

        getRequestLambda = LambdaFactory.createLambda(this, "GetRequestDetailsLambda",
                "com.myorg.lambdas.requests.GetRequestDetails::handleRequest",
                "target/aws-cdk-final-0.1.jar", requestsCrudRole);

        // Mapping Lambdas
        createMappingLambda = LambdaFactory.createLambda(this, "CreateMappingLambda",
                "com.myorg.lambdas.mapping.CreateMapping::handleRequest",
                "target/aws-cdk-final-0.1.jar", mappingCrudRole);

        updateMappingLambda = LambdaFactory.createLambda(this, "UpdateMappingLambda",
                "com.myorg.lambdas.mapping.UpdateMapping::handleRequest",
                "target/aws-cdk-final-0.1.jar", mappingCrudRole);

        listMappingLambda = LambdaFactory.createLambda(this, "ListMappingLambda",
                "com.myorg.lambdas.mapping.ListMapping::handleRequest",
                "target/aws-cdk-final-0.1.jar", mappingReadRole);

        deleteMappingLambda = LambdaFactory.createLambda(this, "DeleteMappingLambda",
                "com.myorg.lambdas.mapping.DeleteMapping::handleRequest",
                "target/aws-cdk-final-0.1.jar", mappingCrudRole);

        // Journal Lambdas
        createJournalLambda = LambdaFactory.createLambda(this, "CreateJournalLambda",
                "com.myorg.lambdas.journals.CreateJournal::handleRequest",
                "target/aws-cdk-final-0.1.jar", journalCrudRole);

        deleteClientJournalLambda = LambdaFactory.createLambda(this, "DeleteClientJournalLambda",
                "com.myorg.lambdas.journals.DeleteClientJournal::handleRequest",
                "target/aws-cdk-final-0.1.jar", journalCrudRole);

        listJournalLambda = LambdaFactory.createLambda(this, "ListJournalLambda",
                "com.myorg.lambdas.journals.ListJournal::handleRequest",
                "target/aws-cdk-final-0.1.jar", journalReadRole);

        retrieveClientJournalsLambda = LambdaFactory.createLambda(this, "RetrieveClientJournalsLambda",
                "com.myorg.lambdas.journals.RetrieveClientJournals::handleRequest",
                "target/aws-cdk-final-0.1.jar", journalReadRole);

        updateRequestStatusLambda.addEnvironment("GET_REQUEST_DETAILS_LAMBDA", getRequestLambda.getFunctionName());
        updateRequestStatusLambda.addEnvironment("CREATE_MAPPING_LAMBDA", createMappingLambda.getFunctionName());
        updateRequestStatusLambda.addEnvironment("UPDATE_MAPPING_LAMBDA", updateMappingLambda.getFunctionName());
        updateRequestStatusLambda.addEnvironment("SESSION_UPDATE_LAMBDA", updateSessionLambda.getFunctionName());
        updateRequestStatusLambda.addToRolePolicy(PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("lambda:InvokeFunction"))
                .resources(List.of(
                        getRequestLambda.getFunctionArn(),
                        createMappingLambda.getFunctionArn(),
                        updateMappingLambda.getFunctionArn(),
                        updateSessionLambda.getFunctionArn()
                ))
                .build());
        getRequestLambda.addPermission("AllowInvokeFromUpdateRequest", Permission.builder()
                .principal(new ServicePrincipal("lambda.amazonaws.com"))
                .action("lambda:InvokeFunction")
                .sourceArn(updateRequestStatusLambda.getFunctionArn())
                .build());

        createMappingLambda.addPermission("AllowInvokeFromUpdateRequest", Permission.builder()
                .principal(new ServicePrincipal("lambda.amazonaws.com"))
                .action("lambda:InvokeFunction")
                .sourceArn(updateRequestStatusLambda.getFunctionArn())
                .build());

        updateMappingLambda.addPermission("AllowInvokeFromUpdateRequest", Permission.builder()
                .principal(new ServicePrincipal("lambda.amazonaws.com"))
                .action("lambda:InvokeFunction")
                .sourceArn(updateRequestStatusLambda.getFunctionArn())
                .build());
        updateSessionLambda.addPermission("AllowInvokeFromUpdateRequest", Permission.builder()
                .principal(new ServicePrincipal("lambda.amazonaws.com"))
                .action("lambda:InvokeFunction")
                .sourceArn(updateRequestStatusLambda.getFunctionArn())
                .build());}

    // General read-only role with optional index permissions
    private Role createReadOnlyRoleWithIndexes(String roleName, String tableArn, List<String> indexArns) {
        Role role = Role.Builder.create(this, roleName)
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole")))
                .build();

        PolicyStatement tableReadPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of("dynamodb:GetItem", "dynamodb:Scan", "dynamodb:Query"))
                .resources(List.of(tableArn))
                .build();

        role.addToPolicy(tableReadPolicy);

        if (indexArns != null && !indexArns.isEmpty()) {
            PolicyStatement indexQueryPolicy = PolicyStatement.Builder.create()
                    .effect(Effect.ALLOW)
                    .actions(List.of("dynamodb:Query"))
                    .resources(indexArns)
                    .build();

            role.addToPolicy(indexQueryPolicy);
        }

        return role;
    }

    // Read-only role without index support
    private Role createReadOnlyRole(String roleName, String resourceArn) {
        return createReadOnlyRoleWithIndexes(roleName, resourceArn, List.of());
    }

    // CRUD role for full access to a table
    private Role createCrudRole(String roleName, String resourceArn) {
        Role role = Role.Builder.create(this, roleName)
                .assumedBy(new ServicePrincipal("lambda.amazonaws.com"))
                .managedPolicies(List.of(ManagedPolicy.fromAwsManagedPolicyName("service-role/AWSLambdaBasicExecutionRole")))
                .build();

        PolicyStatement crudPolicy = PolicyStatement.Builder.create()
                .effect(Effect.ALLOW)
                .actions(List.of(
                        "dynamodb:PutItem",
                        "dynamodb:UpdateItem",
                        "dynamodb:DeleteItem",
                        "dynamodb:GetItem",
                        "dynamodb:Scan",
                        "dynamodb:Query"))
                .resources(List.of(resourceArn))
                .build();

        role.addToPolicy(crudPolicy);
        return role;
    }
}
