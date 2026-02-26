package uk.ac.ed.inf.acpAssignment.service;

import java.sql.Connection;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import uk.ac.ed.inf.acpAssignment.dto.Drone;
import uk.ac.ed.inf.acpAssignment.entity.DroneEntity;
import uk.ac.ed.inf.acpAssignment.mapper.DroneMapper;
import uk.ac.ed.inf.acpAssignment.repository.DroneRepository;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PostgresService {

    private final JdbcTemplate jdbcTemplate;
    private final DroneRepository droneRepository;

    public PostgresService(JdbcTemplate jdbcTemplate, DroneRepository droneRepository) {
        this.jdbcTemplate = jdbcTemplate;
        this.droneRepository = droneRepository;
    }

    /**
     * Retrieves all drones from the database and maps them to Drone DTOs.
     * @return List of Drone DTOs representing all drones in the database.
     */
    public List<Drone> getAllDrones() {
        return jdbcTemplate.query(
                "SELECT * FROM ilp.drones",
                    new BeanPropertyRowMapper<>(DroneEntity.class))
                .stream()
                    .map(DroneMapper::entityToDto)
                .toList();
    }

    public List<Map<String, Object>> getRows(String table) throws Exception {
        String sql = "SELECT * FROM " + table;
        return jdbcTemplate.queryForList(sql);
    }

    public List<String> listTables() {
        String sql = """
            SELECT table_name
            FROM information_schema.tables
            WHERE table_schema = 's2417814'
            ORDER BY table_name
            """;
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public String currentSchema() {
        return jdbcTemplate.queryForObject("select current_schema()", String.class);
    }

    public List<String> listSchemas() {
        String sql = """
            SELECT schema_name
            FROM information_schema.schemata
            ORDER BY schema_name
            """;
        return jdbcTemplate.queryForList(sql, String.class);
    }

    public void addDronesToTable(Drone[] drones, @PathVariable String table) {
        for (Drone drone : drones) {
            createDroneInTableUsingJdbc(drone, table);
        }
    }

    @Transactional
    public String createDroneInTableUsingJdbc(Drone drone, @PathVariable String table) {
        var createDrone = DroneMapper.dtoToEntity(drone);
        // createDrone.setId(UUID.randomUUID().toString());

        String sql = "INSERT INTO " + table + " (id, name, cooling, heating, capacity, "
            + "maxMoves, costPerMove, costInitial, costFinal, costper100moves) VALUES (?, ?, ?, "
            + "?, ?, "
            + "?, "
            + "?, ?, "
            + "?, ?)";

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, createDrone.getId());
            ps.setString(2, createDrone.getName());
            ps.setBoolean(3, createDrone.isCooling());
            ps.setBoolean(4, createDrone.isHeating());
            ps.setBigDecimal(5, createDrone.getCapacity());
            ps.setInt(6, createDrone.getMaxMoves());
            ps.setBigDecimal(7, createDrone.getCostPerMove());
            ps.setBigDecimal(8, createDrone.getCostInitial());
            ps.setBigDecimal(9, createDrone.getCostFinal());
            ps.setBigDecimal(10, createDrone.getCostPer100Moves());
            return ps;
        });

        return createDrone.getId();
    }

    @Transactional
    public String createDroneUsingJdbc(Drone drone) {
        var createDrone = DroneMapper.dtoToEntity(drone);
        // createDrone.setId(UUID.randomUUID().toString());

        String sql = "INSERT INTO s2417814.drones (id, name, cooling, heating, capacity, "
            + "maxMoves, costPerMove, costInitial, costFinal) VALUES (?, ?, ?, ?, ?, ?, ?, ?, "
            + "?)";

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql);
            ps.setString(1, createDrone.getId());
            ps.setString(2, createDrone.getName());
            ps.setBoolean(3, createDrone.isCooling());
            ps.setBoolean(4, createDrone.isHeating());
            ps.setBigDecimal(5, createDrone.getCapacity());
            ps.setInt(6, createDrone.getMaxMoves());
            ps.setBigDecimal(7, createDrone.getCostPerMove());
            ps.setBigDecimal(8, createDrone.getCostInitial());
            ps.setBigDecimal(9, createDrone.getCostFinal());
            return ps;
        });

        return createDrone.getId();
    }

    @Transactional
    public String createDroneUsingJpa(Drone drone) {
        var createDrone = DroneMapper.dtoToEntity(drone);
        createDrone.setId(UUID.randomUUID().toString());
        return droneRepository.save(createDrone).getId();
    }

    @Transactional
    public void deleteDrone(String droneId) {
        droneRepository.deleteById(droneId);
    }
}
