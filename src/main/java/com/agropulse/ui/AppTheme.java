package com.agropulse.ui;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;

/**
 * ╔══════════════════════════════════════════════════════╗
 * ║   🎨  AppTheme  -  AgroPulse                        ║
 * ╚══════════════════════════════════════════════════════╝
 *
 * Centraliza colores, fuentes y estilos visuales de AgroPulse.
 * Paleta verde agrícola profesional.
 */
public class AppTheme {

    // ─── Paleta de colores ───────────────────────────────
    public static final Color PRIMARY        = new Color(0x2E7D32);   // Verde oscuro
    public static final Color PRIMARY_LIGHT  = new Color(0x4CAF50);   // Verde medio
    public static final Color PRIMARY_DARK   = new Color(0x1B5E20);   // Verde muy oscuro
    public static final Color ACCENT         = new Color(0xFF8F00);   // Ámbar
    public static final Color ACCENT_LIGHT   = new Color(0xFFB300);

    public static final Color BG_MAIN        = new Color(0xF5F5F5);   // Fondo principal
    public static final Color BG_SIDEBAR     = new Color(0x1B5E20);   // Fondo sidebar
    public static final Color BG_CARD        = Color.WHITE;
    public static final Color BG_HEADER      = new Color(0x2E7D32);

    public static final Color TEXT_PRIMARY   = new Color(0x212121);
    public static final Color TEXT_SECONDARY = new Color(0x757575);
    public static final Color TEXT_WHITE     = Color.WHITE;
    public static final Color TEXT_MUTED     = new Color(0xBDBDBD);

    public static final Color DANGER         = new Color(0xC62828);
    public static final Color WARNING        = new Color(0xF57F17);
    public static final Color SUCCESS        = new Color(0x2E7D32);
    public static final Color INFO           = new Color(0x0277BD);

    public static final Color BORDER_LIGHT   = new Color(0xE0E0E0);
    public static final Color BORDER_MEDIUM  = new Color(0xBDBDBD);

    // ─── Fuentes ─────────────────────────────────────────
     public static final Font FONT_TITLE      = new Font("Segoe UI Emoji", Font.BOLD, 20);
    public static final Font FONT_SUBTITLE   = new Font("Segoe UI Emoji", Font.BOLD, 15);
    public static final Font FONT_BODY       = new Font("Segoe UI Emoji", Font.PLAIN, 13);
    public static final Font FONT_SMALL      = new Font("Segoe UI Emoji", Font.PLAIN, 11);
    public static final Font FONT_BUTTON     = new Font("Segoe UI Emoji", Font.BOLD, 13);
    public static final Font FONT_MONOSPACE  = new Font("Consolas", Font.PLAIN, 12);

    // ─── Dimensiones ─────────────────────────────────────
    public static final int SIDEBAR_WIDTH    = 220;
    public static final int HEADER_HEIGHT    = 60;
    public static final int PADDING          = 16;
    public static final int RADIUS           = 8;

    // ─── Bordes ──────────────────────────────────────────
    public static Border cardBorder() {
        return BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_LIGHT, 1, true),
            new EmptyBorder(PADDING, PADDING, PADDING, PADDING)
        );
    }

    public static Border paddingBorder(int v, int h) {
        return new EmptyBorder(v, h, v, h);
    }

    // ─── Componentes estilizados ─────────────────────────

    /** Botón primario verde */
    public static JButton primaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(PRIMARY_LIGHT);
        btn.setForeground(TEXT_WHITE);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 36));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) {
                btn.setBackground(PRIMARY);
            }
            public void mouseExited(java.awt.event.MouseEvent e) {
                btn.setBackground(PRIMARY_LIGHT);
            }
        });
        return btn;
    }

    /** Botón secundario / outline */
    public static JButton secondaryButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(Color.WHITE);
        btn.setForeground(PRIMARY);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorder(new LineBorder(PRIMARY_LIGHT, 1, true));
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 36));
        return btn;
    }

    /** Botón de peligro / rojo */
    public static JButton dangerButton(String text) {
        JButton btn = new JButton(text);
        btn.setBackground(DANGER);
        btn.setForeground(TEXT_WHITE);
        btn.setFont(FONT_BUTTON);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(160, 36));
        return btn;
    }

    /** Campo de texto estilizado */
    public static JTextField textField() {
        JTextField field = new JTextField();
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_MEDIUM, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        return field;
    }

    /** Campo de contraseña estilizado */
    public static JPasswordField passwordField() {
        JPasswordField field = new JPasswordField();
        field.setFont(FONT_BODY);
        field.setBorder(BorderFactory.createCompoundBorder(
            new LineBorder(BORDER_MEDIUM, 1, true),
            new EmptyBorder(6, 10, 6, 10)
        ));
        return field;
    }

    /** Panel tipo "card" con fondo blanco y borde */
    public static JPanel cardPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(BG_CARD);
        panel.setBorder(cardBorder());
        return panel;
    }

    /** Encabezado de sección */
    public static JLabel sectionTitle(String text) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SUBTITLE);
        label.setForeground(PRIMARY_DARK);
        return label;
    }

    /** Etiqueta de estado con color */
    public static JLabel statusLabel(String text, Color color) {
        JLabel label = new JLabel(text);
        label.setFont(FONT_SMALL);
        label.setForeground(color);
        label.setOpaque(true);
        label.setBackground(colorWithAlpha(color, 30));
        label.setBorder(new EmptyBorder(2, 8, 2, 8));
        return label;
    }

    private static Color colorWithAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** Separador horizontal */
    public static JSeparator separator() {
        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_LIGHT);
        return sep;
    }

 /** Tabla estilizada con soporte de Emojis en celdas */
    public static JTable styledTable(String[] headers, Object[][] data) {
        JTable table = new JTable(data, headers) {
            @Override
            public boolean isCellEditable(int row, int col) { return false; }
        };
        table.setFont(FONT_BODY);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setFont(FONT_BUTTON);
        table.getTableHeader().setBackground(BG_HEADER);
        table.getTableHeader().setForeground(TEXT_WHITE);
        table.setSelectionBackground(new Color(0xC8E6C9));
        table.setSelectionForeground(PRIMARY_DARK);

        // Alternar colores de filas
        table.setDefaultRenderer(Object.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value,
                    boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                
                // FORZAR LA FUENTE EN LA CELDA para los emojis de "Estado"
                this.setFont(FONT_BODY); 
                
                if (!isSelected) {
                    setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xF9FBE7));
                }
                setBorder(new EmptyBorder(0, 8, 0, 8));
                return this;
            }
        });
        return table;
    }
}
