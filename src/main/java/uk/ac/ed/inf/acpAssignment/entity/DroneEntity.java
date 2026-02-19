package uk.ac.ed.inf.acpAssignment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;

/**
 * JPA Entity mapping for the ilp.drones table.
 */
@Entity
@Table(name = "drones", schema = "ilp")
public class DroneEntity {

    @Id
    @Column(length = 50)
    private String id;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(nullable = false)
    private boolean cooling = false;

    @Column(nullable = false)
    private boolean heating = false;

    @Column(precision = 10, scale = 2, nullable = false)
    private BigDecimal capacity;

    @Column(name = "max_moves", nullable = false)
    private Integer maxMoves;

    @Column(name = "cost_per_move", precision = 10, scale = 2, nullable = false)
    private BigDecimal costPerMove;

    @Column(name = "cost_initial", precision = 10, scale = 2, nullable = false)
    private BigDecimal costInitial;

    @Column(name = "cost_final", precision = 10, scale = 2, nullable = false)
    private BigDecimal costFinal;

    @Column(length = 255)
    private String description;

    public DroneEntity() {
        // JPA requires a no-arg constructor
    }

    public DroneEntity(String id, String name, boolean cooling, boolean heating, BigDecimal capacity, Integer maxMoves,
                       BigDecimal costPerMove, BigDecimal costInitial, BigDecimal costFinal, String description) {
        this.id = id;
        this.name = name;
        this.cooling = cooling;
        this.heating = heating;
        this.capacity = capacity;
        this.maxMoves = maxMoves;
        this.costPerMove = costPerMove;
        this.costInitial = costInitial;
        this.costFinal = costFinal;
        this.description = description;
    }

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCooling() {
        return cooling;
    }

    public void setCooling(boolean cooling) {
        this.cooling = cooling;
    }

    public boolean isHeating() {
        return heating;
    }

    public void setHeating(boolean heating) {
        this.heating = heating;
    }

    public BigDecimal getCapacity() {
        return capacity;
    }

    public void setCapacity(BigDecimal capacity) {
        this.capacity = capacity;
    }

    public Integer getMaxMoves() {
        return maxMoves;
    }

    public void setMaxMoves(Integer maxMoves) {
        this.maxMoves = maxMoves;
    }

    public BigDecimal getCostPerMove() {
        return costPerMove;
    }

    public void setCostPerMove(BigDecimal costPerMove) {
        this.costPerMove = costPerMove;
    }

    public BigDecimal getCostInitial() {
        return costInitial;
    }

    public void setCostInitial(BigDecimal costInitial) {
        this.costInitial = costInitial;
    }

    public BigDecimal getCostFinal() {
        return costFinal;
    }

    public void setCostFinal(BigDecimal costFinal) {
        this.costFinal = costFinal;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
