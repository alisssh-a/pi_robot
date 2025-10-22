package gui;

import model.RobotModel;
import log.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.util.Properties;

/**
 * @brief Класс-менеджер управления окнами приложения
 * @details Управляет созданием, позиционированием и сохранением состояния окон,
 * а также обработкой локализации и конфигурации приложения.
 */
public class WindowManager {

    private final JDesktopPane desktopPane;
    private final RobotModel robotModel;
    private static final String CONFIG_PATH = System.getProperty("user.home") + "/robots_config.properties";
    private static final Dimension NORMAL_SIZE = new Dimension(950, 850);
    private final LocalizationManager localizationManager;
    private static final String LOCALE_KEY = "locale";

    /**
     * @brief Конструктор менеджера окон
     * @param desktopPane Панель для размещения внутренних окон
     * @param robotModel Модель данных робота
     * @details Инициализирует менеджер с заданной панелью и моделью
     */
    public WindowManager(JDesktopPane desktopPane, RobotModel robotModel) {
        this.desktopPane = desktopPane;
        this.robotModel = robotModel;
        this.localizationManager = LocalizationManager.getInstance(this);
    }

    /**
     * @brief Инициализация окон приложения
     * @details Создаёт и размещает основные окна (лог, игровое поле, координаты)
     */
    public void initializeWindows() {

        desktopPane.removeAll();

        LogWindow logWindow = new LogWindow(Logger.getDefaultLogSource(), this); // Передаём this
        logWindow.setBounds(10, 10, 500, 500);
        addWindow(logWindow);

        GameVisualizer visualizer = new GameVisualizer(robotModel);
        GameWindow gameWindow = new GameWindow(visualizer, this); // Передаём this
        gameWindow.setBounds(520, 10, 400, 400);
        addWindow(gameWindow);

        RobotCoordinatesWindow coordsWindow = new RobotCoordinatesWindow(robotModel, this); // Передаём this
        coordsWindow.setBounds(930, 10, 200, 100);
        addWindow(coordsWindow);

        for (JInternalFrame frame : desktopPane.getAllFrames()) {
            localizationManager.updateUI(frame);
        }
    }

    /**
     * @brief Добавление окна на панель
     * @param frame Окно для добавления
     * @details Добавляет окно на панель и делает его видимым
     */
    public void addWindow(JInternalFrame frame) {

        desktopPane.add(frame);
        frame.setVisible(true);
        try {
            frame.setSelected(true);

        } catch (java.beans.PropertyVetoException e) {
            Logger.error(localizationManager.getString("window.selection.error") + ": " + e.getMessage());
        }
    }

    /**
     * @brief Сохранение состояния главного окна
     * @param frame Главное окно приложения
     * @details Сохраняет состояние окна (размер, позицию) и текущую локаль в файл конфигурации
     */
    public void saveWindowState(JFrame frame) {
        Properties props = loadProperties();
        int state = frame.getExtendedState();
        props.setProperty("main.state", String.valueOf(state));

        if (state == Frame.NORMAL) {
            int width = Math.max(frame.getWidth(), NORMAL_SIZE.width);
            int height = Math.max(frame.getHeight(), NORMAL_SIZE.height);
            props.setProperty("main.x", String.valueOf(frame.getX()));
            props.setProperty("main.y", String.valueOf(frame.getY()));
            props.setProperty("main.width", String.valueOf(width));
            props.setProperty("main.height", String.valueOf(height));
            Logger.debug(localizationManager.getString("saved.normal.state") + ": x=" + frame.getX() + ", y=" + frame.getY() + ", width=" + width + ", height=" + height);
        } else {
            Logger.debug(localizationManager.getString("saved.state") + ": state=" + state);
        }

        for (JInternalFrame internalFrame : desktopPane.getAllFrames()) {
            String translationKey = (String) internalFrame.getClientProperty("translationKey");
            if (translationKey != null) {
                saveInternalFrameState(internalFrame, translationKey, props);
            }
        }

        // Явно сохраняем текущую локаль перед записью в файл
        String currentLocale = localizationManager.getCurrentLocale().getLanguage();
        if (currentLocale != null && !currentLocale.isEmpty()) {
            props.setProperty(LOCALE_KEY, currentLocale);
        }

        saveProperties(props);
    }

    /**
     * @brief Загрузка состояния главного окна
     * @param frame Главное окно приложения
     * @details Загружает сохранённое состояние окна из файла конфигурации
     */
    public void loadWindowState(JFrame frame) {
        Properties props = loadProperties();
        File configFile = new File(CONFIG_PATH);

        frame.setMinimumSize(NORMAL_SIZE);

        frame.addWindowStateListener(e -> {
            int oldState = e.getOldState();
            int newState = e.getNewState();
            Logger.debug(localizationManager.getString("window.state.changed") + ": " + localizationManager.getString("old.state") + "=" + oldState + ", " + localizationManager.getString("new.state") + "=" + newState);
            if ((newState == Frame.NORMAL) &&
                    (oldState == Frame.MAXIMIZED_BOTH || oldState == Frame.ICONIFIED)) {
                frame.setSize(NORMAL_SIZE);
                centerWindow(frame);
                SwingUtilities.invokeLater(() -> saveWindowState(frame));
            }
        });

        if (!configFile.exists()) {
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            Logger.debug(localizationManager.getString("first.launch.maximized"));
            return;
        }

        try {
            int state = Integer.parseInt(props.getProperty("main.state", String.valueOf(Frame.MAXIMIZED_BOTH)));
            Logger.debug(localizationManager.getString("loaded.window.state") + ": state=" + state);
            frame.setExtendedState(state);

            if (state == Frame.NORMAL) {
                int x = Integer.parseInt(props.getProperty("main.x", "-1"));
                int y = Integer.parseInt(props.getProperty("main.y", "-1"));
                int width = Integer.parseInt(props.getProperty("main.width", String.valueOf(NORMAL_SIZE.width)));
                int height = Integer.parseInt(props.getProperty("main.height", String.valueOf(NORMAL_SIZE.height)));

                width = Math.max(width, NORMAL_SIZE.width);
                height = Math.max(height, NORMAL_SIZE.height);

                Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
                if (x < 0 || y < 0 || x + width > screenSize.width || y + height > screenSize.height) {
                    Logger.debug(localizationManager.getString("invalid.coordinates") + " (x=" + x + ", y=" + y + "), " + localizationManager.getString("center.window"));
                    centerWindow(frame);
                } else {
                    frame.setBounds(x, y, width, height);
                    Logger.debug(localizationManager.getString("set.saved.coordinates") + ": x=" + x + ", y=" + y + ", width=" + width + ", height=" + height);
                }
            }

            for (JInternalFrame internalFrame : desktopPane.getAllFrames()) {
                String translationKey = (String) internalFrame.getClientProperty("translationKey");
                if (translationKey != null) {
                    loadInternalFrameState(internalFrame, translationKey, props);
                }
            }
        } catch (NumberFormatException e) {
            Logger.error(localizationManager.getString("window.state.load.error") + ": " + e.getMessage());
            frame.setExtendedState(Frame.MAXIMIZED_BOTH);
            Logger.debug(localizationManager.getString("load.error.maximized"));
        }
    }

    /**
     * @brief Сохранение состояния внутреннего окна
     * @param frame Внутреннее окно
     * @param name Ключ для идентификации окна
     * @param props Объект свойств для сохранения
     * @details Сохраняет координаты, размер и состояние минимизации окна
     */
    private void saveInternalFrameState(JInternalFrame frame, String name, Properties props) {
        props.setProperty(name + ".x", String.valueOf(frame.getX()));
        props.setProperty(name + ".y", String.valueOf(frame.getY()));
        props.setProperty(name + ".width", String.valueOf(frame.getWidth()));
        props.setProperty(name + ".height", String.valueOf(frame.getHeight()));
        props.setProperty(name + ".icon", String.valueOf(frame.isIcon()));
    }

    /**
     * @brief Загрузка состояния внутреннего окна
     * @param frame Внутреннее окно
     * @param name Ключ для идентификации окна
     * @param props Объект свойств для загрузки
     * @details Восстанавливает координаты, размер и состояние минимизации окна
     */
    private void loadInternalFrameState(JInternalFrame frame, String name, Properties props) {
        try {
            int x = Integer.parseInt(props.getProperty(name + ".x", "50"));
            int y = Integer.parseInt(props.getProperty(name + ".y", "50"));
            int width = Integer.parseInt(props.getProperty(name + ".width", "300"));
            int height = Integer.parseInt(props.getProperty(name + ".height", "300"));
            boolean icon = Boolean.parseBoolean(props.getProperty(name + ".icon", "false"));

            frame.setBounds(x, y, width, height);
            frame.setIcon(icon);
        } catch (Exception e) {
            Logger.error(localizationManager.getString("internal.window.load.error") + ": " + e.getMessage());
        }
    }

    /**
     * @brief Центрирование главного окна
     * @param frame Главное окно приложения
     * @details Располагает окно по центру экрана
     */
    private void centerWindow(JFrame frame) {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int x = (screenSize.width - frame.getWidth()) / 2;
        int y = (screenSize.height - frame.getHeight()) / 2;
        frame.setLocation(x, y);
        Logger.debug(localizationManager.getString("centering.window") + ": x=" + x + ", y=" + y);
    }

    /**
     * @brief Загрузка свойств из файла конфигурации
     * @return Объект Properties с загруженными данными
     * @details Читает существующие настройки из файла, если он существует
     */
    private Properties loadProperties() {
        Properties props = new Properties();
        File configFile = new File(CONFIG_PATH);
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
            } catch (IOException e) {
                Logger.error(localizationManager.getString("window.state.load.error") + ": " + e.getMessage());
            }
        }
        return props;
    }

    /**
     * @brief Сохранение свойств в файл конфигурации
     * @param props Объект свойств для сохранения
     * @details Записывает свойства в файл с комментарием о конфигурации
     */
    private void saveProperties(Properties props) {
        try {
            File configFile = new File(CONFIG_PATH);
            configFile.getParentFile().mkdirs();
            try (FileOutputStream fos = new FileOutputStream(configFile)) {
                props.store(fos, localizationManager.getString("window.state.config.comment"));
                Logger.debug(localizationManager.getString("config.saved") + " " + CONFIG_PATH);
            }
        } catch (IOException e) {
            Logger.error(localizationManager.getString("window.state.save.error") + ": " + e.getMessage());
        }
    }

    /**
     * @brief Сохранение текущей локали
     * @param language Код языка (например, "ru" или "en")
     * @details Сохраняет выбранный язык в файл конфигурации
     */
    public void saveLocale(String language) {
        Properties properties = loadProperties();
        properties.setProperty(LOCALE_KEY, language);
        saveProperties(properties);
    }

    /**
     * @brief Загрузка сохранённой локали
     * @return Код языка или null, если локаль не найдена
     * @details Читает сохранённый язык из файла конфигурации
     */
    public String loadLocale() {
        Properties properties = loadProperties();
        String language = properties.getProperty(LOCALE_KEY);
        return language;
    }

    /**
     * @brief Завершение работы приложения
     * @details Сохраняет состояние окон и закрывает все внутренние окна
     */
    public void shutdown() {
        JFrame mainFrame = desktopPane.getTopLevelAncestor() instanceof JFrame ? (JFrame) desktopPane.getTopLevelAncestor() : null;
        if (mainFrame != null) {
            saveWindowState(mainFrame);
        }
        for (JInternalFrame frame : desktopPane.getAllFrames()) {
            if (frame instanceof GameWindow) {
                ((GameWindow) frame).shutdown();
            }
            frame.dispose();
        }
    }
}