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
        
        // Inicializar BD
        var db = com.agropulse.pattern.singleton.DatabaseConnection.getInstance();
        System.out.println("  ✓ [DB] Conexión ready");
        
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
        private static final Map<String, Object> controllers = new HashMap<>();

        static {
            // Registrar controladores REST
            controllers.put("/api/auth", new AuthRestController());
            controllers.put("/api/rules", new RulesRestController());
            // ReportRestController comentado por problemas de compilación
            // controllers.put("/api/reports", new ReportRestController());
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws java.io.IOException {
            String path = req.getPathInfo();
            String method = req.getMethod();
            
            try {
                // Encontrar controlador apropiado
                for (var entry : controllers.entrySet()) {
                    if (path != null && path.startsWith(entry.getKey())) {
                        var controller = entry.getValue();
                        if (controller instanceof com.agropulse.api.JsonRestController) {
                            ((com.agropulse.api.JsonRestController) controller).service(req, resp);
                            return;
                        }
                    }
                }
                
                // No encontrado
                resp.setStatus(404);
                resp.getWriter().write("{\"error\": \"Endpoint no encontrado\"}");
                
            } catch (Exception e) {
                resp.setStatus(500);
                resp.getWriter().write("{\"error\": \"" + e.getMessage() + "\"}");
            }
        }
    }
}
