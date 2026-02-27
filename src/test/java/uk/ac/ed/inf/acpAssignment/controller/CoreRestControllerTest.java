package uk.ac.ed.inf.acpAssignment.controller;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import uk.ac.ed.inf.acpAssignment.service.DynamoDbService;
import uk.ac.ed.inf.acpAssignment.service.S3Service;

@SpringBootTest(properties = {
    "ACP_S3=http://localhost:4566",
    "ACP_DYNAMODB=http://localhost:4566"
})
public class CoreRestControllerTest {

  @Autowired
  private CoreRestController coreRestController;

  @Autowired
  private S3Service s3Service;

  @Autowired
  private DynamoDbService dynamoDbService;

  @Test
  public void testGetNonJsonBucketObject() {
    var bucketName = "test-bucket-" + System.currentTimeMillis();
    s3Service.createBucket(bucketName);
    s3Service.addObjectToBucket(bucketName, "test-key", "id: test-content");
    var response = coreRestController.getBucketObjects(bucketName);
    assertAll(
        () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
        () -> assertTrue(response.getBody().toString().contains("id: test-content"))
    );
  }

  @Test
  public void testGetNonJsonBucketObjects() {
    var bucketName = "test-bucket-" + System.currentTimeMillis();
    s3Service.createBucket(bucketName);
    s3Service.addObjectToBucket(bucketName, "test-key", "id: test-content");
    s3Service.addObjectToBucket(bucketName, "test-key-2", "id: test-content-2");
    var response = coreRestController.getBucketObjects(bucketName);
    assertAll(
        () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
        () -> assertTrue(response.getBody().toString().contains("id: test-content")),
        () -> assertTrue(response.getBody().toString().contains("id: test-content-2"))
    );
  }

  @Test
  public void testGetJsonBucketObject() {
    var jsonContent = """
        { "id": "test-content" }
        """;
    var expectedJsonOutput = """
        [{"id":"test-content"}]""";
    var bucketName = "test-bucket-" + System.currentTimeMillis();
    s3Service.createBucket(bucketName);
    s3Service.addObjectToBucket(bucketName, "test-key", jsonContent);
    var response = coreRestController.getBucketObjects(bucketName);
    assertEquals(response.getBody().toString(), expectedJsonOutput);
  }
  @Test
  public void testGetJsonBucketObjects() {
    var jsonContent = """
        { "id": "test-content" }
        """;
    var jsonContent2 = """
        { "id": "test-content-2" }
        """;
    var expectedJsonOutput = """
        [{"id":"test-content"},{"id":"test-content-2"}]""";
    var bucketName = "test-bucket-" + System.currentTimeMillis();
    s3Service.createBucket(bucketName);
    s3Service.addObjectToBucket(bucketName, "test-key", jsonContent);
    s3Service.addObjectToBucket(bucketName, "test-key-2", jsonContent2);
    var response = coreRestController.getBucketObjects(bucketName);
    assertEquals(response.getBody().toString(), expectedJsonOutput);
  }

  @Test
  public void testMissingBucket() {
    var jsonContent = """
        { "id": "test-content" }
        """;
    var expectedJsonOutput = """
        [{"id":"test-content"},{"id":"test-content-2"}]""";
    var bucketName = "test-bucket-" + System.currentTimeMillis();
    s3Service.createBucket(bucketName);
    s3Service.addObjectToBucket(bucketName, "test-key", jsonContent);
    var response = coreRestController.getBucketObjects("test-bucket-bad");
    assertEquals(response.getStatusCode().value(), 404);
  }

  @Test
  public void testGetJsonObjectWithKey() {
    var jsonContent = """
        { "id": "test-content" }
        """;
    var jsonContent2 = """
        { "id": "test-content-2" }
        """;
    var expectedJsonOutput = """
        {"id":"test-content"}""";
    var bucketName = "test-bucket-" + System.currentTimeMillis();
    s3Service.createBucket(bucketName);
    s3Service.addObjectToBucket(bucketName, "test-key", jsonContent);
    s3Service.addObjectToBucket(bucketName, "test-key-2", jsonContent2);
    var response = coreRestController.getBucketObjectWithKey(bucketName, "test-key");
    assertEquals(response.getBody().toString(), expectedJsonOutput);
  }

  @Test
  public void testMissingKey() {
    var jsonContent = """
        { "id": "test-content" }
        """;
    var jsonContent2 = """
        { "id": "test-content-2" }
        """;
    var bucketName = "test-bucket-" + System.currentTimeMillis();
    s3Service.createBucket(bucketName);
    s3Service.addObjectToBucket(bucketName, "test-key", jsonContent);
    s3Service.addObjectToBucket(bucketName, "test-key-2", jsonContent2);
    var response = coreRestController.getBucketObjectWithKey(bucketName, "test-key-bad");
    assertEquals(response.getStatusCode().value(), 404);
  }

  @Test
  public void testReadDynamoTableJson() {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        {"id":"test-content"}""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    var response = coreRestController.getDynamoTableContents(tableName);
    assertEquals(response.getBody().toString(), "[" + jsonContent + "]");
  }

  @Test
  public void testReadDynamoTableJsons() {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        {"id":"test-content"}""";
    var jsonContent2 = """
        {"id":"test-content-2"}""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    dynamoDbService.createObject(tableName, "test-key-2", jsonContent2);
    var response = coreRestController.getDynamoTableContents(tableName);
    assertAll(
        () -> assertTrue(response.getStatusCode().is2xxSuccessful()),
        () -> assertTrue(response.getBody().toString().contains(jsonContent)),
        () -> assertTrue(response.getBody().toString().contains(jsonContent2))
    );
  }

  @Test
  public void testReadDynamoTableNonJson() {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        hey""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    var response = coreRestController.getDynamoTableContents(tableName);
    assertEquals(response.getBody().toString(), "[\"" + jsonContent + "\"]");
  }

  @Test
  public void testReadDynamoTableWithKeyJson() {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        {"id":"test-content"}""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    var response = coreRestController.getDynamoObjectWithKey(tableName, "test-key");
    assertEquals(response.getBody().toString(), jsonContent);
  }

  @Test
  public void testReadDynamoTableWithKeyJsons() {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        {"id":"test-content"}""";
    var jsonContent2 = """
        {"id":"test-content-2"}""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    dynamoDbService.createObject(tableName, "test-key-2", jsonContent2);
    var response = coreRestController.getDynamoObjectWithKey(tableName, "test-key-2");
    assertEquals(response.getBody().toString(), jsonContent2);
  }

  @Test
  public void testReadDynamoTableWithKeyNonJson() {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        hey""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    var response = coreRestController.getDynamoObjectWithKey(tableName, "test-key");
    assertEquals(response.getBody().toString(), "\"" + jsonContent + "\"");
  }

}
