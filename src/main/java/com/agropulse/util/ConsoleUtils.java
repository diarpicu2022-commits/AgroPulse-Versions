package com.agropulse.util;

import java.util.Scanner;

/**
 * Utilidades para la interfaz de consola.
 * Menús bonitos, colores, validaciones de entrada.
 */
public class ConsoleUtils {

    // Colores ANSI para la consola
    public static final String RESET  = "\033[0m";
    public static final String GREEN  = "\033[32m";
    public static final String RED    = "\033[31m";
    public static final String YELLOW = "\033[33m";
    public static final String CYAN   = "\033[36m";
    public static final String BOLD   = "\033[1m";
    public static final String WHITE  = "\033[37m";

    /**
     * Imprimir un encabezado decorado.
     */
    public static void printHeader(String title) {
        String line = "═".repeat(60);
        System.out.println();
        System.out.println(GREEN + "  ╔" + line + "╗" + RESET);
        System.out.println(GREEN + "  ║" + BOLD + centerText(title, 60) + RESET + GREEN + "║" + RESET);
        System.out.println(GREEN + "  ╚" + line + "╝" + RESET);
        System.out.println();
    }

    /**
     * Imprimir un subencabezado.
     */
    public static void printSubHeader(String title) {
        System.out.println();
        System.out.println(CYAN + "  ── " + BOLD + title + RESET + CYAN + " ──" + RESET);
        System.out.println();
    }

    /**
     * Imprimir una opción de menú.
     */
    public static void printOption(int number, String text) {
        System.out.println(YELLOW + "    [" + number + "] " + RESET + text);
    }

    /**
     * Imprimir mensaje de éxito.
     */
    public static void printSuccess(String message) {
        System.out.println(GREEN + "  ✓ " + message + RESET);
    }

    /**
     * Imprimir mensaje de error.
     */
    public static void printError(String message) {
        System.out.println(RED + "  ✗ " + message + RESET);
    }

    /**
     * Imprimir mensaje de información.
     */
    public static void printInfo(String message) {
        System.out.println(CYAN + "  ℹ " + message + RESET);
    }

    /**
     * Imprimir mensaje de advertencia.
     */
    public static void printWarning(String message) {
        System.out.println(YELLOW + "  ⚠ " + message + RESET);
    }

    /**
     * Leer un entero con validación.
     */
    public static int readInt(Scanner scanner, String prompt, int min, int max) {
        while (true) {
            System.out.print(CYAN + "  → " + RESET + prompt);
            try {
                int value = Integer.parseInt(scanner.nextLine().trim());
                if (value >= min && value <= max) return value;
                printError("Ingrese un número entre " + min + " y " + max + ".");
            } catch (NumberFormatException e) {
                printError("Entrada inválida. Ingrese un número.");
            }
        }
    }

    /**
     * Leer un double con validación.
     */
    public static double readDouble(Scanner scanner, String prompt) {
        while (true) {
            System.out.print(CYAN + "  → " + RESET + prompt);
            try {
                return Double.parseDouble(scanner.nextLine().trim());
            } catch (NumberFormatException e) {
                printError("Entrada inválida. Ingrese un número decimal.");
            }
        }
    }

    /**
     * Leer una línea de texto.
     */
    public static String readString(Scanner scanner, String prompt) {
        System.out.print(CYAN + "  → " + RESET + prompt);
        return scanner.nextLine().trim();
    }

    /**
     * Pausar hasta que el usuario presione Enter.
     */
    public static void pause(Scanner scanner) {
        System.out.println();
        System.out.print(WHITE + "  Presione Enter para continuar..." + RESET);
        scanner.nextLine();
    }

    /**
     * Limpiar pantalla (funciona en la mayoría de terminales).
     */
    public static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    /**
     * Centrar texto en un ancho dado.
     */
    private static String centerText(String text, int width) {
        if (text.length() >= width) return text;
        int padding = (width - text.length()) / 2;
        return " ".repeat(padding) + text + " ".repeat(width - text.length() - padding);
    }

    /**
     * Imprimir el banner de AgroPulse.
     */
    public static void printBanner() {
        System.out.println(GREEN);
        System.out.println("   ╔═══════════════════════════════════════════════════════╗");
        System.out.println("   ║                                                       ║");
        System.out.println("   ║     🌱  S M A R T   G R E E N   v1.0  🌱             ║");
        System.out.println("   ║                                                       ║");
        System.out.println("   ║     Sistema de Monitoreo y Control                    ║");
        System.out.println("   ║     Inteligente de Invernadero                        ║");
        System.out.println("   ║                                                       ║");
        System.out.println("   ║     Proyecto MTE - Universidad de Nariño              ║");
        System.out.println("   ║                                                       ║");
        System.out.println("   ╚═══════════════════════════════════════════════════════╝");
        System.out.println(RESET);
    }
}
