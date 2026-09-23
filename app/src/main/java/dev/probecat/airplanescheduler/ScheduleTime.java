package dev.probecat.airplanescheduler;

import java.time.ZonedDateTime;
import java.util.Locale;

final class ScheduleTime {
    private static final int DAY = 24 * 60;

    static boolean isActive(int start, int end, int minute) {
        if (start == end) {
            return false;
        }
        return start < end ? minute >= start && minute < end : minute >= start || minute < end;
    }

    static ZonedDateTime next(ZonedDateTime now, int minute) {
        ZonedDateTime trigger = now.withHour(minute / 60).withMinute(minute % 60).withSecond(0).withNano(0);
        return trigger.isAfter(now) ? trigger : trigger.plusDays(1);
    }

    static int before(int minute, int offset) {
        return Math.floorMod(minute - offset, DAY);
    }

    static String format(int minute) {
        return String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60);
    }
}
