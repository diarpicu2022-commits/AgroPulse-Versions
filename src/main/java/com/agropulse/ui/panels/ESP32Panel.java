package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.SensorReading;
import com.agropulse.service.esp32.*;
import com.agropulse.ui.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.util.List;

/**
 * Panel de conexión con ESP32.
 * Permite configurar y conectar los 4 canales:
 *   🔌 USB/Serial · 📡 LoRa · 🌐 WiFi/HTTP · 💾 Archivo/Memoria
 */
public class ESP32Panel extends javax.swing.JPanel {

    private final GreenhouseController controller;
    private ESP32Manager esp32Manager;

    // Serial
    private JTextField txtSerialPort;
    private JTextField txtBaudRate;
    private JToggleButton tglSerial;
    private ESP32SerialService serialService;

    // LoRa
    private JTextField txtLoraPort;
    private JToggleButton tglLora;
    private ESP32LoRaService loraService;

    // WiFi
    private JTextField txtWifiPort;
    private JToggleButton tglWifi;
    private ESP32WiFiService wifiService;

    // Archivo
    private JTextField txtFilePath;
    private JButton btnBrowse;
    private JButton btnLoadFile;

    // Estado y log
    private JTextArea txtLog;
    private JLabel lblStatus;
    private JSpinner spnInterval;
    private JToggleButton tglPolling;

    public ESP32Panel(GreenhouseController controller) {
        this.controller   = controller;
        this.esp32Manager = new ESP32Manager(controller);

        // Listener para mostrar lecturas en el log
        esp32Manager.addListener(readings -> SwingUtilities.invokeLater(() -> {
            for (SensorReading r : readings) {
                appendLog("📥 " + r.getSensorType().getDisplayName() +
                          ": " + String.format("%.1f", r.getValue()) +
                          " " + r.getSensorType().getUnit());
            }
            lblStatus.setText("✅ Última lectura: " + readings.size() + " valores recibidos");
        }));

        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        // Título
        JLabel title = new JLabel("📟 Conexión con ESP32");
        title.setFont(AppTheme.FONT_TITLE);
        title.setForeground(AppTheme.PRIMARY_DARK);

        // Info
        JLabel info = new JLabel("<html>Conecta sensores físicos desde el ESP32 via Serial, LoRa, WiFi o cargando un archivo de memoria.</html>");
        info.setFont(AppTheme.FONT_SMALL);
        info.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel pnlHeader = new JPanel(new BorderLayout(0, 6));
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        pnlHeader.add(title, BorderLayout.NORTH);
        pnlHeader.add(info,  BorderLayout.SOUTH);

        // Tarjetas de conexión
        JPanel pnlTop = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlTop.setBackground(AppTheme.BG_MAIN);
        pnlTop.add(buildSerialCard());
        pnlTop.add(buildLoraCard());

        JPanel pnlMid = new JPanel(new GridLayout(1, 2, 12, 0));
        pnlMid.setBackground(AppTheme.BG_MAIN);
        pnlMid.add(buildWifiCard());
        pnlMid.add(buildFileCard());

        // Sondeo automático
        JPanel pnlPoll = buildPollingPanel();

        // Log
        JPanel pnlLog = buildLogPanel();

        JPanel pnlCenter = new JPanel(new GridLayout(3, 1, 0, 12));
        pnlCenter.setBackground(AppTheme.BG_MAIN);
        pnlCenter.add(pnlTop);
        pnlCenter.add(pnlMid);
        pnlCenter.add(pnlPoll);

        add(pnlHeader, BorderLayout.NORTH);
        add(pnlCenter, BorderLayout.CENTER);
        add(pnlLog,    BorderLayout.SOUTH);
    }

    // ─── Tarjetas ─────────────────────────────────────────────────────

    private JPanel buildSerialCard() {
        JPanel card = card("🔌 USB / Serial");

        txtSerialPort = fld("COM3");
        txtBaudRate   = fld("115200");
        tglSerial     = actionToggle("Conectar Serial");
        tglSerial.addActionListener(e -> toggleSerial(tglSerial.isSelected()));

        addField(card, "Puerto COM (ej: COM3 o /dev/ttyUSB0)", txtSerialPort);
        addField(card, "Baud Rate", txtBaudRate);
        card.add(Box.createVerticalStrut(8));
        card.add(tglSerial);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildLoraCard() {
        JPanel card = card("📡 LoRa (Gateway Serial)");

        txtLoraPort = fld("COM4");
        tglLora     = actionToggle("Conectar LoRa");
        tglLora.addActionListener(e -> toggleLora(tglLora.isSelected()));

        JLabel hint = small("Conecta el Gateway LoRa por USB. El gateway reenvía mensajes 'LORA:{...}'");
        card.add(hint);
        card.add(Box.createVerticalStrut(8));
        addField(card, "Puerto del Gateway", txtLoraPort);
        card.add(Box.createVerticalStrut(8));
        card.add(tglLora);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildWifiCard() {
        JPanel card = card("🌐 WiFi / HTTP");

        txtWifiPort = fld("8765");
        tglWifi     = actionToggle("Iniciar Servidor HTTP");
        tglWifi.addActionListener(e -> toggleWifi(tglWifi.isSelected()));

        JLabel hint = small("<html>El ESP32 enviará: POST http://&lt;IP_PC&gt;:8765/data<br>con JSON: {\"temp_in\":24.5,\"humidity\":65.0}</html>");
        card.add(hint);
        card.add(Box.createVerticalStrut(8));
        addField(card, "Puerto HTTP del servidor", txtWifiPort);
        card.add(Box.createVerticalStrut(8));
        card.add(tglWifi);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildFileCard() {
        JPanel card = card("💾 Archivo / Memoria SD");

        txtFilePath = fld("ruta/al/archivo.csv");
        txtFilePath.setEditable(false);
        btnBrowse  = AppTheme.primaryButton("📂 Explorar");
        btnBrowse.addActionListener(e -> browseFile());
        btnLoadFile = AppTheme.primaryButton("⬆️ Cargar Datos");
        btnLoadFile.addActionListener(e -> loadFile());

        JLabel hint = small("Formatos: CSV (columnas: temp_in, humidity, soil...) o JSON array/JSONL");
        card.add(hint);
        card.add(Box.createVerticalStrut(8));
        addField(card, "Archivo CSV o JSON", txtFilePath);
        card.add(Box.createVerticalStrut(6));
        JPanel row = new JPanel(new GridLayout(1, 2, 6, 0));
        row.setOpaque(false); row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        row.setAlignmentX(LEFT_ALIGNMENT);
        row.add(btnBrowse); row.add(btnLoadFile);
        card.add(row);
        card.add(Box.createVerticalGlue());
        return card;
    }

    private JPanel buildPollingPanel() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 8));
        p.setBackground(Color.WHITE);
        p.setBorder(AppTheme.cardBorder());

        p.add(new JLabel("⏱ Sondeo automático cada"));
        spnInterval = new JSpinner(new SpinnerNumberModel(5, 1, 60, 1));
        spnInterval.setPreferredSize(new Dimension(60, 28));
        p.add(spnInterval);
        p.add(new JLabel("segundos"));

        tglPolling = new JToggleButton("▶ Iniciar Sondeo");
        tglPolling.setBackground(AppTheme.PRIMARY_DARK);
        tglPolling.setForeground(Color.WHITE);
        tglPolling.setFocusPainted(false);
        tglPolling.addActionListener(e -> togglePolling(tglPolling.isSelected()));
        p.add(tglPolling);

        lblStatus = new JLabel("⭕ Sin conexión activa");
        lblStatus.setFont(AppTheme.FONT_SMALL);
        lblStatus.setForeground(AppTheme.TEXT_SECONDARY);
        p.add(lblStatus);

        return p;
    }

    private JPanel buildLogPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(Color.WHITE);
        p.setBorder(AppTheme.cardBorder());
        p.setPreferredSize(new Dimension(0, 140));

        JLabel lbl = new JLabel("  📋 Log de datos recibidos");
        lbl.setFont(AppTheme.FONT_SUBTITLE);
        lbl.setForeground(AppTheme.PRIMARY_DARK);
        p.add(lbl, BorderLayout.NORTH);

        txtLog = new JTextArea();
        txtLog.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtLog.setEditable(false);
        txtLog.setBackground(new Color(0xF5F5F5));
        txtLog.setText("  Esperando datos del ESP32...\n");

        JButton btnClear = new JButton("Limpiar");
        btnClear.addActionListener(e -> txtLog.setText(""));
        p.add(new JScrollPane(txtLog), BorderLayout.CENTER);
        p.add(btnClear, BorderLayout.EAST);
        return p;
    }

    // ─── Acciones ─────────────────────────────────────────────────────

    private void toggleSerial(boolean on) {
        if (on) {
            String port = txtSerialPort.getText().trim();
            int baud;
            try { baud = Integer.parseInt(txtBaudRate.getText().trim()); }
            catch (NumberFormatException e) { baud = 115200; }
            serialService = new ESP32SerialService(port, baud);
            boolean ok = serialService.connect();
            if (ok) {
                esp32Manager.addSource(serialService);
                
                // Auto-iniciar sondeo si no está activo
                if (!esp32Manager.isPolling()) {
                    int interval = (int) spnInterval.getValue();
                    esp32Manager.startPolling(interval);
                    tglPolling.setText("⏹ Detener Sondeo");
                    tglPolling.setSelected(true);
                }
                
                appendLog("✅ Serial conectado: " + port);
                tglSerial.setText("🔌 Desconectar Serial");
            } else {
                tglSerial.setSelected(false);
                appendLog("❌ No se pudo conectar a " + port);
                JOptionPane.showMessageDialog(this,
                    "No se pudo abrir " + port + ".\n" +
                    "Verifica que:\n• El ESP32 está conectado\n• El puerto COM es correcto\n• jSerialComm está en el pom.xml",
                    "Error Serial", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            if (serialService != null) {
                esp32Manager.removeSource(serialService);
                serialService = null;
            }
            tglSerial.setText("Conectar Serial");
            appendLog("⭕ Serial desconectado.");
        }
    }

    private void toggleLora(boolean on) {
        if (on) {
            String port = txtLoraPort.getText().trim();
            loraService = new ESP32LoRaService(port);
            boolean ok = loraService.connect();
            if (ok) {
                esp32Manager.addSource(loraService);
                appendLog("✅ LoRa Gateway conectado: " + port);
                tglLora.setText("📡 Desconectar LoRa");
            } else {
                tglLora.setSelected(false);
                appendLog("❌ No se pudo conectar Gateway LoRa en " + port);
            }
        } else {
            if (loraService != null) { esp32Manager.removeSource(loraService); loraService = null; }
            tglLora.setText("Conectar LoRa");
            appendLog("⭕ LoRa desconectado.");
        }
    }

    private void toggleWifi(boolean on) {
        if (on) {
            int port;
            try { port = Integer.parseInt(txtWifiPort.getText().trim()); }
            catch (NumberFormatException e) { port = 8765; }
            wifiService = new ESP32WiFiService(port);
            boolean ok = wifiService.connect();
            if (ok) {
                esp32Manager.addSource(wifiService);
                
                // Auto-iniciar sondeo si no está activo
                if (!esp32Manager.isPolling()) {
                    int interval = (int) spnInterval.getValue();
                    esp32Manager.startPolling(interval);
                    tglPolling.setText("⏹ Detener Sondeo");
                    tglPolling.setSelected(true);
                }
                
                appendLog("✅ Servidor HTTP iniciado en puerto " + port);
                appendLog("   ESP32 envía POST a: http://[IP_PC]:" + port + "/data");
                tglWifi.setText("🌐 Detener Servidor HTTP");
            } else {
                tglWifi.setSelected(false);
                appendLog("❌ No se pudo iniciar servidor en puerto " + port);
            }
        } else {
            if (wifiService != null) { esp32Manager.removeSource(wifiService); wifiService = null; }
            tglWifi.setText("Iniciar Servidor HTTP");
            appendLog("⭕ Servidor HTTP detenido.");
        }
    }

    private void browseFile() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Seleccionar archivo de datos ESP32");
        fc.setFileFilter(new FileNameExtensionFilter("CSV y JSON (*.csv, *.json, *.jsonl)", "csv", "json", "jsonl"));
        if (fc.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            txtFilePath.setText(fc.getSelectedFile().getAbsolutePath());
        }
    }

    private void loadFile() {
        String path = txtFilePath.getText().trim();
        if (path.isEmpty() || path.equals("ruta/al/archivo.csv")) {
            JOptionPane.showMessageDialog(this, "Selecciona un archivo primero.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        new SwingWorker<Integer, Void>() {
            @Override protected Integer doInBackground() {
                return esp32Manager.loadFromFile(path);
            }
            @Override protected void done() {
                setCursor(Cursor.getDefaultCursor());
                try {
                    int n = get();
                    appendLog("✅ Archivo cargado: " + n + " lecturas importadas de " + path);
                    lblStatus.setText("✅ " + n + " lecturas importadas desde archivo.");
                } catch (Exception ex) {
                    appendLog("❌ Error: " + ex.getMessage());
                }
            }
        }.execute();
    }

    private void togglePolling(boolean on) {
        if (on) {
            int interval = (int) spnInterval.getValue();
            esp32Manager.startPolling(interval);
            tglPolling.setText("⏹ Detener Sondeo");
            appendLog("▶ Sondeo automático iniciado cada " + interval + "s");
        } else {
            esp32Manager.stopPolling();
            tglPolling.setText("▶ Iniciar Sondeo");
            appendLog("⏹ Sondeo detenido.");
        }
    }

    private void appendLog(String msg) {
        txtLog.append(msg + "\n");
        txtLog.setCaretPosition(txtLog.getDocument().getLength());
    }

    // ─── Helpers de UI ────────────────────────────────────────────────

    private JPanel card(String title) {
        JPanel p = new JPanel();
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        p.setBackground(Color.WHITE);
        p.setBorder(AppTheme.cardBorder());
        JLabel lbl = new JLabel(title);
        lbl.setFont(AppTheme.FONT_SUBTITLE);
        lbl.setForeground(AppTheme.PRIMARY_DARK);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(10));
        return p;
    }

    private JTextField fld(String placeholder) {
        JTextField f = new JTextField(placeholder);
        f.setFont(AppTheme.FONT_BODY);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        f.setAlignmentX(LEFT_ALIGNMENT);
        return f;
    }

    private JToggleButton actionToggle(String text) {
        JToggleButton t = new JToggleButton(text);
        t.setFont(AppTheme.FONT_BUTTON);
        t.setBackground(AppTheme.PRIMARY_DARK);
        t.setForeground(Color.WHITE);
        t.setFocusPainted(false);
        t.setBorderPainted(false);
        t.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        t.setAlignmentX(LEFT_ALIGNMENT);
        return t;
    }

    private JLabel small(String text) {
        JLabel l = new JLabel("<html>" + text + "</html>");
        l.setFont(AppTheme.FONT_SMALL);
        l.setForeground(AppTheme.TEXT_SECONDARY);
        l.setAlignmentX(LEFT_ALIGNMENT);
        return l;
    }

    private void addField(JPanel p, String label, JComponent field) {
        JLabel lbl = new JLabel(label);
        lbl.setFont(AppTheme.FONT_SMALL);
        lbl.setForeground(AppTheme.TEXT_SECONDARY);
        lbl.setAlignmentX(LEFT_ALIGNMENT);
        p.add(lbl); p.add(Box.createVerticalStrut(3));
        p.add(field); p.add(Box.createVerticalStrut(6));
    }
}
