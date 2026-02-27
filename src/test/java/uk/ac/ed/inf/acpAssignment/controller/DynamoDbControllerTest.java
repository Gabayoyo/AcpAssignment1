package uk.ac.ed.inf.acpAssignment.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
public class DynamoDbControllerTest {

    @Autowired
    private DynamoDbController dynamoDbController;

    @Test
    public void testCreateTableSucceeds() {
        String tableName = "test-table-" + System.currentTimeMillis();
        dynamoDbController.createTable(tableName);
        
        // Verify table exists
        assertTrue(dynamoDbController.listTables().contains(tableName));
    }


    @Test
    public void testCreateObjectSucceeds() {
        String tableName = "test-table-" + System.currentTimeMillis();
        dynamoDbController.createTable(tableName);
        
        String key = "key-1";
        String content = "hello world";
        dynamoDbController.createObject(tableName, key, content);
        
        // Verify object (key) exists in list
        String responseBody =
            Objects.requireNonNull(dynamoDbController.listTableObjects(tableName).getBody().getFirst().key())
            .toString();
        assertTrue(responseBody.contains("key-1"));
    }

}
