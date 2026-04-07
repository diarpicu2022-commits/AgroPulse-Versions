package com.agropulse.dao;

import com.agropulse.model.SystemLog;
import com.agropulse.pattern.singleton.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para logs del sistema.
 */
public class SystemLogDao implements GenericDao<SystemLog> {

    private final Connection conn;

    public SystemLogDao() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public int save(SystemLog log) {
        String sql = "INSERT INTO system_logs (action, details, performed_by, timestamp) VALUES (?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, log.getAction());
            ps.setString(2, log.getDetails());
            ps.setString(3, log.getPerformedBy());
            ps.setString(4, log.getTimestamp().toString());
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                log.setId(id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al guardar log: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public void update(SystemLog log) { /* Los logs no se actualizan */ }

    @Override
    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM system_logs WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
    }

    @Override
    public Optional<SystemLog> findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM system_logs WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<SystemLog> findAll() {
        List<SystemLog> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT * FROM system_logs ORDER BY timestamp DESC LIMIT 100")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return list;
    }

    private SystemLog mapRow(ResultSet rs) throws SQLException {
        SystemLog l = new SystemLog();
        l.setId(rs.getInt("id"));
        l.setAction(rs.getString("action"));
        l.setDetails(rs.getString("details"));
        l.setPerformedBy(rs.getString("performed_by"));
        try {
            l.setTimestamp(LocalDateTime.parse(rs.getString("timestamp")));
        } catch (Exception e) {
            l.setTimestamp(LocalDateTime.now());
        }
        return l;
    }
}
