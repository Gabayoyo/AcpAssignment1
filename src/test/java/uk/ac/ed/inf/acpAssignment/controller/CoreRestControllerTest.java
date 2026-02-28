package uk.ac.ed.inf.acpAssignment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import uk.ac.ed.inf.acpAssignment.service.DynamoDbService;
import uk.ac.ed.inf.acpAssignment.service.PostgresService;
import uk.ac.ed.inf.acpAssignment.service.S3Service;

@SpringBootTest(properties = {
    "ACP_S3=http://localhost:4566",
    "ACP_DYNAMODB=http://localhost:4566",
    "ACP_POSTGRES=jdbc:postgresql://localhost:5433/acp?currentSchema=s2417814"
})
@AutoConfigureMockMvc
public class CoreRestControllerTest {

  @Autowired
  MockMvc mockMvc;

  @Autowired
  private CoreRestController coreRestController;

  @Autowired
  private S3Service s3Service;

  @Autowired
  private DynamoDbService dynamoDbService;

  @Autowired
  private PostgresService postgresService;

  @Autowired
  JdbcTemplate jdbcTemplate;

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
  public void testMissingKey() throws Exception {
    var bucketName = "test-bucket-" + System.currentTimeMillis();
    var jsonContent = """
        {"id":"test-content"}""";
    s3Service.createBucket(bucketName);
    s3Service.addObjectToBucket(bucketName, "test-key", jsonContent);
    mockMvc.perform(get("/api/v1/acp/single/s3/{bucket}/{key}", bucketName, "test-key-bad"))
        .andExpect(status().isNotFound());
  }

  @Test
  public void testReadDynamoTableJson() throws Exception {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        {"id":"test-content"}""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    mockMvc.perform(get("/api/v1/acp/all/dynamo/{table}", tableName))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json("[{\"id\":\"test-content\"}]"));
  }


  @Test
  public void testReadDynamoTableJsons() throws Exception {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        {"id":"test-content"}""";
    var jsonContent2 = """
        {"id":"test-content-2"}""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    dynamoDbService.createObject(tableName, "test-key-2", jsonContent2);
    mockMvc.perform(get("/api/v1/acp/all/dynamo/{table}", tableName))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json("[{\"id\":\"test-content-2\"},{\"id\":\"test-content\"}]"));
  }

  @Test
  public void testReadDynamoTableWithKeyJson() throws Exception {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        {"id":"test-content"}""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    mockMvc.perform(get("/api/v1/acp/single/dynamo/{table}/{key}", tableName, "test-key"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json(jsonContent));
  }

  @Test
  public void testReadDynamoTableWithKeyJsons() throws Exception {
    var tableName = "test-table-" + System.currentTimeMillis();
    var jsonContent = """
        {"id":"test-content"}""";
    var jsonContent2 = """
        {"id":"test-content-2"}""";
    dynamoDbService.createTable(tableName);
    dynamoDbService.createObject(tableName, "test-key", jsonContent);
    dynamoDbService.createObject(tableName, "test-key-2", jsonContent2);
    mockMvc.perform(get("/api/v1/acp/single/dynamo/{table}/{key}", tableName, "test-key-2"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json(jsonContent2));
  }


  @Test
  public void testReadEmptyPostgresTable() throws Exception {
    var tableName = "test_table_" + System.currentTimeMillis();
    jdbcTemplate.execute("CREATE TABLE \"" + tableName + "\" (id INTEGER, name VARCHAR(255), age INTEGER)");
    mockMvc.perform(get("/api/v1/acp/all/postgres/{table}", tableName))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json("[]"));
  }

  @Test
  public void testReadPostgresTable() throws Exception {
    var tableName = "test_table_" + System.currentTimeMillis();
    jdbcTemplate.execute("CREATE TABLE \"" + tableName + "\" (id INTEGER, name VARCHAR(255), age INTEGER)");
    jdbcTemplate.execute("INSERT INTO \"" + tableName + "\" (id, name, age) VALUES (1, 'Alice', 30)");
    mockMvc.perform(get("/api/v1/acp/all/postgres/{table}", tableName))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json("[{\"id\":1,\"name\":\"Alice\",\"age\":30}]"));
  }

  @Test
  public void testProcessDump() throws Exception {
    var body = """
    {"urlPath": "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/drones"}""";
    mockMvc.perform(post("/api/v1/acp/process/dump").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json("[{\"id\":\"1\",\"name\":\"Drone 1\",\"capability\":{\"cooling\":true,\"heating\":true,\"capacity\":4.0,\"maxMoves\":2000,\"costPerMove\":0.01,\"costInitial\":4.3,\"costFinal\":6.5},\"costPer100Moves\":11.80},"
            + "{\"id\":\"2\",\"name\":\"Drone 2\",\"capability\":{\"cooling\":false,\"heating\":true,\"capacity\":8.0,\"maxMoves\":1000,\"costPerMove\":0.03,\"costInitial\":2.6,\"costFinal\":5.4},\"costPer100Moves\":11.00},"
            + "{\"id\":\"3\",\"name\":\"Drone 3\",\"capability\":{\"cooling\":false,\"heating\":false,\"capacity\":20.0,\"maxMoves\":4000,\"costPerMove\":0.05,\"costInitial\":9.5,\"costFinal\":11.5},\"costPer100Moves\":26.00},"
            + "{\"id\":\"4\",\"name\":\"Drone 4\",\"capability\":{\"cooling\":false,\"heating\":true,\"capacity\":8.0,\"maxMoves\":1000,\"costPerMove\":0.02,\"costInitial\":1.4,\"costFinal\":2.5},\"costPer100Moves\":5.90},"
            + "{\"id\":\"5\",\"name\":\"Drone 5\",\"capability\":{\"cooling\":true,\"heating\":true,\"capacity\":12.0,\"maxMoves\":1500,\"costPerMove\":0.04,\"costInitial\":1.8,\"costFinal\":3.5},\"costPer100Moves\":9.30},"
            + "{\"id\":\"6\",\"name\":\"Drone 6\",\"capability\":{\"cooling\":false,\"heating\":true,\"capacity\":4.0,\"maxMoves\":2000,\"costPerMove\":0.03,\"costInitial\":3.0,\"costFinal\":4.0},\"costPer100Moves\":10.00},"
            + "{\"id\":\"7\",\"name\":\"Drone 7\",\"capability\":{\"cooling\":false,\"heating\":true,\"capacity\":8.0,\"maxMoves\":1000,\"costPerMove\":0.015,\"costInitial\":1.4,\"costFinal\":2.2},\"costPer100Moves\":5.100},"
            + "{\"id\":\"8\",\"name\":\"Drone 8\",\"capability\":{\"cooling\":true,\"heating\":false,\"capacity\":20.0,\"maxMoves\":4000,\"costPerMove\":0.04,\"costInitial\":5.4,\"costFinal\":12.5},\"costPer100Moves\":21.90},"
            + "{\"id\":\"9\",\"name\":\"Drone 9\",\"capability\":{\"cooling\":true,\"heating\":true,\"capacity\":8.0,\"maxMoves\":1000,\"costPerMove\":0.06,\"costInitial\":2.4,\"costFinal\":1.5},\"costPer100Moves\":9.90},"
            + "{\"id\":\"10\",\"name\":\"Drone 10\",\"capability\":{\"cooling\":false,\"heating\":false,\"capacity\":12.0,\"maxMoves\":1500,\"costPerMove\":0.07,\"costInitial\":1.4,\"costFinal\":3.5},\"costPer100Moves\":11.90}]\n"));
  }

  @Test
  public void testProcessDumpExtraFields() throws Exception {
    var body = """
    {"urlPath": "https://ilp-rest-2025-bvh6e9hschfagrgy.ukwest-01.azurewebsites.net/drones",
    "extraFields": "extraField"}""";
    mockMvc.perform(post("/api/v1/acp/process/dump").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("application/json"))
        .andExpect(content().json("[{\"id\":\"1\",\"name\":\"Drone 1\",\"capability\":{\"cooling\":true,\"heating\":true,\"capacity\":4.0,\"maxMoves\":2000,\"costPerMove\":0.01,\"costInitial\":4.3,\"costFinal\":6.5},\"costPer100Moves\":11.80},"
            + "{\"id\":\"2\",\"name\":\"Drone 2\",\"capability\":{\"cooling\":false,\"heating\":true,\"capacity\":8.0,\"maxMoves\":1000,\"costPerMove\":0.03,\"costInitial\":2.6,\"costFinal\":5.4},\"costPer100Moves\":11.00},"
            + "{\"id\":\"3\",\"name\":\"Drone 3\",\"capability\":{\"cooling\":false,\"heating\":false,\"capacity\":20.0,\"maxMoves\":4000,\"costPerMove\":0.05,\"costInitial\":9.5,\"costFinal\":11.5},\"costPer100Moves\":26.00},"
            + "{\"id\":\"4\",\"name\":\"Drone 4\",\"capability\":{\"cooling\":false,\"heating\":true,\"capacity\":8.0,\"maxMoves\":1000,\"costPerMove\":0.02,\"costInitial\":1.4,\"costFinal\":2.5},\"costPer100Moves\":5.90},"
            + "{\"id\":\"5\",\"name\":\"Drone 5\",\"capability\":{\"cooling\":true,\"heating\":true,\"capacity\":12.0,\"maxMoves\":1500,\"costPerMove\":0.04,\"costInitial\":1.8,\"costFinal\":3.5},\"costPer100Moves\":9.30},"
            + "{\"id\":\"6\",\"name\":\"Drone 6\",\"capability\":{\"cooling\":false,\"heating\":true,\"capacity\":4.0,\"maxMoves\":2000,\"costPerMove\":0.03,\"costInitial\":3.0,\"costFinal\":4.0},\"costPer100Moves\":10.00},"
            + "{\"id\":\"7\",\"name\":\"Drone 7\",\"capability\":{\"cooling\":false,\"heating\":true,\"capacity\":8.0,\"maxMoves\":1000,\"costPerMove\":0.015,\"costInitial\":1.4,\"costFinal\":2.2},\"costPer100Moves\":5.100},"
            + "{\"id\":\"8\",\"name\":\"Drone 8\",\"capability\":{\"cooling\":true,\"heating\":false,\"capacity\":20.0,\"maxMoves\":4000,\"costPerMove\":0.04,\"costInitial\":5.4,\"costFinal\":12.5},\"costPer100Moves\":21.90},"
            + "{\"id\":\"9\",\"name\":\"Drone 9\",\"capability\":{\"cooling\":true,\"heating\":true,\"capacity\":8.0,\"maxMoves\":1000,\"costPerMove\":0.06,\"costInitial\":2.4,\"costFinal\":1.5},\"costPer100Moves\":9.90},"
            + "{\"id\":\"10\",\"name\":\"Drone 10\",\"capability\":{\"cooling\":false,\"heating\":false,\"capacity\":12.0,\"maxMoves\":1500,\"costPerMove\":0.07,\"costInitial\":1.4,\"costFinal\":3.5},\"costPer100Moves\":11.90}]\n"));
  }

}
