package com.agropulse.ui.panels;

import com.agropulse.controller.GreenhouseController;
import com.agropulse.model.User;
import com.agropulse.ui.AppTheme;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/** Panel Simulación de Lecturas – compatible NetBeans */
public class SimulationPanel extends javax.swing.JPanel {

    private final GreenhouseController controller;
    private final User user;

    public SimulationPanel(GreenhouseController controller, User user) {
        this.controller = controller;
        this.user       = user;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 16));
        setBackground(AppTheme.BG_MAIN);
        setBorder(new EmptyBorder(16, 16, 16, 16));

        pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(AppTheme.BG_MAIN);
        lblTitle = new JLabel("📈 Simulación de Lecturas");
        lblTitle.setFont(AppTheme.FONT_TITLE);
        lblTitle.setForeground(AppTheme.PRIMARY_DARK);
        pnlHeader.add(lblTitle, BorderLayout.WEST);

        // Config card
        pnlConfig = new JPanel(new GridBagLayout());
        pnlConfig.setBackground(Color.WHITE);
        pnlConfig.setBorder(AppTheme.cardBorder());

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6); g.anchor = GridBagConstraints.WEST;

        lblRounds = new JLabel("Número de rondas:");
        lblRounds.setFont(AppTheme.FONT_BODY);
        spinRounds = new JSpinner(new SpinnerNumberModel(3, 1, 50, 1));
        spinRounds.setFont(AppTheme.FONT_BODY);
        spinRounds.setPreferredSize(new Dimension(100, 32));

        lblDelay = new JLabel("Intervalo (ms):");
        lblDelay.setFont(AppTheme.FONT_BODY);
        spinDelay = new JSpinner(new SpinnerNumberModel(500, 0, 3000, 100));
        spinDelay.setFont(AppTheme.FONT_BODY);
        spinDelay.setPreferredSize(new Dimension(100, 32));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressBar.setString("Listo");
        progressBar.setForeground(AppTheme.PRIMARY_LIGHT);

        btnSimulate = AppTheme.primaryButton("▶ Ejecutar Simulación");
        btnSimulate.setPreferredSize(new Dimension(200, 38));
        btnSimulate.addActionListener(e -> runSimulation());

        g.gridx = 0; g.gridy = 0; pnlConfig.add(lblRounds, g);
        g.gridx = 1; pnlConfig.add(spinRounds, g);
        g.gridx = 0; g.gridy = 1; pnlConfig.add(lblDelay, g);
        g.gridx = 1; pnlConfig.add(spinDelay, g);
        g.gridx = 0; g.gridy = 2; g.gridwidth = 2; g.fill = GridBagConstraints.HORIZONTAL;
        pnlConfig.add(progressBar, g);
        g.gridy = 3;
        pnlConfig.add(btnSimulate, g);

        // Log output
        txtLog = new JTextArea();
        txtLog.setFont(AppTheme.FONT_MONOSPACE);
        txtLog.setEditable(false);
        txtLog.setBackground(new Color(0x1E1E1E));
        txtLog.setForeground(new Color(0x4CAF50));
        txtLog.setBorder(new EmptyBorder(10, 10, 10, 10));

        pnlLogCard = new JPanel(new BorderLayout());
        pnlLogCard.setBackground(Color.WHITE);
        pnlLogCard.setBorder(AppTheme.cardBorder());
        lblLogTitle = new JLabel("  📟 Salida de Simulación");
        lblLogTitle.setFont(AppTheme.FONT_SUBTITLE);
        lblLogTitle.setForeground(AppTheme.PRIMARY_DARK);
        pnlLogCard.add(lblLogTitle, BorderLayout.NORTH);
        pnlLogCard.add(new JScrollPane(txtLog), BorderLayout.CENTER);

        splitMain = new JSplitPane(JSplitPane.VERTICAL_SPLIT, pnlConfig, pnlLogCard);
        splitMain.setDividerLocation(200);
        splitMain.setBorder(null);

        add(pnlHeader,  BorderLayout.NORTH);
        add(splitMain,  BorderLayout.CENTER);
    }

    private void runSimulation() {
        int rounds = (int) spinRounds.getValue();
        int delay  = (int) spinDelay.getValue();
        btnSimulate.setEnabled(false);
        txtLog.setText("");
        progressBar.setMaximum(rounds);
        progressBar.setValue(0);

        new SwingWorker<Void, String>() {
            @Override protected Void doInBackground() throws Exception {
                for (int i = 1; i <= rounds; i++) {
                    publish("▶ Ronda " + i + "/" + rounds);
                    controller.simulateReadings();
                    Thread.sleep(delay);
                    setProgress(i);
                }
                return null;
            }
            @Override protected void process(java.util.List<String> c) {
                c.forEach(m -> txtLog.append(m + "\n"));
                progressBar.setValue(getProgress());
                progressBar.setString("Ronda " + getProgress() + "/" + rounds);
            }
            @Override protected void done() {
                txtLog.append("\n✅ Completado: " + rounds + " rondas.\n");
                progressBar.setString("Completado");
                btnSimulate.setEnabled(true);
            }
        }.execute();
    }

    // Variables privadas (estilo NetBeans)
    private javax.swing.JPanel     pnlHeader;
    private javax.swing.JPanel     pnlConfig;
    private javax.swing.JPanel     pnlLogCard;
    private javax.swing.JLabel     lblTitle;
    private javax.swing.JLabel     lblRounds;
    private javax.swing.JLabel     lblDelay;
    private javax.swing.JLabel     lblLogTitle;
    private javax.swing.JSpinner   spinRounds;
    private javax.swing.JSpinner   spinDelay;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JButton    btnSimulate;
    private javax.swing.JTextArea  txtLog;
    private javax.swing.JSplitPane splitMain;
}
