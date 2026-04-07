package com.agropulse.dao;

import com.agropulse.model.User;
import com.agropulse.model.enums.UserRole;
import com.agropulse.pattern.singleton.DatabaseConnection;
import com.agropulse.util.CryptoUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * DAO para la entidad User con cifrado automático.
 *
 * CIFRADO:
 *   - password  → PBKDF2-SHA512 (hash, no reversible)
 *   - full_name → AES-256-GCM (cifrado, descifrable)
 *   - phone     → AES-256-GCM (cifrado, descifrable)
 *
 * MIGRACIÓN AUTOMÁTICA:
 *   Al autenticar con contraseña plana (sistema antiguo), el hash
 *   se actualiza automáticamente al nuevo formato seguro.
 */
public class UserDao implements GenericDao<User> {

    private final Connection localConn;
    private final DatabaseConnection dbConn;

    public UserDao() {
        this.dbConn    = DatabaseConnection.getInstance();
        this.localConn = dbConn.getConnection();
    }

    @Override
    public int save(User user) {
        String sql = "INSERT INTO users (username, password, full_name, phone, role, active, created_at) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        // Cifrar datos sensibles antes de guardar
        String hashedPwd    = CryptoUtils.hashPassword(user.getPassword());
        String encFullName  = CryptoUtils.encrypt(user.getFullName());
        String encPhone     = CryptoUtils.encrypt(user.getPhone() != null ? user.getPhone() : "");

        int id = executeInsert(localConn, sql, user.getUsername(), hashedPwd,
                               encFullName, encPhone, user.getRole().name(),
                               user.isActive() ? 1 : 0, user.getCreatedAt().toString());

        // Sincronizar a BD online si disponible
        syncToOnline(sql, user.getUsername(), hashedPwd, encFullName, encPhone,
                     user.getRole().name(), user.isActive() ? 1 : 0, user.getCreatedAt().toString());

        if (id > 0) user.setId(id);
        return id;
    }

    @Override
    public void update(User user) {
        String sql = "UPDATE users SET username=?, password=?, full_name=?, phone=?, role=?, active=? WHERE id=?";

        // Solo re-hashear si la contraseña no es ya un hash seguro
        String storedPwd = CryptoUtils.isLegacyPassword(user.getPassword())
                ? CryptoUtils.hashPassword(user.getPassword())
                : user.getPassword();
        String encName  = CryptoUtils.encrypt(user.getFullName());
        String encPhone = CryptoUtils.encrypt(user.getPhone() != null ? user.getPhone() : "");

        executeUpdate(localConn, sql, user.getUsername(), storedPwd,
                      encName, encPhone, user.getRole().name(),
                      user.isActive() ? 1 : 0, user.getId());

        syncUpdateOnline(sql, user.getUsername(), storedPwd, encName, encPhone,
                         user.getRole().name(), user.isActive() ? 1 : 0, user.getId());
    }

    @Override
    public void delete(int id) {
        executeUpdate(localConn, "DELETE FROM users WHERE id=?", id);
        syncUpdateOnline("DELETE FROM users WHERE id=?", id);
    }

    @Override
    public Optional<User> findById(int id) {
        try (PreparedStatement ps = localConn.prepareStatement("SELECT * FROM users WHERE id=?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return Optional.of(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al buscar usuario: " + e.getMessage());
        }
        return Optional.empty();
    }

    @Override
    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        try (Statement stmt = localConn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM users")) {
            while (rs.next()) users.add(mapRow(rs));
        } catch (SQLException e) {
            System.err.println("  [DAO] Error al listar usuarios: " + e.getMessage());
        }
        return users;
    }

    /**
     * Autenticación con:
     *  1. Verificación PBKDF2 del hash.
     *  2. Migración automática de contraseñas planas al nuevo formato.
     */
    public Optional<User> authenticate(String username, String rawPassword) {
        String sql = "SELECT * FROM users WHERE username=? AND active=1";
        try (PreparedStatement ps = localConn.prepareStatement(sql)) {
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                String storedHash = rs.getString("password");

                // Verificar contraseña (soporta hash nuevo Y texto plano antiguo)
                boolean valid = CryptoUtils.verifyPassword(rawPassword, storedHash);
                if (!valid) return Optional.empty();

                User user = mapRow(rs);

                // ── Migración automática al primer login ──────────────
                if (CryptoUtils.isLegacyPassword(storedHash)) {
                    System.out.println("  [Auth] Migrando contraseña de '" + username + "' a hash seguro.");
                    String newHash = CryptoUtils.hashPassword(rawPassword);
                    executeUpdate(localConn, "UPDATE users SET password=? WHERE id=?",
                                  newHash, user.getId());
                    user.setPassword(rawPassword); // Mantener en memoria como texto para sesión
                }

                return Optional.of(user);
            }
        } catch (SQLException e) {
            System.err.println("  [DAO] Error en autenticación: " + e.getMessage());
        }
        return Optional.empty();
    }

    // ─── Mapeo fila → modelo (con descifrado) ─────────────────────────

    private User mapRow(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setUsername(rs.getString("username"));
        user.setPassword(rs.getString("password")); // Hash almacenado
        // Descifrar datos personales
        user.setFullName(CryptoUtils.decrypt(rs.getString("full_name")));
        user.setPhone(CryptoUtils.decrypt(rs.getString("phone")));
        user.setRole(UserRole.valueOf(rs.getString("role")));
        user.setActive(rs.getInt("active") == 1);
        String ts = rs.getString("created_at");
        try {
            user.setCreatedAt(LocalDateTime.parse(ts));
        } catch (Exception e) {
            user.setCreatedAt(LocalDateTime.now());
        }
        return user;
    }

    // ─── Helpers para dual-write ──────────────────────────────────────

    private int executeInsert(Connection conn, String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            setParams(ps, params);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("  [DAO] Error INSERT: " + e.getMessage());
        }
        return -1;
    }

    private void executeUpdate(Connection conn, String sql, Object... params) {
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            setParams(ps, params);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [DAO] Error UPDATE: " + e.getMessage());
        }
    }

    private void syncToOnline(String sql, Object... params) {
        Connection online = dbConn.getOnlineConnection();
        if (online == null) return;
        executeInsert(online, sql, params);
    }

    private void syncUpdateOnline(String sql, Object... params) {
        Connection online = dbConn.getOnlineConnection();
        if (online == null) return;
        executeUpdate(online, sql, params);
    }

    private void setParams(PreparedStatement ps, Object... params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            if      (params[i] instanceof String)  ps.setString(i+1, (String) params[i]);
            else if (params[i] instanceof Integer) ps.setInt(i+1, (Integer) params[i]);
            else if (params[i] instanceof Long)    ps.setLong(i+1, (Long) params[i]);
            else if (params[i] instanceof Double)  ps.setDouble(i+1, (Double) params[i]);
            else if (params[i] == null)            ps.setNull(i+1, Types.VARCHAR);
            else                                   ps.setObject(i+1, params[i]);
        }
    }
}
