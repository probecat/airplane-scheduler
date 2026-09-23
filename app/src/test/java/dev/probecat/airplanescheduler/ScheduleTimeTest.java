package dev.probecat.airplanescheduler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class ScheduleTimeTest {
    @Test
    public void overnightBoundaries() {
        assertFalse(ScheduleTime.isActive(23 * 60, 7 * 60, 22 * 60 + 59));
        assertTrue(ScheduleTime.isActive(23 * 60, 7 * 60, 23 * 60));
        assertTrue(ScheduleTime.isActive(23 * 60, 7 * 60, 6 * 60 + 59));
        assertFalse(ScheduleTime.isActive(23 * 60, 7 * 60, 7 * 60));
    }

    @Test
    public void sameDayBoundaries() {
        assertFalse(ScheduleTime.isActive(9 * 60, 17 * 60, 8 * 60 + 59));
        assertTrue(ScheduleTime.isActive(9 * 60, 17 * 60, 9 * 60));
        assertTrue(ScheduleTime.isActive(9 * 60, 17 * 60, 16 * 60 + 59));
        assertFalse(ScheduleTime.isActive(9 * 60, 17 * 60, 17 * 60));
    }

    @Test
    public void equalTimesAreNeverActive() {
        assertFalse(ScheduleTime.isActive(0, 0, 0));
        assertFalse(ScheduleTime.isActive(12 * 60, 12 * 60, 23 * 60));
    }

    @Test
    public void futureBoundaryStaysToday() {
        ZonedDateTime now = ZonedDateTime.parse("2026-09-21T22:59:59+06:00[Asia/Dhaka]");
        assertEquals(
                ZonedDateTime.parse("2026-09-21T23:00:00+06:00[Asia/Dhaka]"),
                ScheduleTime.next(now, 23 * 60));
    }

    @Test
    public void reachedBoundaryMovesToTomorrow() {
        ZonedDateTime now = ZonedDateTime.parse("2026-09-21T23:00:00+06:00[Asia/Dhaka]");
        assertEquals(
                ZonedDateTime.parse("2026-09-22T23:00:00+06:00[Asia/Dhaka]"),
                ScheduleTime.next(now, 23 * 60));
    }

    @Test
    public void daylightSavingGapUsesNextValidLocalTime() {
        ZoneId zone = ZoneId.of("America/New_York");
        ZonedDateTime now = ZonedDateTime.of(2026, 3, 8, 1, 55, 0, 0, zone);
        ZonedDateTime trigger = ScheduleTime.next(now, 2 * 60 + 30);
        assertEquals(3, trigger.getHour());
        assertEquals(30, trigger.getMinute());
        assertTrue(trigger.isAfter(now));
    }

    @Test
    public void reminderWrapsBeforeMidnight() {
        assertEquals(22 * 60, ScheduleTime.before(23 * 60, 60));
        assertEquals(23 * 60 + 30, ScheduleTime.before(30, 60));
        assertEquals(23 * 60, ScheduleTime.before(0, 60));
    }

    @Test
    public void formatsTwentyFourHourTime() {
        assertEquals("00:05", ScheduleTime.format(5));
        assertEquals("23:59", ScheduleTime.format(23 * 60 + 59));
    }
}
