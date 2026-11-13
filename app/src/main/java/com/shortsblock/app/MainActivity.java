package com.shortsblock.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;

/** Экран настроек: статус службы, переключатель и кнопки перехода в системные настройки. */
public final class MainActivity extends Activity {

    private TextView status;
    private Switch toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = dp(20);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("Shorts Blocker");
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, 26);
        root.addView(title);

        TextView about = new TextView(this);
        about.setText("Закрывает YouTube Shorts и Instagram Reels, а TikTok не даёт открыть. "
                + "Обычные видео YouTube и лента Instagram работают как раньше.");
        about.setPadding(0, dp(8), 0, dp(16));
        root.addView(about);

        status = new TextView(this);
        status.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        status.setPadding(0, 0, 0, dp(12));
        root.addView(status);

        toggle = new Switch(this);
        toggle.setText("Блокировка включена");
        toggle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        toggle.setChecked(Prefs.isEnabled(this));
        toggle.setOnCheckedChangeListener((b, checked) -> {
            Prefs.setEnabled(this, checked);
            refresh();
        });
        root.addView(toggle);

        Button openA11y = new Button(this);
        openA11y.setText("Открыть «Специальные возможности»");
        openA11y.setOnClickListener(v -> open(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.topMargin = dp(16);
        root.addView(openA11y, lp);

        Button openAppInfo = new Button(this);
        openAppInfo.setText("Открыть «О приложении» (разрешить ограниченные настройки)");
        openAppInfo.setOnClickListener(v -> open(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", getPackageName(), null))));
        root.addView(openAppInfo, new LinearLayout.LayoutParams(lp));

        TextView help = new TextView(this);
        help.setText("Как включить:\n"
                + "1. Нажмите «Открыть «Специальные возможности»», найдите Shorts Blocker "
                + "(иногда в разделе «Установленные приложения») и включите.\n"
                + "2. Если на Android 13+ переключатель серый и пишет «Ограниченная настройка»: "
                + "нажмите вторую кнопку, затем ⋮ в правом верхнем углу → «Разрешить ограниченные "
                + "настройки», и повторите шаг 1.\n"
                + "3. На Xiaomi/Huawei/Samsung отключите для приложения экономию батареи, "
                + "иначе система может выключать службу.");
        help.setPadding(0, dp(20), 0, 0);
        help.setGravity(Gravity.START);
        root.addView(help);

        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        boolean serviceOn = isServiceEnabled();
        boolean switchOn = Prefs.isEnabled(this);
        if (!serviceOn) {
            status.setText("⚠ Служба выключена — включите её в «Специальных возможностях»");
        } else if (!switchOn) {
            status.setText("⏸ Служба включена, но блокировка на паузе");
        } else {
            status.setText("✅ Блокировка работает");
        }
        if (toggle.isChecked() != switchOn) {
            toggle.setChecked(switchOn);
        }
    }

    private boolean isServiceEnabled() {
        String enabled = Settings.Secure.getString(getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (TextUtils.isEmpty(enabled)) {
            return false;
        }
        ComponentName me = new ComponentName(this, BlockerService.class);
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        splitter.setString(enabled);
        for (String s : splitter) {
            ComponentName cn = ComponentName.unflattenFromString(s);
            if (me.equals(cn)) {
                return true;
            }
        }
        return false;
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_SETTINGS));
        }
    }

    private int dp(int v) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v,
                getResources().getDisplayMetrics()));
    }
}
