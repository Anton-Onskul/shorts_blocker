package com.shortsblock.app.core;

/** Реальное состояние службы. ЗАГЛУШКА — реализация на шаге 4. */
public final class ServiceHealth {
    public static final String OFF = "OFF";
    public static final String PAUSED = "PAUSED";
    public static final String NOT_RUNNING = "NOT_RUNNING";
    public static final String OK = "OK";

    private ServiceHealth() { }

    /** connectedAtMs <= 0 — служба ни разу не подключилась. */
    public static String evaluate(boolean inSettings, boolean switchOn, long connectedAtMs, long nowMs) {
        throw new UnsupportedOperationException("не реализовано");
    }
}
