package uk.ac.ed.inf.acpAssignment.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import redis.clients.jedis.Jedis;
import uk.ac.ed.inf.acpAssignment.dto.SplitterRequest;
import uk.ac.ed.inf.acpAssignment.dto.TransformRequest;
import uk.ac.ed.inf.acpAssignment.service.DynamoDbService;
import uk.ac.ed.inf.acpAssignment.service.KafkaService;
import uk.ac.ed.inf.acpAssignment.service.RabbitMqService;
import uk.ac.ed.inf.acpAssignment.service.S3Service;
import uk.ac.ed.inf.acpAssignment.service.TransformService;

@SpringBootTest(properties = {
    "ACP_S3=http://localhost:4566",
    "ACP_DYNAMODB=http://localhost:4566",
    "ACP_POSTGRES=jdbc:postgresql://localhost:5433/acp?currentSchema=s2417814"
})
@AutoConfigureMockMvc
public class CoreRestControllerTest {

  @BeforeEach
  void reset() {
    transformService.resetState();
  }

  @Autowired
  MockMvc mockMvc;

  @Autowired
  private CoreRestController coreRestController;

  @Autowired
  private S3Service s3Service;

  @Autowired
  private DynamoDbService dynamoDbService;

  @Autowired
  private RabbitMqService rabbitMqService;

  @Autowired
  private KafkaService kafkaService;

  @Autowired
  JdbcTemplate jdbcTemplate;
  @Autowired
  private TransformService transformService;

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

  @Test
  public void testPutAndGetRabbitMqMessages() throws Exception {

    var queueName = "test-queue-" + System.currentTimeMillis();
    coreRestController.sendMessagesRabbit(queueName, 5);

    List<String> messages = rabbitMqService.getMessages(queueName, 10000);
    assertEquals(5, messages.size());

    for (int i = 0; i < 5; i++) {
      String expected = String.format("{\"uid\":\"%s\",\"counter\":%d}", "s2417814", i);
      assertEquals(expected, messages.get(i));
    }
  }

  @Test
  public void testGetRabbitMqMessagesEmpty() {

    var queueName = "empty-queue-" + System.currentTimeMillis();
    var response = coreRestController.getMessagesRabbit(queueName, 10000);

    assertEquals(200, response.getStatusCode().value());
    List<String> messages = (List<String>) response.getBody();
    assertTrue(messages.isEmpty());
  }

  @Test
  public void testRabbitMqDoesNotExceedTimeout() throws Exception {

    var queueName = "timing-queue-" + System.currentTimeMillis();
    long timeout = 1000;

    long start = System.currentTimeMillis();
    coreRestController.getMessagesRabbit(queueName, timeout);
    long duration = System.currentTimeMillis() - start;

    assertTrue(duration <= timeout + 200);
  }

  @Test
  public void testPutAndGetKafkaMessages() throws Exception {

    var topic = "test-topic-" + System.currentTimeMillis();
    var messageCount = 5;

    coreRestController.sendMessagesKafka(topic, messageCount);

    List<String> messages = kafkaService.getMessages(topic, 10000);

    assertEquals(messageCount, messages.size());

    for (int i = 0; i < messageCount; i++) {
      String expected = String.format(
          "{\"uid\":\"%s\",\"counter\":%d}",
          "s2417814",
          i
      );

      assertEquals(expected, messages.get(i));
    }
  }

  @Test
  public void testGetKafkaMessagesNoTopic() throws Exception {

    var topic = "empty-topic-" + System.currentTimeMillis();
    long timeout = 1000;

    var response = coreRestController.getMessagesKafka(topic, timeout);
    assertEquals(404, response.getStatusCode().value());
  }

  @Test
  public void testKafkaDoesNotExceedTimeout5000() throws Exception {

    var queueName = "timing-queue-" + System.currentTimeMillis();
    long timeout = 5000;

    long start = System.currentTimeMillis();
    coreRestController.getMessagesKafka(queueName, timeout);
    long duration = System.currentTimeMillis() - start;

    assertTrue(duration <= timeout + 200);
  }

  @Test
  public void testKafkaDoesNotExceedTimeout500() throws Exception {

    var queueName = "timing-queue-" + System.currentTimeMillis();
    long timeout = 500;

    long start = System.currentTimeMillis();
    coreRestController.getMessagesKafka(queueName, timeout);
    long duration = System.currentTimeMillis() - start;

    assertTrue(duration <= timeout + 200);
  }

  @Test
  public void testGetSortedRabbitMqMessages() throws Exception {

    var queueName = "sorted-queue-" + System.currentTimeMillis();
    int messageCount = 5;
    rabbitMqService.seedQueue(queueName, messageCount);

    var response = coreRestController.getMessagesToConsiderRabbit(queueName, messageCount);
    assertEquals(200, response.getStatusCode().value());

    List<String> messages = (List<String>) response.getBody();
    assertEquals(messageCount, messages.size());

    for (int i = 0; i < messageCount; i++) {
      String expected = String.format(
          "{\"Id\":%d,\"Payload\":\"String-data-%d\"}",
          i, i
      );
      assertEquals(expected, messages.get(i));
    }
  }

  @Test
  public void testGetSortedKafkaMessages() throws Exception {

    var topic = "sorted-topic-" + System.currentTimeMillis();
    int messageCount = 5;
    kafkaService.seedTopic(topic, messageCount);

    var response = coreRestController.getMessagesToConsiderKafka(topic, messageCount);
    assertEquals(200, response.getStatusCodeValue());

    List<String> messages = (List<String>) response.getBody();
    assertEquals(messageCount, messages.size());

    // ✔ Check sorting by Id
    for (int i = 0; i < messageCount; i++) {
      String expected = String.format(
          "{\"Id\":%d,\"Payload\":\"String-data-%d\"}",
          i, i
      );
      assertEquals(expected, messages.get(i));
    }
  }

  @Test
  public void testSplitterEven() throws Exception {

    var queueName = "splitter-queue-" + System.currentTimeMillis();
    var topicEven = "splitter-even-" + System.currentTimeMillis();
    var topicOdd = "splitter-odd-" + System.currentTimeMillis();
    var redisEven = "redis-even-" + System.currentTimeMillis();
    var redisOdd = "redis-odd-" + System.currentTimeMillis();

    rabbitMqService.clearQueue(queueName);

    rabbitMqService.seedQueueSplitterOnce(queueName, 4, 40.0, "even");

    SplitterRequest request = new SplitterRequest(
        queueName,
        topicOdd,
        redisOdd,
        topicEven,
        redisEven,
        1
    );

    try (Jedis jedis = new Jedis("localhost", 6379)) {
      jedis.flushDB();
      coreRestController.splitter(request);

      List<String> evenMessages = kafkaService.getMessages(topicEven, 5000);

      assertEquals(1, evenMessages.size());

      assertEquals("1", jedis.get("count_even"));
      assertEquals("40.00", jedis.get("average_even"));

      assertEquals("{\"Id\":4,\"Value\":40.0,\"AdditionalData\":\"even\"}", jedis.hget(redisEven, "4"));
    }
  }

  @Test
  public void testSplitterOdd() throws Exception {

    var queueName = "splitter-queue-" + System.currentTimeMillis();
    var topicEven = "splitter-even-" + System.currentTimeMillis();
    var topicOdd = "splitter-odd-" + System.currentTimeMillis();
    var redisEven = "redis-even-" + System.currentTimeMillis();
    var redisOdd = "redis-odd-" + System.currentTimeMillis();

    rabbitMqService.clearQueue(queueName);

    rabbitMqService.seedQueueSplitterOnce(queueName, 5, 25.0, "odd");

    SplitterRequest request = new SplitterRequest(
        queueName,
        topicOdd,
        redisOdd,
        topicEven,
        redisEven,
        1
    );

    try (Jedis jedis = new Jedis("localhost", 6379)) {
      jedis.flushDB();
      coreRestController.splitter(request);

      List<String> oddMessages = kafkaService.getMessages(topicOdd, 5000);

      assertEquals(1, oddMessages.size());

      assertEquals("1", jedis.get("count_odd"));
      assertEquals("25.00", jedis.get("average_odd"));

      assertEquals("{\"Id\":5,\"Value\":25.0,\"AdditionalData\":\"odd\"}", jedis.hget(redisOdd, "5"));
    }
  }

  @Test
  public void testSplitterEvenOdd() throws Exception {

    var queueName = "splitter-queue-" + System.currentTimeMillis();
    var topicEven = "splitter-even-" + System.currentTimeMillis();
    var topicOdd = "splitter-odd-" + System.currentTimeMillis();
    var redisEven = "redis-even-" + System.currentTimeMillis();
    var redisOdd = "redis-odd-" + System.currentTimeMillis();

    rabbitMqService.clearQueue(queueName);

    rabbitMqService.seedQueueSplitterOnce(queueName, 1, 10.0, "odd");
    rabbitMqService.seedQueueSplitterOnce(queueName, 2, 20.0, "even");

    SplitterRequest request = new SplitterRequest(
        queueName,
        topicOdd,
        redisOdd,
        topicEven,
        redisEven,
        2
    );

    try (Jedis jedis = new Jedis("localhost", 6379)) {
      jedis.flushDB();
      coreRestController.splitter(request);

      List<String> evenMessages = kafkaService.getMessages(topicEven, 5000);
      List<String> oddMessages = kafkaService.getMessages(topicOdd, 5000);

      assertEquals(1, evenMessages.size());
      assertEquals(1, oddMessages.size());

      assertEquals("1", jedis.get("count_even"));
      assertEquals("1", jedis.get("count_odd"));
      assertEquals("10.00", jedis.get("average_odd"));
      assertEquals("20.00", jedis.get("average_even"));

      assertEquals("{\"Id\":1,\"Value\":10.0,\"AdditionalData\":\"odd\"}", jedis.hget(redisOdd, "1"));
      assertEquals("{\"Id\":2,\"Value\":20.0,\"AdditionalData\":\"even\"}", jedis.hget(redisEven, "2"));
    }
  }

  @Test
  public void testSplitterAverage() throws Exception {

    var queueName = "splitter-queue-" + System.currentTimeMillis();
    var topicEven = "splitter-even-" + System.currentTimeMillis();
    var topicOdd = "splitter-odd-" + System.currentTimeMillis();
    var redisEven = "redis-even-" + System.currentTimeMillis();
    var redisOdd = "redis-odd-" + System.currentTimeMillis();

    rabbitMqService.clearQueue(queueName);

    try (Jedis jedis = new Jedis("localhost", 6379)) {

      jedis.flushDB();

      rabbitMqService.seedQueueSplitterOnce(queueName, 1, 10.0, "odd");
      rabbitMqService.seedQueueSplitterOnce(queueName, 3, 20.0, "odd");

      SplitterRequest request = new SplitterRequest(
          queueName,
          topicOdd,
          redisOdd,
          topicEven,
          redisEven,
          2
      );
      coreRestController.splitter(request);

      List<String> oddMessages = kafkaService.getMessages(topicOdd, 5000);

      assertEquals(2, oddMessages.size());
      assertEquals("2", jedis.get("count_odd"));
      assertEquals("15.00", jedis.get("average_odd"));
      assertEquals("{\"Id\":1,\"Value\":10.0,\"AdditionalData\":\"odd\"}", jedis.hget(redisOdd, "1"));
      assertEquals("{\"Id\":3,\"Value\":20.0,\"AdditionalData\":\"odd\"}", jedis.hget(redisOdd, "3"));
    }
  }

  @Test
  public void testTransformSingle() throws Exception {

    var readQueue = "transform-read-" + System.currentTimeMillis();
    var writeQueue = "transform-write-" + System.currentTimeMillis();

    rabbitMqService.clearQueue(readQueue);
    rabbitMqService.clearQueue(writeQueue);

    try (Jedis jedis = new Jedis("localhost", 6379)) {
      jedis.flushDB();

      transformService.sendSingleMessage(readQueue, "ABC", 1, 100);
      TransformRequest request = new TransformRequest(readQueue, writeQueue, 1);
      coreRestController.transformMessages(request);

      List<String> output = rabbitMqService.getMessages(writeQueue, 10000);

      assertEquals(1, output.size());

      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(output.getFirst());

      assertEquals("ABC", node.get("key").asText());
      assertEquals(1, node.get("version").asInt());
      assertEquals(110.5, node.get("value").asDouble(), 0.001);

      assertEquals(1, transformService.getTotalMessagesWritten());
      assertEquals(1, transformService.getTotalMessagesProcessed());
      assertEquals(1, transformService.getTotalRedisUpdates());
      assertEquals(110.5, transformService.getTotalValueWritten());
      assertEquals(10.5, transformService.getTotalAdded());
    }
  }

  @Test
  public void testTransformDouble() throws Exception {

    var readQueue = "transform-read-" + System.currentTimeMillis();
    var writeQueue = "transform-write-" + System.currentTimeMillis();

    rabbitMqService.clearQueue(readQueue);
    rabbitMqService.clearQueue(writeQueue);

    try (Jedis jedis = new Jedis("localhost", 6379)) {
      jedis.flushDB();

      transformService.sendSingleMessage(readQueue, "ABC", 1, 100);
      TransformRequest request = new TransformRequest(readQueue, writeQueue, 1);
      coreRestController.transformMessages(request);

      rabbitMqService.clearQueue(readQueue);
      rabbitMqService.clearQueue(writeQueue);
      transformService.sendSingleMessage(readQueue, "ABC", 1, 200);
      coreRestController.transformMessages(request);

      List<String> output = rabbitMqService.getMessages(writeQueue, 10000);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(output.getFirst());

      assertEquals("ABC", node.get("key").asText());
      assertEquals(1, node.get("version").asInt());
      assertEquals(200, node.get("value").asDouble(), 0.001);

      assertEquals(2, transformService.getTotalMessagesWritten());
      assertEquals(1, transformService.getTotalMessagesProcessed());
      assertEquals(1, transformService.getTotalRedisUpdates());
      assertEquals(310.5, transformService.getTotalValueWritten());
      assertEquals(10.5, transformService.getTotalAdded());
    }
  }

  @Test
  public void testTransformTombstone() throws Exception {

    var readQueue = "transform-read-" + System.currentTimeMillis();
    var writeQueue = "transform-write-" + System.currentTimeMillis();

    rabbitMqService.clearQueue(readQueue);
    rabbitMqService.clearQueue(writeQueue);

    try (Jedis jedis = new Jedis("localhost", 6379)) {
      jedis.flushDB();

      transformService.sendSingleMessage(readQueue, "ABC", 1, 100);
      TransformRequest request = new TransformRequest(readQueue, writeQueue, 1);
      coreRestController.transformMessages(request);

      rabbitMqService.clearQueue(readQueue);
      rabbitMqService.clearQueue(writeQueue);
      transformService.sendTombstone(readQueue);
      coreRestController.transformMessages(request);

      List<String> output = rabbitMqService.getMessages(writeQueue, 10000);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(output.getFirst());

      assertEquals(1, node.get("totalMessagesWritten").asInt());
      assertEquals(1, node.get("totalMessagesProcessed").asInt());
      assertEquals(2, node.get("totalRedisUpdates").asInt());
      assertEquals(110.5, node.get("totalValueWritten").asDouble(), 0.001);
      assertEquals(10.5, node.get("totalAdded").asDouble(), 0.001);

      assertNull(jedis.get("ABC"));

      assertNotEquals(0,  transformService.getTotalMessagesWritten());
      assertNotEquals(0,  transformService.getTotalMessagesProcessed());
      assertNotEquals(0,  transformService.getTotalRedisUpdates());
      assertNotEquals(0,  transformService.getTotalValueWritten());
      assertNotEquals(0,  transformService.getTotalAdded());
    }
  }

  @Test
  public void testTransformHigherVersion() throws Exception {

    var readQueue = "transform-read-" + System.currentTimeMillis();
    var writeQueue = "transform-write-" + System.currentTimeMillis();

    rabbitMqService.clearQueue(readQueue);
    rabbitMqService.clearQueue(writeQueue);

    try (Jedis jedis = new Jedis("localhost", 6379)) {
      jedis.flushDB();

      transformService.sendSingleMessage(readQueue, "ABC", 1, 100);
      TransformRequest request = new TransformRequest(readQueue, writeQueue, 1);
      coreRestController.transformMessages(request);

      rabbitMqService.clearQueue(readQueue);
      rabbitMqService.clearQueue(writeQueue);
      transformService.sendSingleMessage(readQueue, "ABC", 3, 400);
      coreRestController.transformMessages(request);

      List<String> output = rabbitMqService.getMessages(writeQueue, 10000);

      ObjectMapper mapper = new ObjectMapper();
      JsonNode node = mapper.readTree(output.getFirst());

      assertEquals("ABC", node.get("key").asText());
      assertEquals(3, node.get("version").asInt());
      assertEquals(410.5, node.get("value").asDouble(), 0.001);

      assertEquals(2, transformService.getTotalMessagesWritten());
      assertEquals(2, transformService.getTotalMessagesProcessed());
      assertEquals(2, transformService.getTotalRedisUpdates());
      assertEquals(521, transformService.getTotalValueWritten());
      assertEquals(21, transformService.getTotalAdded());
    }
  }
}
