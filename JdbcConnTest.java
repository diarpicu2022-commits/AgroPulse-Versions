import java.sql.*;
import java.util.Properties;

/**
 * Test JDBC PostgreSQL directo
 * Verifica credenciales y conexión real
 */
public class JdbcConnTest {
    public static void main(String[] args) {
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("🔍 TEST CONEXIÓN JDBC POSTGRESQL");
        System.out.println("═══════════════════════════════════════════════════");
        
        // Parámetros
        String url = "jdbc:postgresql://aws-1-us-east-1.pooler.supabase.com:6543/postgres?sslmode=require";
        String user = "postgres.lujulcpskvwpijpjpahk";
        String pass = "3185683836Xyz";
        
        System.out.println("URL: " + url.split("@")[1]);
        System.out.println("User: " + user);
        System.out.println();
        
        // Test 1: Cargar driver
        System.out.println("1️⃣  Cargando driver PostgreSQL...");
        try {
            Class.forName("org.postgresql.Driver");
            System.out.println("   ✅ Driver cargado");
        } catch (ClassNotFoundException e) {
            System.out.println("   ❌ Driver NO encontrado: " + e.getMessage());
            return;
        }
        
        // Test 2: Intentar conexión
        System.out.println();
        System.out.println("2️⃣  Conectando a PostgreSQL...");
        try (Connection conn = DriverManager.getConnection(url, user, pass)) {
            System.out.println("   ✅ CONEXIÓN EXITOSA!");
            
            // Obtener metadata
            DatabaseMetaData meta = conn.getMetaData();
            System.out.println();
            System.out.println("📋 INFORMACIÓN DE BASE DE DATOS:");
            System.out.println("   Database: " + meta.getDatabaseProductName());
            System.out.println("   Version: " + meta.getDatabaseProductVersion());
            System.out.println("   Driver: " + meta.getDriverName());
            
        } catch (SQLException e) {
            System.out.println("   ❌ ERROR: " + e.getMessage());
            System.out.println("   SQLState: " + e.getSQLState());
            System.out.println("   VendorCode: " + e.getErrorCode());
            
            if (e.getCause() != null) {
                System.out.println("   Causa: " + e.getCause().getMessage());
            }
            
            System.out.println();
            System.out.println("💡 POSIBLES PROBLEMAS:");
            System.out.println("   • Credenciales incorrectas (usuario/contraseña)");
            System.out.println("   • Usuario/base de datos no existe");
            System.out.println("   • Usuario sin permisos de conexión");
            System.out.println("   • Base de datos PostgreSQL en Supabase rechazando la conexión");
            System.out.println();
            System.out.println("🔧 VERIFICAR EN SUPABASE:");
            System.out.println("   1. Ir a: https://app.supabase.com/project/[PROJECT]/settings/database");
            System.out.println("   2. Copiar \"Connection string - JDBC\" desde Session pooler (puerto 6543)");
            System.out.println("   3. Verificar que usuario/contraseña están correctos");
        }
    }
}
