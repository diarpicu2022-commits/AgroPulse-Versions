package com.agropulse.dao;

import com.agropulse.model.Actuator;
import com.agropulse.model.enums.ActuatorType;
import com.agropulse.pattern.singleton.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para actuadores.
 */
public class ActuatorDao implements GenericDao<Actuator> {

    private final Connection conn;

    public ActuatorDao() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public int save(Actuator actuator) {
        String sql = "INSERT INTO actuators (name, type, enabled, auto_mode, last_toggled) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, actuator.getName());
            ps.setString(2, actuator.getType().name());
            ps.setInt(3, actuator.isEnabled() ? 1 : 0);
            ps.setInt(4, actuator.isAutoMode() ? 1 : 0);
            ps.setString(5, actuator.getLastToggled() != null ? actuator.getLastToggled().toString() : null);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                actuator.setId(id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al guardar actuador: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public void update(Actuator actuator) {
        String sql = "UPDATE actuators SET name=?, type=?, enabled=?, auto_mode=?, last_toggled=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, actuator.getName());
            ps.setString(2, actuator.getType().name());
            ps.setInt(3, actuator.isEnabled() ? 1 : 0);
            ps.setInt(4, actuator.isAutoMode() ? 1 : 0);
            ps.setString(5, actuator.getLastToggled() != null ? actuator.getLastToggled().toString() : null);
            ps.setInt(6, actuator.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al actualizar actuador: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM actuators WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
    }

    @Override
    public Optional<Actuator> findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM actuators WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Actuator> findAll() {
        List<Actuator> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM actuators")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return list;
    }

    private Actuator mapRow(ResultSet rs) throws SQLException {
        Actuator a = new Actuator();
        a.setId(rs.getInt("id"));
        a.setName(rs.getString("name"));
        a.setType(ActuatorType.valueOf(rs.getString("type")));
        a.setEnabled(rs.getInt("enabled") == 1);
        a.setAutoMode(rs.getInt("auto_mode") == 1);
        String toggled = rs.getString("last_toggled");
        if (toggled != null) {
            try {
                a.setLastToggled(LocalDateTime.parse(toggled));
            } catch (Exception e) { /* ignorar */ }
        }
        return a;
    }
}
