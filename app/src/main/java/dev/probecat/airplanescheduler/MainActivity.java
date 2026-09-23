package dev.probecat.airplanescheduler;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import rikka.shizuku.Shizuku;

@SuppressLint("SetTextI18n")
public final class MainActivity extends Activity {
    private Ui ui;
    private TextView start;
    private TextView end;
    private Switch disableWifi;
    private Switch enableWifi;
    private Switch remindShizuku;
    private Button remove;
    private TextView status;
    private TextView scheduleStatus;
    private final Shizuku.OnBinderReceivedListener binderListener = () -> runOnUiThread(() -> {
        refresh();
        requestShizuku();
    });
    private final Shizuku.OnRequestPermissionResultListener permissionListener = (requestCode, grantResult) -> runOnUiThread(() -> {
        refresh();
        if (grantResult == PackageManager.PERMISSION_GRANTED) {
            AirplaneController.applyCurrent(this, this::refresh);
        }
        if (Scheduler.isSaved(this) && Scheduler.remindShizuku(this)) {
            requestNotifications();
        }
    });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Airplane Scheduler");
        configureSystemBars();
        ui = new Ui(this);

        ScrollView root = new ScrollView(this);
        root.setFillViewport(true);
        root.setBackgroundColor(getColor(R.color.app_background));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        int outerPadding = ui.dp(Ui.PAD);
        content.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(outerPadding + bars.left, outerPadding + bars.top,
                    outerPadding + bars.right, outerPadding + bars.bottom);
            return windowInsets;
        });

        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);

        TextView heading = ui.text("Airplane Scheduler", 22, R.color.app_on_surface);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        column.addView(heading);

        LinearLayout states = new LinearLayout(this);
        status = ui.pill();
        scheduleStatus = ui.pill();
        states.addView(status);
        states.addView(scheduleStatus, spaced(wrapWrap(), true));
        column.addView(states, below(wrapWrap(), Ui.GAP));

        start = ui.field(view -> pick((TextView) view));
        end = ui.field(view -> pick((TextView) view));
        start.setText(format(Scheduler.start(this)));
        end.setText(format(Scheduler.end(this)));
        LinearLayout labels = new LinearLayout(this);
        labels.addView(ui.label("START"), weighted());
        labels.addView(ui.label("END"), spaced(weighted(), true));
        column.addView(labels, below(matchWrap(), Ui.PAD * 2));
        LinearLayout times = new LinearLayout(this);
        times.addView(start, weighted());
        times.addView(end, spaced(weighted(), true));
        column.addView(times, below(matchWrap(), Ui.GAP / 2));

        LinearLayout options = ui.panel();
        column.addView(options, below(matchWrap(), Ui.PAD));
        disableWifi = ui.toggle(options, "Wi-Fi off at start",
                "Airplane mode can leave Wi-Fi on. This turns it off.",
                Scheduler.disableWifi(this));
        enableWifi = ui.toggle(options, "Wi-Fi on at end",
                "Turns Wi-Fi back on when airplane mode ends.",
                Scheduler.enableWifi(this));
        remindShizuku = ui.toggle(options, "Shizuku reminder",
                "Notifies you an hour before start if Shizuku isn't ready.",
                Scheduler.remindShizuku(this));
        remindShizuku.setOnCheckedChangeListener((view, checked) -> {
            if (checked) {
                requestNotifications();
            }
        });

        LinearLayout actions = new LinearLayout(this);
        actions.addView(ui.button("Save", true, view -> save()), weighted());
        remove = ui.button("Remove", false, view -> remove());
        actions.addView(remove, spaced(weighted(), true));
        column.addView(actions, below(matchWrap(), Ui.PAD));

        TextView info = ui.label("Debug info");
        info.setGravity(Gravity.CENTER);
        info.setMinHeight(ui.dp(Ui.TOUCH));
        info.setOnClickListener(view -> showDebugInfo());
        column.addView(info, below(matchWrap(), Ui.GAP));

        int width = Math.min(getResources().getDisplayMetrics().widthPixels - ui.dp(Ui.PAD * 2), ui.dp(440));
        content.addView(column, new LinearLayout.LayoutParams(width, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(content, matchMatch());
        setContentView(root);

        Shizuku.addRequestPermissionResultListener(permissionListener);
        Shizuku.addBinderReceivedListenerSticky(binderListener);
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeBinderReceivedListener(binderListener);
        Shizuku.removeRequestPermissionResultListener(permissionListener);
        super.onDestroy();
    }

    private void pick(TextView field) {
        int value = parse(field.getText().toString());
        new TimePickerDialog(this,
                (picker, hour, minute) -> field.setText(format(hour * 60 + minute)),
                value / 60, value % 60, true).show();
    }

    private void configureSystemBars() {
        getWindow().setDecorFitsSystemWindows(false);
        boolean dark = (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                == Configuration.UI_MODE_NIGHT_YES;
        int lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
                | WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS;
        getWindow().getDecorView().post(() -> {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.setSystemBarsAppearance(dark ? 0 : lightBars, lightBars);
            }
        });
    }

    private LinearLayout.LayoutParams below(LinearLayout.LayoutParams params, int gap) {
        params.topMargin = ui.dp(gap);
        return params;
    }

    private LinearLayout.LayoutParams spaced(LinearLayout.LayoutParams params, boolean after) {
        params.leftMargin = after ? ui.dp(Ui.GAP) : 0;
        return params;
    }

    private void save() {
        try {
            int startMinute = parse(start.getText().toString());
            int endMinute = parse(end.getText().toString());
            if (startMinute == endMinute) {
                throw new IllegalArgumentException();
            }
            Scheduler.save(this, startMinute, endMinute, disableWifi.isChecked(), enableWifi.isChecked(),
                    remindShizuku.isChecked());
            Scheduler.scheduleAll(this);
            Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show();
            boolean prompting = false;
            if (hasShizukuAccess()) {
                AirplaneController.applyCurrent(this, this::refresh);
            } else {
                prompting = requestShizuku();
            }
            // Ask after the Shizuku prompt instead so the two dialogs don't stack.
            if (remindShizuku.isChecked() && !prompting) {
                requestNotifications();
            }
            refresh();
        } catch (IllegalArgumentException e) {
            Toast.makeText(this, "Use two different 24-hour times such as 23:00 and 07:00", Toast.LENGTH_LONG).show();
        }
    }

    private void remove() {
        Scheduler.remove(this);
        Scheduler.scheduleAll(this);
        Toast.makeText(this, "Schedule removed", Toast.LENGTH_SHORT).show();
        if (Scheduler.cleanupPending(this) && !hasShizukuAccess()) {
            requestShizuku();
        } else {
            AirplaneController.applyCurrent(this, this::refresh);
        }
        refresh();
    }

    private void showDebugInfo() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            String shizuku = !Shizuku.pingBinder() ? "offline"
                    : hasShizukuAccess() ? "ready" : "permission required";
            String details = "Version: " + info.versionName + " (" + info.getLongVersionCode() + ")\n"
                    + "Package: " + getPackageName() + "\n"
                    + "Android: " + Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")\n"
                    + "Shizuku: " + shizuku + "\n"
                    + "Schedule: " + (Scheduler.isSaved(this) ? "enabled" : "disabled") + "\n"
                    + "Window: " + format(Scheduler.start(this)) + "–" + format(Scheduler.end(this)) + "\n"
                    + "Wi-Fi at start: " + (Scheduler.disableWifi(this) ? "disable" : "leave unchanged") + "\n"
                    + "Wi-Fi at end: " + (Scheduler.enableWifi(this) ? "enable" : "leave unchanged") + "\n"
                    + "Shizuku reminder: " + (Scheduler.remindShizuku(this)
                    ? format(ScheduleTime.before(Scheduler.start(this), Scheduler.REMIND_BEFORE_MINUTES)) : "off") + "\n"
                    + "Notifications: " + (canNotify() ? "allowed" : "not allowed") + "\n"
                    + "Exact alarms: " + (getSystemService(AlarmManager.class).canScheduleExactAlarms()
                    ? "allowed" : "not allowed");
            TextView text = new TextView(this);
            text.setText(details);
            text.setTextIsSelectable(true);
            text.setTextSize(14);
            text.setTypeface(Typeface.MONOSPACE);
            text.setPadding(ui.dp(24), ui.dp(Ui.GAP), ui.dp(24), 0);
            new AlertDialog.Builder(this)
                    .setTitle("Debug information")
                    .setView(text)
                    .setNeutralButton("Copy", (dialog, which) -> {
                        getSystemService(ClipboardManager.class).setPrimaryClip(
                                ClipData.newPlainText("Airplane Scheduler debug information", details));
                        Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show();
                    })
                    .setPositiveButton("Close", null)
                    .show();
        } catch (PackageManager.NameNotFoundException ignored) {
        }
    }

    private boolean requestShizuku() {
        if (!Shizuku.pingBinder()) {
            return false;
        }
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1);
                return true;
            }
        } catch (RuntimeException ignored) {
        }
        return false;
    }

    private void requestNotifications() {
        if (!canNotify()) {
            requestPermissions(new String[] {Manifest.permission.POST_NOTIFICATIONS}, 2);
        }
    }

    private boolean canNotify() {
        return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
    }

    private boolean hasShizukuAccess() {
        return AirplaneController.hasAccess();
    }

    private void refresh() {
        if (status == null || scheduleStatus == null) {
            return;
        }
        boolean saved = Scheduler.isSaved(this);
        remove.setEnabled(saved);
        remove.setAlpha(saved ? 1f : 0.45f);
        ui.setPill(scheduleStatus, saved ? "Schedule on" : "Schedule off",
                saved ? "Schedule is enabled" : "Schedule is disabled", saved);
        if (!Shizuku.pingBinder()) {
            ui.setPill(status, "Shizuku offline", "Shizuku is not running", false);
        } else if (hasShizukuAccess()) {
            ShizukuReminder.dismiss(this);
            ui.setPill(status, "Shizuku ready", "Shizuku is ready", true);
        } else {
            ui.setPill(status, "Shizuku locked", "Shizuku permission is required", false);
        }
    }

    private static int parse(String value) {
        String[] parts = value.trim().split(":", -1);
        if (parts.length != 2) {
            throw new IllegalArgumentException();
        }
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        if (hour < 0 || hour > 23 || minute < 0 || minute > 59) {
            throw new IllegalArgumentException();
        }
        return hour * 60 + minute;
    }

    private static String format(int minute) {
        return ScheduleTime.format(minute);
    }

    private static LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static LinearLayout.LayoutParams wrapWrap() {
        return new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    private static ScrollView.LayoutParams matchMatch() {
        return new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
    }

    private static LinearLayout.LayoutParams weighted() {
        return new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
    }
}
