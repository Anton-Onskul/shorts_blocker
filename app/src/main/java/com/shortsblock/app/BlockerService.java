package com.shortsblock.app;

import android.accessibilityservice.AccessibilityService;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.Toast;

import com.shortsblock.app.core.Action;
import com.shortsblock.app.core.Decision;
import com.shortsblock.app.core.ScreenDump;
import com.shortsblock.app.core.ShortFormDetector;
import com.shortsblock.app.core.Throttle;
import com.shortsblock.app.core.WindowInfo;
import com.shortsblock.app.core.WindowPicker;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Служба специальных возможностей: следит за окнами YouTube, Instagram и TikTok
 * и закрывает короткие видео. Решение принимает core/ShortFormDetector.
 */
public final class BlockerService extends AccessibilityService {

    private static final long COOLDOWN_MS = 800;
    private static final long TOAST_COOLDOWN_MS = 4000;
    /** События приходят пачками — проверяем экран не чаще раза в CHECK_DELAY_MS, но всегда после последнего. */
    private static final long CHECK_DELAY_MS = 250;
    private static final int DUMP_MAX_NODES = 1500;

    /** Время подключения службы в этом процессе (0 — не подключена). Читает MainActivity. */
    static volatile long connectedAt;

    private final ShortFormDetector detector = new ShortFormDetector();
    private final Throttle actionThrottle = new Throttle(COOLDOWN_MS);
    private final Throttle toastThrottle = new Throttle(TOAST_COOLDOWN_MS);
    private final Throttle heartbeatThrottle = new Throttle(5000);
    private final Throttle dumpThrottle = new Throttle(3000);
    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean checkPending;
    private final Runnable check = () -> {
        checkPending = false;
        checkScreen();
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        connectedAt = System.currentTimeMillis();
    }

    @Override
    public void onDestroy() {
        connectedAt = 0;
        handler.removeCallbacks(check);
        super.onDestroy();
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null) {
            return;
        }
        CharSequence pkg = event.getPackageName();
        if (pkg != null && !ShortFormDetector.isWatchedPackage(pkg.toString())) {
            return;
        }
        if (pkg != null && heartbeatThrottle.tryAcquire(SystemClock.uptimeMillis())) {
            Prefs.setLastEventAt(this, System.currentTimeMillis());
        }
        if (!checkPending) {
            checkPending = true;
            handler.postDelayed(check, CHECK_DELAY_MS);
        }
    }

    private void checkScreen() {
        boolean diag = Prefs.diagUntil(this) > System.currentTimeMillis();
        if (!diag && !Prefs.isEnabled(this)) {
            return;
        }
        Map<WindowInfo, AccessibilityNodeInfo> roots = new IdentityHashMap<>();
        List<WindowInfo> all = collectWindows(roots);
        List<WindowInfo> picked = WindowPicker.pick(all);

        if (diag) {
            recordDump(all, picked, roots);
            return; // в режиме диагностики не блокируем, чтобы можно было открыть Shorts
        }
        for (WindowInfo w : picked) {
            Decision d;
            try {
                d = detector.decide(w.packageName, new AndroidScreenNode(roots.get(w)));
            } catch (RuntimeException e) {
                continue; // окно закрылось во время обхода
            }
            if (d.action != Action.NONE) {
                act(d);
                return;
            }
        }
    }

    /** Все окна с корнями; если система не отдаёт список окон — только активное. */
    private List<WindowInfo> collectWindows(Map<WindowInfo, AccessibilityNodeInfo> roots) {
        List<WindowInfo> out = new ArrayList<>();
        List<AccessibilityWindowInfo> windows;
        try {
            windows = getWindows();
        } catch (RuntimeException e) {
            windows = null;
        }
        if (windows != null) {
            for (AccessibilityWindowInfo w : windows) {
                AccessibilityNodeInfo root = w == null ? null : w.getRoot();
                if (root == null || root.getPackageName() == null) {
                    continue;
                }
                WindowInfo info = new WindowInfo(root.getPackageName().toString(), mapType(w.getType()),
                        w.isActive(), w.isFocused());
                out.add(info);
                roots.put(info, root);
            }
        }
        if (out.isEmpty()) {
            AccessibilityNodeInfo root;
            try {
                root = getRootInActiveWindow();
            } catch (RuntimeException e) {
                root = null;
            }
            if (root != null && root.getPackageName() != null) {
                WindowInfo info = new WindowInfo(root.getPackageName().toString(),
                        WindowInfo.TYPE_APPLICATION, true, true);
                out.add(info);
                roots.put(info, root);
            }
        }
        return out;
    }

    private static int mapType(int t) {
        if (t == AccessibilityWindowInfo.TYPE_APPLICATION) {
            return WindowInfo.TYPE_APPLICATION;
        }
        if (t == AccessibilityWindowInfo.TYPE_INPUT_METHOD) {
            return WindowInfo.TYPE_INPUT_METHOD;
        }
        return WindowInfo.TYPE_SYSTEM;
    }

    private void act(Decision d) {
        long now = SystemClock.uptimeMillis();
        if (!actionThrottle.tryAcquire(now)) {
            return;
        }
        boolean done = performGlobalAction(d.action == Action.HOME ? GLOBAL_ACTION_HOME : GLOBAL_ACTION_BACK);
        if (!done) {
            return;
        }
        Prefs.setLastBlock(this, System.currentTimeMillis(), d.reason);
        if (toastThrottle.tryAcquire(now)) {
            String msg = d.action == Action.HOME ? "TikTok заблокирован" : "Короткие видео заблокированы";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private void recordDump(List<WindowInfo> all, List<WindowInfo> picked,
                            Map<WindowInfo, AccessibilityNodeInfo> roots) {
        if (picked.isEmpty() || !dumpThrottle.tryAcquire(SystemClock.uptimeMillis())) {
            return;
        }
        StringBuilder sb = new StringBuilder("окна: ").append(all);
        for (WindowInfo w : picked) {
            Decision d;
            String tree;
            try {
                AndroidScreenNode node = new AndroidScreenNode(roots.get(w));
                d = detector.decide(w.packageName, node);
                tree = ScreenDump.dump(w.packageName, node, DUMP_MAX_NODES);
            } catch (RuntimeException e) {
                continue;
            }
            sb.append("\nрешение: ").append(d).append('\n').append(tree);
        }
        Diagnostics.LOG.add(sb.toString());
    }

    @Override
    public void onInterrupt() {
        // Нечего прерывать: служба не воспроизводит звук и не держит долгих операций.
    }
}
