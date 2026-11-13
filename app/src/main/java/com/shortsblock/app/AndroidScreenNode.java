package com.shortsblock.app;

import android.view.accessibility.AccessibilityNodeInfo;

import com.shortsblock.app.core.ScreenNode;

/** Адаптер AccessibilityNodeInfo → ScreenNode, чтобы логика из core работала с реальным экраном. */
final class AndroidScreenNode implements ScreenNode {
    private final AccessibilityNodeInfo n;

    AndroidScreenNode(AccessibilityNodeInfo n) {
        this.n = n;
    }

    @Override public String viewId() { return n.getViewIdResourceName(); }
    @Override public String className() {
        CharSequence c = n.getClassName();
        return c == null ? null : c.toString();
    }
    @Override public CharSequence text() { return n.getText(); }
    @Override public CharSequence contentDescription() { return n.getContentDescription(); }
    @Override public boolean isVisible() { return n.isVisibleToUser(); }
    @Override public boolean isSelected() { return n.isSelected(); }
    @Override public int childCount() { return n.getChildCount(); }

    @Override
    public ScreenNode child(int index) {
        try {
            AccessibilityNodeInfo c = n.getChild(index);
            return c == null ? null : new AndroidScreenNode(c);
        } catch (RuntimeException e) {
            return null; // узел успел исчезнуть
        }
    }
}
