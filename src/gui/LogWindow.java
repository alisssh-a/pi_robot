package gui;

import log.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class LogWindow extends JInternalFrame implements LogChangeListener {
    private final LogWindowSource logSource;
    private final JTextArea logContent;
    private final Timer updateTimer;
    private LogLevel currentFilterLevel = LogLevel.Trace;
    private final LocalizationManager localizationManager;

    public LogWindow(LogWindowSource logSource, WindowManager windowManager) {
        super("", true, true, true, true);
        this.logSource = logSource;
        this.logSource.registerListener(this);
        this.localizationManager = LocalizationManager.getInstance(windowManager);

        putClientProperty("translationKey", "log.window.title");

        logContent = new JTextArea();
        logContent.setEditable(false);
        logContent.setFont(new Font("Monospaced", Font.PLAIN, 12));

        JScrollPane scrollPane = new JScrollPane(logContent);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);

        JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));

        JComboBox<LogLevel> levelFilter = new JComboBox<>(LogLevel.values());
        levelFilter.setSelectedItem(LogLevel.Trace);
        levelFilter.addActionListener(e -> {
            currentFilterLevel = (LogLevel) levelFilter.getSelectedItem();
            updateLogContent();
        });
        JLabel levelLabel = new JLabel();
        levelLabel.putClientProperty("translationKey", "level.label");
        levelLabel.getAccessibleContext().setAccessibleName("level.label");
        controlPanel.add(levelLabel);
        controlPanel.add(levelFilter);

        JButton clearButton = new JButton();
        clearButton.putClientProperty("translationKey", "clear.button");
        clearButton.addActionListener(e -> logContent.setText(""));
        controlPanel.add(clearButton);

        JButton saveButton = new JButton();
        saveButton.putClientProperty("translationKey", "save.button");
        saveButton.addActionListener(this::saveLogToFile);
        controlPanel.add(saveButton);

        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(controlPanel, BorderLayout.NORTH);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        getContentPane().add(mainPanel);
        pack();

        updateTimer = new Timer(100, e -> updateLogContent());
        updateTimer.setRepeats(false);
        updateLogContent();

        localizationManager.updateUI(this);
    }

    private void updateLogContent() {
        StringBuilder content = new StringBuilder();
        for (LogEntry entry : logSource.all()) {
            if (entry.getLevel().level() >= currentFilterLevel.level()) {
                content.append(String.format("[%s] %s%n",
                        entry.getLevel().name(), entry.getMessage()));
            }
        }
        logContent.setText(content.toString());
        logContent.setCaretPosition(logContent.getDocument().getLength());
    }

    private void saveLogToFile(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle(localizationManager.getString("save.log.title"));
        fileChooser.setSelectedFile(new File("robot_log.txt"));

        if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            try (PrintWriter writer = new PrintWriter(file)) {
                for (LogEntry entry : logSource.all()) {
                    writer.printf("[%s] %s%n",
                            entry.getLevel().name(), entry.getMessage());
                }
                Logger.debug(localizationManager.getString("log.saved.message") + ": " + file.getAbsolutePath());
            } catch (IOException ex) {
                Logger.error(localizationManager.getString("log.save.error") + ": " + ex.getMessage());
                JOptionPane.showMessageDialog(this,
                        localizationManager.getString("log.save.error.message"),
                        localizationManager.getString("error.title"),
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    @Override
    public void onLogChanged() {
        if (!updateTimer.isRunning()) {
            updateTimer.start();
        }
    }

    @Override
    public void dispose() {
        logSource.unregisterListener(this);
        updateTimer.stop();
        super.dispose();
    }
}