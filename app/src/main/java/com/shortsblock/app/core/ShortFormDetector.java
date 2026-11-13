package com.shortsblock.app.core;

import java.util.Arrays;
import java.util.Collections;
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

    /** id элементов, которые есть только в плеере Shorts (не в ленте и не в обычном плеере). */
    static final List<String> YOUTUBE_SHORTS_IDS = Collections.unmodifiableList(Arrays.asList(
            "reel_recycler",
            "reel_player_page_container",
            "reel_watch_player"
    ));

    /** id элементов просмотрщика Reels в Instagram (не кнопки вкладки Reels в меню). */
    static final List<String> INSTAGRAM_REELS_IDS = Collections.unmodifiableList(Arrays.asList(
            "clips_viewer_view_pager",
            "clips_viewer_container"
    ));

    /** Максимум узлов за один обход. */
    public static final int MAX_NODES = 3000;

    /** ЗАГЛУШКА — реализация на шаге 4. */
    public Decision decide(String packageName, ScreenNode root) {
        throw new UnsupportedOperationException("не реализовано");
    }

    /** true, если приложение относится к отслеживаемым (YouTube, Instagram, TikTok). */
    public static boolean isWatchedPackage(String packageName) {
        return packageName != null && (TIKTOK_PACKAGES.contains(packageName)
                || YOUTUBE_PACKAGES.contains(packageName)
                || INSTAGRAM.equals(packageName));
    }
}
