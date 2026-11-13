package com.shortsblock.app.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Выбирает окна, которые нужно проверить: окна приложений YouTube/Instagram/TikTok,
 * сначала активное или в фокусе. Системная панель, клавиатура и прочие окна пропускаются —
 * иначе, пока они сверху, служба не видит YouTube под ними.
 */
public final class WindowPicker {
    private WindowPicker() { }

    public static List<WindowInfo> pick(List<WindowInfo> windows) {
        if (windows == null) {
            return Collections.emptyList();
        }
        List<WindowInfo> first = new ArrayList<>();
        List<WindowInfo> rest = new ArrayList<>();
        for (WindowInfo w : windows) {
            if (w == null || w.type != WindowInfo.TYPE_APPLICATION
                    || !ShortFormDetector.isWatchedPackage(w.packageName)) {
                continue;
            }
            (w.active || w.focused ? first : rest).add(w);
        }
        first.addAll(rest);
        return first;
    }
}
