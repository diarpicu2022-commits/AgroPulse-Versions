package com.agropulse.dao;

import com.agropulse.model.Crop;
import com.agropulse.pattern.singleton.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para cultivos.
 */
public class CropDao implements GenericDao<Crop> {

    private final Connection conn;

    public CropDao() {
        this.conn = DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public int save(Crop crop) {
        String sql = "INSERT INTO crops (name, variety, temp_min, temp_max, humidity_min, " +
                     "humidity_max, soil_moisture_min, soil_moisture_max, planting_date, active) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, crop.getName());
            ps.setString(2, crop.getVariety());
            ps.setDouble(3, crop.getTempMin());
            ps.setDouble(4, crop.getTempMax());
            ps.setDouble(5, crop.getHumidityMin());
            ps.setDouble(6, crop.getHumidityMax());
            ps.setDouble(7, crop.getSoilMoistureMin());
            ps.setDouble(8, crop.getSoilMoistureMax());
            ps.setString(9, crop.getPlantingDate().toString());
            ps.setInt(10, crop.isActive() ? 1 : 0);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                int id = rs.getInt(1);
                crop.setId(id);
                return id;
            }
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al guardar cultivo: " + e.getMessage());
        }
        return -1;
    }

    @Override
    public void update(Crop crop) {
        String sql = "UPDATE crops SET name=?, variety=?, temp_min=?, temp_max=?, humidity_min=?, " +
                     "humidity_max=?, soil_moisture_min=?, soil_moisture_max=?, planting_date=?, " +
                     "active=? WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, crop.getName());
            ps.setString(2, crop.getVariety());
            ps.setDouble(3, crop.getTempMin());
            ps.setDouble(4, crop.getTempMax());
            ps.setDouble(5, crop.getHumidityMin());
            ps.setDouble(6, crop.getHumidityMax());
            ps.setDouble(7, crop.getSoilMoistureMin());
            ps.setDouble(8, crop.getSoilMoistureMax());
            ps.setString(9, crop.getPlantingDate().toString());
            ps.setInt(10, crop.isActive() ? 1 : 0);
            ps.setInt(11, crop.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al actualizar cultivo: " + e.getMessage());
        }
    }

    @Override
    public void delete(int id) {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM crops WHERE id=?")) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
    }

    @Override
    public Optional<Crop> findById(int id) {
        try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM crops WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<Crop> findAll() {
        List<Crop> list = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM crops")) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return list;
    }

    /**
     * Obtener el cultivo activo actual.
     */
    public Optional<Crop> findActiveCrop() {
        String sql = "SELECT * FROM crops WHERE active=1 ORDER BY id DESC LIMIT 1";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error: " + e.getMessage());
        }
        return Optional.empty();
    }

    private Crop mapRow(ResultSet rs) throws SQLException {
        Crop c = new Crop();
        c.setId(rs.getInt("id"));
        c.setName(rs.getString("name"));
        c.setVariety(rs.getString("variety"));
        c.setTempMin(rs.getDouble("temp_min"));
        c.setTempMax(rs.getDouble("temp_max"));
        c.setHumidityMin(rs.getDouble("humidity_min"));
        c.setHumidityMax(rs.getDouble("humidity_max"));
        c.setSoilMoistureMin(rs.getDouble("soil_moisture_min"));
        c.setSoilMoistureMax(rs.getDouble("soil_moisture_max"));
        String date = rs.getString("planting_date");
        if (date != null) {
            try {
                c.setPlantingDate(LocalDate.parse(date));
            } catch (Exception e) {
                c.setPlantingDate(LocalDate.now());
            }
        }
        c.setActive(rs.getInt("active") == 1);
        return c;
    }
}
