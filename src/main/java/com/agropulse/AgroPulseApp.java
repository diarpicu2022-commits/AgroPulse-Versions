package com.agropulse;

import com.agropulse.pattern.singleton.DatabaseConnection;
import com.agropulse.ui.LoginFrame;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.*;

/**
 * ╔═══════════════════════════════════════════════════════════╗
 * ║     🌿  A G R O P U L S E   v1.0  🌿                    ║
 * ║                                                           ║
 * ║  Sistema de Monitoreo y Control Inteligente               ║
 * ║  de Invernadero                                           ║
 * ║                                                           ║
 * ║  Proyecto de Semestre 2026 - Universidad de Nariño             ║
 * ║  Departamento de Nariño, Colombia                         ║
 * ╚═══════════════════════════════════════════════════════════╝
 *
 * PATRONES DE DISEÑO:
 *  1. SINGLETON  → DatabaseConnection, AppConfig
 *  2. OBSERVER   → GreenhouseSubject + GreenhouseObserver
 *  3. STRATEGY   → ActuatorStrategy (Temperature, Humidity)
 *  4. FACTORY    → SensorFactory
 *  5. DAO        → GenericDao<T> + implementaciones
 *
 * CREDENCIALES POR DEFECTO:
 *  Usuario: admin  |  Contraseña: admin123
 *
 * @author Proyecto MTE - AgroPulse
 * @version 2.0
 */
public class AgroPulseApp {

    public static void main(String[] args) {
        // ── Look & Feel moderno (FlatLaf) ─────────────────
        try {
            FlatLightLaf.setup();
            UIManager.put("Button.arc", 8);
            UIManager.put("Component.arc", 8);
            UIManager.put("TextComponent.arc", 6);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        } catch (Exception e) {
            // FlatLaf no disponible; usar look & feel del sistema
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {}
        }

        // ── Iniciar UI en el Event Dispatch Thread ─────────
        SwingUtilities.invokeLater(() -> {
            try {
                LoginFrame login = new LoginFrame();
                login.setVisible(true);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(null,
                    "Error al iniciar AgroPulse:\n" + e.getMessage(),
                    "Error Fatal", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        });

        // ── Conectar BD online si está configurada ────────
        java.util.concurrent.Executors.newSingleThreadExecutor().submit(() -> {
            try {
                com.agropulse.pattern.singleton.DatabaseConnection db =
                    com.agropulse.pattern.singleton.DatabaseConnection.getInstance();
                String url = db.getOnlineUrl();
                // Leer desde SQLite directamente si DatabaseConnection no tiene la URL
                if (url == null || url.isBlank()) {
                    // Leer de system_config
                    java.sql.PreparedStatement ps = db.getConnection().prepareStatement(
                        "SELECT value FROM system_config WHERE key='online_db_url'");
                    java.sql.ResultSet rs = ps.executeQuery();
                    if (rs.next()) url = rs.getString(1);
                }
                String enabled = "";
                java.sql.PreparedStatement ps2 = db.getConnection().prepareStatement(
                    "SELECT value FROM system_config WHERE key='online_db_enabled'");
                java.sql.ResultSet rs2 = ps2.executeQuery();
                if (rs2.next()) enabled = rs2.getString(1);

                if ("true".equals(enabled) && url != null && !url.isBlank()) {
                    System.out.println("  [App] Conectando BD online al inicio...");
                    boolean ok = db.configureOnline(url, true);
                    System.out.println("  [App] BD online: " + (ok ? "✅ Conectada" : "❌ Falló"));
                }
            } catch (Exception e) {
                System.err.println("  [App] BD online auto-connect: " + e.getMessage());
            }
        });

        // ── Hook de cierre: cerrar BD limpiamente ──────────
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            DatabaseConnection.getInstance().close();
            System.out.println("\n  AgroPulse finalizado correctamente.\n");
        }));
    }
}
