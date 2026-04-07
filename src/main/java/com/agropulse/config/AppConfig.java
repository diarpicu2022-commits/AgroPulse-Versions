package com.agropulse.config;

import com.agropulse.pattern.singleton.DatabaseConnection;

import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.Properties;

/**
 * Configuración del sistema AgroPulse.
 * Almacena y recupera configuraciones de la base de datos.
 * Usa el patrón Singleton para mantener una sola instancia.
 *
 * ─── SEGURIDAD PARA GITHUB ───────────────────────────────────────────
 *  Las API keys NO se guardan en el código fuente.
 *  Se leen (en orden de prioridad):
 *    1. Variables de entorno del sistema (recomendado para producción/CI)
 *    2. Archivo .env en el directorio de trabajo (desarrollo local)
 *    3. Base de datos local (configuradas por el usuario en la UI)
 *  El archivo .env NUNCA debe subirse a GitHub (.gitignore lo excluye).
 *  Copia .env.example → .env y rellena tus claves.
 * ─────────────────────────────────────────────────────────────────────
 */
public class AppConfig {

    private static AppConfig instance;
    private final Connection conn;

    /** Propiedades leídas del archivo .env (si existe) */
    private static final Properties ENV_PROPS = new Properties();

    static {
        loadDotEnv();
    }

    /** Lee el archivo .env del directorio de trabajo o del JAR. */
    private static void loadDotEnv() {
        String userDir = System.getProperty("user.dir");
        String[] candidates = {
            ".env",                                              // directorio actual
            userDir + File.separator + ".env",                   // user.dir
            userDir + File.separator + "AgroPulse" + File.separator + ".env",  // subcarpeta AgroPulse
            "AgroPulse" + File.separator + ".env",               // relativo
            ".." + File.separator + ".env",                      // carpeta padre
        };
        for (String path : candidates) {
            File f = new File(path);
            if (f.exists()) {
                try (InputStream is = new FileInputStream(f)) {
                    // Parseo manual línea-a-línea (soporta comentarios # y líneas KEY=VALUE)
                    BufferedReader br = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        int eq = line.indexOf('=');
                        if (eq > 0) {
                            String key = line.substring(0, eq).trim();
                            String val = line.substring(eq + 1).trim();
                            // Quitar comillas opcionales: "valor" o 'valor'
                            if ((val.startsWith("\"") && val.endsWith("\"")) ||
                                (val.startsWith("'")  && val.endsWith("'"))) {
                                val = val.substring(1, val.length() - 1);
                            }
                            ENV_PROPS.setProperty(key, val);
                        }
                    }
                    System.out.println("  [AppConfig] ✅ Archivo .env cargado desde: " + f.getAbsolutePath());
                    System.out.println("  [AppConfig]    Keys encontradas: " + ENV_PROPS.size());
                } catch (IOException e) {
                    System.err.println("  [AppConfig] No se pudo leer .env: " + e.getMessage());
                }
                return; // Encontrado, no buscar más
            }
        }
        System.out.println("  [AppConfig] ⚠️  No se encontró archivo .env — las APIs quedarán sin configurar.");
        System.out.println("  [AppConfig]    Copia .env.example → .env y rellena tus claves.");
    }

    /**
     * Resuelve una clave leyendo (en orden):
     *  1. Variable de entorno del sistema (System.getenv)
     *  2. Archivo .env cargado al inicio
     *  3. Devuelve null si no se encontró en ningún lado
     */
    private static String envGet(String envKey) {
        String v = System.getenv(envKey);
        if (v != null && !v.isBlank()) return v;
        v = ENV_PROPS.getProperty(envKey);
        if (v != null && !v.isBlank()) return v;
        return null;
    }

    // ═══════════════════════════════════════════════════════════════
    //  VALORES POR DEFECTO (NO sensibles — sin keys reales)
    // ═══════════════════════════════════════════════════════════════

    private static final String DEFAULT_OPENAI_KEY        = "";
    private static final String DEFAULT_OPENROUTER_KEY    = "";   // Poner en .env: OPENROUTER_API_KEY=sk-or-...
    private static final String DEFAULT_GROQ_KEY          = "";   // Poner en .env: GROQ_API_KEY=gsk_...
    private static final String DEFAULT_OLLAMA_HOST       = "http://localhost:11434";
    private static final String DEFAULT_OLLAMA_MODEL      = "llama3";
    private static final String DEFAULT_MISTRAL_KEY      = "";   // Poner en .env: MISTRAL_API_KEY=...
    private static final String DEFAULT_ONLINE_DB_URL     = "";   // Poner en .env: ONLINE_DB_URL=jdbc:postgresql://...
    private static final String DEFAULT_ONLINE_DB_ENABLED = "false";
    private static final String DEFAULT_GREEN_API_URL     = "https://7107.api.greenapi.com";
    private static final String DEFAULT_GREEN_ID_INSTANCE = "";
    private static final String DEFAULT_GREEN_TOKEN       = "";
    private static final String DEFAULT_ALERT_PHONE       = "+573001234567";

    // ═══════════════════════════════════════════════════════════════

    private AppConfig() {
        this.conn = DatabaseConnection.getInstance().getConnection();
        initializeDefaults();
    }

    public static synchronized AppConfig getInstance() {
        if (instance == null) instance = new AppConfig();
        return instance;
    }

    public String get(String key) {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT value FROM system_config WHERE key=?")) {
            ps.setString(1, key);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString("value");
        } catch (SQLException e) {
            System.err.println("  [Config] Error al leer '" + key + "': " + e.getMessage());
        }
        return "";
    }

    public void set(String key, String value) {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT OR REPLACE INTO system_config (key, value) VALUES (?, ?)")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("  [Config] Error al guardar '" + key + "': " + e.getMessage());
        }
    }

    // ─── OpenAI ──────────────────────────────────────────────────────
    public String getOpenAIKey()            { return get("openai_api_key"); }
    public boolean isOpenAIEnabled()        { return "true".equals(get("openai_enabled")); }
    public void setOpenAIEnabled(boolean v) { set("openai_enabled", String.valueOf(v)); }

    // ─── OpenRouter ────────────────────────────────────────────────────
    public String getOpenRouterKey()              { return get("openrouter_api_key"); }
    public boolean isOpenRouterEnabled()          { return "true".equals(get("openrouter_enabled")); }
    public void setOpenRouterEnabled(boolean v)   { set("openrouter_enabled", String.valueOf(v)); }

    // ─── Groq ────────────────────────────────────────────────────────
    public String getGroqKey()              { return get("groq_api_key"); }
    public boolean isGroqEnabled()          { return "true".equals(get("groq_enabled")); }
    public void setGroqEnabled(boolean v)   { set("groq_enabled", String.valueOf(v)); }

    // ─── Ollama ──────────────────────────────────────────────────────
    public String getOllamaHost()           { return get("ollama_host"); }
    public String getOllamaModel()          { return get("ollama_model"); }
    public boolean isOllamaEnabled()        { return "true".equals(get("ollama_enabled")); }
    public void setOllamaEnabled(boolean v) { set("ollama_enabled", String.valueOf(v)); }

    // ─── GitHub Models ─────────────────────────────────────────────
    public String getGitHubToken() { return get("github_token"); }
    public boolean isGitHubEnabled() { return "true".equals(get("github_enabled")); }
    public void setGitHubEnabled(boolean v) { set("github_enabled", String.valueOf(v)); }

    // ─── Mistral ────────────────────────────────────────────────────
    public String getMistralKey()              { return get("mistral_api_key"); }
    public boolean isMistralEnabled()          { return "true".equals(get("mistral_enabled")); }
    public void setMistralEnabled(boolean v)   { set("mistral_enabled", String.valueOf(v)); }

    // ─── Estado global de IA ─────────────────────────────────────────
    public boolean isAIEnabled() {
        return isGroqEnabled() || isOllamaEnabled() || isGitHubEnabled();
    }

    public void setAIEnabled(boolean enabled) {
        setGroqEnabled(enabled);
        setOllamaEnabled(enabled);
        setGitHubEnabled(enabled);
    }

    // ─── WhatsApp ────────────────────────────────────────────────────
    public String getGreenApiUrl()              { return get("green_api_url"); }
    public String getGreenIdInstance()          { return get("green_id_instance"); }
    public String getGreenToken()               { return get("green_api_token"); }
    public String getAlertPhone()               { return get("alert_phone"); }
    public boolean isWhatsAppEnabled()          { return "true".equals(get("whatsapp_enabled")); }
    public void setWhatsAppEnabled(boolean v)   { set("whatsapp_enabled", String.valueOf(v)); }

    // ─── Google OAuth ─────────────────────────────────────────────────
    public String getGoogleClientId()     { return get("google_client_id"); }
    public String getGoogleClientSecret() { return get("google_client_secret"); }

    // ─── Supabase WebApp ──────────────────────────────────────────────
    public String getSupabaseUrl()     { return get("supabase_url"); }
    public String getSupabaseAnonKey() { return get("supabase_anon_key"); }

    // ─── BD Online ───────────────────────────────────────────────────
    public String getOnlineDbUrl()              { return get("online_db_url"); }
    public boolean isOnlineDbEnabled()          { return "true".equals(get("online_db_enabled")); }
    public void setOnlineDbEnabled(boolean v)   { set("online_db_enabled", String.valueOf(v)); }

    // ─── Inicialización de valores por defecto ────────────────────────
    private void initializeDefaults() {
        // Para cada clave, si hay un valor en .env/entorno, ese tiene prioridad.
        // Luego se guarda en la BD solo si la BD no tiene nada todavía.
        initKey("openai_api_key",      "OPENAI_API_KEY",      DEFAULT_OPENAI_KEY);
        initEnabled("openai_enabled",  "OPENAI_API_KEY",      "false");

        initKey("openrouter_api_key",  "OPENROUTER_API_KEY",  DEFAULT_OPENROUTER_KEY);
        initEnabled("openrouter_enabled", "OPENROUTER_API_KEY", "false");

        initKey("groq_api_key",        "GROQ_API_KEY",        DEFAULT_GROQ_KEY);
        initEnabled("groq_enabled",    "GROQ_API_KEY",        "false");

        setIfEmpty("ollama_host",  DEFAULT_OLLAMA_HOST);
        setIfEmpty("ollama_model", DEFAULT_OLLAMA_MODEL);
        setIfEmpty("ollama_enabled", "false");

        initKey("github_token", "GITHUB_TOKEN", "");
        initEnabled("github_enabled", "GITHUB_TOKEN", "false");

        initKey("mistral_api_key",    "MISTRAL_API_KEY",    "");
        initEnabled("mistral_enabled", "MISTRAL_API_KEY",    "false");

        initKey("online_db_url",       "ONLINE_DB_URL",       DEFAULT_ONLINE_DB_URL);
        initEnabled("online_db_enabled", "ONLINE_DB_URL",     DEFAULT_ONLINE_DB_ENABLED);

        initKey("green_api_url",       "GREEN_API_URL",       DEFAULT_GREEN_API_URL);
        initKey("green_id_instance",   "GREEN_ID_INSTANCE",   DEFAULT_GREEN_ID_INSTANCE);
        initKey("green_api_token",     "GREEN_API_TOKEN",     DEFAULT_GREEN_TOKEN);
        initKey("alert_phone",         "ALERT_PHONE",         DEFAULT_ALERT_PHONE);
        setIfEmpty("whatsapp_enabled", "false");

        // ─── Google OAuth 2.0 ─────────────────────────────────────────
        initKey("google_client_id",     "GOOGLE_CLIENT_ID",     "");
        initKey("google_client_secret", "GOOGLE_CLIENT_SECRET",  "");

        // ─── Supabase WebApp ──────────────────────────────────────────
        initKey("supabase_url",      "SUPABASE_URL",      "");
        initKey("supabase_anon_key", "SUPABASE_ANON_KEY", "");
    }

    /**
     * Inicializa una clave en la BD: si el entorno/env tiene valor, lo usa;
     * si no, usa el defecto. Solo escribe si la BD está vacía (setIfEmpty).
     */
    private void initKey(String dbKey, String envKey, String fallback) {
        String envVal = envGet(envKey);
        String value  = (envVal != null) ? envVal : fallback;
        // Si hay valor de entorno, siempre actualiza (puede haber cambiado)
        if (envVal != null) {
            set(dbKey, envVal);
        } else {
            setIfEmpty(dbKey, value);
        }
    }

    /**
     * Si el .env tiene una key válida → fuerza enabled=true en la BD.
     * Si el .env NO tiene key (o no existe .env) → deja el valor que ya tenía la BD,
     * o pone el default si la BD está vacía.
     */
    private void initEnabled(String enabledDbKey, String envKeyForApiKey, String defaultEnabled) {
        String v = envGet(envKeyForApiKey);
        if (v != null && !v.isBlank()) {
            // Hay key en .env → activar siempre
            set(enabledDbKey, "true");
        } else {
            // No hay key en .env → solo poner default si la BD está vacía
            setIfEmpty(enabledDbKey, defaultEnabled);
        }
    }

    private void setIfEmpty(String key, String defaultValue) {
        if (get(key).isEmpty()) set(key, defaultValue);
    }
}
