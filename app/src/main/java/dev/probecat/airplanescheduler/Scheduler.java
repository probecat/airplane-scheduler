package dev.probecat.airplanescheduler;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import java.time.ZonedDateTime;

final class Scheduler {
    static final String START = "dev.probecat.airplanescheduler.START";
    static final String END = "dev.probecat.airplanescheduler.END";
    static final String REMIND = "dev.probecat.airplanescheduler.REMIND";
    static final int REMIND_BEFORE_MINUTES = 60;
    private static final String PREFS = "schedule";

    static void save(Context context, int start, int end, boolean disableWifi, boolean enableWifi,
            boolean remindShizuku) {
        preferences(context).edit()
                .putInt("start", start)
                .putInt("end", end)
                .putBoolean("disableWifi", disableWifi)
                .putBoolean("enableWifi", enableWifi)
                .putBoolean("remindShizuku", remindShizuku)
                .putBoolean("cleanupPending", false)
                .putBoolean("saved", true)
                .apply();
    }

    static void remove(Context context) {
        boolean cleanup = isSaved(context) && shouldEnableNow(context);
        preferences(context).edit()
                .putBoolean("saved", false)
                .putBoolean("cleanupPending", cleanup)
                .putBoolean("cleanupEnableWifi", enableWifi(context))
                .apply();
    }

    static boolean cleanupPending(Context context) {
        return preferences(context).getBoolean("cleanupPending", false);
    }

    static boolean cleanupEnableWifi(Context context) {
        return preferences(context).getBoolean("cleanupEnableWifi", true);
    }

    static void cleanupFinished(Context context) {
        preferences(context).edit().putBoolean("cleanupPending", false).apply();
    }

    static boolean isSaved(Context context) {
        return preferences(context).getBoolean("saved", false);
    }

    static int start(Context context) {
        return preferences(context).getInt("start", 23 * 60);
    }

    static int end(Context context) {
        return preferences(context).getInt("end", 7 * 60);
    }

    static boolean disableWifi(Context context) {
        return preferences(context).getBoolean("disableWifi", true);
    }

    static boolean enableWifi(Context context) {
        return preferences(context).getBoolean("enableWifi", true);
    }

    static boolean remindShizuku(Context context) {
        return preferences(context).getBoolean("remindShizuku", false);
    }

    static boolean shouldEnableNow(Context context) {
        int start = start(context);
        int end = end(context);
        ZonedDateTime now = ZonedDateTime.now();
        int minute = now.getHour() * 60 + now.getMinute();
        return ScheduleTime.isActive(start, end, minute);
    }

    static void scheduleAll(Context context) {
        cancel(context, START);
        cancel(context, END);
        cancel(context, REMIND);
        if (isSaved(context)) {
            schedule(context, true);
            schedule(context, false);
            scheduleReminder(context);
        }
    }

    static void schedule(Context context, boolean startEvent) {
        schedule(context, startEvent ? START : END, startEvent ? start(context) : end(context));
    }

    static void scheduleReminder(Context context) {
        if (isSaved(context) && remindShizuku(context)) {
            schedule(context, REMIND, ScheduleTime.before(start(context), REMIND_BEFORE_MINUTES));
        }
    }

    @SuppressLint("MissingPermission")
    private static void schedule(Context context, String action, int minute) {
        // Recompute each daily alarm in local time so clock and time-zone changes stay correct.
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime trigger = ScheduleTime.next(now, minute);
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toInstant().toEpochMilli(), intent(context, action));
    }

    private static void cancel(Context context, String action) {
        context.getSystemService(AlarmManager.class).cancel(intent(context, action));
    }

    private static PendingIntent intent(Context context, String action) {
        int requestCode = START.equals(action) ? 1 : END.equals(action) ? 2 : 3;
        Intent intent = new Intent(context, AlarmReceiver.class).setAction(action);
        return PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
