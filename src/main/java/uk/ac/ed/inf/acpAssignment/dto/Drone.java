package uk.ac.ed.inf.acpAssignment.dto;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.math.BigDecimal;
import uk.ac.ed.inf.acpAssignment.jsonUtils.DroneDeserializer;

public record Drone(
    String id,
    String name,
    Capability capability,
    BigDecimal costPer100Moves
) {
  public static Drone withComputedCostPer100Moves(String id, String name, Capability capability) {
    BigDecimal costPerMove = (capability == null) ? null : capability.costPerMove();
    BigDecimal costInitial = (capability == null) ? null : capability.costInitial();
    BigDecimal costFinal = (capability == null) ? null : capability.costFinal();
    BigDecimal computedCost = costPerMove.multiply(BigDecimal.valueOf(100)).add(costInitial).add(costFinal);

    return new Drone(id, name, capability, computedCost);
  }

  public Drone withComputedCostPer100Moves() {
    return withComputedCostPer100Moves(this.id(), this.name(), this.capability());
  }

  public record Capability(
      boolean cooling,
      boolean heating,
      BigDecimal capacity,
      Integer maxMoves,
      @JsonDeserialize(using = DroneDeserializer.class)
      BigDecimal costPerMove,
      @JsonDeserialize(using = DroneDeserializer.class)
      BigDecimal costInitial,
      @JsonDeserialize(using = DroneDeserializer.class)
      BigDecimal costFinal
  ) {}
}
