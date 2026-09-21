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
    private static final String PREFS = "schedule";

    static void save(Context context, int start, int end, boolean disableWifi, boolean enableWifi) {
        preferences(context).edit()
                .putInt("start", start)
                .putInt("end", end)
                .putBoolean("disableWifi", disableWifi)
                .putBoolean("enableWifi", enableWifi)
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

    static boolean shouldEnableNow(Context context) {
        int start = start(context);
        int end = end(context);
        ZonedDateTime now = ZonedDateTime.now();
        int minute = now.getHour() * 60 + now.getMinute();
        return ScheduleTime.isActive(start, end, minute);
    }

    static void scheduleAll(Context context) {
        cancel(context, true);
        cancel(context, false);
        if (isSaved(context)) {
            schedule(context, true);
            schedule(context, false);
        }
    }

    @SuppressLint("MissingPermission")
    static void schedule(Context context, boolean startEvent) {
        // Recompute each daily alarm in local time so clock and time-zone changes stay correct.
        int minute = startEvent ? start(context) : end(context);
        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime trigger = ScheduleTime.next(now, minute);
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger.toInstant().toEpochMilli(), intent(context, startEvent));
    }

    private static void cancel(Context context, boolean startEvent) {
        context.getSystemService(AlarmManager.class).cancel(intent(context, startEvent));
    }

    private static PendingIntent intent(Context context, boolean startEvent) {
        Intent intent = new Intent(context, AlarmReceiver.class).setAction(startEvent ? START : END);
        return PendingIntent.getBroadcast(context, startEvent ? 1 : 2, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }
}
