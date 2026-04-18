package uk.ac.ed.inf.acpAssignment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ed.inf.acpAssignment.service.SplitterService;
import uk.ac.ed.inf.acpAssignment.service.TransformService;

@RestController()
@RequestMapping("/api/v1/acp/transform")
public class TransformController {

  private final SplitterService splitterService;
  private final TransformService transformService;

  public TransformController(SplitterService splitterService, TransformService transformService) {
    this.splitterService = splitterService;
    this.transformService = transformService;
  }

  @GetMapping("/seed-queue/{queueName}/{number}")
  public ResponseEntity<?> seedTransformMessages(@PathVariable String queueName, @PathVariable int number) {
    try {
      transformService.seedTransformMessages(queueName, number);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/seed-queue/{queueName}/{key}/{version}/{value}")
  public ResponseEntity<?> sendSingleMessage(@PathVariable String queueName, @PathVariable String key,
      @PathVariable int version, @PathVariable double value) {
    try {
      transformService.sendSingleMessage(queueName, key, version, value);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }

  @GetMapping("/reset-state")
  public ResponseEntity<?> resetState() {
    transformService.resetState();
    return new ResponseEntity<>(HttpStatus.OK);
  }

  @GetMapping("/reset-DB")
  public ResponseEntity<?> resetDB() {
    try {
      transformService.resetDb();
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}

