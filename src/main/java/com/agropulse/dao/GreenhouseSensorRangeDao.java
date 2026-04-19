package com.agropulse.dao;

import com.agropulse.model.GreenhouseSensorRange;
import com.agropulse.pattern.singleton.DatabaseConnection;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class GreenhouseSensorRangeDao {

    private Connection conn;

    public GreenhouseSensorRangeDao() {
        this.conn = DatabaseConnection.getInstance().getConnection();
        createTableIfNotExists();
    }

    private void createTableIfNotExists() {
        try (Statement st = conn.createStatement()) {
            // Detectar tipo de BD
            boolean isPg = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
            String autoInc = isPg ? "SERIAL PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
            
            st.execute("CREATE TABLE IF NOT EXISTS greenhouse_sensor_ranges (" +
                "id " + autoInc + ", " +
                "greenhouse_id INTEGER NOT NULL, " +
                "sensor_type VARCHAR(50) NOT NULL, " +
                "range_min REAL NOT NULL, " +
                "range_max REAL NOT NULL, " +
                "UNIQUE(greenhouse_id, sensor_type))");
        } catch (SQLException e) {
            System.err.println("Error creando tabla: " + e.getMessage());
        }
    }

    public void save(GreenhouseSensorRange r) {
        try {
            // Detectar tipo de BD
            boolean isPg = conn.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
            
            String sql;
            if (isPg) {
                // PostgreSQL UPSERT
                sql = "INSERT INTO greenhouse_sensor_ranges (greenhouse_id, sensor_type, range_min, range_max) " +
                      "VALUES (?, ?, ?, ?) " +
                      "ON CONFLICT (greenhouse_id, sensor_type) " +
                      "DO UPDATE SET range_min=EXCLUDED.range_min, range_max=EXCLUDED.range_max";
            } else {
                // SQLite INSERT OR REPLACE
                sql = "INSERT OR REPLACE INTO greenhouse_sensor_ranges (greenhouse_id, sensor_type, range_min, range_max) VALUES (?, ?, ?, ?)";
            }
            
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                ps.setInt(1, r.getGreenhouseId());
                ps.setString(2, r.getSensorType());
                ps.setDouble(3, r.getRangeMin());
                ps.setDouble(4, r.getRangeMax());
                ps.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Error guardando rango: " + e.getMessage());
        }
    }

    public List<GreenhouseSensorRange> findByGreenhouse(int greenhouseId) {
        List<GreenhouseSensorRange> list = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM greenhouse_sensor_ranges WHERE greenhouse_id = ?")) {
            ps.setInt(1, greenhouseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                GreenhouseSensorRange r = new GreenhouseSensorRange();
                r.setId(rs.getInt("id"));
                r.setGreenhouseId(rs.getInt("greenhouse_id"));
                r.setSensorType(rs.getString("sensor_type"));
                r.setRangeMin(rs.getDouble("range_min"));
                r.setRangeMax(rs.getDouble("range_max"));
                list.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Error leyendo rangos: " + e.getMessage());
        }
        return list;
    }
}