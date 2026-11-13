package com.shortsblock.app.core;

/** Реальное состояние службы, а не только галочка в настройках. */
public final class ServiceHealth {
    public static final String OFF = "OFF";
    public static final String PAUSED = "PAUSED";
    public static final String NOT_RUNNING = "NOT_RUNNING";
    public static final String OK = "OK";

    private ServiceHealth() { }

    /** connectedAtMs <= 0 — служба ни разу не подключилась с момента запуска процесса. */
    public static String evaluate(boolean inSettings, boolean switchOn, long connectedAtMs, long nowMs) {
        if (!inSettings) {
            return OFF;
        }
        if (!switchOn) {
            return PAUSED;
        }
        if (connectedAtMs <= 0) {
            return NOT_RUNNING;
        }
        return OK;
    }
}
