package com.agropulse.dao;

import com.agropulse.model.Alert;
import com.agropulse.model.enums.AlertLevel;
import com.agropulse.pattern.singleton.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para alertas.
 */
public class AlertDao implements GenericDao<Alert> {

    private final Connection conn;

    public AlertDao() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public int save(Alert alert) {
        String sql = "INSERT INTO alerts (message, level, sent, created_at) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, alert.getMessage());
            ps.setString(2, alert.getLevel().name());
            ps.setInt(3, alert.isSent() ? 1 : 0);
            ps.setString(4, alert.getCreatedAt().toString());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                alert.setId(id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al guardar alerta: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public void update(Alert alert) {
        String sql = "UPDATE alerts SET message=?, level=?, sent=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, alert.getMessage());
            ps.setString(2, alert.getLevel().name());
            ps.setInt(3, alert.isSent() ? 1 : 0);
            ps.setInt(4, alert.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM alerts WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
    }

    @Override
    public Optional<Alert> findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM alerts WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Alert> findAll() {
        List<Alert> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT * FROM alerts ORDER BY created_at DESC LIMIT 50")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Obtener alertas no enviadas por WhatsApp.
     */
    public List<Alert> findUnsent() {
        List<Alert> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT * FROM alerts WHERE sent=0 ORDER BY created_at ASC")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return list;
    }

    private Alert mapRow(ResultSet rs) throws SQLException {
        Alert a = new Alert();
        a.setId(rs.getInt("id"));
        a.setMessage(rs.getString("message"));
        a.setLevel(AlertLevel.valueOf(rs.getString("level")));
        a.setSent(rs.getInt("sent") == 1);
        try {
            a.setCreatedAt(LocalDateTime.parse(rs.getString("created_at")));
        } catch (Exception e) {
            a.setCreatedAt(LocalDateTime.now());
        }
        return a;
    }
}
