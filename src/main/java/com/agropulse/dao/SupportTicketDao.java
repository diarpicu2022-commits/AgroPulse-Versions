package com.agropulse.dao;

import com.agropulse.model.SupportTicket;
import com.agropulse.model.SupportTicket.Priority;
import com.agropulse.model.SupportTicket.Status;
import com.agropulse.pattern.singleton.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/** DAO para tickets de soporte técnico. */
public class SupportTicketDao implements GenericDao<SupportTicket> {

    private final Connection        local;
    private final DatabaseConnection db;

    public SupportTicketDao() {
        this.db    = DatabaseConnection.getInstance();
        this.local = db.getConnection();
        ensureTable();
    }

    @Override
    public int save(SupportTicket t) {
        String sql = "INSERT INTO support_tickets " +
                "(user_id, greenhouse_id, subject, description, status, priority, created_at, updated_at)" +
                " VALUES (?,?,?,?,?,?,?,?)";
        int id = exec(local, sql,
                t.getUserId(), t.getGreenhouseId(), t.getSubject(), t.getDescription(),
                t.getStatus().name(), t.getPriority().name(),
                t.getCreatedAt().toString(), t.getUpdatedAt().toString());
        if (id > 0) {
            t.setId(id);
            Connection o = db.getOnlineConnection();
            if (o != null) exec(o, sql,
                t.getUserId(), t.getGreenhouseId(), t.getSubject(), t.getDescription(),
                t.getStatus().name(), t.getPriority().name(),
                t.getCreatedAt().toString(), t.getUpdatedAt().toString());
        }
        return id;
    }

    @Override
    public void update(SupportTicket t) {
        t.setUpdatedAt(LocalDateTime.now());
        String sql = "UPDATE support_tickets SET status=?, priority=?, admin_response=?, updated_at=? WHERE id=?";
        execVoid(local, sql, t.getStatus().name(), t.getPriority().name(),
                 t.getAdminResponse(), t.getUpdatedAt().toString(), t.getId());
        Connection o = db.getOnlineConnection();
        if (o != null) execVoid(o, sql, t.getStatus().name(), t.getPriority().name(),
                 t.getAdminResponse(), t.getUpdatedAt().toString(), t.getId());
    }

    @Override
    public void delete(int id) {
        execVoid(local, "DELETE FROM support_tickets WHERE id=?", id);
        Connection o = db.getOnlineConnection();
        if (o != null) execVoid(o, "DELETE FROM support_tickets WHERE id=?", id);
    }

    @Override
    public Optional<SupportTicket> findById(int id) {
        try (PreparedStatement ps = local.prepareStatement(
                "SELECT t.*, u.full_name, g.name as gname FROM support_tickets t" +
                " LEFT JOIN users u ON t.user_id=u.id" +
                " LEFT JOIN greenhouses g ON t.greenhouse_id=g.id" +
                " WHERE t.id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(map(rs));
        } catch (SQLException e) { err(e); }
        return Optional.empty();
    }

    @Override
    public List<SupportTicket> findAll() {
        return query("SELECT t.*, u.full_name, g.name as gname FROM support_tickets t" +
                " LEFT JOIN users u ON t.user_id=u.id" +
                " LEFT JOIN greenhouses g ON t.greenhouse_id=g.id" +
                " ORDER BY t.created_at DESC");
    }

    /** Tickets de un usuario específico. */
    public List<SupportTicket> findByUser(int userId) {
        List<SupportTicket> list = new ArrayList<>();
        String sql = "SELECT t.*, u.full_name, g.name as gname FROM support_tickets t" +
                " LEFT JOIN users u ON t.user_id=u.id" +
                " LEFT JOIN greenhouses g ON t.greenhouse_id=g.id" +
                " WHERE t.user_id=? ORDER BY t.created_at DESC";
        try (PreparedStatement ps = local.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { err(e); }
        return list;
    }

    /** Tickets abiertos (para el admin). */
    public List<SupportTicket> findOpen() {
        return query("SELECT t.*, u.full_name, g.name as gname FROM support_tickets t" +
                " LEFT JOIN users u ON t.user_id=u.id" +
                " LEFT JOIN greenhouses g ON t.greenhouse_id=g.id" +
                " WHERE t.status IN ('OPEN','IN_PROGRESS') ORDER BY t.created_at DESC");
    }

    // ─── tabla ────────────────────────────────────────────────────────

    private void ensureTable() {
        try (Statement st = local.createStatement()) {
            // Detectar tipo de BD
            boolean isPg = local.getMetaData().getDatabaseProductName().toLowerCase().contains("postgresql");
            String autoInc = isPg ? "SERIAL PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";
            
            st.execute("CREATE TABLE IF NOT EXISTS support_tickets (" +
                "id             " + autoInc + "," +
                "user_id        INTEGER NOT NULL," +
                "greenhouse_id  INTEGER DEFAULT 0," +
                "subject        TEXT    NOT NULL," +
                "description    TEXT    NOT NULL," +
                "admin_response TEXT," +
                "status         TEXT    NOT NULL DEFAULT 'OPEN'," +
                "priority       TEXT    NOT NULL DEFAULT 'MEDIUM'," +
                "created_at     TEXT    NOT NULL," +
                "updated_at     TEXT    NOT NULL" +
            ")");
        } catch (SQLException e) { err(e); }
    }

    // ─── helpers ──────────────────────────────────────────────────────

    private SupportTicket map(ResultSet rs) throws SQLException {
        SupportTicket t = new SupportTicket();
        t.setId(rs.getInt("id"));
        t.setUserId(rs.getInt("user_id"));
        t.setGreenhouseId(rs.getInt("greenhouse_id"));
        t.setSubject(rs.getString("subject"));
        t.setDescription(rs.getString("description"));
        t.setAdminResponse(rs.getString("admin_response"));
        try { t.setStatus(Status.valueOf(rs.getString("status"))); } catch (Exception e) { t.setStatus(Status.OPEN); }
        try { t.setPriority(Priority.valueOf(rs.getString("priority"))); } catch (Exception e) { t.setPriority(Priority.MEDIUM); }
        try { t.setCreatedAt(LocalDateTime.parse(rs.getString("created_at"))); } catch (Exception ignored) {}
        try { t.setUpdatedAt(LocalDateTime.parse(rs.getString("updated_at"))); } catch (Exception ignored) {}
        // joined fields
        try { t.setUserName(rs.getString("full_name")); } catch (Exception ignored) {}
        try { t.setGreenhouseName(rs.getString("gname")); } catch (Exception ignored) {}
        return t;
    }

    private List<SupportTicket> query(String sql) {
        List<SupportTicket> list = new ArrayList<>();
        try (Statement st = local.createStatement(); ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) list.add(map(rs));
        } catch (SQLException e) { err(e); }
        return list;
    }

    private int exec(Connection conn, String sql, Object... p) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) { err(e); }
        return -1;
    }

    private void execVoid(Connection conn, String sql, Object... p) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (int i = 0; i < p.length; i++) ps.setObject(i + 1, p[i]);
            ps.executeUpdate();
        } catch (SQLException e) { err(e); }
    }

    private void err(SQLException e) { System.err.println("  [TicketDao] " + e.getMessage()); }
}
