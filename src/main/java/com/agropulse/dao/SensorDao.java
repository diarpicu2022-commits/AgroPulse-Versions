package com.agropulse.dao;

import com.agropulse.model.Sensor;
import com.agropulse.model.enums.SensorType;
import com.agropulse.pattern.singleton.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para sensores.
 */
public class SensorDao implements GenericDao<Sensor> {

    private final Connection conn;

    public SensorDao() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public int save(Sensor sensor) {
        String sql = "INSERT INTO sensors (name, type, location, last_value, active) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, sensor.getName());
            ps.setString(2, sensor.getType().name());
            ps.setString(3, sensor.getLocation());
            ps.setDouble(4, sensor.getLastValue());
            ps.setInt(5, sensor.isActive() ? 1 : 0);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                sensor.setId(id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al guardar sensor: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public void update(Sensor sensor) {
        String sql = "UPDATE sensors SET name=?, type=?, location=?, last_value=?, active=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sensor.getName());
            ps.setString(2, sensor.getType().name());
            ps.setString(3, sensor.getLocation());
            ps.setDouble(4, sensor.getLastValue());
            ps.setInt(5, sensor.isActive() ? 1 : 0);
            ps.setInt(6, sensor.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al actualizar sensor: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM sensors WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al eliminar sensor: " + e.getMessage());
        }
    }

    @Override
    public Optional<Sensor> findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM sensors WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al buscar sensor: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Sensor> findAll() {
        List<Sensor> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM sensors")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al listar sensores: " + e.getMessage());
        }
        return list;
    }

    public List<Sensor> findByType(SensorType type) {
        List<Sensor> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM sensors WHERE type=?")) {
            ps.setString(1, type.name());
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return list;
    }

    private Sensor mapRow(ResultSet rs) throws SQLException {
        Sensor s = new Sensor();
        s.setId(rs.getInt("id"));
        s.setName(rs.getString("name"));
        s.setType(SensorType.valueOf(rs.getString("type")));
        s.setLocation(rs.getString("location"));
        s.setLastValue(rs.getDouble("last_value"));
        s.setActive(rs.getInt("active") == 1);
        return s;
    }
}
