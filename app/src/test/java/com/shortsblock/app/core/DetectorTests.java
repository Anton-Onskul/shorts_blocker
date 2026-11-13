package com.shortsblock.app.core;

import static com.shortsblock.app.core.FakeNode.id;
import static com.shortsblock.app.core.FakeNode.node;
import static com.shortsblock.app.core.FakeNode.root;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Юнит-тесты логики (обычная JVM, без Android и JUnit): ./run_tests.sh
 * Код возврата 0 — всё прошло, 1 — есть провалы (строки FAIL с причиной).
 */
public final class DetectorTests {

    private static int passed = 0;
    private static int failed = 0;

    private static final String YT = "com.google.android.youtube";
    private static final String RV = "app.revanced.android.youtube";
    private static final String IG = "com.instagram.android";
    private static final String ME = "com.shortsblock.app";
    private static final ShortFormDetector D = new ShortFormDetector();

    // ---------- мини-фреймворк ----------
    private static void check(String name, Object expected, Object actual) {
        if (expected == null ? actual == null : expected.equals(actual)) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name + " — ожидалось <" + expected + ">, получено <" + actual + ">");
        }
    }

    private static void checkTrue(String name, boolean cond, String details) {
        if (cond) {
            passed++;
            System.out.println("PASS  " + name);
        } else {
            failed++;
            System.out.println("FAIL  " + name + " — " + details);
        }
    }

    private static void run(String name, Runnable test) {
        try {
            test.run();
        } catch (Throwable t) {
            failed++;
            System.out.println("FAIL  " + name + " — исключение: " + t);
        }
    }

    private static Action act(String pkg, ScreenNode root) {
        return D.decide(pkg, root).action;
    }

    private static FakeNode bareId(String raw) {
        FakeNode n = node();
        n.id = raw;
        return n;
    }

    /** Типичный экран YouTube: вкладки внизу, выбрана «Главная». */
    private static FakeNode ytNav(boolean shortsSelected) {
        FakeNode home = node().desc("Главная");
        FakeNode shorts = node().desc("Shorts");
        if (shortsSelected) {
            shorts.selected();
        } else {
            home.selected();
        }
        return id(YT, "pivot_bar").add(home, shorts, node().desc("Подписки"));
    }

    public static void main(String[] args) {
        detectorBasics();
        tiktok();
        youtubeIds();
        youtubeNegatives();
        youtubeTabFallback();
        instagram();
        traversalLimits();
        watchedPackages();
        throttle();
        windowPicker();
        serviceHealth();
        ago();
        screenDump();
        dumpLog();

        System.out.println();
        System.out.println("Итого: прошло " + passed + ", провалено " + failed);
        System.exit(failed == 0 ? 0 : 1);
    }

    // ---------- 1. Детектор: пустой ввод и чужие приложения ----------
    private static void detectorBasics() {
        run("null-пакет", () -> check("null-пакет -> NONE", Action.NONE, act(null, FakeNode.BOMB)));
        run("чужое приложение", () -> check("Chrome -> NONE, дерево не обходится", Action.NONE,
                act("com.android.chrome", FakeNode.BOMB)));
        run("null-экран", () -> check("YouTube без дерева -> NONE", Action.NONE, act(YT, null)));
        run("Decision.NONE без причины", () -> check("причина NONE пустая", "",
                D.decide("com.android.chrome", FakeNode.BOMB).reason));
    }

    // ---------- 2. TikTok целиком ----------
    private static void tiktok() {
        for (String p : new String[]{"com.zhiliaoapp.musically", "com.ss.android.ugc.trill",
                "com.ss.android.ugc.aweme", "com.zhiliaoapp.musically.go"}) {
            run("TikTok " + p, () -> check("TikTok " + p + " -> HOME без обхода дерева", Action.HOME,
                    act(p, FakeNode.BOMB)));
        }
    }

    // ---------- 3. YouTube: плеер Shorts по id ----------
    private static void youtubeIds() {
        String[] playerIds = {
                "reel_recycler",                // старые id из v1.0
                "reel_player_page_container",
                "reel_watch_player",
                "reel_player_overlay_v2",       // семейство reel_player*
                "reel_watch_fragment_root",     // семейство reel_watch*
                "shorts_player_container",      // семейство shorts_player*
                "shorts_container"
        };
        for (String n : playerIds) {
            run("YT " + n, () -> check("YouTube, плеер Shorts (" + n + ") -> BACK", Action.BACK,
                    act(YT, root(ytNav(false), node().add(id(YT, n))))));
        }
        run("YT причина", () -> checkTrue("причина содержит сработавший id",
                D.decide(YT, root(id(YT, "reel_recycler"))).reason.contains("reel_recycler"),
                "reason=" + D.decide(YT, root(id(YT, "reel_recycler"))).reason));
        run("YT глубоко в дереве", () -> {
            FakeNode cur = root();
            FakeNode top = cur;
            for (int i = 0; i < 60; i++) {
                FakeNode next = node();
                cur.add(next);
                cur = next;
            }
            cur.add(id(YT, "reel_recycler"));
            check("плеер на глубине 60 -> BACK", Action.BACK, act(YT, top));
        });
        run("YT null-ребёнок", () -> check("null среди детей пропускается, плеер найден", Action.BACK,
                act(YT, root(null, id(YT, "reel_recycler")))));
        run("ReVanced с префиксом YouTube", () -> check("ReVanced, id YouTube -> BACK", Action.BACK,
                act(RV, root(id(YT, "reel_recycler")))));
        run("ReVanced со своим префиксом", () -> check("ReVanced, свой id -> BACK", Action.BACK,
                act(RV, root(id(RV, "reel_watch_player")))));
    }

    private static void youtubeNegatives() {
        run("YT обычное видео", () -> check("обычный плеер (watch_player) -> NONE", Action.NONE,
                act(YT, root(ytNav(false), id(YT, "watch_player"), id(YT, "player_view")))));
        run("YT полка Shorts", () -> check("полка Shorts в ленте (reel_shelf*) -> NONE", Action.NONE,
                act(YT, root(ytNav(false), id(YT, "reel_shelf"), id(YT, "reel_shelf_header")))));
        run("YT миниатюры Shorts", () -> check("миниатюры Shorts в поиске (reel_item*) -> NONE", Action.NONE,
                act(YT, root(id(YT, "reel_item_thumbnail")))));
        run("YT скрытый плеер", () -> check("невидимый плеер -> NONE", Action.NONE,
                act(YT, root(id(YT, "reel_recycler").hidden()))));
        run("YT под скрытым родителем", () -> check("плеер под невидимым родителем -> NONE", Action.NONE,
                act(YT, root(node().hidden().add(id(YT, "reel_recycler"))))));
        run("YT id Instagram", () -> check("id Instagram в YouTube -> NONE", Action.NONE,
                act(YT, root(id(IG, "clips_viewer_view_pager")))));
        run("YT чужой префикс", () -> check("reel_recycler с префиксом другого приложения -> NONE", Action.NONE,
                act(YT, root(id("com.example", "reel_recycler")))));
        run("YT id без :id/", () -> check("id без префикса (просто reel_recycler) -> NONE", Action.NONE,
                act(YT, root(bareId("reel_recycler")))));
    }

    // ---------- 4. YouTube: резервный признак — выбрана вкладка Shorts ----------
    private static void youtubeTabFallback() {
        run("YT вкладка Shorts", () -> {
            Decision d = D.decide(YT, root(ytNav(true)));
            check("выбрана вкладка Shorts (desc) -> BACK", Action.BACK, d.action);
            checkTrue("причина начинается с tab:", d.reason.startsWith("tab:"), "reason=" + d.reason);
        });
        run("YT вкладка Shorts текстом", () -> check("выбран узел с текстом \" shorts \" -> BACK", Action.BACK,
                act(YT, root(node().text(" shorts ").selected()))));
        run("YT вкладка не выбрана", () -> check("вкладка Shorts есть, но не выбрана -> NONE", Action.NONE,
                act(YT, root(ytNav(false)))));
        run("YT слово в названии видео", () -> check("выбран узел «Лучшие Shorts 2026» -> NONE", Action.NONE,
                act(YT, root(node().text("Лучшие Shorts 2026").selected()))));
        run("YT скрытая выбранная вкладка", () -> check("выбранная, но невидимая Shorts -> NONE", Action.NONE,
                act(YT, root(node().desc("Shorts").selected().hidden()))));
    }

    // ---------- 5. Instagram ----------
    private static void instagram() {
        for (String n : new String[]{"clips_viewer_view_pager", "clips_viewer_container", "clips_viewer_fragment_v3"}) {
            run("IG " + n, () -> check("Instagram, просмотр Reels (" + n + ") -> BACK", Action.BACK,
                    act(IG, root(id(IG, "feed_tab"), node().add(id(IG, n))))));
        }
        run("IG кнопка Reels", () -> check("кнопка Reels в меню (clips_tab) -> NONE", Action.NONE,
                act(IG, root(id(IG, "clips_tab").desc("Reels"), id(IG, "feed_tab").selected()))));
        run("IG выбрана вкладка Reels", () -> check("выбрана вкладка Reels -> BACK", Action.BACK,
                act(IG, root(id(IG, "clips_tab").desc("Reels").selected()))));
        run("IG лента и сторис", () -> check("лента/сторис -> NONE", Action.NONE,
                act(IG, root(id(IG, "reel_viewer_root"), id(IG, "row_feed_photo_imageview")))));
        run("IG id YouTube", () -> check("id YouTube в Instagram -> NONE", Action.NONE,
                act(IG, root(id(YT, "reel_recycler")))));
        run("IG скрытый просмотрщик", () -> check("невидимый clips_viewer -> NONE", Action.NONE,
                act(IG, root(id(IG, "clips_viewer_view_pager").hidden()))));
    }

    // ---------- 6. Лимиты обхода ----------
    private static void traversalLimits() {
        run("лимит узлов", () -> {
            FakeNode r = root();
            for (int i = 0; i < ShortFormDetector.MAX_NODES + 500; i++) {
                r.add(node());
            }
            FakeNode.touches = 0;
            check("большое дерево без плеера -> NONE", Action.NONE, act(YT, r));
            checkTrue("обойдено не больше MAX_NODES узлов", FakeNode.touches <= ShortFormDetector.MAX_NODES,
                    "touches=" + FakeNode.touches);
        });
        run("глубокая цепочка", () -> {
            FakeNode top = root();
            FakeNode cur = top;
            for (int i = 0; i < 20_000; i++) {
                FakeNode next = node();
                cur.add(next);
                cur = next;
            }
            check("цепочка 20000 узлов: без StackOverflow, NONE", Action.NONE, act(YT, top));
        });
    }

    // ---------- 7. Отслеживаемые пакеты ----------
    private static void watchedPackages() {
        run("isWatchedPackage", () -> {
            check("YouTube отслеживается", true, ShortFormDetector.isWatchedPackage(YT));
            check("ReVanced отслеживается", true, ShortFormDetector.isWatchedPackage(RV));
            check("Instagram отслеживается", true, ShortFormDetector.isWatchedPackage(IG));
            check("TikTok отслеживается", true, ShortFormDetector.isWatchedPackage("com.zhiliaoapp.musically"));
            check("Telegram не отслеживается", false, ShortFormDetector.isWatchedPackage("org.telegram.messenger"));
            check("null не отслеживается", false, ShortFormDetector.isWatchedPackage(null));
            check("YouTube Music не отслеживается", false,
                    ShortFormDetector.isWatchedPackage("com.google.android.apps.youtube.music"));
        });
    }

    // ---------- 8. Throttle (из v1.0) ----------
    private static void throttle() {
        run("throttle", () -> {
            Throttle t = new Throttle(800);
            check("throttle: первое действие разрешено", true, t.tryAcquire(10_000));
            check("throttle: через 100 мс запрещено", false, t.tryAcquire(10_100));
            check("throttle: через 799 мс запрещено", false, t.tryAcquire(10_799));
            check("throttle: через 800 мс разрешено", true, t.tryAcquire(10_800));
            check("throttle: сразу после — снова запрещено", false, t.tryAcquire(10_801));
        });
        run("throttle назад во времени", () -> {
            Throttle t = new Throttle(800);
            t.tryAcquire(50_000);
            check("throttle: время ушло назад -> разрешено", true, t.tryAcquire(1_000));
        });
    }

    // ---------- 9. Выбор окна ----------
    private static WindowInfo app(String pkg, boolean active) {
        return new WindowInfo(pkg, WindowInfo.TYPE_APPLICATION, active, active);
    }

    private static void windowPicker() {
        WindowInfo sysui = new WindowInfo("com.android.systemui", WindowInfo.TYPE_SYSTEM, true, false);
        WindowInfo ime = new WindowInfo("com.google.android.inputmethod.latin", WindowInfo.TYPE_INPUT_METHOD, true, true);
        WindowInfo yt = app(YT, false);
        WindowInfo ytActive = app(YT, true);
        WindowInfo ig = app(IG, false);

        run("окно: системная панель сверху", () -> check("активна systemui, под ней YouTube -> [YouTube]",
                Collections.singletonList(yt), WindowPicker.pick(Arrays.asList(sysui, yt))));
        run("окно: клавиатура", () -> check("клавиатура в фокусе, Instagram -> [Instagram]",
                Collections.singletonList(ig), WindowPicker.pick(Arrays.asList(ime, ig))));
        run("окно: порядок", () -> check("активное окно первым",
                Arrays.asList(ytActive, ig), WindowPicker.pick(Arrays.asList(ig, ytActive))));
        run("окно: своё приложение", () -> check("окно Shorts Blocker пропускается",
                Collections.singletonList(yt), WindowPicker.pick(Arrays.asList(app(ME, true), yt))));
        run("окно: чужие приложения", () -> check("только Chrome -> []",
                Collections.emptyList(), WindowPicker.pick(Collections.singletonList(app("com.android.chrome", true)))));
        run("окно: системный тип у YouTube", () -> check("окно YouTube не типа APPLICATION -> []",
                Collections.emptyList(),
                WindowPicker.pick(Collections.singletonList(new WindowInfo(YT, WindowInfo.TYPE_SYSTEM, true, true)))));
        run("окно: пусто", () -> check("пустой список -> []", Collections.emptyList(),
                WindowPicker.pick(new ArrayList<WindowInfo>())));
        run("окно: null", () -> check("null -> []", Collections.emptyList(), WindowPicker.pick(null)));
        run("окно: null-элемент", () -> check("null в списке пропускается", Collections.singletonList(yt),
                WindowPicker.pick(Arrays.asList(null, yt))));
    }

    // ---------- 10. Реальное состояние службы ----------
    private static void serviceHealth() {
        long now = 1_000_000;
        run("health OFF", () -> check("не включена в настройках -> OFF (важнее паузы)", ServiceHealth.OFF,
                ServiceHealth.evaluate(false, false, now - 10, now)));
        run("health PAUSED", () -> check("включена, переключатель выкл -> PAUSED", ServiceHealth.PAUSED,
                ServiceHealth.evaluate(true, false, now - 10, now)));
        run("health NOT_RUNNING", () -> check("включена, но ни разу не подключилась -> NOT_RUNNING",
                ServiceHealth.NOT_RUNNING, ServiceHealth.evaluate(true, true, 0, now)));
        run("health OK", () -> check("включена и подключилась -> OK", ServiceHealth.OK,
                ServiceHealth.evaluate(true, true, now - 60_000, now)));
        run("health часы назад", () -> check("connectedAt в будущем (смена времени) -> OK", ServiceHealth.OK,
                ServiceHealth.evaluate(true, true, now + 5_000, now)));
    }

    // ---------- 11. «… назад» ----------
    private static void ago() {
        long now = 10_000_000_000L;
        run("ago никогда", () -> check("0 -> никогда", "никогда", Ago.format(0, now)));
        run("ago только что", () -> check("0,5 с -> только что", "только что", Ago.format(now - 500, now)));
        run("ago секунды", () -> check("12 с", "12 с назад", Ago.format(now - 12_000, now)));
        run("ago минуты", () -> check("5 мин", "5 мин назад", Ago.format(now - 5 * 60_000 - 30_000, now)));
        run("ago часы", () -> check("3 ч", "3 ч назад", Ago.format(now - 3 * 3_600_000L - 1, now)));
        run("ago дни", () -> check("2 дн", "2 дн назад", Ago.format(now - 2 * 86_400_000L - 1, now)));
        run("ago будущее", () -> check("время в будущем -> только что", "только что", Ago.format(now + 60_000, now)));
    }

    // ---------- 12. Дамп экрана ----------
    private static void screenDump() {
        FakeNode tree = root(
                id(YT, "pivot_bar").cls("android.widget.FrameLayout").add(
                        node().cls("android.widget.Button").desc("Shorts").selected()),
                node().cls("android.widget.TextView").text("строка1\nстрока2"),
                node().cls("android.widget.TextView").text("0123456789012345678901234567890123456789ДЛИННО"),
                id(YT, "reel_recycler").hidden(),
                null);
        tree.cls("android.widget.FrameLayout");

        run("dump шапка", () -> {
            String s = ScreenDump.dump(YT, tree, 100);
            check("первая строка — пакет", "== " + YT, s.split("\n", -1)[0]);
        });
        run("dump строка узла", () -> {
            String s = ScreenDump.dump(YT, tree, 100);
            checkTrue("узел с id и отступом 2 пробела",
                    s.contains("\n  FrameLayout id=" + YT + ":id/pivot_bar\n"), s);
            checkTrue("ребёнок: отступ 4, desc и [sel]",
                    s.contains("\n    Button desc=\"Shorts\" [sel]\n"), s);
            checkTrue("скрытый узел помечен [hidden]",
                    s.contains("  View id=" + YT + ":id/reel_recycler [hidden]"), s);
        });
        run("dump экранирование", () -> {
            String s = ScreenDump.dump(YT, tree, 100);
            checkTrue("перевод строки в тексте экранирован", s.contains("text=\"строка1\\nстрока2\""), s);
        });
        run("dump обрезка текста", () -> {
            String s = ScreenDump.dump(YT, tree, 100);
            checkTrue("текст обрезан до 40 символов + …",
                    s.contains("text=\"0123456789012345678901234567890123456789…\""), s);
        });
        run("dump лимит узлов", () -> {
            String s = ScreenDump.dump(YT, tree, 2);
            String[] lines = s.split("\n");
            check("шапка + 2 узла + отметка обрезки", 4, lines.length);
            check("последняя строка — обрезано", "… обрезано", lines[lines.length - 1]);
        });
        run("dump null", () -> check("null-дерево", "== " + YT + "\n(нет окна)", ScreenDump.dump(YT, null, 10)));
        run("dump без StackOverflow", () -> {
            FakeNode top = root();
            FakeNode cur = top;
            for (int i = 0; i < 20_000; i++) {
                FakeNode next = node();
                cur.add(next);
                cur = next;
            }
            checkTrue("глубокое дерево дампится с лимитом", ScreenDump.dump(YT, top, 50).endsWith("… обрезано"),
                    "нет отметки обрезки");
        });
    }

    // ---------- 13. Журнал дампов ----------
    private static void dumpLog() {
        run("log дубликат подряд", () -> {
            DumpLog log = new DumpLog(5, 10_000);
            check("первая запись добавлена", true, log.add("A"));
            check("тот же экран подряд не дублируется", false, log.add("A"));
            check("другой экран добавлен", true, log.add("B"));
            check("A снова (не подряд) добавлен", true, log.add("A"));
            check("записи по порядку", Arrays.asList("A", "B", "A"), log.entries());
        });
        run("log ёмкость", () -> {
            DumpLog log = new DumpLog(3, 10_000);
            for (String s : new String[]{"1", "2", "3", "4", "5"}) {
                log.add(s);
            }
            check("хранятся последние 3", Arrays.asList("3", "4", "5"), log.entries());
        });
        run("log лимит символов", () -> {
            DumpLog log = new DumpLog(10, 10);
            log.add("aaaa");
            log.add("bbbb");
            log.add("cccc");
            check("старые вытеснены по размеру", Arrays.asList("bbbb", "cccc"), log.entries());
        });
        run("log слишком большая запись", () -> {
            DumpLog log = new DumpLog(10, 5);
            log.add("0123456789");
            check("одна огромная запись обрезается до лимита", Collections.singletonList("01234"), log.entries());
        });
        run("log отчёт", () -> {
            DumpLog log = new DumpLog(5, 1000);
            log.add("A");
            log.add("B");
            check("шапка и разделители", "HDR\n-----\nA\n-----\nB", log.report("HDR"));
        });
        run("log пустой отчёт", () -> check("пустой журнал", "HDR\n(экранов не записано)",
                new DumpLog(5, 1000).report("HDR")));
        run("log неверные лимиты", () -> {
            boolean thrown = false;
            try {
                new DumpLog(0, 10);
            } catch (IllegalArgumentException e) {
                thrown = true;
            }
            check("maxEntries=0 -> IllegalArgumentException", true, thrown);
        });
    }
}
