package com.shortsblock.app.core;

import java.util.List;

/** Кольцевой буфер дампов. ЗАГЛУШКА — реализация на шаге 4. */
public final class DumpLog {
    public DumpLog(int maxEntries, int maxChars) {
        if (maxEntries <= 0 || maxChars <= 0) {
            throw new IllegalArgumentException("лимиты должны быть > 0");
        }
    }

    /** true, если запись добавлена (false — дубликат предыдущей). */
    public boolean add(String dump) {
        throw new UnsupportedOperationException("не реализовано");
    }

    public List<String> entries() {
        throw new UnsupportedOperationException("не реализовано");
    }

    /** Отчёт: шапка + все записи, разделённые строкой "-----". */
    public String report(String header) {
        throw new UnsupportedOperationException("не реализовано");
    }
}
