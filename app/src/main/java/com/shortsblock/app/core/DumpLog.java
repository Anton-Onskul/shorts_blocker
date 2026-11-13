package com.shortsblock.app.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** Кольцевой буфер дампов экрана: ограничен числом записей и суммарным размером. */
public final class DumpLog {
    private final int maxEntries;
    private final int maxChars;
    private final Deque<String> items = new ArrayDeque<>();
    private int chars;

    public DumpLog(int maxEntries, int maxChars) {
        if (maxEntries <= 0 || maxChars <= 0) {
            throw new IllegalArgumentException("лимиты должны быть > 0");
        }
        this.maxEntries = maxEntries;
        this.maxChars = maxChars;
    }

    /** true, если запись добавлена (false — дубликат предыдущей). */
    public synchronized boolean add(String dump) {
        if (dump == null) {
            return false;
        }
        String d = dump.length() > maxChars ? dump.substring(0, maxChars) : dump;
        if (d.equals(items.peekLast())) {
            return false;
        }
        items.addLast(d);
        chars += d.length();
        while (items.size() > maxEntries || chars > maxChars) {
            chars -= items.removeFirst().length();
        }
        return true;
    }

    public synchronized List<String> entries() {
        return new ArrayList<>(items);
    }

    public synchronized void clear() {
        items.clear();
        chars = 0;
    }

    /** Отчёт: шапка + все записи, разделённые строкой "-----". */
    public synchronized String report(String header) {
        StringBuilder sb = new StringBuilder(header);
        if (items.isEmpty()) {
            return sb.append("\n(экранов не записано)").toString();
        }
        for (String e : items) {
            sb.append("\n-----\n").append(e);
        }
        return sb.toString();
    }
}
