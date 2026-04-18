package uk.ac.ed.inf.acpAssignment.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RedisConfiguration {

  @Value("${redis.host:localhost}")
  private String redisHost;

  @Value("${redis.port:6379}")
  private int redisPort;

  @Bean(name = "redisHost")
  public String redisHost() {
    return redisHost;
  }

  @Bean(name = "redisPort")
  public Integer redisPort() {
    return redisPort;
  }
}