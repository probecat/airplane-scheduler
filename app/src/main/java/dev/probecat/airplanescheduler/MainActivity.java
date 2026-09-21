package dev.probecat.airplanescheduler;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.res.ColorStateList;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Insets;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

import rikka.shizuku.Shizuku;

@SuppressLint("SetTextI18n")
public final class MainActivity extends Activity {
    private EditText start;
    private EditText end;
    private CheckBox disableWifi;
    private CheckBox enableWifi;
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
    });

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setTitle("Airplane Scheduler");
        configureSystemBars();

        ScrollView root = new ScrollView(this);
        root.setFillViewport(true);
        root.setBackgroundColor(getColor(R.color.app_background));

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setGravity(Gravity.CENTER);
        int outerPadding = dp(20);
        content.setOnApplyWindowInsetsListener((view, windowInsets) -> {
            Insets bars = windowInsets.getInsets(WindowInsets.Type.systemBars());
            view.setPadding(outerPadding + bars.left, outerPadding + bars.top,
                    outerPadding + bars.right, outerPadding + bars.bottom);
            return windowInsets;
        });

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(24), dp(28), dp(24), dp(24));
        card.setElevation(dp(2));
        card.setBackground(rounded(getColor(R.color.app_surface), 28));

        TextView heading = new TextView(this);
        heading.setText("Airplane Scheduler");
        heading.setTextSize(26);
        heading.setTextColor(getColor(R.color.app_on_surface));
        heading.setGravity(Gravity.CENTER);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(heading, matchWrap());

        LinearLayout states = new LinearLayout(this);
        states.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams statesParams = matchWrap();
        statesParams.topMargin = dp(12);
        card.addView(states, statesParams);
        status = pill();
        scheduleStatus = pill();
        states.addView(status, wrapWrap());
        LinearLayout.LayoutParams scheduleStatusParams = wrapWrap();
        scheduleStatusParams.leftMargin = dp(8);
        states.addView(scheduleStatus, scheduleStatusParams);

        start = field();
        end = field();
        start.setText(format(Scheduler.start(this)));
        end.setText(format(Scheduler.end(this)));

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams timeParams = matchWrap();
        timeParams.topMargin = dp(24);
        card.addView(timeRow, timeParams);
        timeRow.addView(timeColumn("START", start), weighted());
        TextView arrow = new TextView(this);
        arrow.setText("→");
        arrow.setTextSize(22);
        arrow.setGravity(Gravity.CENTER);
        timeRow.addView(arrow, new LinearLayout.LayoutParams(dp(44), ViewGroup.LayoutParams.WRAP_CONTENT));
        timeRow.addView(timeColumn("END", end), weighted());

        LinearLayout options = new LinearLayout(this);
        options.setOrientation(LinearLayout.VERTICAL);
        options.setPadding(dp(12), dp(6), dp(12), dp(6));
        options.setBackground(rounded(getColor(R.color.app_surface_container), 16));
        LinearLayout.LayoutParams optionsParams = matchWrap();
        optionsParams.topMargin = dp(18);
        card.addView(options, optionsParams);

        disableWifi = new CheckBox(this);
        disableWifi.setText("Disable Wi-Fi at start");
        disableWifi.setTextColor(getColor(R.color.app_on_surface));
        disableWifi.setChecked(Scheduler.disableWifi(this));
        options.addView(disableWifi, matchWrap());

        enableWifi = new CheckBox(this);
        enableWifi.setText("Enable Wi-Fi at end");
        enableWifi.setTextColor(getColor(R.color.app_on_surface));
        enableWifi.setChecked(Scheduler.enableWifi(this));
        options.addView(enableWifi, matchWrap());

        LinearLayout actions = new LinearLayout(this);
        actions.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout.LayoutParams actionsParams = matchWrap();
        actionsParams.topMargin = dp(18);
        card.addView(actions, actionsParams);

        Button save = new Button(this);
        save.setText("Save");
        save.setAllCaps(false);
        save.setTextSize(16);
        save.setTextColor(getColor(R.color.app_on_primary));
        save.setMinHeight(dp(52));
        save.setBackground(new RippleDrawable(ColorStateList.valueOf(0x33FFFFFF),
                rounded(getColor(R.color.app_primary), 14), null));
        save.setElevation(dp(1));
        save.setOnClickListener(view -> save());
        actions.addView(save, new LinearLayout.LayoutParams(0, dp(52), 1));

        remove = new Button(this);
        remove.setText("Remove");
        remove.setAllCaps(false);
        remove.setTextSize(16);
        remove.setTextColor(getColor(R.color.app_primary));
        remove.setMinHeight(dp(48));
        remove.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000),
                rounded(getColor(R.color.app_surface_container), 14), null));
        remove.setOnClickListener(view -> remove());
        LinearLayout.LayoutParams removeParams = new LinearLayout.LayoutParams(0, dp(52), 1);
        removeParams.leftMargin = dp(8);
        actions.addView(remove, removeParams);

        Button info = new Button(this);
        info.setText("Debug info");
        info.setAllCaps(false);
        info.setTextSize(12);
        info.setTextColor(getColor(R.color.app_on_surface_muted));
        info.setMinHeight(0);
        info.setMinimumHeight(0);
        info.setPadding(dp(12), dp(7), dp(12), dp(7));
        info.setBackground(new RippleDrawable(ColorStateList.valueOf(0x22000000),
                rounded(getColor(R.color.app_surface_container), 12), null));
        info.setOnClickListener(view -> showDebugInfo());
        LinearLayout.LayoutParams infoParams = wrapWrap();
        infoParams.gravity = Gravity.CENTER_HORIZONTAL;
        infoParams.topMargin = dp(12);
        card.addView(info, infoParams);

        int cardWidth = Math.min(getResources().getDisplayMetrics().widthPixels - dp(40), dp(440));
        content.addView(card, new LinearLayout.LayoutParams(cardWidth, ViewGroup.LayoutParams.WRAP_CONTENT));
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

    private EditText field() {
        EditText field = new EditText(this);
        field.setGravity(Gravity.CENTER);
        field.setSingleLine(true);
        field.setFocusable(false);
        field.setCursorVisible(false);
        field.setTextSize(24);
        field.setTextColor(getColor(R.color.app_on_surface));
        field.setMinHeight(dp(64));
        field.setPadding(dp(12), dp(10), dp(12), dp(10));
        int ripple = (getColor(R.color.app_primary) & 0x00ffffff) | 0x26000000;
        field.setBackground(new RippleDrawable(ColorStateList.valueOf(ripple), outlined(), null));
        field.setOnClickListener(view -> pick(field));
        return field;
    }

    private void pick(EditText field) {
        int value = parse(field.getText().toString());
        new TimePickerDialog(this,
                (picker, hour, minute) -> field.setText(format(hour * 60 + minute)),
                value / 60, value % 60, true).show();
    }

    private LinearLayout timeColumn(String label, EditText field) {
        LinearLayout column = new LinearLayout(this);
        column.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(this);
        title.setText(label);
        title.setTextSize(12);
        title.setTextColor(getColor(R.color.app_on_surface_muted));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        column.addView(title, matchWrap());
        column.addView(field, matchWrap());
        return column;
    }

    private TextView pill() {
        TextView pill = new TextView(this);
        pill.setGravity(Gravity.CENTER);
        pill.setTextSize(12);
        pill.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        pill.setPadding(dp(11), dp(6), dp(11), dp(6));
        return pill;
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

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radius));
        return drawable;
    }

    private GradientDrawable outlined() {
        GradientDrawable drawable = rounded(getColor(R.color.app_surface_container), 14);
        drawable.setStroke(dp(2), getColor(R.color.app_primary));
        return drawable;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private void save() {
        try {
            int startMinute = parse(start.getText().toString());
            int endMinute = parse(end.getText().toString());
            if (startMinute == endMinute) {
                throw new IllegalArgumentException();
            }
            Scheduler.save(this, startMinute, endMinute, disableWifi.isChecked(), enableWifi.isChecked());
            Scheduler.scheduleAll(this);
            Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show();
            if (hasShizukuAccess()) {
                AirplaneController.applyCurrent(this, this::refresh);
            } else {
                requestShizuku();
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
                    + "Exact alarms: " + (getSystemService(AlarmManager.class).canScheduleExactAlarms()
                    ? "allowed" : "not allowed");
            TextView text = new TextView(this);
            text.setText(details);
            text.setTextIsSelectable(true);
            text.setTextSize(14);
            text.setTypeface(Typeface.MONOSPACE);
            text.setPadding(dp(24), dp(8), dp(24), 0);
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

    private void requestShizuku() {
        if (!Shizuku.pingBinder()) {
            return;
        }
        try {
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(1);
            }
        } catch (RuntimeException ignored) {
        }
    }

    private boolean hasShizukuAccess() {
        try {
            return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
        } catch (RuntimeException e) {
            return false;
        }
    }

    private void refresh() {
        if (status == null || scheduleStatus == null) {
            return;
        }
        boolean saved = Scheduler.isSaved(this);
        remove.setEnabled(saved);
        remove.setAlpha(saved ? 1f : 0.45f);
        setPill(scheduleStatus, saved ? "Schedule on" : "Schedule off",
                saved ? "Schedule is enabled" : "Schedule is disabled",
                saved ? R.color.status_ready_background : R.color.status_off_background,
                saved ? R.color.status_ready_foreground : R.color.status_off_foreground);
        if (!Shizuku.pingBinder()) {
            setPill(status, "Shizuku offline", "Shizuku is not running",
                    R.color.status_off_background, R.color.status_off_foreground);
        } else if (hasShizukuAccess()) {
            setPill(status, "Shizuku ready", "Shizuku is ready",
                    R.color.status_ready_background, R.color.status_ready_foreground);
        } else {
            setPill(status, "Shizuku locked", "Shizuku permission is required",
                    R.color.status_off_background, R.color.status_off_foreground);
        }
    }

    private void setPill(TextView pill, String text, String description, int background, int foreground) {
        pill.setText(text);
        pill.setContentDescription(description);
        pill.setTextColor(getColor(foreground));
        pill.setBackground(rounded(getColor(background), 20));
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
        return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
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
