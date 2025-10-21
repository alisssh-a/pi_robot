package gui;

import model.RobotModel;
import log.Logger;

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Locale;

public class MainApplicationFrame extends JFrame {
    private final JDesktopPane desktopPane = new JDesktopPane();
    private final RobotModel robotModel = new RobotModel();
    private final WindowManager windowManager;
    private final LocalizationManager localizationManager;

    // Создание окон
    public MainApplicationFrame() {
        Logger.debug("Test debug message");
        Logger.info("Test info message");
        Logger.error("Test error message");

        windowManager = new WindowManager(desktopPane, robotModel);
        localizationManager = LocalizationManager.getInstance(windowManager);

        setContentPane(desktopPane);
        setJMenuBar(generateMenuBar());
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        // Устанавливаем translationKey на JRootPane
        getRootPane().putClientProperty("translationKey", "app.title");
        localizationManager.updateUI(this);

        windowManager.initializeWindows();
        windowManager.loadWindowState(this);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                exitApplication();
            }
        });
    }

    // Генерация панели меню
    private JMenuBar generateMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        menuBar.add(createLookAndFeelMenu());
        menuBar.add(createTestMenu());
        menuBar.add(createFileMenu());
        localizationManager.updateUI(menuBar); // Обновляем меню после создания
        return menuBar;
    }

    // Меню управления стилем отображения
    private JMenu createLookAndFeelMenu() {
        JMenu menu = new JMenu();
        menu.setMnemonic(KeyEvent.VK_V);
        menu.putClientProperty("translationKey", "look.and.feel.menu");
        menu.getAccessibleContext().setAccessibleDescription(localizationManager.getString("look.and.feel.description"));

        JMenuItem systemItem = createMenuItem("system.look.and.feel", KeyEvent.VK_S,
                () -> setLookAndFeel(UIManager.getSystemLookAndFeelClassName()));
        JMenuItem crossPlatformItem = createMenuItem("cross.platform.look.and.feel", KeyEvent.VK_U,
                () -> setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()));

        menu.add(systemItem);
        menu.add(crossPlatformItem);
        return menu;
    }

    // Тестовое меню с командами
    private JMenu createTestMenu() {
        JMenu menu = new JMenu();
        menu.setMnemonic(KeyEvent.VK_T);
        menu.putClientProperty("translationKey", "test.menu");
        menu.getAccessibleContext().setAccessibleDescription(localizationManager.getString("test.menu.description"));

        JMenuItem logMessageItem = createMenuItem("log.message", KeyEvent.VK_L,
                () -> Logger.debug(localizationManager.getString("new.line.message")));
        menu.add(logMessageItem);
        return menu;
    }

    // Метод для создания пунктов меню
    private JMenuItem createMenuItem(String translationKey, int mnemonic, Runnable action) {
        JMenuItem item = new JMenuItem();
        item.setMnemonic(mnemonic);
        item.putClientProperty("translationKey", translationKey);
        item.addActionListener(event -> action.run());
        localizationManager.updateUI(item);
        return item;
    }

    // Стиль отображения
    private void setLookAndFeel(String className) {
        try {
            UIManager.setLookAndFeel(className);
            SwingUtilities.updateComponentTreeUI(this);
        } catch (Exception e) {
            Logger.debug(localizationManager.getString("look.and.feel.error") + ": " + e.getMessage());
        }
    }

    // Метод выхода и смены языка
    private JMenu createFileMenu() {
        JMenu menu = new JMenu();
        menu.setMnemonic(KeyEvent.VK_F);
        menu.putClientProperty("translationKey", "file.menu");
        menu.getAccessibleContext().setAccessibleDescription(localizationManager.getString("file.menu.description"));

        JMenu languageMenu = new JMenu();
        languageMenu.putClientProperty("translationKey", "language.menu");
        localizationManager.updateUI(languageMenu);

        JMenuItem russianItem = new JMenuItem();
        russianItem.putClientProperty("translationKey", "language.russian");
        russianItem.addActionListener(e -> {
            changeLocale(new Locale("ru"));
        });
        localizationManager.updateUI(russianItem);

        JMenuItem englishItem = new JMenuItem();
        englishItem.putClientProperty("translationKey", "language.english");
        englishItem.addActionListener(e -> {
            changeLocale(new Locale("en"));
        });
        localizationManager.updateUI(englishItem);

        languageMenu.add(russianItem);
        languageMenu.add(englishItem);
        menu.add(languageMenu);

        JMenuItem exitItem = new JMenuItem();
        exitItem.setMnemonic(KeyEvent.VK_Q);
        exitItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Q, InputEvent.CTRL_DOWN_MASK));
        exitItem.putClientProperty("translationKey", "exit.item");
        exitItem.addActionListener(event -> exitApplication());
        localizationManager.updateUI(exitItem);
        menu.add(exitItem);

        return menu;
    }

    private void changeLocale(Locale locale) {
        localizationManager.setLocale(locale);
        localizationManager.updateUI(this);
        for (JInternalFrame frame : desktopPane.getAllFrames()) {
            localizationManager.updateUI(frame);
        }
    }

    // Закрытие приложения
    private void exitApplication() {
        UIManager.put("OptionPane.yesButtonText", localizationManager.getString("yes.button"));
        UIManager.put("OptionPane.noButtonText", localizationManager.getString("no.button"));
        UIManager.put("OptionPane.cancelButtonText", localizationManager.getString("cancel.button"));

        int result = JOptionPane.showConfirmDialog(
                this,
                localizationManager.getString("exit.confirmation"),
                localizationManager.getString("exit.confirmation.title"),
                JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);

        if (result == JOptionPane.YES_OPTION) {
            windowManager.saveWindowState(this);
            windowManager.shutdown();
            robotModel.shutdown();
            dispose();

            EventQueue.invokeLater(() -> {
                boolean allWindowsClosed = true;
                for (Window window : Window.getWindows()) {
                    if (window.isDisplayable()) {
                        allWindowsClosed = false;
                        break;
                    }
                }
                if (allWindowsClosed) {
                    Logger.debug(localizationManager.getString("application.closed.message"));
                }
            });
        }
    }
}