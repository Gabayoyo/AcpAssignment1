package uk.ac.ed.inf.acpAssignment.service;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.GetResponse;
import java.nio.charset.StandardCharsets;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.stereotype.Service;
import uk.ac.ed.inf.acpAssignment.configuration.KafkaConfiguration;
import uk.ac.ed.inf.acpAssignment.configuration.RabbitMqConfiguration;
import uk.ac.ed.inf.acpAssignment.configuration.RedisConfiguration;
import uk.ac.ed.inf.acpAssignment.dto.SplitterRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import redis.clients.jedis.Jedis;
import uk.ac.ed.inf.acpAssignment.dto.TestMessage;

@Service
public class SplitterService {

  private final RedisConfiguration redisConfiguration;
  private final RabbitMqConfiguration rabbitMqConfiguration;

  private final KafkaConfiguration kafkaConfiguration;

  public SplitterService(RedisConfiguration redisConfiguration,
                         RabbitMqConfiguration rabbitMqConfiguration,
                         KafkaConfiguration kafkaConfiguration) {
    this.redisConfiguration = redisConfiguration;
    this.rabbitMqConfiguration = rabbitMqConfiguration;
    this.kafkaConfiguration = kafkaConfiguration;
  }

  public void process(SplitterRequest request) throws Exception {
    ObjectMapper mapper = new ObjectMapper();
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost(rabbitMqConfiguration.getRabbitMqHost());

    try (Jedis jedis = new Jedis(redisConfiguration.redisHost(), redisConfiguration.redisPort());
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        KafkaProducer<String, String> producer =
            new KafkaProducer<>(kafkaConfiguration.kafkaProducerProperties())) {

      channel.queueDeclare(request.readQueue(), true, false, false, null);

      int countEven = Integer.parseInt(jedis.get("count_even") != null ? jedis.get("count_even") : "0");
      int countOdd = Integer.parseInt(jedis.get("count_odd") != null ? jedis.get("count_odd") : "0");
      double avgEven = Double.parseDouble(jedis.get("average_even") != null ? jedis.get("average_even") : "0");
      double avgOdd = Double.parseDouble(jedis.get("average_odd") != null ? jedis.get("average_odd") : "0");
      double sumEven = avgEven * countEven;
      double sumOdd = avgOdd * countOdd;

      int processed = 0;

      while (processed < request.messageCount()) {

        GetResponse response = channel.basicGet(request.readQueue(), true);
        if (response == null) continue;

        String message = new String(response.getBody(), StandardCharsets.UTF_8);
        JsonNode json = mapper.readTree(message);

        int id = json.get("Id").asInt();
        double value = json.get("Value").asDouble();

        if (id % 2 == 0) {
          // if even
          jedis.hset(request.redisHashEven(), String.valueOf(id), message);
          producer.send(new ProducerRecord<>(request.writeTopicEven(), message));
          countEven++;
          sumEven += value;

        } else {
          // else odd
          jedis.hset(request.redisHashOdd(), String.valueOf(id), message);
          producer.send(new ProducerRecord<>(request.writeTopicOdd(), message));
          countOdd++;
          sumOdd += value;
        }
        processed++;
      }
      producer.flush();

      double newAvgEven = countEven == 0 ? 0 : sumEven / countEven;
      double newAvgOdd = countOdd == 0 ? 0 : sumOdd / countOdd;

      jedis.set("average_even", String.format("%.2f", newAvgEven));
      jedis.set("average_odd", String.format("%.2f", newAvgOdd));

      jedis.set("count_even", String.valueOf(countEven));
      jedis.set("count_odd", String.valueOf(countOdd));
    }
  }

  public void seedRedisHash(String redisHashName, int count, boolean even) throws Exception {
    ObjectMapper mapper = new ObjectMapper();

    try (Jedis jedis = new Jedis(redisConfiguration.redisHost(),
        redisConfiguration.redisPort())) {

      for (int i = 0; i < count; i++) {
        int id = even ? i * 2 : i * 2 + 1;
        TestMessage msg = new TestMessage(id, 0.5 + i, "seed-data-" + id);
        String json = mapper.writeValueAsString(msg);

        jedis.hset(redisHashName, String.valueOf(id), json);
      }
    }
  }
}
