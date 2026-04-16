package uk.ac.ed.inf.acpAssignment.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfiguration {
  @Value("${rabbitmq.host:localhost}")
  private String host;

  @Value("${rabbitmq.port:5672}")
  private int port;

  @Bean(name = "rabbitMqHost")
  public String getRabbitMqHost() {
    return host;
  }

  @Bean(name = "rabbitMqPort")
  public Integer getRabbitMqPort() {
    return port;
  }
}
