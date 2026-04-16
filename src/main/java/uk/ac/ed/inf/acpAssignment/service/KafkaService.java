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

  public KafkaService(final KafkaConfiguration kafkaConfiguration) {
    this.kafkaConfiguration = kafkaConfiguration;
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
      List<String> results = new ArrayList<>();
      long endTime = System.currentTimeMillis() + timeoutInMsec;
      long pollTimeout = 100;

      try (KafkaConsumer<String, String> consumer =
          new KafkaConsumer<>(kafkaConsumerProperties)) {

        consumer.subscribe(List.of(writeTopic));

        while (System.currentTimeMillis() < endTime) {
          long remaining = endTime - System.currentTimeMillis();
          if (remaining <= 0) break;

          ConsumerRecords<String, String> records =
              consumer.poll(Duration.ofMillis(Math.min(pollTimeout, remaining)));

          for (ConsumerRecord<String, String> record : records) {
            results.add(record.value());
          }
          if (System.currentTimeMillis() + 50 > endTime) break;
        }
      return results;
    }
  }
}
