package com.shortsblock.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Хранение переключателя «Блокировка включена». */
final class Prefs {
    private static final String FILE = "settings";
    private static final String KEY_ENABLED = "enabled";

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
}
