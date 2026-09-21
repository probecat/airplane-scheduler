package dev.probecat.airplanescheduler;

import java.time.ZonedDateTime;

final class ScheduleTime {
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
}
