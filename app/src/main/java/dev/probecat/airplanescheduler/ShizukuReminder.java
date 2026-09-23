package dev.probecat.airplanescheduler;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import rikka.shizuku.Shizuku;

final class ShizukuReminder {
    private static final String CHANNEL = "shizuku_reminder";
    private static final int NOTIFICATION = 1;

    // One check per night instead of watching Shizuku: stay silent when the schedule will work.
    static void check(Context context, Runnable done) {
        Context app = context.getApplicationContext();
        AirplaneController.awaitBinder(received -> {
            if (AirplaneController.hasAccess()) {
                dismiss(app);
            } else if (Scheduler.isSaved(app) && Scheduler.remindShizuku(app)) {
                notify(app, Shizuku.pingBinder());
            }
            done.run();
        });
    }

    static void dismiss(Context context) {
        context.getSystemService(NotificationManager.class).cancel(NOTIFICATION);
    }

    private static void notify(Context context, boolean running) {
        NotificationManager notifications = context.getSystemService(NotificationManager.class);
        notifications.createNotificationChannel(new NotificationChannel(
                CHANNEL, "Shizuku reminders", NotificationManager.IMPORTANCE_DEFAULT));
        String start = ScheduleTime.format(Scheduler.start(context));
        Intent open = new Intent(context, MainActivity.class).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        Notification notification = new Notification.Builder(context, CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle(running ? "Shizuku permission missing" : "Shizuku isn't running")
                .setContentText(running
                        ? "Airplane mode may not turn on at " + start + ". Open the app to grant access."
                        : "Airplane mode may not turn on at " + start + ". Start Shizuku before then.")
                .setCategory(Notification.CATEGORY_REMINDER)
                .setContentIntent(PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_IMMUTABLE))
                .setAutoCancel(true)
                .build();
        notifications.notify(NOTIFICATION, notification);
    }
}
