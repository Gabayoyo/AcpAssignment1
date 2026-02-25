package uk.ac.ed.inf.acpAssignment.utils;

import java.util.List;
import uk.ac.ed.inf.acpAssignment.dto.Drone;

public class DroneUtils {
  public static Drone[] computeDronesCostPer100Moves(Drone[] drones) {
    return java.util.Arrays.stream(drones)
        .map(Drone::withComputedCostPer100Moves)
        .toArray(Drone[]::new);
  }

}
