package gui;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.BiConsumer;

public class LocalizationManager {
    private static LocalizationManager instance;
    private Locale currentLocale;
    private ResourceBundle messages;

    private final Map<Class<? extends Component>, BiConsumer<Component, String>> translationActions;

    private static final String LOCALE_KEY = "locale";
    private final WindowManager windowManager;

    // Маппинг типов компонентов на действия для установки текста
    private LocalizationManager(WindowManager windowManager) {
        this.windowManager = windowManager;
        currentLocale = loadSavedLocale();
        if (currentLocale == null) {
            currentLocale = new Locale("ru"); // Русский по умолчанию при первом запуске
        }

        translationActions = new HashMap<>();
        translationActions.put(JButton.class, (component, text) -> ((JButton) component).setText(text));
        translationActions.put(JLabel.class, (component, text) -> ((JLabel) component).setText(text));
        translationActions.put(JMenu.class, (component, text) -> {
            ((JMenu) component).setText(text);
            component.revalidate();
            component.repaint();
        });
        translationActions.put(JMenuItem.class, (component, text) -> {
            ((JMenuItem) component).setText(text);
            component.revalidate();
            component.repaint();
        });
        translationActions.put(JInternalFrame.class, (component, text) -> {
            ((JInternalFrame) component).setTitle(text);
        });
    }

    public static LocalizationManager getInstance(WindowManager windowManager) {
        if (instance == null) {
            instance = new LocalizationManager(windowManager);
        }
        return instance;
    }

    public void setLocale(Locale locale) {
        currentLocale = locale;
        messages = null; // Сбрасываем текущий ResourceBundle
        saveLocale(locale); // Сохраняем выбранную локаль
    }

    public Locale getCurrentLocale() {
        return currentLocale;
    }

    private void loadLocaleIfNeeded() {
        if (messages == null) {
            try {
                messages = ResourceBundle.getBundle("messages", currentLocale);
            } catch (Exception e) {
                System.err.println("Failed to load ResourceBundle for locale: " + currentLocale);
                e.printStackTrace();
                messages = ResourceBundle.getBundle("messages", Locale.ENGLISH); // Резервный вариант
            }
        }
    }

    public String getString(String key) {
        loadLocaleIfNeeded();
        try {
            String value = messages.getString(key);
            return value;
        } catch (Exception e) {
            System.err.println("Missing translation for key: " + key);
            return key; // Возвращаем ключ как запасной вариант
        }
    }

    private void saveLocale(Locale locale) {
        windowManager.saveLocale(locale.getLanguage());
    }

    private Locale loadSavedLocale() {
        String language = windowManager.loadLocale();
        if (language != null && !language.isEmpty()) {
            return new Locale(language);
        }
        return null;
    }

    public void updateUI(Component component) {
        if (component == null) return;

        if (component instanceof JComponent) {
            JComponent jComponent = (JComponent) component;
            String translationKey = (String) jComponent.getClientProperty("translationKey");
            if (translationKey != null) {
                translationActions.entrySet().stream()
                        .filter(entry -> entry.getKey().isInstance(component))
                        .findFirst()
                        .ifPresent(entry -> entry.getValue().accept(component, getString(translationKey)));
            }

            if (component instanceof JMenu) {
                for (Component menuChild : ((JMenu) component).getMenuComponents()) {
                    updateUI(menuChild);
                }
            }

            for (Component child : jComponent.getComponents()) {
                updateUI(child);
            }
        }

        if (component instanceof JFrame) {
            JFrame frame = (JFrame) component;
            JRootPane rootPane = frame.getRootPane();
            String titleKey = (String) rootPane.getClientProperty("translationKey");
            if (titleKey != null) {
                frame.setTitle(getString(titleKey));
            }
            updateUI(frame.getJMenuBar());
            for (Window window : frame.getOwnedWindows()) {
                updateUI(window);
            }
        }
    }
}