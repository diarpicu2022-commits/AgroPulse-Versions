package com.agropulse.dao;

import com.agropulse.model.SensorReading;
import com.agropulse.model.enums.SensorType;
import com.agropulse.pattern.singleton.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para lecturas de sensores con dual-write (SQLite local + BD online).
 * Cada lectura se guarda localmente primero; luego se replica online si hay conexión.
 */
public class SensorReadingDao implements GenericDao<SensorReading> {

    private final Connection       localConn;
    private final DatabaseConnection dbConn;

    public SensorReadingDao() {
        this.dbConn    = DatabaseConnection.getInstance();
        this.localConn = dbConn.getConnection();
    }

    /**
     * Guarda una lectura asociada a un invernadero específico.
     * Usa esto cuando quieras filtrar datos por invernadero después.
     */
    public int saveWithGreenhouse(SensorReading reading, int greenhouseId) {
        String sql = "INSERT INTO sensor_readings (sensor_id, sensor_type, value, timestamp, source, greenhouse_id) " +
                     "VALUES (?, ?, ?, ?, ?, ?)";
        String source = reading.getSource() != null ? reading.getSource() : "MANUAL";
        int id = -1;
        try (PreparedStatement ps = localConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,    reading.getSensorId());
            ps.setString(2, reading.getSensorType().name());
            ps.setDouble(3, reading.getValue());
            ps.setString(4, reading.getTimestamp().toString());
            ps.setString(5, source);
            ps.setInt(6,    greenhouseId);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) { id = rs.getInt(1); reading.setId(id); }
        } catch (SQLException e) {
            System.err.println("  [DAO] Error guardando lectura con invernadero: " + e.getMessage());
        }
        Connection online = dbConn.getOnlineConnection();
        if (online != null) {
            try (PreparedStatement ps = online.prepareStatement(sql)) {
                ps.setInt(1, reading.getSensorId()); ps.setString(2, reading.getSensorType().name());
                ps.setDouble(3, reading.getValue()); ps.setString(4, reading.getTimestamp().toString());
                ps.setString(5, source);             ps.setInt(6, greenhouseId);
                ps.executeUpdate();
            } catch (SQLException e) { System.err.println("  [DAO-Online] " + e.getMessage()); }
        }
        return id;
    }

    /**
     * Devuelve las últimas 100 lecturas de un invernadero específico.
     * Si greenhouseId == 0, devuelve todas (sin filtro).
     */
    public List<SensorReading> findByGreenhouse(int greenhouseId) {
        if (greenhouseId == 0) return findAll();
        List<SensorReading> list = new ArrayList<>();
        try (PreparedStatement ps = localConn.prepareStatement(
                "SELECT * FROM sensor_readings WHERE greenhouse_id=? ORDER BY timestamp DESC LIMIT 100")) {
            ps.setInt(1, greenhouseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("  [DAO] " + e.getMessage()); }
        return list;
    }

    /**
     * Devuelve lecturas de un tipo específico filtradas por invernadero.
     */
    public List<SensorReading> findByGreenhouseAndType(int greenhouseId, SensorType type, int limit) {
        if (greenhouseId == 0) return findByType(type, limit);
        List<SensorReading> list = new ArrayList<>();
        try (PreparedStatement ps = localConn.prepareStatement(
                "SELECT * FROM sensor_readings WHERE greenhouse_id=? AND sensor_type=? " +
                "ORDER BY timestamp DESC LIMIT ?")) {
            ps.setInt(1, greenhouseId); ps.setString(2, type.name()); ps.setInt(3, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("  [DAO] " + e.getMessage()); }
        return list;
    }

    @Override
    public int save(SensorReading reading) {
        String sql = "INSERT INTO sensor_readings (sensor_id, sensor_type, value, timestamp, source) " +
                     "VALUES (?, ?, ?, ?, ?)";
        String source = reading.getSource() != null ? reading.getSource() : "MANUAL";
        int id = -1;

        // 1️⃣ Guardar local siempre
        try (PreparedStatement ps = localConn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1,    reading.getSensorId());
            ps.setString(2, reading.getSensorType().name());
            ps.setDouble(3, reading.getValue());
            ps.setString(4, reading.getTimestamp().toString());
            ps.setString(5, source);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) { id = rs.getInt(1); reading.setId(id); }
        } catch (SQLException e) {
            System.err.println("  [DAO] Error guardando lectura local: " + e.getMessage());
        }

        // 2️⃣ Replicar online (fire-and-forget, sin bloquear)
        Connection online = dbConn.getOnlineConnection();
        if (online != null) {
            try (PreparedStatement ps = online.prepareStatement(sql)) {
                ps.setInt(1,    reading.getSensorId());
                ps.setString(2, reading.getSensorType().name());
                ps.setDouble(3, reading.getValue());
                ps.setString(4, reading.getTimestamp().toString());
                ps.setString(5, source);
                ps.executeUpdate();
            } catch (SQLException e) {
                // No es fatal — datos ya están en SQLite local
                System.err.println("  [DAO-Online] Fallo replicación lectura: " + e.getMessage());
            }
        }

        return id;
    }

    @Override public void update(SensorReading r) { /* Lecturas son inmutables */ }

    @Override
    public void delete(int id) {
        exec(localConn, "DELETE FROM sensor_readings WHERE id=?", id);
        Connection online = dbConn.getOnlineConnection();
        if (online != null) exec(online, "DELETE FROM sensor_readings WHERE id=?", id);
    }

    @Override
    public Optional<SensorReading> findById(int id) {
        try (PreparedStatement ps = localConn.prepareStatement(
                "SELECT * FROM sensor_readings WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) { System.err.println("  [DAO] " + e.getMessage()); }
        return Optional.empty();
    }

    @Override
    public List<SensorReading> findAll() {
        return query("SELECT * FROM sensor_readings ORDER BY timestamp DESC LIMIT 100");
    }

    public List<SensorReading> findBySensorId(int sensorId, int limit) {
        List<SensorReading> list = new ArrayList<>();
        try (PreparedStatement ps = localConn.prepareStatement(
                "SELECT * FROM sensor_readings WHERE sensor_id=? ORDER BY timestamp DESC LIMIT ?")) {
            ps.setInt(1, sensorId); ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("  [DAO] " + e.getMessage()); }
        return list;
    }

    public List<SensorReading> findByType(SensorType type, int limit) {
        List<SensorReading> list = new ArrayList<>();
        try (PreparedStatement ps = localConn.prepareStatement(
                "SELECT * FROM sensor_readings WHERE sensor_type=? ORDER BY timestamp DESC LIMIT ?")) {
            ps.setString(1, type.name()); ps.setInt(2, limit);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("  [DAO] " + e.getMessage()); }
        return list;
    }

    public double getAverage(SensorType type, int lastN) {
        String sql = "SELECT AVG(value) FROM " +
                     "(SELECT value FROM sensor_readings WHERE sensor_type=? ORDER BY timestamp DESC LIMIT ?)";
        try (PreparedStatement ps = localConn.prepareStatement(sql)) {
            ps.setString(1, type.name()); ps.setInt(2, lastN);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) { System.err.println("  [DAO] " + e.getMessage()); }
        return 0.0;
    }

    // ─── helpers ─────────────────────────────────────────────────────

    private List<SensorReading> query(String sql) {
        List<SensorReading> list = new ArrayList<>();
        try (Statement st = localConn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) { System.err.println("  [DAO] " + e.getMessage()); }
        return list;
    }

    private void exec(Connection conn, String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < params.length; i++) ps.setObject(i + 1, params[i]);
            ps.executeUpdate();
        } catch (SQLException e) { System.err.println("  [DAO] " + e.getMessage()); }
    }

    private SensorReading mapRow(ResultSet rs) throws SQLException {
        SensorReading r = new SensorReading();
        r.setId(rs.getInt("id"));
        r.setSensorId(rs.getInt("sensor_id"));
        r.setSensorType(SensorType.valueOf(rs.getString("sensor_type")));
        r.setValue(rs.getDouble("value"));
        try { r.setTimestamp(LocalDateTime.parse(rs.getString("timestamp"))); }
        catch (Exception e) { r.setTimestamp(LocalDateTime.now()); }
        // source column (puede no existir en BD antiguas)
        try { r.setSource(rs.getString("source")); } catch (Exception ignored) {}
        return r;
    }
}
