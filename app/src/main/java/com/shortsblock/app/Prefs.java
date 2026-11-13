package com.shortsblock.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Настройки и «пульс» службы. */
final class Prefs {
    private static final String FILE = "settings";
    private static final String KEY_ENABLED = "enabled";
    private static final String KEY_LAST_EVENT = "last_event_at";
    private static final String KEY_LAST_BLOCK = "last_block_at";
    private static final String KEY_LAST_REASON = "last_block_reason";
    private static final String KEY_DIAG_UNTIL = "diag_until";

    private Prefs() { }

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }

    static boolean isEnabled(Context c) {
        return sp(c).getBoolean(KEY_ENABLED, true);
    }

    static void setEnabled(Context c, boolean enabled) {
        sp(c).edit().putBoolean(KEY_ENABLED, enabled).apply();
    }

    static long lastEventAt(Context c) {
        return sp(c).getLong(KEY_LAST_EVENT, 0);
    }

    static void setLastEventAt(Context c, long t) {
        sp(c).edit().putLong(KEY_LAST_EVENT, t).apply();
    }

    static long lastBlockAt(Context c) {
        return sp(c).getLong(KEY_LAST_BLOCK, 0);
    }

    static String lastBlockReason(Context c) {
        return sp(c).getString(KEY_LAST_REASON, "");
    }

    static void setLastBlock(Context c, long t, String reason) {
        sp(c).edit().putLong(KEY_LAST_BLOCK, t).putString(KEY_LAST_REASON, reason).apply();
    }

    static long diagUntil(Context c) {
        return sp(c).getLong(KEY_DIAG_UNTIL, 0);
    }

    static void setDiagUntil(Context c, long t) {
        sp(c).edit().putLong(KEY_DIAG_UNTIL, t).apply();
    }
}
