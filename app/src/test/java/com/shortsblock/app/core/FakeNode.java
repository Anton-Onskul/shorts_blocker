package com.shortsblock.app.core;

import java.util.ArrayList;
import java.util.List;

/** Фейковый узел экрана для тестов. */
final class FakeNode implements ScreenNode {
    String id;
    String cls = "android.view.View";
    CharSequence text;
    CharSequence desc;
    boolean visible = true;
    boolean selected;
    final List<ScreenNode> children = new ArrayList<>();
    /** Сколько раз узел опрашивали — чтобы проверять лимит обхода. */
    static int touches;

    static FakeNode node() {
        return new FakeNode();
    }

    /** Узел с полным id "pkg:id/name". */
    static FakeNode id(String pkg, String name) {
        FakeNode n = new FakeNode();
        n.id = pkg + ":id/" + name;
        return n;
    }

    static FakeNode root(ScreenNode... kids) {
        return node().add(kids);
    }

    FakeNode add(ScreenNode... kids) {
        for (ScreenNode k : kids) {
            children.add(k);
        }
        return this;
    }

    FakeNode text(String t) { text = t; return this; }
    FakeNode desc(String d) { desc = d; return this; }
    FakeNode cls(String c) { cls = c; return this; }
    FakeNode hidden() { visible = false; return this; }
    FakeNode selected() { selected = true; return this; }

    @Override public String viewId() { touches++; return id; }
    @Override public String className() { return cls; }
    @Override public CharSequence text() { return text; }
    @Override public CharSequence contentDescription() { return desc; }
    @Override public boolean isVisible() { return visible; }
    @Override public boolean isSelected() { return selected; }
    @Override public int childCount() { return children.size(); }
    @Override public ScreenNode child(int i) { return children.get(i); }

    /** Узел, который падает при любом обращении (проверяем, что дерево зря не обходят). */
    static final ScreenNode BOMB = new ScreenNode() {
        private AssertionError boom() { return new AssertionError("дерево не должно обходиться"); }
        @Override public String viewId() { throw boom(); }
        @Override public String className() { throw boom(); }
        @Override public CharSequence text() { throw boom(); }
        @Override public CharSequence contentDescription() { throw boom(); }
        @Override public boolean isVisible() { throw boom(); }
        @Override public boolean isSelected() { throw boom(); }
        @Override public int childCount() { throw boom(); }
        @Override public ScreenNode child(int i) { throw boom(); }
    };
}
