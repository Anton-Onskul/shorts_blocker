package com.shortsblock.app.core;

/** Узел дерева экрана. В Android — обёртка над AccessibilityNodeInfo, в тестах — фейк. */
public interface ScreenNode {
    /** Полный id вида "pkg:id/name" или null. */
    String viewId();
    String className();
    CharSequence text();
    CharSequence contentDescription();
    boolean isVisible();
    boolean isSelected();
    int childCount();
    /** Может вернуть null (узел успел исчезнуть). */
    ScreenNode child(int index);
}
