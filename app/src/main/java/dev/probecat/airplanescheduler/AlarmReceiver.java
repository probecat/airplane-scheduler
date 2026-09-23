package dev.probecat.airplanescheduler;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public final class AlarmReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (Scheduler.REMIND.equals(action)) {
            Scheduler.scheduleReminder(context);
            // Only check Shizuku here; applying the schedule outside the window would end airplane mode.
            PendingResult result = goAsync();
            ShizukuReminder.check(context, result::finish);
            return;
        }
        if (Scheduler.START.equals(action) || Scheduler.END.equals(action)) {
            Scheduler.schedule(context, Scheduler.START.equals(action));
        } else {
            Scheduler.scheduleAll(context);
        }
        if (Scheduler.isSaved(context)) {
            PendingResult result = goAsync();
            // Current time wins over the action in case Android delivered an old alarm late.
            AirplaneController.applyCurrent(context, result::finish);
        }
    }
}
