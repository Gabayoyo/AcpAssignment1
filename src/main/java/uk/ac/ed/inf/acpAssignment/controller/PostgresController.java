package uk.ac.ed.inf.acpAssignment.controller;

import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.ac.ed.inf.acpAssignment.configuration.PostgresConfiguration;
import uk.ac.ed.inf.acpAssignment.dto.Drone;
import uk.ac.ed.inf.acpAssignment.entity.DroneEntity;
import uk.ac.ed.inf.acpAssignment.service.PostgresService;

import java.util.List;

@RestController()
@RequestMapping("/api/v1/acp/postgres")
public class PostgresController {

    private final PostgresConfiguration postgresConfiguration;
    private final PostgresService postgresService;

    public PostgresController(PostgresConfiguration postgresConfiguration, PostgresService postgresService) {
        this.postgresConfiguration = postgresConfiguration;
        this.postgresService = postgresService;
    }
    @GetMapping("/endpoint")
    public String getPostgresEndpoint() {
        return postgresConfiguration.getPostgresEndpoint();
    }

    @GetMapping("/drones")
    public List<Drone> listDrones() {
        return postgresService.getAllDrones();
    }

    @PutMapping("/drones-jdbc")
    public ResponseEntity<String> createDroneUsingJdpc(@RequestBody Drone drone) {
        return ResponseEntity.ok(postgresService.createDroneUsingJdbc(drone));
    }

    @PutMapping("/drones-jpa")
    public ResponseEntity<String> createDroneUsingJpa(@RequestBody Drone drone) {
        return ResponseEntity.ok(postgresService.createDroneUsingJpa(drone));
    }

    @DeleteMapping("/drones/{droneId}")
    public ResponseEntity<Void> deleteDrone(@PathVariable String droneId) {
        postgresService.deleteDrone(droneId);
        return ResponseEntity.ok().build();
    }


}
