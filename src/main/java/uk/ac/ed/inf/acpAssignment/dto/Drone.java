package uk.ac.ed.inf.acpAssignment.dto;

import java.math.BigDecimal;

public record Drone(
        String id,
        String name,
        boolean cooling,
        boolean heating,
        BigDecimal capacity,
        Integer maxMoves,
        BigDecimal costPerMove,
        BigDecimal costInitial,
        BigDecimal costFinal,
        String description
) {
}
