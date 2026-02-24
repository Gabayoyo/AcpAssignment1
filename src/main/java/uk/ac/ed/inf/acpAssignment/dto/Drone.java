package uk.ac.ed.inf.acpAssignment.dto;

import java.math.BigDecimal;

public record Drone(
    String id,
    String name,
    Capability capability,
    String description
) {
  public record Capability(
      boolean cooling,
      boolean heating,
      BigDecimal capacity,
      Integer maxMoves,
      BigDecimal costPerMove,
      BigDecimal costInitial,
      BigDecimal costFinal
  ) {}
}
