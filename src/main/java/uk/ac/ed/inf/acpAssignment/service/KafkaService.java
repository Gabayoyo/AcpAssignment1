package uk.ac.ed.inf.acpAssignment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.PartitionInfo;
import org.apache.kafka.common.TopicPartition;
import org.springframework.stereotype.Service;
import uk.ac.ed.inf.acpAssignment.configuration.KafkaConfiguration;

@Slf4j
@Service
public class KafkaService {

  public KafkaConfiguration kafkaConfiguration;
  public Properties kafkaProducerProperties;
  public Properties kafkaConsumerProperties;

  public KafkaService(KafkaConfiguration kafkaConfiguration,
                      Properties kafkaProducerProperties,
                      Properties kafkaConsumerProperties) {
    this.kafkaConfiguration = kafkaConfiguration;
    this.kafkaProducerProperties = kafkaProducerProperties;
    this.kafkaConsumerProperties = kafkaConsumerProperties;
  }

  public void sendMessages(String writeTopic, int messageCount) {
    try(KafkaProducer<String, String> producer =
        new KafkaProducer<>(kafkaProducerProperties)) {
      for (int i = 0; i < messageCount; i++) {
          String message = String.format("{\"uid\":\"%s\",\"counter\":%d}", "s2417814", i);
          ProducerRecord<String, String> record = new ProducerRecord<>(writeTopic, message);

          producer.send(record);
      }
      producer.flush();
    }
  }

  public List<String> getMessages(String topic, long timeoutInMsec) {

    List<String> messages = new ArrayList<>();
    long deadline = System.currentTimeMillis() + timeoutInMsec;

    try (KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(kafkaConsumerProperties)) {

      List<PartitionInfo> partitions = consumer.partitionsFor(topic);
      List<TopicPartition> topicPartitions = partitions.stream()
          .map(p -> new TopicPartition(topic, p.partition()))
          .toList();

      consumer.assign(topicPartitions);
      consumer.seekToBeginning(topicPartitions);

      while (System.currentTimeMillis() < deadline) {

        long remaining = deadline - System.currentTimeMillis();
        if (remaining < 250) break;

        ConsumerRecords<String, String> records =
            consumer.poll(Duration.ofMillis(100));

        for (ConsumerRecord<String, String> record : records) {
          messages.add(record.value());
        }
      }
    }

    return messages;
  }

  public List<String> getSortedMessages(String topic, int messagesToConsider) throws Exception {

    List<JsonNode> buffer = new ArrayList<>();
    ObjectMapper mapper = new ObjectMapper();

    try (KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(kafkaConsumerProperties)) {
      consumer.subscribe(List.of(topic));

      long hardDeadline = System.currentTimeMillis() + 10000;

      while (buffer.size() < messagesToConsider &&
          System.currentTimeMillis() < hardDeadline) {
        ConsumerRecords<String, String> records =
            consumer.poll(Duration.ofMillis(100));

        for (ConsumerRecord<String, String> record : records) {
          JsonNode node = mapper.readTree(record.value());
          buffer.add(node);

          if (buffer.size() >= messagesToConsider) break;
        }
      }
    }
    buffer.sort(Comparator.comparingInt(n -> n.get("Id").asInt()));

    List<String> result = new ArrayList<>();
    for (JsonNode node : buffer) {
      result.add(node.toString());
    }

    return result;
  }

  public void seedTopic(String topic, int count) {
    try (KafkaProducer<String, String> producer =
        new KafkaProducer<>(kafkaProducerProperties)) {

      for (int i = 0; i < count; i++) {
        String message = String.format("{\"Id\":%d,\"Payload\":\"String-data-%d\"}", i, i);
        producer.send(new ProducerRecord<>(topic, message));
      }

      producer.flush();
    }
  }
}
