package com.agropulse.pattern.singleton;

import java.sql.*;

/**
 * PATRÓN SINGLETON — Conexión dual a bases de datos.
 *
 * LOCAL:   SQLite  (siempre disponible, sin configuración)
 * ONLINE:  PostgreSQL via Supabase (opcional, configurable)
 *
 * Estrategia: "Local-First"
 *   1. Los datos siempre se guardan en SQLite primero.
 *   2. Si hay conexión online activa, también se sincronizan.
 *   3. Si la BD online falla, la app sigue funcionando con SQLite.
 *
 * Configurar Supabase:
 *   1. Crear proyecto en https://supabase.com (gratis)
 *   2. Ir a Settings → Database → Connection string (JDBC)
 *   3. Pegar en el panel  🔌 Configurar APIs → Base de datos online
 */
public class DatabaseConnection {

    private static DatabaseConnection instance;

    // Conexión local SQLite
    private Connection localConn;
    private static final String LOCAL_DB_URL = "jdbc:sqlite:agropulse.db";

    // Conexión online PostgreSQL (Supabase)
    private Connection onlineConn;
    private boolean onlineEnabled = false;
    private String  onlineUrl     = "";

    // ─── Singleton ────────────────────────────────────────────────────

    private DatabaseConnection() {
        connectLocal();
        initializeDatabase(localConn);
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
    }

    // ─── Conexiones ───────────────────────────────────────────────────

    /** Conexión local SQLite (siempre disponible). */
    public Connection getConnection() {
        try {
            if (localConn == null || localConn.isClosed()) connectLocal();
        } catch (SQLException e) {
            connectLocal();
        }
        return localConn;
    }

    /** Conexión online PostgreSQL/Supabase (puede ser null si no está configurada). */
    public Connection getOnlineConnection() {
        if (!onlineEnabled || onlineUrl.isBlank()) return null;
        try {
            if (onlineConn == null || onlineConn.isClosed()) connectOnline();
        } catch (SQLException e) {
            System.err.println("  [DB-Online] Reconexión fallida: " + e.getMessage());
            return null;
        }
        return onlineConn;
    }

    /** Intenta conectar a la BD online con la URL dada. Devuelve true si éxito. */
    public boolean configureOnline(String jdbcUrl, boolean enabled) {
        this.onlineUrl     = jdbcUrl;
        this.onlineEnabled = enabled;
        if (!enabled || jdbcUrl.isBlank()) {
            closeOnline();
            return false;
        }
        return connectOnline();
    }

    public boolean isOnlineAvailable() {
        try {
            return onlineEnabled && onlineConn != null && !onlineConn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public String getOnlineUrl()       { return onlineUrl; }
    public boolean isOnlineEnabled()   { return onlineEnabled; }

    // ─── Privados ─────────────────────────────────────────────────────

    private void connectLocal() {
        try {
            localConn = DriverManager.getConnection(LOCAL_DB_URL);
            System.out.println("  [DB-Local] SQLite conectado.");
        } catch (SQLException e) {
            System.err.println("  [DB-Local] Error: " + e.getMessage());
        }
    }

    private boolean connectOnline() {
        try {
            // PostgreSQL driver class
            Class.forName("org.postgresql.Driver");
            onlineConn = DriverManager.getConnection(onlineUrl);
            System.out.println("  [DB-Online] PostgreSQL/Supabase conectado.");
            initializeDatabase(onlineConn);  // Crear tablas también en online
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("  [DB-Online] Driver PostgreSQL no encontrado. Agregar dependencia.");
            return false;
        } catch (SQLException e) {
            System.err.println("  [DB-Online] Error de conexión: " + e.getMessage());
            onlineConn = null;
            return false;
        }
    }

    private void closeOnline() {
        try {
            if (onlineConn != null && !onlineConn.isClosed()) {
                onlineConn.close();
            }
        } catch (SQLException ignored) {}
        onlineConn = null;
    }

    // ─── Inicialización de tablas ─────────────────────────────────────

    /**
     * Crea todas las tablas si no existen.
     * Compatible con SQLite y PostgreSQL (uso de TEXT/REAL/INTEGER estándar).
     */
    public void initializeDatabase(Connection conn) {
        if (conn == null) return;
        try (Statement stmt = conn.createStatement()) {
            boolean isPg = conn.getMetaData().getDatabaseProductName()
                              .toLowerCase().contains("postgresql");

            String autoInc = isPg ? "SERIAL PRIMARY KEY" : "INTEGER PRIMARY KEY AUTOINCREMENT";

            stmt.execute("CREATE TABLE IF NOT EXISTS users (" +
                "id          " + autoInc + "," +
                "username    TEXT    UNIQUE NOT NULL," +
                "password    TEXT    NOT NULL," +       // PBKDF2-SHA512 hash
                "full_name   TEXT    NOT NULL," +       // AES-256-GCM cifrado
                "phone       TEXT," +                  // AES-256-GCM cifrado
                "role        TEXT    NOT NULL DEFAULT 'USER'," +
                "active      INTEGER NOT NULL DEFAULT 1," +
                "created_at  TEXT    NOT NULL" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS sensors (" +
                "id         " + autoInc + "," +
                "name       TEXT NOT NULL," +
                "type       TEXT NOT NULL," +
                "location   TEXT," +
                "last_value REAL DEFAULT 0.0," +
                "active     INTEGER NOT NULL DEFAULT 1" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS sensor_readings (" +
                "id          " + autoInc + "," +
                "sensor_id   INTEGER NOT NULL," +
                "sensor_type TEXT    NOT NULL," +
                "value       REAL    NOT NULL," +
                "timestamp   TEXT    NOT NULL," +
                "source      TEXT    DEFAULT 'MANUAL'," + // MANUAL|ESP32_SERIAL|ESP32_LORA|ESP32_WIFI|FILE
                "device_id   TEXT" +                      // ID del ESP32 origen
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS actuators (" +
                "id           " + autoInc + "," +
                "name         TEXT    NOT NULL," +
                "type         TEXT    NOT NULL," +
                "enabled      INTEGER NOT NULL DEFAULT 0," +
                "auto_mode    INTEGER NOT NULL DEFAULT 1," +
                "last_toggled TEXT" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS crops (" +
                "id                " + autoInc + "," +
                "name              TEXT NOT NULL," +
                "variety           TEXT," +
                "temp_min          REAL NOT NULL," +
                "temp_max          REAL NOT NULL," +
                "humidity_min      REAL NOT NULL," +
                "humidity_max      REAL NOT NULL," +
                "soil_moisture_min REAL NOT NULL," +
                "soil_moisture_max REAL NOT NULL," +
                "planting_date     TEXT," +
                "active            INTEGER NOT NULL DEFAULT 1" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS alerts (" +
                "id         " + autoInc + "," +
                "message    TEXT NOT NULL," +
                "level      TEXT NOT NULL," +
                "sent       INTEGER NOT NULL DEFAULT 0," +
                "created_at TEXT    NOT NULL" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS system_logs (" +
                "id           " + autoInc + "," +
                "action       TEXT NOT NULL," +
                "details      TEXT," +
                "performed_by TEXT," +
                "timestamp    TEXT NOT NULL" +
                ")");

            stmt.execute("CREATE TABLE IF NOT EXISTS system_config (" +
                "key   TEXT PRIMARY KEY," +
                "value TEXT NOT NULL" +
                ")");

            // ESP32 devices registry
            stmt.execute("CREATE TABLE IF NOT EXISTS esp32_devices (" +
                "id           " + autoInc + "," +
                "device_id    TEXT UNIQUE NOT NULL," +
                "name         TEXT NOT NULL," +
                "connection   TEXT NOT NULL DEFAULT 'SERIAL'," + // SERIAL|LORA|WIFI
                "last_seen    TEXT," +
                "active       INTEGER NOT NULL DEFAULT 1" +
                ")");

            // Invernaderos
            stmt.execute("CREATE TABLE IF NOT EXISTS greenhouses (" +
                "id          " + autoInc + "," +
                "name        TEXT    NOT NULL," +
                "location    TEXT," +
                "description TEXT," +
                "owner_id    INTEGER NOT NULL DEFAULT 1," +
                "active      INTEGER NOT NULL DEFAULT 1," +
                "created_at  TEXT    NOT NULL" +
            ")");

            // Asignaciones usuario-invernadero
            stmt.execute("CREATE TABLE IF NOT EXISTS user_greenhouse (" +
                "user_id       INTEGER NOT NULL," +
                "greenhouse_id INTEGER NOT NULL," +
                "assigned_at   TEXT    NOT NULL," +
                "PRIMARY KEY (user_id, greenhouse_id)" +
            ")");

            // Tickets de soporte técnico
            stmt.execute("CREATE TABLE IF NOT EXISTS support_tickets (" +
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

            // Admin por defecto (solo en SQLite local para no duplicar en cada reconexión online)
            if (!isPg) {
                // Migración: insertar con contraseña plana (se actualizará al primer login)
                stmt.execute(
                    "INSERT OR IGNORE INTO users " +
                    "(username, password, full_name, phone, role, created_at) " +
                    "VALUES ('admin', 'admin123', 'Administrador', '+573001234567', 'ADMIN', " +
                    "datetime('now'))");
            }

            // Invernadero por defecto
            if (!isPg) {
                stmt.execute(
                    "INSERT OR IGNORE INTO greenhouses (id,name,location,description,owner_id,created_at)" +
                    " VALUES (1,'Invernadero Principal','Sede principal','Invernadero por defecto',1,datetime('now'))");
            }

            // ── Migración: agregar greenhouse_id a sensor_readings si no existe ──
            try {
                stmt.execute("ALTER TABLE sensor_readings ADD COLUMN greenhouse_id INTEGER DEFAULT 1");
                System.out.println("  [DB] Migración: columna greenhouse_id añadida a sensor_readings.");
            } catch (SQLException ignored) {
                // La columna ya existe — ignorar error de ALTER TABLE duplicado
            }

            System.out.println("  [DB] Tablas inicializadas en " +
                (isPg ? "PostgreSQL/Supabase" : "SQLite") + ".");

        } catch (SQLException e) {
            System.err.println("  [DB] Error al crear tablas: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (localConn  != null && !localConn.isClosed())  localConn.close();
        } catch (SQLException ignored) {}
        closeOnline();
        System.out.println("  [DB] Conexiones cerradas.");
    }
}
