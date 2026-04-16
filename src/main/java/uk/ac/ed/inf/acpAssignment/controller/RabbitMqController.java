package uk.ac.ed.inf.acpAssignment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ed.inf.acpAssignment.configuration.RabbitMqConfiguration;
import uk.ac.ed.inf.acpAssignment.service.RabbitMqService;

@RestController()
@RequestMapping("/api/v1/acp/rabbit")
public class RabbitMqController {
  private final RabbitMqConfiguration rabbitMqConfiguration;
  private final RabbitMqService rabbitMqService;

  public RabbitMqController(RabbitMqConfiguration rabbitMqConfiguration, RabbitMqService rabbitMqService) {
    this.rabbitMqConfiguration = rabbitMqConfiguration;
    this.rabbitMqService = rabbitMqService;
  }

  @GetMapping("/host")
  public String getHost() {
    return rabbitMqConfiguration.getRabbitMqHost();
  }

  @GetMapping("/port")
  public int getPort() {
    return rabbitMqConfiguration.getRabbitMqPort();
  }

  @GetMapping("/seed-test-queue/{number}")
  public ResponseEntity<?> seedTestQueue(@PathVariable int number) {
    try {
      rabbitMqService.seedQueue("test", number);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/clear-queue/{queueName}")
  public ResponseEntity<?> clearQueue(@PathVariable String queueName) {
    try {
      rabbitMqService.clearQueue(queueName);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch  (Exception e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}
