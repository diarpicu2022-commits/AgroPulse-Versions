package com.agropulse.pattern.singleton;

import java.sql.*;

/**
 * PATRÓN SINGLETON — Conexión dual a bases de datos.
 *
 * PRIMARY:   PostgreSQL via Supabase (PRINCIPAL - en la nube)
 * FALLBACK:  SQLite local (respaldo si Supabase no responde)
 *
 * Estrategia: "Online-First"
 *   1. Intenta conectar a Supabase (PostgreSQL en la nube)
 *   2. Si Supabase está disponible, úsalo como BD principal
 *   3. Si Supabase falla, responde con SQLite local (fallback)
 *   4. Sincronización automática cuando Supabase se recupera
 *
 * Configurar Supabase (OBLIGATORIO):
 *   1. Crear proyecto en https://supabase.com (gratis)
 *   2. Ir a Settings → Database → Connection string (JDBC, Session pooler)
 *   3. Copiar URL: jdbc:postgresql://postgres:PASSWORD@db.XXXXX.supabase.co:5432/postgres
 *   4. En AgroPulseApp, pasar a DatabaseConnection.getInstance().configureOnline(url, true)
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
        // 1. Conectar a SQLite local primero (para tener respaldo)
        connectLocal();
        initializeDatabase(localConn);
        
        // 2. Intentar cargar Supabase desde variables de entorno
        String supabaseUrl = System.getenv("SUPABASE_JDBC_URL");
        if (supabaseUrl != null && !supabaseUrl.isBlank()) {
            System.out.println("  [DB] Supabase URL detectada en env variables, conectando...");
            configureOnline(supabaseUrl, true);
        }
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) instance = new DatabaseConnection();
        return instance;
    }

    // ─── Conexiones ───────────────────────────────────────────────────

    /**
     * NUEVA LÓGICA (Online-First):
     * 1. Si Supabase está configurado y disponible → usarlo (PRINCIPAL)
     * 2. Si Supabase falla o no está configurado → caer a SQLite (FALLBACK)
     */
    public Connection getConnection() {
        // Intentar Supabase primero (si está habilitado)
        if (onlineEnabled && !onlineUrl.isBlank()) {
            try {
                // SIEMPRE crear nueva conexión para evitar problemas de conexión cerrada
                System.out.println("  [DB-Online] Creando nueva conexión...");
                connectOnline();
                if (onlineConn != null && !onlineConn.isClosed()) {
                    return onlineConn;  // ✅ Supabase disponible
                }
            } catch (SQLException e) {
                System.err.println("  [DB] Supabase no disponible, usando SQLite: " + e.getMessage());
                try { onlineConn = null; } catch (Exception ex) {}
            }
        }
        
        // Fallback a SQLite local
        connectLocal();
        return localConn;
    }

    /** Obtener conexión online directamente (devuelve null si no disponible). */
    public Connection getOnlineConnection() {
        if (!onlineEnabled || onlineUrl.isBlank()) return null;
        try {
            if (onlineConn == null || onlineConn.isClosed() || !onlineConn.isValid(2)) {
                System.out.println("  [DB-Online] Reconectando...");
                connectOnline();
            }
        } catch (SQLException e) {
            System.err.println("  [DB-Online] Reconexión fallida: " + e.getMessage());
            try { onlineConn = null; } catch (Exception ex) {}
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

    /**
     * Obtiene la BD activa actual (PRINCIPAL si Supabase disponible, SQLite si fallback)
     */
    public String getActiveDatabase() {
        if (isOnlineAvailable()) {
            return "PostgreSQL/Supabase (PRINCIPAL)";
        } else {
            return "SQLite Local (FALLBACK)";
        }
    }

    /**
     * Reporte de estado completo
     */
    public String getStatusReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("\n════════════════════════════════════════\n");
        sb.append("  📊 Estado de la Base de Datos\n");
        sb.append("════════════════════════════════════════\n");
        
        sb.append("  SQLite Local:        ✅ Disponible\n");
        
        if (onlineEnabled) {
            if (isOnlineAvailable()) {
                sb.append("  Supabase:            ✅ Conectado (PRINCIPAL)\n");
            } else {
                sb.append("  Supabase:            ❌ Desconectado (fallback a SQLite)\n");
            }
        } else {
            sb.append("  Supabase:            ⏳ No habilitado\n");
        }
        
        sb.append("  BD Activa:           " + getActiveDatabase() + "\n");
        sb.append("════════════════════════════════════════\n");
        
        return sb.toString();
    }

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
            System.out.println("  [DB-Online] Extrayendo credenciales de URL...");
            
            // Extraer credenciales y URL de conexión
            String cleanUrl = onlineUrl;
            String user = "";
            String pass = "";
            
            // Si la URL contiene credenciales embebidas (jdbc:postgresql://user:pass@host...)
            if (onlineUrl.contains("@")) {
                String[] parts = onlineUrl.split("@");
                String urlWithCreds = parts[0]; // jdbc:postgresql://user:pass
                String urlWithoutCreds = parts[1]; // host:port/db
                
                // Extraer user y pass
                String[] creds = urlWithCreds.substring("jdbc:postgresql://".length()).split(":");
                if (creds.length >= 2) {
                    user = creds[0];
                    pass = creds[1];
                }
                
                // Reconstruir URL sin credenciales para DriverManager
                cleanUrl = "jdbc:postgresql://" + urlWithoutCreds;
            }
            
            System.out.println("  [DB-Online] User: " + user);
            System.out.println("  [DB-Online] Conectando a: " + cleanUrl);
            
            // Usar DriverManager.getConnection con parámetros separados
            onlineConn = DriverManager.getConnection(cleanUrl, user, pass);
            System.out.println("  [DB-Online] ✅ PostgreSQL/Supabase conectado.");
            initializeDatabase(onlineConn);  // Crear tablas también en online
            return true;
        } catch (ClassNotFoundException e) {
            System.err.println("  [DB-Online] ❌ Driver PostgreSQL no encontrado. Agregar dependencia.");
            return false;
        } catch (SQLException e) {
            System.err.println("  [DB-Online] ❌ Error de conexión: " + e.getMessage());
            if (e.getCause() != null) {
                System.err.println("      Causa: " + e.getCause().getMessage());
            }
            // Mostrar mensaje SQLState para obtener pista del error
            System.err.println("      SQLState: " + e.getSQLState());
            onlineConn = null;
            return false;
        }
    }

    /** Sanitiza URL para no mostrar contraseña en logs */
    private String sanitizeUrl(String url) {
        if (url == null || !url.contains("@")) return url;
        // Mostrar solo host y puerto, no credenciales
        String[] parts = url.split("@");
        if (parts.length > 1) {
            return "jdbc:postgresql://[USER:PASS]@" + parts[1];
        }
        return url;
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
                "password    TEXT," +                    // Puede ser null para usuarios Google
                "full_name   TEXT," +                   // AES-256-GCM cifrado
                "phone       TEXT," +                  // AES-256-GCM cifrado
                "email       TEXT," +
                "avatar      TEXT," +
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

            // ── Migración: agregar email y avatar a users si no existen ──
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN email TEXT DEFAULT ''");
                System.out.println("  [DB] Migración: columna email añadida a users.");
            } catch (SQLException ignored) {}
            try {
                stmt.execute("ALTER TABLE users ADD COLUMN avatar TEXT DEFAULT ''");
                System.out.println("  [DB] Migración: columna avatar añadida a users.");
            } catch (SQLException ignored) {}

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
