package uk.ac.ed.inf.acpAssignment.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.ac.ed.inf.acpAssignment.entity.DroneEntity;

@Repository
public interface DroneRepository extends JpaRepository<DroneEntity, String> {
}
