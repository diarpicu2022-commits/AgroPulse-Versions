package com.agropulse;

import com.agropulse.api.*;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.HashMap;
import java.util.Map;

/**
 * Servidor REST embebido para AgroPulse - VERSIÓN STANDALONE (sin GUI)
 * Ejecuta solo la API REST sin dependencias de JavaFX
 * 
 * Puertos:
 * - 8080: REST API
 */
public class RestServerStandalone {

    private static final int REST_PORT = 8080;
    private static Server server;

    public static void main(String[] args) throws Exception {
        System.out.println("=== AgroPulse REST Server (Standalone) ===");
        System.out.println();
        
        // Inicializar BD (con Supabase si está disponible)
        var db = com.agropulse.pattern.singleton.DatabaseConnection.getInstance();
        System.out.println("  ✓ [DB] Conexión ready");
        
        // Intentar conectar a Supabase si no está ya configurado
        if (!db.isOnlineEnabled()) {
            String supabaseUrl = System.getenv("SUPABASE_JDBC_URL");
            if (supabaseUrl != null && !supabaseUrl.isBlank()) {
                System.out.println("  [DB] Intentando conectar a Supabase...");
                boolean ok = db.configureOnline(supabaseUrl, true);
                System.out.println("  [DB] Supabase: " + (ok ? "✅ Conectado (PRINCIPAL)" : "⚠️ Falló, usando SQLite"));
            }
        }
        
        // Mostrar estado de la BD
        System.out.println(db.getStatusReport());
        
        // Iniciar servicio de ejecución de reglas automáticas
        com.agropulse.service.RuleExecutorService.start();
        System.out.println("  ✓ [Scheduler] Iniciado (ejecuta cada 30 segundos)");
        
        // Crear servidor Jetty
        server = new Server(REST_PORT);
        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        handler.setContextPath("/");
        
        // Registrar servlets REST
        handler.addServlet(new ServletHolder(new RestServlet()), "/*");
        
        server.setHandler(handler);
        
        // Iniciar
        server.start();
        System.out.println();
        System.out.println("  ✓ [REST] Servidor iniciado en puerto " + REST_PORT);
        System.out.println("  ✓ [REST] Endpoints disponibles:");
        System.out.println("      POST   http://localhost:8080/api/auth/login");
        System.out.println("      GET    http://localhost:8080/api/auth/me");
        System.out.println("      GET    http://localhost:8080/api/rules");
        System.out.println("      POST   http://localhost:8080/api/rules");
        System.out.println("      PUT    http://localhost:8080/api/rules/{id}");
        System.out.println("      DELETE http://localhost:8080/api/rules/{id}");
        System.out.println();
        System.out.println("  Presiona CTRL+C para detener el servidor");
        System.out.println("=".repeat(50));
        System.out.println();
        
        // Mantener el servidor corriendo
        server.join();
    }

    /**
     * Servlet principal que rutea todas las peticiones REST
     */
    public static class RestServlet extends HttpServlet {
        private static final Map<String, com.agropulse.api.JsonRestController> controllers = new HashMap<>();

        static {
            // Registrar controladores REST
            controllers.put("/api/auth", new AuthRestController());
            controllers.put("/api/rules", new RulesRestController());
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            String path = req.getRequestURI();
            String method = req.getMethod();
            
            // CORS Headers - Permitir requests desde cualquier origen
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Requested-With, X-Admin-Email");
            resp.setHeader("Access-Control-Allow-Credentials", "true");
            resp.setHeader("Access-Control-Max-Age", "3600");
            
            // Manejar preflight requests (OPTIONS)
            if ("OPTIONS".equalsIgnoreCase(method)) {
                resp.setStatus(200);
                return;
            }
            
            System.out.println("  [REST] " + method + " " + path);
            
            try {
                // Encontrar controlador apropiado
                com.agropulse.api.JsonRestController controller = null;
                
                for (var entry : controllers.entrySet()) {
                    if (path.startsWith(entry.getKey())) {
                        controller = entry.getValue();
                        break;
                    }
                }
                
                if (controller == null) {
                    resp.setStatus(404);
                    resp.setContentType("application/json");
                    resp.getWriter().write("{\"error\": \"Endpoint no encontrado\"}");
                    return;
                }
                
                controller.handle(path, method, req, resp);
                
            } catch (Exception e) {
                resp.setStatus(500);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }
}
