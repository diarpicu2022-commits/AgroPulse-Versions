package com.agropulse.dao;

import com.agropulse.model.AutomationRule;
import com.agropulse.pattern.singleton.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class AutomationRuleDao {
    private static final String TABLE = "automation_rules";

    public AutomationRuleDao() {
        initializeTable();
    }

    private void initializeTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS %s (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT NOT NULL,
                condition_type TEXT NOT NULL,
                condition_value REAL NOT NULL,
                action_type TEXT NOT NULL,
                enabled BOOLEAN DEFAULT 1,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                last_executed TIMESTAMP,
                FOREIGN KEY(username) REFERENCES users(username)
            )
            """.formatted(TABLE);
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (SQLException e) {
            System.err.println("Error creating automation_rules table: " + e.getMessage());
        }
    }

    public void save(AutomationRule rule) throws SQLException {
        String sql = "INSERT INTO %s (username, condition_type, condition_value, action_type, enabled) VALUES (?, ?, ?, ?, ?)".formatted(TABLE);
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, rule.getUsername());
            stmt.setString(2, rule.getConditionType());
            stmt.setDouble(3, rule.getConditionValue());
            stmt.setString(4, rule.getActionType());
            stmt.setBoolean(5, rule.isEnabled());
            stmt.executeUpdate();

            ResultSet keys = stmt.getGeneratedKeys();
            if (keys.next()) {
                rule.setId(keys.getLong(1));
            }
        }
    }

    public void update(AutomationRule rule) throws SQLException {
        String sql = "UPDATE %s SET condition_type=?, condition_value=?, action_type=?, enabled=? WHERE id=?".formatted(TABLE);
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, rule.getConditionType());
            stmt.setDouble(2, rule.getConditionValue());
            stmt.setString(3, rule.getActionType());
            stmt.setBoolean(4, rule.isEnabled());
            stmt.setLong(5, rule.getId());
            stmt.executeUpdate();
        }
    }

    public void delete(long id) throws SQLException {
        String sql = "DELETE FROM %s WHERE id=?".formatted(TABLE);
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            stmt.executeUpdate();
        }
    }

    public AutomationRule findById(long id) throws SQLException {
        String sql = "SELECT * FROM %s WHERE id=?".formatted(TABLE);
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapRow(rs);
            }
        }
        return null;
    }

    public List<AutomationRule> findByUsername(String username) throws SQLException {
        String sql = "SELECT * FROM %s WHERE username=? ORDER BY created_at DESC".formatted(TABLE);
        List<AutomationRule> rules = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, username);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rules.add(mapRow(rs));
            }
        }
        return rules;
    }

    public List<AutomationRule> findAllEnabled() throws SQLException {
        String sql = "SELECT * FROM %s WHERE enabled=1 ORDER BY username".formatted(TABLE);
        List<AutomationRule> rules = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()) {
                rules.add(mapRow(rs));
            }
        }
        return rules;
    }

    public void updateLastExecuted(long id, LocalDateTime timestamp) throws SQLException {
        String sql = "UPDATE %s SET last_executed=? WHERE id=?".formatted(TABLE);
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(timestamp));
            stmt.setLong(2, id);
            stmt.executeUpdate();
        }
    }

    private AutomationRule mapRow(ResultSet rs) throws SQLException {
        AutomationRule rule = new AutomationRule();
        rule.setId(rs.getLong("id"));
        rule.setUsername(rs.getString("username"));
        rule.setConditionType(rs.getString("condition_type"));
        rule.setConditionValue(rs.getDouble("condition_value"));
        rule.setActionType(rs.getString("action_type"));
        rule.setEnabled(rs.getBoolean("enabled"));
        rule.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
        
        Timestamp lastExecuted = rs.getTimestamp("last_executed");
        if (lastExecuted != null) {
            rule.setLastExecuted(lastExecuted.toLocalDateTime());
        }
        return rule;
    }
}
