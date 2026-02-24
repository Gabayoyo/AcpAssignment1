package uk.ac.ed.inf.acpAssignment.mapper;

import org.springframework.stereotype.Component;
import uk.ac.ed.inf.acpAssignment.dto.Drone;
import uk.ac.ed.inf.acpAssignment.entity.DroneEntity;

@Component
public class DroneMapper {

    public static Drone entityToDto(DroneEntity e) {
        if (e == null) return null;
        Drone.Capability capability = new Drone.Capability(
            e.isCooling(),
            e.isHeating(),
            e.getCapacity(),
            e.getMaxMoves(),
            e.getCostPerMove(),
            e.getCostInitial(),
            e.getCostFinal()
        );
        return new Drone(
                e.getId(),
                e.getName(),
                capability,
                e.getCostPer100Moves()
        );
    }

    public static DroneEntity dtoToEntity(uk.ac.ed.inf.acpAssignment.dto.Drone d) {
        if (d == null) return null;
        DroneEntity e = new DroneEntity();
        e.setId(d.id());
        e.setName(d.name());
        e.setCooling(d.capability().cooling());
        e.setHeating(d.capability().heating());
        e.setCapacity(d.capability().capacity());
        e.setMaxMoves(d.capability().maxMoves());
        e.setCostPerMove(d.capability().costPerMove());
        e.setCostInitial(d.capability().costInitial());
        e.setCostFinal(d.capability().costFinal());
        e.setCostPer100Moves(d.costPer100Moves());
        return e;
    }
}
