package uk.ac.ed.inf.acpAssignment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.lang.annotation.Target;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import uk.ac.ed.inf.acpAssignment.configuration.KafkaConfiguration;
import uk.ac.ed.inf.acpAssignment.configuration.RabbitMqConfiguration;
import uk.ac.ed.inf.acpAssignment.configuration.RedisConfiguration;
import uk.ac.ed.inf.acpAssignment.dto.TransformMessage;
import uk.ac.ed.inf.acpAssignment.dto.TransformRequest;

@Service
public class TransformService {

  private final RedisConfiguration redisConfiguration;
  private final RabbitMqConfiguration rabbitMqConfiguration;

  private final KafkaConfiguration kafkaConfiguration;

  private long totalMessagesWritten = 0;
  private long totalMessagesProcessed = 0;
  private long totalRedisUpdates = 0;

  private double totalValueWritten = 0.0;
  private double totalAdded = 0.0;

  public TransformService(RedisConfiguration redisConfiguration,
                         RabbitMqConfiguration rabbitMqConfiguration,
                         KafkaConfiguration kafkaConfiguration) {
    this.redisConfiguration = redisConfiguration;
    this.rabbitMqConfiguration = rabbitMqConfiguration;
    this.kafkaConfiguration = kafkaConfiguration;
  }

  public void process(TransformRequest request) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());

    int messagesRead = 0;

    try (Jedis jedis = new Jedis(redisConfiguration.redisHost(), redisConfiguration.redisPort());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();) {

      while (messagesRead < request.messageCount()) {
        GetResponse response = channel.basicGet(request.readQueue(), true);
        if (response == null) continue;

        String message = new String(response.getBody(), StandardCharsets.UTF_8);
        JsonNode node = mapper.readTree(message);
        String key = node.get("key").asText();

        if (!key.equals("TOMBSTONE")) {
          // normal
          String outputMessage;
          double newValue;
          boolean shouldUpdate = false;
          int version = node.get("version").asInt();
          double value = node.get("value").asDouble();

          String storedVersionStr = jedis.get(key);

          // if not present or older ver
          if (storedVersionStr == null) {
            shouldUpdate = true;
          } else {
            int storedVersion = Integer.parseInt(storedVersionStr);
            if (storedVersion < version) {
              shouldUpdate = true;
            }
          }

          // store in redis and increment packet for writeQueue
          if (shouldUpdate) {
            jedis.set(key, String.valueOf(version));
            newValue = value + 10.5;

            totalMessagesProcessed++;
            totalAdded += 10.5;
            totalRedisUpdates++;

            ObjectNode newNode = mapper.createObjectNode();
            newNode.put("key", key);
            newNode.put("version", version);
            newNode.put("value", newValue);

            outputMessage = mapper.writeValueAsString(newNode);
          } else {
            newValue = value;
            outputMessage = message;
          }
          totalValueWritten += newValue;

          channel.basicPublish(
              "",
              request.writeQueue(),
              null,
              outputMessage.getBytes(StandardCharsets.UTF_8)
          );
          totalMessagesWritten++;
        } else {
          // else is tombstone
          jedis.flushDB();
          totalRedisUpdates++;

          ObjectNode summary = mapper.createObjectNode();
          summary.put("totalMessagesWritten", totalMessagesWritten);
          summary.put("totalMessagesProcessed", totalMessagesProcessed);
          summary.put("totalRedisUpdates", totalRedisUpdates);
          summary.put("totalValueWritten", totalValueWritten);
          summary.put("totalAdded", totalAdded);

          String tombstoneMessage = mapper.writeValueAsString(summary);

          channel.basicPublish(
              "",
              request.writeQueue(),
              null,
              tombstoneMessage.getBytes(StandardCharsets.UTF_8)
          );

          return;
        }
        messagesRead++;
      }
    }
  }

  public void seedTransformMessages(String queueName, int count) throws Exception {

    ObjectMapper mapper = new ObjectMapper();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());
    factory.setPort(rabbitMqConfiguration.getRabbitMqPort());

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {

      channel.queueDeclare(queueName, true, false, false, null);

      for (int i = 0; i < count; i++) {

        TransformMessage msg = new TransformMessage(
            "key-" + (i % 5),
            i % 3 + 1,
            100.0 + i
        );

        String json = mapper.writeValueAsString(msg);

        channel.basicPublish(
            "",
            queueName,
            null,
            json.getBytes(StandardCharsets.UTF_8)
        );
      }
    }
  }

  public void sendSingleMessage(String queueName, String key, int version, double value) throws Exception {

    ObjectMapper mapper = new ObjectMapper();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());
    factory.setPort(rabbitMqConfiguration.getRabbitMqPort());

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {
      channel.queueDeclare(queueName, true, false, false, null);
      channel.queuePurge(queueName);

      TransformMessage msg = new TransformMessage(key, version, value);
      String json = mapper.writeValueAsString(msg);

      channel.basicPublish(
          "",
          queueName,
          null,
          json.getBytes(StandardCharsets.UTF_8)
      );
    }
  }

  public void resetState() {
    totalMessagesWritten = 0;
    totalMessagesProcessed = 0;
    totalRedisUpdates = 0;
    totalValueWritten = 0.0;
    totalAdded = 0.0;
  }

  public void resetDb() throws Exception {
    Jedis jedis = new Jedis(redisConfiguration.redisHost(), redisConfiguration.redisPort());
    jedis.flushDB();
  }
}
