package log;

// Упрощает запись в лог, предоставляя методы для каждого уровня

public final class Logger {
    private static final LogWindowSource defaultLogSource = new LogWindowSource(20); // Размер буфера

    private Logger() {}

    public static void trace(String message) {
        defaultLogSource.append(LogLevel.Trace, message);
    }

    public static void debug(String message) {
        defaultLogSource.append(LogLevel.Debug, message);
    }

    public static void info(String message) {
        defaultLogSource.append(LogLevel.Info, message);
    }

    public static void warn(String message) {
        defaultLogSource.append(LogLevel.Warning, message);
    }

    public static void error(String message) {
        defaultLogSource.append(LogLevel.Error, message);
    }

    public static void fatal(String message) {
        defaultLogSource.append(LogLevel.Fatal, message);
    }

    public static LogWindowSource getDefaultLogSource() {
        return defaultLogSource;
    }
}
