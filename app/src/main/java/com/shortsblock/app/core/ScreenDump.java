package com.shortsblock.app.core;

import java.util.ArrayDeque;
import java.util.Deque;

/** Текстовый дамп дерева экрана для диагностики: строка на узел, отступ по глубине. */
public final class ScreenDump {
    static final int MAX_TEXT = 40;

    private ScreenDump() { }

    private static final class Item {
        final ScreenNode node;
        final int depth;

        Item(ScreenNode node, int depth) {
            this.node = node;
            this.depth = depth;
        }
    }

    public static String dump(String packageName, ScreenNode root, int maxNodes) {
        StringBuilder sb = new StringBuilder("== ").append(packageName);
        if (root == null) {
            return sb.append("\n(нет окна)").toString();
        }
        Deque<Item> stack = new ArrayDeque<>();
        stack.push(new Item(root, 0));
        int count = 0;
        while (!stack.isEmpty()) {
            if (count >= maxNodes) {
                sb.append("\n… обрезано");
                break;
            }
            Item it = stack.pop();
            ScreenNode n = it.node;
            count++;
            sb.append('\n');
            for (int i = 0; i < it.depth; i++) {
                sb.append("  ");
            }
            sb.append(shortClass(n.className()));
            String id = n.viewId();
            if (id != null) {
                sb.append(" id=").append(id);
            }
            appendQuoted(sb, " desc=", n.contentDescription());
            appendQuoted(sb, " text=", n.text());
            if (n.isSelected()) {
                sb.append(" [sel]");
            }
            if (!n.isVisible()) {
                sb.append(" [hidden]");
            }
            for (int i = n.childCount() - 1; i >= 0; i--) {
                ScreenNode c = n.child(i);
                if (c != null) {
                    stack.push(new Item(c, it.depth + 1));
                }
            }
        }
        return sb.toString();
    }

    private static String shortClass(String cls) {
        if (cls == null || cls.isEmpty()) {
            return "?";
        }
        return cls.substring(cls.lastIndexOf('.') + 1);
    }

    private static void appendQuoted(StringBuilder sb, String key, CharSequence cs) {
        if (cs == null || cs.length() == 0) {
            return;
        }
        String s = cs.toString();
        boolean cut = s.length() > MAX_TEXT;
        if (cut) {
            s = s.substring(0, MAX_TEXT);
        }
        s = s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
        sb.append(key).append('"').append(s).append(cut ? "…" : "").append('"');
    }
}
