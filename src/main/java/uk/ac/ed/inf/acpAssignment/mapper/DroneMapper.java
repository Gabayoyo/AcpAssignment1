package uk.ac.ed.inf.acpAssignment.mapper;

import org.springframework.stereotype.Component;
import uk.ac.ed.inf.acpAssignment.entity.DroneEntity;

@Component
public class DroneMapper {

    public static uk.ac.ed.inf.acpAssignment.dto.Drone entityToDto(DroneEntity e) {
        if (e == null) return null;
        return new uk.ac.ed.inf.acpAssignment.dto.Drone(
                e.getId(),
                e.getName(),
                e.isCooling(),
                e.isHeating(),
                e.getCapacity(),
                e.getMaxMoves(),
                e.getCostPerMove(),
                e.getCostInitial(),
                e.getCostFinal(),
                e.getDescription()
        );
    }

    public static DroneEntity dtoToEntity(uk.ac.ed.inf.acpAssignment.dto.Drone d) {
        if (d == null) return null;
        DroneEntity e = new DroneEntity();
        e.setId(d.id());
        e.setName(d.name());
        e.setCooling(d.cooling());
        e.setHeating(d.heating());
        e.setCapacity(d.capacity());
        e.setMaxMoves(d.maxMoves());
        e.setCostPerMove(d.costPerMove());
        e.setCostInitial(d.costInitial());
        e.setCostFinal(d.costFinal());
        e.setDescription(d.description());
        return e;
    }
}
