/**
 * @brief Внутреннее окно приложения для отображения игровой визуализации
 * @details Обеспечивает управление окном игры, интеграцию с системой локализации
 * и корректное завершение работы визуализатора при закрытии
 */
package gui;

import javax.swing.*;
import java.awt.*;

public class GameWindow extends JInternalFrame {
    private final GameVisualizer m_visualizer;
    private final LocalizationManager localizationManager;

    public GameWindow(GameVisualizer visualizer, WindowManager windowManager) {
        super("", true, true, true, true);
        this.m_visualizer = visualizer;
        this.localizationManager = LocalizationManager.getInstance(windowManager);

        putClientProperty("translationKey", "game.window.title");

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(m_visualizer, BorderLayout.CENTER);
        getContentPane().add(panel);
        pack();

        addInternalFrameListener(new javax.swing.event.InternalFrameAdapter() {
            @Override
            public void internalFrameClosing(javax.swing.event.InternalFrameEvent e) {
                shutdown();
            }
        });

        localizationManager.updateUI(this);
    }

    public void shutdown() {
        m_visualizer.shutdown();
    }
}