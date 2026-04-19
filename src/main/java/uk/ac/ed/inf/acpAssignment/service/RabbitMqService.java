package uk.ac.ed.inf.acpAssignment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.GetResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import uk.ac.ed.inf.acpAssignment.configuration.RabbitMqConfiguration;
import uk.ac.ed.inf.acpAssignment.dto.MessageWrapper;
import uk.ac.ed.inf.acpAssignment.dto.TestMessage;

@Slf4j
@Service
public class RabbitMqService {

  private final RabbitMqConfiguration rabbitMqConfiguration;

  public RabbitMqService(RabbitMqConfiguration rabbitMqConfiguration) {
    this.rabbitMqConfiguration = rabbitMqConfiguration;
  }

  public void sendMessages(String queueName, int messageCount) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {
      channel.queueDeclare(queueName, true, false, false, null);

      for (int i = 0; i < messageCount; i++) {
        String message = String.format("{\"uid\":\"%s\",\"counter\":%d}", "s2417814", i);
        channel.basicPublish("", queueName, null, message.getBytes());
        log.info("Sent message: {}", message);
      }

    }
  }

  public List<String> getMessages(String queueName, long timeoutInMsec) throws Exception {
    List<String> messages = new ArrayList<>();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());

    long deadline = System.currentTimeMillis() + timeoutInMsec - 50;

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {
      channel.queueDeclare(queueName, true, false, false, null);

      while (System.currentTimeMillis() < deadline) {
        GetResponse response = channel.basicGet(queueName, true);

        if (response == null) break;

        String message = new String(response.getBody(), StandardCharsets.UTF_8);
        messages.add(message);
      }
    }
    return messages;
  }

  public List<String> getSortedMessages(String queueName, int messagesToConsider) throws Exception {

    List<String> messages = new ArrayList<>();
    List<MessageWrapper> parsedMessages = new ArrayList<>();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());
    factory.setPort(rabbitMqConfiguration.getRabbitMqPort());

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {
      channel.queueDeclare(queueName, true, false, false, null);

      ObjectMapper mapper = new ObjectMapper();
      long start = System.currentTimeMillis();

      while (parsedMessages.size() < messagesToConsider) {
        if (System.currentTimeMillis() - start > 1000) break;
        GetResponse response = channel.basicGet(queueName, true);

        // read
        String json = new String(response.getBody(), StandardCharsets.UTF_8);
        JsonNode node = mapper.readTree(json);
        int id = node.get("Id").asInt();

        // store
        parsedMessages.add(new MessageWrapper(id, json));
      }
      // sort
      parsedMessages.sort(Comparator.comparingInt(MessageWrapper::id));

      for (MessageWrapper entry : parsedMessages) {
        messages.add(entry.json());
      }

    }
    // deliver
    return messages;
  }

  public void seedQueue(String queueName, int messageCount) throws Exception {

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());
    factory.setPort(rabbitMqConfiguration.getRabbitMqPort());

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {

      channel.queueDeclare(queueName, true, false, false, null);

      for (int i = 0; i < messageCount; i++) {
        String message = String.format("{\"Id\":%d,\"Payload\":\"String-data-%d\"}", i, i);
        channel.basicPublish("", queueName, null, message.getBytes());
      }
    }
  }

  public void clearQueue(String queueName) throws Exception {

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());
    factory.setPort(rabbitMqConfiguration.getRabbitMqPort());

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {

      channel.queueDeclare(queueName, true, false, false, null);
      channel.queuePurge(queueName);
    }
  }

  public void seedQueueSplitter(String queueName, int count) throws Exception {

    ObjectMapper mapper = new ObjectMapper();

    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());
    factory.setPort(rabbitMqConfiguration.getRabbitMqPort());

    try (Connection connection = factory.newConnection();
        Channel channel = connection.createChannel()) {

      channel.queueDeclare(queueName, true, false, false, null);

      for (int i = 0; i < count; i++) {
        TestMessage msg = new TestMessage(i, 0.1 * i, "seed-data-" + i);
        String json = mapper.writeValueAsString(msg);

        channel.basicPublish("", queueName, null, json.getBytes(StandardCharsets.UTF_8));
      }
    }
  }

  public void seedQueueSplitterOnce(String queueName, int id, double value,
      String additionalData) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());
    factory.setPort(rabbitMqConfiguration.getRabbitMqPort());

    try(Connection connection  = factory.newConnection();
    Channel channel = connection.createChannel()) {
      channel.queueDeclare(queueName, true, false, false, null);
      TestMessage msg = new TestMessage(id, value, additionalData);
      String json = mapper.writeValueAsString(msg);
      channel.basicPublish("", queueName, null, json.getBytes(StandardCharsets.UTF_8));
    }
  }
}
