package com.shortsblock.app;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
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
import android.widget.Toast;

import com.shortsblock.app.core.Ago;
import com.shortsblock.app.core.ServiceHealth;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/** Экран настроек: статус службы, переключатель и кнопки перехода в системные настройки. */
public final class MainActivity extends Activity {

    private TextView status;
    private TextView details;
    private Button diagButton;
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

        details = new TextView(this);
        details.setPadding(0, 0, 0, dp(12));
        root.addView(details);

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

        diagButton = new Button(this);
        diagButton.setOnClickListener(v -> toggleDiagnostics());
        root.addView(diagButton, new LinearLayout.LayoutParams(lp));

        Button send = new Button(this);
        send.setText("Отправить отчёт диагностики");
        send.setOnClickListener(v -> sendReport());
        root.addView(send, new LinearLayout.LayoutParams(lp));

        TextView diagHelp = new TextView(this);
        diagHelp.setText("Если Shorts или Reels снова не закрываются: нажмите «Диагностика», "
                + "за 2 минуты откройте Shorts (вкладкой и из ленты) и Reels, подержите каждый экран "
                + "5 секунд, вернитесь сюда и нажмите «Отправить отчёт». Блокировка на это время выключается. "
                + "В отчёт попадает структура экранов YouTube/Instagram (включая короткие подписи).");
        diagHelp.setPadding(0, dp(8), 0, 0);
        root.addView(diagHelp);

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
        boolean switchOn = Prefs.isEnabled(this);
        String health = ServiceHealth.evaluate(isServiceEnabled(), switchOn, BlockerService.connectedAt,
                System.currentTimeMillis());
        if (ServiceHealth.OFF.equals(health)) {
            status.setText("⚠ Служба выключена — включите её в «Специальных возможностях»");
        } else if (ServiceHealth.PAUSED.equals(health)) {
            status.setText("⏸ Служба включена, но блокировка на паузе");
        } else if (ServiceHealth.NOT_RUNNING.equals(health)) {
            status.setText("⚠ Служба включена в настройках, но не запущена системой. "
                    + "Выключите и снова включите её в «Специальных возможностях» и отключите экономию батареи.");
        } else {
            status.setText("✅ Блокировка работает");
        }
        long now = System.currentTimeMillis();
        String reason = Prefs.lastBlockReason(this);
        details.setText("Последнее событие из YouTube/Instagram/TikTok: " + Ago.format(Prefs.lastEventAt(this), now)
                + "\nПоследняя блокировка: " + Ago.format(Prefs.lastBlockAt(this), now)
                + (reason.isEmpty() ? "" : " (" + reason + ")"));
        boolean diag = Prefs.diagUntil(this) > now;
        diagButton.setText(diag ? "Остановить диагностику (записано экранов: "
                + Diagnostics.LOG.entries().size() + ")" : "Диагностика (2 минуты)");
        if (toggle.isChecked() != switchOn) {
            toggle.setChecked(switchOn);
        }
    }

    private void toggleDiagnostics() {
        long now = System.currentTimeMillis();
        if (Prefs.diagUntil(this) > now) {
            Prefs.setDiagUntil(this, 0);
        } else {
            Diagnostics.LOG.clear();
            Prefs.setDiagUntil(this, now + Diagnostics.DURATION_MS);
            Toast.makeText(this, "Диагностика 2 минуты: откройте Shorts и Reels", Toast.LENGTH_LONG).show();
        }
        refresh();
    }

    private void sendReport() {
        String header = "Shorts Blocker " + versionOf(getPackageName())
                + "\nYouTube " + versionOf("com.google.android.youtube")
                + "\nInstagram " + versionOf("com.instagram.android")
                + "\nТелефон: " + Build.MANUFACTURER + " " + Build.MODEL + ", Android " + Build.VERSION.RELEASE
                + " (API " + Build.VERSION.SDK_INT + ")"
                + "\nСтатус: " + status.getText()
                + "\n" + details.getText();
        String report = Diagnostics.LOG.report(header);
        try (Writer w = new OutputStreamWriter(new FileOutputStream(ReportProvider.file(this)),
                StandardCharsets.UTF_8)) {
            w.write(report);
        } catch (IOException e) {
            Toast.makeText(this, "Не удалось сохранить отчёт: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("text/plain");
        send.putExtra(Intent.EXTRA_STREAM, ReportProvider.URI);
        send.putExtra(Intent.EXTRA_SUBJECT, "Shorts Blocker: отчёт диагностики");
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        open(Intent.createChooser(send, "Отправить отчёт"));
    }

    @SuppressWarnings("deprecation")
    private String versionOf(String pkg) {
        try {
            return getPackageManager().getPackageInfo(pkg, 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "не установлен";
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
