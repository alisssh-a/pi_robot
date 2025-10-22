package log;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.concurrent.locks.ReentrantReadWriteLock;

// Кольцевой буфер для хранения логов с потокобезопасностью
public class CircularLogBuffer implements Iterable<LogEntry> {
    private final LogEntry[] buffer; // Массив для хранения записей
    private final int capacity; // Максимальная ёмкость
    private int head; // Индекс начала (где хранится самая старая запись)
    private int size; // Текущее количество записей
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock(); // Для синхронизации

    public CircularLogBuffer(int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        this.capacity = capacity;
        this.buffer = new LogEntry[capacity];
        this.head = 0;
        this.size = 0;
    }

    // Добавление записи (вытесняет старую, если буфер полон)
    public void append(LogLevel logLevel, String message) {
        LogEntry entry = new LogEntry(logLevel, message);
        lock.writeLock().lock();
        try {
            int index = (head + size) % capacity;
            buffer[index] = entry;
            if (size < capacity) {
                size++;
            } else {
                head = (head + 1) % capacity; // Сдвигаем начало, вытесняя старую запись
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    // Текущий размер буфера
    public int size() {
        lock.readLock().lock();
        try {
            return size;
        } finally {
            lock.readLock().unlock();
        }
    }

    // Получение сегмента записей (от startFrom до startFrom + count)
    public Iterable<LogEntry> range(int startFrom, int count) {
        lock.readLock().lock();
        try {
            if (startFrom < 0 || startFrom >= size || count <= 0) {
                return Collections.emptyList();
            }
            int endIndex = Math.min(startFrom + count, size);
            int actualCount = endIndex - startFrom;
            LogEntry[] result = new LogEntry[actualCount];
            for (int i = 0; i < actualCount; i++) {
                result[i] = buffer[(head + startFrom + i) % capacity];
            }
            // Возвращаем Iterable на основе массива
            return () -> Arrays.stream(result).iterator();
        } finally {
            lock.readLock().unlock();
        }
    }

    // Получение всех записей
    public Iterable<LogEntry> all() {
        return range(0, size());
    }

    // Потокобезопасный итератор (работает с копией данных)
    @Override
    public Iterator<LogEntry> iterator() {
        return all().iterator();
    }
}