package com.shortsblock.app;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.widget.Toast;

import com.shortsblock.app.core.Action;
import com.shortsblock.app.core.ScreenProbe;
import com.shortsblock.app.core.ShortFormDetector;
import com.shortsblock.app.core.Throttle;

import java.util.List;

/**
 * Служба специальных возможностей: следит за экраном YouTube, Instagram и TikTok
 * и закрывает короткие видео.
 */
public final class BlockerService extends AccessibilityService {

    private static final long COOLDOWN_MS = 800;
    private static final long TOAST_COOLDOWN_MS = 4000;

    private final ShortFormDetector detector = new ShortFormDetector();
    private final Throttle actionThrottle = new Throttle(COOLDOWN_MS);
    private final Throttle toastThrottle = new Throttle(TOAST_COOLDOWN_MS);

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || !Prefs.isEnabled(this)) {
            return;
        }
        CharSequence eventPkg = event.getPackageName();
        if (eventPkg == null || !ShortFormDetector.isWatchedPackage(eventPkg.toString())) {
            return;
        }

        final AccessibilityNodeInfo root;
        try {
            root = getRootInActiveWindow();
        } catch (RuntimeException e) {
            return; // окно успело закрыться — просто пропускаем событие
        }
        if (root == null || root.getPackageName() == null) {
            return;
        }
        // Решаем по тому приложению, которое реально на экране, а не по источнику события.
        String pkg = root.getPackageName().toString();

        ScreenProbe probe = fullId -> hasVisibleView(root, fullId);
        Action action = detector.decide(pkg, probe);
        if (action == Action.NONE) {
            return;
        }
        long now = SystemClock.uptimeMillis();
        if (!actionThrottle.tryAcquire(now)) {
            return;
        }

        boolean done = performGlobalAction(action == Action.HOME ? GLOBAL_ACTION_HOME : GLOBAL_ACTION_BACK);
        if (done && toastThrottle.tryAcquire(now)) {
            String msg = action == Action.HOME ? "TikTok заблокирован" : "Короткие видео заблокированы";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        }
    }

    private static boolean hasVisibleView(AccessibilityNodeInfo root, String fullId) {
        List<AccessibilityNodeInfo> nodes;
        try {
            nodes = root.findAccessibilityNodeInfosByViewId(fullId);
        } catch (RuntimeException e) {
            return false;
        }
        if (nodes == null) {
            return false;
        }
        for (AccessibilityNodeInfo n : nodes) {
            if (n != null && n.isVisibleToUser()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void onInterrupt() {
        // Нечего прерывать: служба не воспроизводит звук и не держит долгих операций.
    }
}
