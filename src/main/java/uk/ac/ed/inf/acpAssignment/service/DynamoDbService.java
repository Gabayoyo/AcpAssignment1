package uk.ac.ed.inf.acpAssignment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;
import software.amazon.awssdk.services.dynamodb.model.*;
import uk.ac.ed.inf.acpAssignment.configuration.DynamoDbConfiguration;
import uk.ac.ed.inf.acpAssignment.configuration.SystemEnvironment;

import java.net.URI;
import java.util.List;
import uk.ac.ed.inf.acpAssignment.dto.Drone;
import uk.ac.ed.inf.acpAssignment.dto.DynamoObject;

@Slf4j
@Service
public class DynamoDbService {

    private final DynamoDbConfiguration dynamoDbConfiguration;
    private final SystemEnvironment systemEnvironment;
    private static final String KEY_COLUMN_NAME = "key";

    public DynamoDbService(DynamoDbConfiguration dynamoDbConfiguration, SystemEnvironment systemEnvironment) {
        this.dynamoDbConfiguration = dynamoDbConfiguration;
        this.systemEnvironment = systemEnvironment;
    }

    public List<String> listTables() {
        return getDynamoDbClient().listTables().tableNames();
    }


    public List<DynamoObject> listTableObjects(@PathVariable String table) {
        ObjectMapper objectMapper = new ObjectMapper();
        return getDynamoDbClient()
            .scanPaginator(ScanRequest.builder().tableName(table).build())
            .items()
            .stream()
            .map(e -> {
                String key = e.get("key").s();
                String content = e.get("content").s();
                try {
                    JsonNode node = objectMapper.readTree(content);
                    return new DynamoObject(key, node);
                } catch (Exception ex) {
                    return new DynamoObject(key, objectMapper.createObjectNode().put("content", content));
                }
            })
            .toList();
    }

    public List<String> listTableContents(@PathVariable String table) {
        return getDynamoDbClient()
                .scanPaginator(ScanRequest.builder()
                        .tableName(table)
                        .build())
                .items()
                .stream()
                .map(e -> e.get("content").s())
                .toList();
    }

    public String getObjectContent(@PathVariable String table, @PathVariable String key) {
        GetItemRequest request = GetItemRequest.builder()
                .tableName(table)
                .key(java.util.Map.of(KEY_COLUMN_NAME, AttributeValue.builder().s(key).build()))
                .build();

        GetItemResponse response = getDynamoDbClient().getItem(request);

        return response.item().get("content").s();
    }

    public void createTable(@PathVariable String table) {
        getDynamoDbClient().createTable(b -> b.tableName(table)
                .attributeDefinitions(AttributeDefinition.builder()
                        .attributeName(KEY_COLUMN_NAME)
                        .attributeType(ScalarAttributeType.S)
                        .build())
                .keySchema(KeySchemaElement.builder()
                        .attributeName(KEY_COLUMN_NAME)
                        .keyType(KeyType.HASH)
                        .build())
                .provisionedThroughput(ProvisionedThroughput.builder()
                        .readCapacityUnits(5L)
                        .writeCapacityUnits(5L)
                        .build())
        );
    }

    public void createObject(@PathVariable String table, @PathVariable String key, @RequestBody String objectContent) {
        getDynamoDbClient().putItem(b -> b.tableName(table).item(
                java.util.Map.of("key", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(key).build(),
                        "content", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(objectContent).build())
        ));
    }

    public void addDronesToTable(Drone[] drones) throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        for (Drone drone : drones) {
            createObject("s2417814", drone.name(),
                objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(drone));
        }
    }

    public String getTablePrimaryKey(
            @Parameter(name = "table", description = "The name of the DynamoDB table")
            @PathVariable(required = true)
            String table) {

        DescribeTableRequest request = DescribeTableRequest.builder()
                .tableName(table)
                .build();

        DescribeTableResponse response = getDynamoDbClient().describeTable(request);

        return response.table().keySchema().stream()
                .filter(k -> k.keyType().toString().equals("HASH"))
                .map(KeySchemaElement::attributeName)
                .findFirst()
                .orElseThrow();
    }



    private DynamoDbClient getDynamoDbClient() {
        return DynamoDbClient.builder()
                .endpointOverride(URI.create(dynamoDbConfiguration.getDynamoDbEndpoint()))
                .region(systemEnvironment.getAwsRegion())
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create(systemEnvironment.getAwsUser(), systemEnvironment.getAwsSecret())))
                .build();
    }

    public void saveMessageToDynamoDb(String sqsTableInDynamoDb, String key, String message) {
        if (! getDynamoDbClient().listTables().tableNames().contains(sqsTableInDynamoDb)) {
            createTable(sqsTableInDynamoDb);
        }
        createObject(sqsTableInDynamoDb, key, message);
    }

    public void clearTable(@PathVariable String table) {
        getDynamoDbClient().scanPaginator(ScanRequest.builder()
                .tableName(table)
                .build())
                .items()
                .forEach(e -> getDynamoDbClient().deleteItem(b -> b.tableName(table).key(
                        java.util.Map.of("key", software.amazon.awssdk.services.dynamodb.model.AttributeValue.builder().s(e.get("key").s()).build())
                )));
    }
}
