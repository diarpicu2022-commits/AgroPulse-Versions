package com.agropulse.dao;

import com.agropulse.model.Greenhouse;
import com.agropulse.pattern.singleton.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * DAO para invernaderos.
 * Incluye gestión de asignaciones usuario↔invernadero.
 */
public class GreenhouseDao implements GenericDao<Greenhouse> {

    private final Connection       local;
    private final DatabaseConnection db;

    public GreenhouseDao() {
        this.db    = DatabaseConnection.getInstance();
        this.local = db.getConnection();
        ensureTables();
    }

    // ─── CRUD ─────────────────────────────────────────────────────────

    @Override
    public int save(Greenhouse g) {
        String sql = "INSERT INTO greenhouses (name, location, description, owner_id, active, created_at)" +
                     " VALUES (?,?,?,?,?,?)";
        int id = exec(local, sql,
                g.getName(), g.getLocation(), g.getDescription(),
                g.getOwnerId(), g.isActive() ? 1 : 0, g.getCreatedAt().toString());
        if (id > 0) {
            g.setId(id);
            syncOnline(sql, g.getName(), g.getLocation(), g.getDescription(),
                    g.getOwnerId(), g.isActive() ? 1 : 0, g.getCreatedAt().toString());
        }
        return id;
    }

    @Override
    public void update(Greenhouse g) {
        String sql = "UPDATE greenhouses SET name=?, location=?, description=?, active=? WHERE id=?";
        execVoid(local, sql, g.getName(), g.getLocation(), g.getDescription(),
                 g.isActive() ? 1 : 0, g.getId());
        syncOnlineVoid(sql, g.getName(), g.getLocation(), g.getDescription(),
                 g.isActive() ? 1 : 0, g.getId());
    }

    @Override
    public void delete(int id) {
        execVoid(local, "DELETE FROM greenhouses WHERE id=?", id);
        syncOnlineVoid("DELETE FROM greenhouses WHERE id=?", id);
    }

    @Override
    public Optional<Greenhouse> findById(int id) {
        try (PreparedStatement ps = local.prepareStatement(
                "SELECT * FROM greenhouses WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        } catch (SQLException e) { err(e); }
        return Optional.empty();
    }

    @Override
    public List<Greenhouse> findAll() {
        return query("SELECT * FROM greenhouses WHERE active=1 ORDER BY name");
    }

    // ─── Consultas especiales ─────────────────────────────────────────

    /** Todos los invernaderos de un admin (creados por él). */
    public List<Greenhouse> findByOwner(int ownerId) {
        List<Greenhouse> list = new ArrayList<>();
        try (PreparedStatement ps = local.prepareStatement(
                "SELECT * FROM greenhouses WHERE owner_id=? AND active=1 ORDER BY name")) {
            ps.setInt(1, ownerId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { err(e); }
        return list;
    }

    /** Invernaderos asignados a un usuario regular. */
    public List<Greenhouse> findByUser(int userId) {
        List<Greenhouse> list = new ArrayList<>();
        String sql = "SELECT g.* FROM greenhouses g " +
                     "INNER JOIN user_greenhouse ug ON g.id=ug.greenhouse_id " +
                     "WHERE ug.user_id=? AND g.active=1 ORDER BY g.name";
        try (PreparedStatement ps = local.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { err(e); }
        return list;
    }

    // ─── Asignaciones usuario↔invernadero ─────────────────────────────

    /** Asignar un invernadero a un usuario. */
    public void assignUser(int userId, int greenhouseId) {
        String sql = "INSERT OR IGNORE INTO user_greenhouse (user_id, greenhouse_id, assigned_at) VALUES (?,?,?)";
        execVoid(local, sql, userId, greenhouseId, LocalDateTime.now().toString());
        syncOnlineVoid(sql, userId, greenhouseId, LocalDateTime.now().toString());
    }

    /** Desasignar un invernadero de un usuario. */
    public void unassignUser(int userId, int greenhouseId) {
        String sql = "DELETE FROM user_greenhouse WHERE user_id=? AND greenhouse_id=?";
        execVoid(local, sql, userId, greenhouseId);
        syncOnlineVoid(sql, userId, greenhouseId);
    }

    /** IDs de invernaderos asignados a un usuario. */
    public List<Integer> getAssignedGreenhouseIds(int userId) {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = local.prepareStatement(
                "SELECT greenhouse_id FROM user_greenhouse WHERE user_id=?")) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) { err(e); }
        return ids;
    }

    /** Usuarios asignados a un invernadero. */
    public List<Integer> getAssignedUserIds(int greenhouseId) {
        List<Integer> ids = new ArrayList<>();
        try (PreparedStatement ps = local.prepareStatement(
                "SELECT user_id FROM user_greenhouse WHERE greenhouse_id=?")) {
            ps.setInt(1, greenhouseId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) ids.add(rs.getInt(1));
        } catch (SQLException e) { err(e); }
        return ids;
    }

    // ─── Inicialización de tablas ─────────────────────────────────────

    public void ensureTables() {
        try (Statement st = local.createStatement()) {
            st.execute("CREATE TABLE IF NOT EXISTS greenhouses (" +
                "id          INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name        TEXT    NOT NULL," +
                "location    TEXT," +
                "description TEXT," +
                "owner_id    INTEGER NOT NULL," +
                "active      INTEGER NOT NULL DEFAULT 1," +
                "created_at  TEXT    NOT NULL" +
            ")");
            st.execute("CREATE TABLE IF NOT EXISTS user_greenhouse (" +
                "user_id       INTEGER NOT NULL," +
                "greenhouse_id INTEGER NOT NULL," +
                "assigned_at   TEXT    NOT NULL," +
                "PRIMARY KEY (user_id, greenhouse_id)" +
            ")");
            // Invernadero por defecto si no hay ninguno
            st.execute(
                "INSERT OR IGNORE INTO greenhouses (id, name, location, description, owner_id, created_at)" +
                " VALUES (1, 'Invernadero Principal', 'Sede principal', 'Invernadero por defecto', 1, datetime('now'))");
        } catch (SQLException e) { err(e); }
    }

    // ─── Helpers ──────────────────────────────────────────────────────

    private Greenhouse map(ResultSet rs) throws SQLException {
        Greenhouse g = new Greenhouse();
        g.setId(rs.getInt("id"));
        g.setName(rs.getString("name"));
        g.setLocation(rs.getString("location"));
        g.setDescription(rs.getString("description"));
        g.setOwnerId(rs.getInt("owner_id"));
        g.setActive(rs.getInt("active") == 1);
        try { g.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"))); }
        catch (Exception e) { g.setCreatedAt(LocalDateTime.now()); }
        return g;
    }

    private List<Greenhouse> query(String sql) {
        List<Greenhouse> list = new ArrayList<>();
        try (Statement st = local.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { err(e); }
        return list;
    }

    private int exec(Connection conn, String sql, Object... p) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setP(ps, p); ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { err(e); }
        return -1;
    }

    private void execVoid(Connection conn, String sql, Object... p) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setP(ps, p); ps.executeUpdate();
        } catch (SQLException e) { err(e); }
    }

    private void syncOnline(String sql, Object... p) {
        Connection o = db.getOnlineConnection();
        if (o != null) exec(o, sql, p);
    }

    private void syncOnlineVoid(String sql, Object... p) {
        Connection o = db.getOnlineConnection();
        if (o != null) execVoid(o, sql, p);
    }

    private void setP(PreparedStatement ps, Object... p) throws SQLException {
        for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
    }

    private void err(SQLException e) {
        System.err.println("  [GreenhouseDao] " + e.getMessage());
    }
}
