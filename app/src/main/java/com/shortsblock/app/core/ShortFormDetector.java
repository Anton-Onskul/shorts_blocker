package com.shortsblock.app.core;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Решает, нужно ли блокировать текущий экран.
 *
 * TikTok блокируется целиком. В YouTube и Instagram блокируется только полноэкранный
 * просмотр коротких видео (плеер Shorts / просмотрщик Reels); остальное приложение работает.
 *
 * Плеер определяется по id элементов экрана. Эти id задают разработчики YouTube/Instagram
 * и могут меняться в новых версиях — тогда список ниже нужно обновить.
 */
public final class ShortFormDetector {

    /** Пакеты TikTok (глобальный, азиатский, китайский Douyin, Lite). */
    static final Set<String> TIKTOK_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            "com.zhiliaoapp.musically",
            "com.ss.android.ugc.trill",
            "com.ss.android.ugc.aweme",
            "com.zhiliaoapp.musically.go"
    )));

    static final String YOUTUBE = "com.google.android.youtube";
    static final String INSTAGRAM = "com.instagram.android";

    /** Пакеты YouTube, включая модифицированные клиенты. */
    static final Set<String> YOUTUBE_PACKAGES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            YOUTUBE,
            "app.revanced.android.youtube"
    )));

    /**
     * Семейства id плеера Shorts: точное имя или префикс (reel_player_overlay_v2 → reel_player).
     * Полка и миниатюры в ленте (reel_shelf*, reel_item*) сюда намеренно не входят.
     */
    static final List<String> YOUTUBE_SHORTS_ID_FAMILIES = Collections.unmodifiableList(Arrays.asList(
            "reel_recycler",
            "reel_player",
            "reel_watch",
            "shorts_player",
            "shorts_container"
    ));

    /** Семейство id просмотрщика Reels в Instagram (не кнопка вкладки clips_tab). */
    static final List<String> INSTAGRAM_REELS_ID_FAMILIES = Collections.unmodifiableList(Arrays.asList(
            "clips_viewer"
    ));

    /** Подписи вкладок: если такая вкладка выбрана — пользователь в коротких видео. */
    static final String YOUTUBE_TAB = "Shorts";
    static final String INSTAGRAM_TAB = "Reels";

    /** Максимум узлов за один обход. */
    public static final int MAX_NODES = 3000;

    public Decision decide(String packageName, ScreenNode root) {
        if (packageName == null) {
            return Decision.NONE;
        }
        if (TIKTOK_PACKAGES.contains(packageName)) {
            return new Decision(Action.HOME, "app:tiktok");
        }
        if (root == null) {
            return Decision.NONE;
        }
        if (YOUTUBE_PACKAGES.contains(packageName)) {
            // Модифицированные клиенты часто сохраняют префикс ресурсов оригинального YouTube.
            return scan(root, new String[]{packageName + ":id/", YOUTUBE + ":id/"},
                    YOUTUBE_SHORTS_ID_FAMILIES, YOUTUBE_TAB);
        }
        if (INSTAGRAM.equals(packageName)) {
            return scan(root, new String[]{INSTAGRAM + ":id/"}, INSTAGRAM_REELS_ID_FAMILIES, INSTAGRAM_TAB);
        }
        return Decision.NONE;
    }

    /** Итеративный обход видимой части дерева (без рекурсии — дерево бывает очень глубоким). */
    private static Decision scan(ScreenNode root, String[] idPrefixes, List<String> families, String tabLabel) {
        Deque<ScreenNode> stack = new ArrayDeque<>();
        stack.push(root);
        int visited = 0;
        while (!stack.isEmpty() && visited < MAX_NODES) {
            ScreenNode n = stack.pop();
            if (!n.isVisible()) {
                continue; // невидимый узел — пропускаем вместе с поддеревом
            }
            visited++;
            String name = idName(n.viewId(), idPrefixes);
            if (name != null && inFamily(name, families)) {
                return new Decision(Action.BACK, "id:" + name);
            }
            if (n.isSelected() && (isLabel(n.contentDescription(), tabLabel) || isLabel(n.text(), tabLabel))) {
                return new Decision(Action.BACK, "tab:" + tabLabel);
            }
            for (int i = n.childCount() - 1; i >= 0; i--) {
                ScreenNode c = n.child(i);
                if (c != null) {
                    stack.push(c);
                }
            }
        }
        return Decision.NONE;
    }

    private static String idName(String fullId, String[] prefixes) {
        if (fullId == null) {
            return null;
        }
        for (String p : prefixes) {
            if (fullId.startsWith(p)) {
                return fullId.substring(p.length());
            }
        }
        return null;
    }

    private static boolean inFamily(String name, List<String> families) {
        for (String f : families) {
            if (name.startsWith(f)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLabel(CharSequence cs, String label) {
        return cs != null && cs.toString().trim().equalsIgnoreCase(label);
    }

    /** true, если приложение относится к отслеживаемым (YouTube, Instagram, TikTok). */
    public static boolean isWatchedPackage(String packageName) {
        return packageName != null && (TIKTOK_PACKAGES.contains(packageName)
                || YOUTUBE_PACKAGES.contains(packageName)
                || INSTAGRAM.equals(packageName));
    }
}
