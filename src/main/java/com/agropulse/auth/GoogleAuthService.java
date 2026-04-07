package com.agropulse.auth;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;

import java.awt.Desktop;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

import com.agropulse.config.AppConfig;

/**
 * Google OAuth 2.0 - AgroPulse (proyecto: agropulse-491322)
 */
public class GoogleAuthService {

    // Se leen dinámicamente de AppConfig cada vez que se usan
    private String clientId;
    private String clientSecret;
    private static final String REDIRECT_URI  = "http://localhost:8765/callback";
    private static final int    CALLBACK_PORT = 8765;

    private static final String AUTH_URL     = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL    = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://www.googleapis.com/oauth2/v3/userinfo";
    private static final String SCOPE        = "openid email profile";

    private final OkHttpClient http = new OkHttpClient();

    public GoogleAuthService() {
        reloadKeys();
    }

    /** Recarga las keys desde AppConfig (por si el .env se cargó después). */
    private void reloadKeys() {
        AppConfig cfg = AppConfig.getInstance();
        this.clientId     = cfg.get("google_client_id");
        this.clientSecret = cfg.get("google_client_secret");
        System.out.println("  [GoogleAuth] client_id: " + 
            (clientId != null && !clientId.isBlank() ? clientId.substring(0, Math.min(20, clientId.length())) + "..." : "(VACÍO - revisa .env)"));
    }

    public static class GoogleUser {
        public final String sub, email, name, picture;
        public GoogleUser(String sub, String email, String name, String picture) {
            this.sub = sub; this.email = email; this.name = name; this.picture = picture;
        }
        @Override public String toString() { return name + " <" + email + ">"; }
    }

    public void authenticate(Consumer<GoogleUser> onSuccess, Consumer<String> onError) {
        new Thread(() -> {
            try {
                // Recargar keys por si no estaban listas en el constructor
                reloadKeys();
                if (clientId == null || clientId.isBlank()) {
                    onError.accept("GOOGLE_CLIENT_ID no configurado.\nRevisa que el archivo .env esté al lado del pom.xml.");
                    return;
                }
                String state = Long.toHexString(System.nanoTime());
                String authUrl = AUTH_URL
                    + "?client_id="     + enc(clientId)
                    + "&redirect_uri="  + enc(REDIRECT_URI)
                    + "&response_type=code"
                    + "&scope="         + enc(SCOPE)
                    + "&state="         + state
                    + "&access_type=offline&prompt=select_account";

                if (Desktop.isDesktopSupported())
                    Desktop.getDesktop().browse(new URI(authUrl));
                else { onError.accept("No se puede abrir el navegador.\nURL: " + authUrl); return; }

                String code = waitForCallback(state, onError);
                if (code == null) return;
                String token = exchangeCode(code);
                if (token == null) { onError.accept("Error al obtener token de Google."); return; }
                GoogleUser user = getUserInfo(token);
                if (user == null) { onError.accept("Error al obtener perfil de Google."); return; }
                onSuccess.accept(user);
            } catch (Exception e) { onError.accept("Error OAuth: " + e.getMessage()); }
        }, "agropulse-oauth").start();
    }

    private String waitForCallback(String expectedState, Consumer<String> onError) {
        try (ServerSocket server = new ServerSocket(CALLBACK_PORT)) {
            server.setSoTimeout(120_000);
            try (Socket socket = server.accept()) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String line = reader.readLine();
                String html = "<html><head><meta charset='UTF-8'><style>"
                    + "body{font-family:Segoe UI,sans-serif;text-align:center;margin-top:80px;background:#f5f5f5}"
                    + "h1{color:#2E7D32}</style></head><body>"
                    + "<h1>✅ AgroPulse</h1><p>Autenticación exitosa. Puedes cerrar esta ventana.</p></body></html>";
                PrintWriter out = new PrintWriter(socket.getOutputStream());
                out.println("HTTP/1.1 200 OK\r\nContent-Type: text/html; charset=UTF-8\r\nContent-Length: "
                    + html.getBytes().length + "\r\nConnection: close\r\n\r\n" + html);
                out.flush();
                if (line != null && line.contains("code=")) {
                    String q = line.split(" ")[1];
                    if (q.contains("?")) q = q.split("\\?")[1];
                    Map<String,String> p = parseQuery(q);
                    if (!expectedState.equals(p.get("state"))) { onError.accept("State inválido."); return null; }
                    return p.get("code");
                } else if (line != null && line.contains("error=")) {
                    onError.accept("El usuario canceló la autorización.");
                }
            }
        } catch (java.net.SocketTimeoutException e) { onError.accept("Tiempo agotado (2 min)."); }
          catch (Exception e) { onError.accept("Error callback: " + e.getMessage()); }
        return null;
    }

    private String exchangeCode(String code) {
        try {
            RequestBody body = new FormBody.Builder()
                .add("code", code).add("client_id", clientId)
                .add("client_secret", clientSecret).add("redirect_uri", REDIRECT_URI)
                .add("grant_type", "authorization_code").build();
            try (Response r = http.newCall(new Request.Builder().url(TOKEN_URL).post(body).build()).execute()) {
                if (r.body() != null) {
                    JsonObject j = JsonParser.parseString(r.body().string()).getAsJsonObject();
                    if (j.has("access_token")) return j.get("access_token").getAsString();
                }
            }
        } catch (Exception e) { System.err.println("[OAuth] token: " + e.getMessage()); }
        return null;
    }

    private GoogleUser getUserInfo(String token) {
        try {
            Request req = new Request.Builder().url(USERINFO_URL)
                .addHeader("Authorization", "Bearer " + token).build();
            try (Response r = http.newCall(req).execute()) {
                if (r.body() != null) {
                    JsonObject j = JsonParser.parseString(r.body().string()).getAsJsonObject();
                    return new GoogleUser(s(j,"sub"), s(j,"email"),
                        j.has("name") ? j.get("name").getAsString() : s(j,"email"), s(j,"picture"));
                }
            }
        } catch (Exception e) { System.err.println("[OAuth] userinfo: " + e.getMessage()); }
        return null;
    }

    private Map<String,String> parseQuery(String q) {
        Map<String,String> m = new HashMap<>();
        for (String p : q.split("&")) {
            String[] kv = p.split("=", 2);
            if (kv.length == 2) try {
                m.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8),
                      URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
            } catch (Exception ignored) {}
        }
        return m;
    }
    private String enc(String s) { return java.net.URLEncoder.encode(s, StandardCharsets.UTF_8); }
    private String s(JsonObject j, String k) { return j.has(k) ? j.get(k).getAsString() : ""; }
    public static boolean isConfigured() {
        AppConfig cfg = AppConfig.getInstance();
        String id = cfg.get("google_client_id");
        return id != null && !id.isBlank();
    }
}
