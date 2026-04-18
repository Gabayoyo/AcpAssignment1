package uk.ac.ed.inf.acpAssignment.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.ac.ed.inf.acpAssignment.service.SplitterService;

@RestController()
@RequestMapping("/api/v1/acp/splitter")
public class SplitterController {

  private final SplitterService splitterService;

  public SplitterController(SplitterService splitterService) {
    this.splitterService = splitterService;
  }

  @GetMapping("/seed-queue/{hashEven}/{hashOdd}")
  public ResponseEntity<?> seedRedisHashes(@PathVariable String hashEven, @PathVariable String hashOdd) {
    try {
      splitterService.seedRedisHash(hashEven, 50, true);
      splitterService.seedRedisHash(hashOdd, 50, false);
      return new ResponseEntity<>(HttpStatus.OK);
    } catch (Exception e) {
      return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }
  }
}

