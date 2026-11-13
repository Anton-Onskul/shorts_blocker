package com.shortsblock.app.core;

/**
 * Что сделать, когда на экране обнаружено короткое видео.
 *
 * Сделано классом с константами, а не enum: javac из JDK 21 добавляет в enum атрибут
 * MethodParameters без имени, на котором падает d8 из build-tools 34.
 */
public final class Action {
    /** Ничего не делать. */
    public static final Action NONE = new Action("NONE");
    /** Нажать «Назад» — закрыть плеер Shorts/Reels, оставив приложение открытым. */
    public static final Action BACK = new Action("BACK");
    /** Перейти на домашний экран — используется для TikTok целиком. */
    public static final Action HOME = new Action("HOME");

    private final String name;

    private Action(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
