package com.shortsblock.app.core;

/** Решение детектора: что сделать и какой признак сработал. */
public final class Decision {
    public static final Decision NONE = new Decision(Action.NONE, "");

    public final Action action;
    /** Например "id:reel_recycler" или "tab:Shorts"; пусто для NONE. */
    public final String reason;

    public Decision(Action action, String reason) {
        this.action = action;
        this.reason = reason;
    }

    @Override
    public String toString() {
        return action + (reason.isEmpty() ? "" : "(" + reason + ")");
    }
}
