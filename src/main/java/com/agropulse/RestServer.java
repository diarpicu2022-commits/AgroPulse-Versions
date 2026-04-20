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
 * Servidor REST embebido para AgroPulse.
 * Ejecuta junto con la aplicación de escritorio o单独.
 * 
 * Puertos:
 * - 8080: REST API
 * - La GUI de escritorio sigue en puerto nativo
 */
public class RestServer {

    private static final int REST_PORT = 8080;
    private static Server server;

    public static void main(String[] args) throws Exception {
        System.out.println("=== AgroPulse REST Server ===");
        
        // Inicializar BD
        var db = com.agropulse.pattern.singleton.DatabaseConnection.getInstance();
        System.out.println("  [DB] Conexión ready");
        
        // Iniciar servicio de ejecución de reglas automáticas
        com.agropulse.service.RuleExecutorService.start();
        
        // Crear servidor Jetty
        server = new Server(REST_PORT);
        ServletContextHandler handler = new ServletContextHandler(ServletContextHandler.NO_SESSIONS);
        handler.setContextPath("/");
        
        // Registrar servlets REST
        handler.addServlet(new ServletHolder(new RestServlet()), "/*");
        
        server.setHandler(handler);
        
        // Iniciar
        server.start();
        System.out.println("  [REST] Servidor iniciado en puerto " + REST_PORT);
        System.out.println("  [REST] Endpoints disponibles:");
        System.out.println("    POST   /api/auth/login");
        System.out.println("    GET    /api/sensors");
        System.out.println("    GET    /api/crops");
        System.out.println("    GET    /api/greenhouses");
        System.out.println("    GET    /api/actuators");
        System.out.println("    GET    /api/users");
        System.out.println("    GET    /api/readings");
        System.out.println("    GET    /api/alerts");
        System.out.println("    GET    /api/logs");
        System.out.println("    POST   /api/seed");
        
        server.join();
    }

    public static class RestServlet extends HttpServlet {
        
        private static final Map<String, JsonRestController> controllers = new HashMap<>();
        
        static {
            controllers.put("/api/auth", new AuthRestController());
            controllers.put("/api/sensors", new SensorRestController());
            controllers.put("/api/crops", new CropRestController());
            controllers.put("/api/greenhouses", new GreenhouseRestController());
            controllers.put("/api/actuators", new ActuatorRestController());
            controllers.put("/api/users", new UserRestController());
            controllers.put("/api/readings", new ReadingRestController());
            controllers.put("/api/alerts", new AlertRestController());
            controllers.put("/api/logs", new LogRestController());
            controllers.put("/api/seed", new SeedRestController());
            controllers.put("/api/rules", new RulesRestController());
            // TODO: ReportRestController tiene problemas de compilación - pendiente
            // controllers.put("/api/reports", new ReportRestController());
        }
        
        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) 
                throws java.io.IOException, javax.servlet.ServletException {
            
            // CORS headers
            resp.setHeader("Access-Control-Allow-Origin", "*");
            resp.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            resp.setHeader("Access-Control-Allow-Headers", "Content-Type, Authorization, X-Admin-Email, X-Requested-With");
            resp.setHeader("Access-Control-Max-Age", "3600");
            
            if ("OPTIONS".equalsIgnoreCase(req.getMethod())) {
                resp.setStatus(HttpServletResponse.SC_OK);
                return;
            }
            
            String path = req.getRequestURI();
            String method = req.getMethod();
            
            System.out.println("  [REST] " + method + " " + path);
            
            // Buscar controller
            JsonRestController controller = null;
            
            for (Map.Entry<String, JsonRestController> entry : controllers.entrySet()) {
                if (path.startsWith(entry.getKey())) {
                    controller = entry.getValue();
                    break;
                }
            }
            
            if (controller == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("application/json");
                resp.getWriter().print("{\"error\":\"Endpoint no encontrado\"}");
                return;
            }
            
            try {
                controller.handle(path, method, req, resp);
            } catch (Exception e) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("application/json");
                resp.getWriter().print("{\"error\":\"" + e.getMessage() + "\"}");
            }
        }
    }
}