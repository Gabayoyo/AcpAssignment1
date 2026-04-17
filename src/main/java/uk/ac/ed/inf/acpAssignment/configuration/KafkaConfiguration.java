package uk.ac.ed.inf.acpAssignment.configuration;

import java.util.Properties;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaConfiguration {

  @Value("${kafka.bootstrap-servers}")
  private String bootstrapServers;

  @Value("${kafka.acks}")
  private String acks;

  @Value("${kafka.key-serializer}")
  private String keySerializer;

  @Value("${kafka.value-serializer}")
  private String valueSerializer;

  @Value("${kafka.key-deserializer}")
  private String keyDeserializer;

  @Value("${kafka.value-deserializer}")
  private String valueDeserializer;

  @Value("${kafka.group-id:assignment-consumer}")
  private String groupId;

  @Value("${kafka.auto-offset-reset:earliest}")
  private String autoOffsetReset;

  @Value("${kafka.enable-auto-commit:true}")
  private String enableAutoCommit;

  @Bean(name = "kafkaBootstrapServers")
  public String kafkaBootstrapServers() {
    return bootstrapServers;
  }

  @Bean(name = "kafkaProducerProperties")
  public Properties kafkaProducerProperties() {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ProducerConfig.ACKS_CONFIG, acks);
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, keySerializer);
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, valueSerializer);
    return props;
  }

  @Bean(name = "kafkaConsumerProperties")
  public Properties kafkaConsumerProperties() {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, autoOffsetReset);
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, keyDeserializer);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, valueDeserializer);
    props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, enableAutoCommit);
    return props;
  }
}
