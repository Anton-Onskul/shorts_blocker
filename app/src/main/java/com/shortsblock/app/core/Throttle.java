package com.shortsblock.app.core;

/**
 * Ограничивает частоту действий: после срабатывания следующее разрешено не раньше,
 * чем через cooldownMs. Нужно, чтобы не нажимать «Назад» много раз подряд, пока
 * экран Shorts ещё закрывается.
 */
public final class Throttle {

    private final long cooldownMs;
    private long lastMs;
    private boolean fired;

    public Throttle(long cooldownMs) {
        if (cooldownMs < 0) {
            throw new IllegalArgumentException("cooldownMs < 0");
        }
        this.cooldownMs = cooldownMs;
    }

    public synchronized boolean tryAcquire(long nowMs) {
        // Если часы ушли назад, считаем паузу истёкшей, чтобы не заблокироваться навсегда.
        if (!fired || nowMs < lastMs || nowMs - lastMs >= cooldownMs) {
            fired = true;
            lastMs = nowMs;
            return true;
        }
        return false;
    }
}
