import java.net.Socket;
import java.net.InetAddress;

/**
 * Diagnóstico de conectividad a Supabase
 * Compilar: javac SupabaseConnTest.java
 * Ejecutar: java SupabaseConnTest
 */
public class SupabaseConnTest {
    public static void main(String[] args) {
        String host = "aws-1-us-east-1.pooler.supabase.com";
        int port = 6543;

        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("🔍 TEST DE CONECTIVIDAD SUPABASE");
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("Host: " + host);
        System.out.println("Port: " + port);
        System.out.println();

        // 1. Test DNS
        System.out.println("1️⃣  Resolviendo DNS...");
        try {
            InetAddress addr = InetAddress.getByName(host);
            System.out.println("   ✅ DNS resuelto: " + addr.getHostAddress());
        } catch (Exception e) {
            System.out.println("   ❌ DNS FALLIDO: " + e.getMessage());
            return;
        }

        // 2. Test conexión TCP
        System.out.println();
        System.out.println("2️⃣  Conectando a puerto " + port + "...");
        try (Socket socket = new Socket(host, port)) {
            System.out.println("   ✅ CONEXIÓN EXITOSA");
            System.out.println("   Host Local: " + socket.getLocalAddress());
            System.out.println("   Puerto Local: " + socket.getLocalPort());
        } catch (Exception e) {
            System.out.println("   ❌ CONEXIÓN FALLIDA: " + e.getMessage());
            e.printStackTrace();
            
            System.out.println();
            System.out.println("💡 SUGERENCIAS:");
            System.out.println("   • Verificar si el firewall bloquea puerto 6543");
            System.out.println("   • Intentar con vpn activada");
            System.out.println("   • Verificar que Supabase está activo");
            System.out.println("   • Intentar con puerto 5432 (PostgreSQL estándar)");
            return;
        }

        System.out.println();
        System.out.println("═══════════════════════════════════════════════════");
        System.out.println("✅ Diagnóstico completado. La conexión debería funcionar.");
        System.out.println("═══════════════════════════════════════════════════");
    }
}
