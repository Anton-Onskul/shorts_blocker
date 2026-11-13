package com.shortsblock.app.core;

/** Человекочитаемое «5 мин назад». */
public final class Ago {
    private Ago() { }

    /** thenMs <= 0 — «никогда». */
    public static String format(long thenMs, long nowMs) {
        if (thenMs <= 0) {
            return "никогда";
        }
        long d = nowMs - thenMs;
        if (d < 1000) {
            return "только что"; // в том числе время в будущем после смены часов
        }
        if (d < 60_000) {
            return (d / 1000) + " с назад";
        }
        if (d < 3_600_000L) {
            return (d / 60_000) + " мин назад";
        }
        if (d < 86_400_000L) {
            return (d / 3_600_000L) + " ч назад";
        }
        return (d / 86_400_000L) + " дн назад";
    }
}
