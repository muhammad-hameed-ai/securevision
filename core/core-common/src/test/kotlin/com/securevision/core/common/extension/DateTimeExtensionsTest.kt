package com.securevision.core.common.extension

import java.time.Instant
import java.time.ZoneId
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the timestamp formatting the whole app displays.
 *
 * Zone and locale are supplied explicitly so these assertions hold on any
 * machine — a test that passes only in one time zone is not a test.
 */
class DateTimeExtensionsTest {

    @Test
    fun `formats a full date and time`() {
        assertEquals("08 Aug 2026, 14:32:07", REFERENCE.toFormattedDateTime(UTC, Locale.ENGLISH))
    }

    @Test
    fun `formats a date only`() {
        assertEquals("08 Aug 2026", REFERENCE.toFormattedDate(UTC, Locale.ENGLISH))
    }

    @Test
    fun `formats a time only`() {
        assertEquals("14:32:07", REFERENCE.toFormattedTime(UTC, Locale.ENGLISH))
    }

    @Test
    fun `renders the supplied zone rather than the machine default`() {
        val karachi = ZoneId.of("Asia/Karachi")

        assertEquals("19:32:07", REFERENCE.toFormattedTime(karachi, Locale.ENGLISH))
    }

    @Test
    fun `classifies under a minute as just now`() {
        val now = REFERENCE + seconds(30)

        assertEquals(RelativeTime.JustNow, REFERENCE.toRelativeTime(now, UTC))
    }

    @Test
    fun `classifies under an hour in minutes`() {
        val now = REFERENCE + minutes(5)

        assertEquals(RelativeTime.MinutesAgo(5), REFERENCE.toRelativeTime(now, UTC))
    }

    @Test
    fun `classifies earlier the same day in hours`() {
        val now = REFERENCE + hours(3)

        assertEquals(RelativeTime.HoursAgo(3), REFERENCE.toRelativeTime(now, UTC))
    }

    @Test
    fun `classifies the previous calendar day as yesterday`() {
        val now = REFERENCE + hours(24)

        assertEquals(RelativeTime.Yesterday, REFERENCE.toRelativeTime(now, UTC))
    }

    @Test
    fun `classifies anything older as absolute`() {
        val now = REFERENCE + hours(72)

        assertEquals(RelativeTime.Absolute, REFERENCE.toRelativeTime(now, UTC))
    }

    @Test
    fun `prefers minutes over yesterday when only minutes apart across midnight`() {
        val justBeforeMidnight = Instant.parse("2026-08-08T23:58:00Z").toEpochMilli()
        val justAfterMidnight = Instant.parse("2026-08-09T00:02:00Z").toEpochMilli()

        assertEquals(
            RelativeTime.MinutesAgo(4),
            justBeforeMidnight.toRelativeTime(justAfterMidnight, UTC),
        )
    }

    @Test
    fun `formats a sub hour duration as minutes and seconds`() {
        assertEquals("01:05", seconds(65).toDurationLabel())
    }

    @Test
    fun `formats a multi hour duration with an hours component`() {
        assertEquals("1:02:05", (hours(1) + minutes(2) + seconds(5)).toDurationLabel())
    }

    @Test
    fun `formats zero and negative durations as zero`() {
        assertEquals("00:00", 0L.toDurationLabel())
        assertEquals("00:00", (-5_000L).toDurationLabel())
    }

    @Test
    fun `same day comparison respects the supplied zone`() {
        val lateUtc = Instant.parse("2026-08-08T22:00:00Z").toEpochMilli()
        val earlyNextUtc = Instant.parse("2026-08-09T01:00:00Z").toEpochMilli()

        assertFalse(lateUtc.isSameDayAs(earlyNextUtc, UTC))
        // Both fall on 9 August once shifted into UTC+05:00.
        assertTrue(lateUtc.isSameDayAs(earlyNextUtc, ZoneId.of("Asia/Karachi")))
    }

    private fun seconds(value: Long) = value * 1_000L
    private fun minutes(value: Long) = seconds(value * 60)
    private fun hours(value: Long) = minutes(value * 60)

    private companion object {
        val UTC: ZoneId = ZoneId.of("UTC")

        /** 8 August 2026, 14:32:07 UTC. */
        val REFERENCE: Long = Instant.parse("2026-08-08T14:32:07Z").toEpochMilli()
    }
}
