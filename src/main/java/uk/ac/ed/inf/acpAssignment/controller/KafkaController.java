package uk.ac.ed.inf.acpAssignment.controller;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ed.inf.acpAssignment.configuration.KafkaConfiguration;
import uk.ac.ed.inf.acpAssignment.service.DynamoDbService;
import uk.ac.ed.inf.acpAssignment.service.KafkaService;

@RestController()
@RequestMapping("/api/v1/acp/kafka")
public class KafkaController {

  private final KafkaConfiguration kafkaConfiguration;

  private final KafkaService kafkaService;

  public KafkaController(KafkaConfiguration kafkaConfiguration,  KafkaService kafkaService) {
    this.kafkaConfiguration = kafkaConfiguration;
    this.kafkaService = kafkaService;
  }

  @GetMapping("/servers")
  public String getBootstrapServers() {
    return kafkaConfiguration.kafkaBootstrapServers();
  }

  @GetMapping("/seed-topic/{number}")
  public void seedTopic(@PathVariable int number) {
    kafkaService.seedTopic("test-topic-new", number);
  }
}
