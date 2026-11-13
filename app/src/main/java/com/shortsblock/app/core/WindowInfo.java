package com.shortsblock.app.core;

/** Сведения об окне на экране (из AccessibilityWindowInfo). */
public final class WindowInfo {
    public static final int TYPE_APPLICATION = 1;
    public static final int TYPE_INPUT_METHOD = 2;
    public static final int TYPE_SYSTEM = 3;

    public final String packageName;
    public final int type;
    public final boolean active;
    public final boolean focused;

    public WindowInfo(String packageName, int type, boolean active, boolean focused) {
        this.packageName = packageName;
        this.type = type;
        this.active = active;
        this.focused = focused;
    }

    @Override
    public String toString() {
        return packageName + "/" + type + (active ? "/active" : "") + (focused ? "/focused" : "");
    }
}
