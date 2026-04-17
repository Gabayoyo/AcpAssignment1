package uk.ac.ed.inf.acpAssignment.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
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

  public List<String> getMessages(String writeTopic, long timeoutInMsec) {

    List<String> messages = new ArrayList<>();
    long deadline = System.currentTimeMillis() + timeoutInMsec - 250;

    try (KafkaConsumer<String, String> consumer =
        new KafkaConsumer<>(kafkaConsumerProperties)) {
      consumer.subscribe(List.of(writeTopic));
      consumer.poll(Duration.ofMillis(0));

      while (System.currentTimeMillis() < deadline) {
        long remaining = deadline - System.currentTimeMillis();
        if (remaining <= 250) break;

        ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(100));

        for (ConsumerRecord<String, String> record : records) {
          messages.add(record.value());
        }
      }
    }
    return messages;
  }
}
