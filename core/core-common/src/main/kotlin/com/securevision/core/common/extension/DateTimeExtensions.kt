package com.securevision.core.common.extension

import com.securevision.core.common.Constants
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

/**
 * Date and time formatting for epoch-millisecond timestamps.
 *
 * Every alert, recording and history entry is stamped in UTC epoch milliseconds
 * and rendered in the device's own zone and locale at display time. The zone and
 * locale are parameters with sensible defaults so that tests can pin them and
 * assert exact strings.
 *
 * `java.time` is used directly rather than through desugaring — `minSdk` is 26,
 * where it is part of the platform.
 */

/**
 * Formats as a full date and time, e.g. `08 Aug 2026, 14:32:07`.
 *
 * @param zone Time zone to render in; defaults to the device zone.
 * @param locale Locale for month names; defaults to the device locale.
 */
fun Long.toFormattedDateTime(
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = format(Constants.DateTime.DATE_TIME_PATTERN, zone, locale)

/**
 * Formats as a date only, e.g. `08 Aug 2026`.
 *
 * @param zone Time zone to render in; defaults to the device zone.
 * @param locale Locale for month names; defaults to the device locale.
 */
fun Long.toFormattedDate(
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = format(Constants.DateTime.DATE_PATTERN, zone, locale)

/**
 * Formats as a wall-clock time only, e.g. `14:32:07`.
 *
 * @param zone Time zone to render in; defaults to the device zone.
 * @param locale Locale to format with; defaults to the device locale.
 */
fun Long.toFormattedTime(
    zone: ZoneId = ZoneId.systemDefault(),
    locale: Locale = Locale.getDefault(),
): String = format(Constants.DateTime.TIME_PATTERN, zone, locale)

/**
 * Classifies this timestamp relative to [now] so the caller can pick a localised
 * string for it.
 *
 * Returns a [RelativeTime] rather than a formatted string on purpose: this module
 * has no access to `strings.xml`, and hard-coding English words like "Yesterday"
 * here would defeat localisation.
 *
 * @param now Reference instant in epoch milliseconds; defaults to the current time.
 * @param zone Time zone used to decide calendar-day boundaries.
 */
fun Long.toRelativeTime(
    now: Long = System.currentTimeMillis(),
    zone: ZoneId = ZoneId.systemDefault(),
): RelativeTime {
    val elapsed = (now - this).milliseconds

    return when {
        elapsed.inWholeMinutes < 1 -> RelativeTime.JustNow
        elapsed.inWholeHours < 1 -> RelativeTime.MinutesAgo(elapsed.inWholeMinutes.toInt())
        isSameDayAs(now, zone) -> RelativeTime.HoursAgo(elapsed.inWholeHours.toInt())
        isYesterdayRelativeTo(now, zone) -> RelativeTime.Yesterday
        else -> RelativeTime.Absolute
    }
}

/**
 * Formats a duration in milliseconds as `mm:ss`, or `h:mm:ss` once it passes an
 * hour — the conventional shape for a recording's length.
 */
fun Long.toDurationLabel(): String {
    val totalSeconds = (this / MILLIS_PER_SECOND).coerceAtLeast(0L)
    val hours = totalSeconds / SECONDS_PER_HOUR
    val minutes = (totalSeconds % SECONDS_PER_HOUR) / SECONDS_PER_MINUTE
    val seconds = totalSeconds % SECONDS_PER_MINUTE

    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%02d:%02d", minutes, seconds)
    }
}

/** Returns `true` when this timestamp and [other] fall on the same calendar day in [zone]. */
fun Long.isSameDayAs(other: Long, zone: ZoneId = ZoneId.systemDefault()): Boolean =
    toLocalDate(zone) == other.toLocalDate(zone)

/** The calendar date this timestamp falls on, in [zone]. */
fun Long.toLocalDate(zone: ZoneId = ZoneId.systemDefault()): LocalDate =
    Instant.ofEpochMilli(this).atZone(zone).toLocalDate()

/**
 * How a timestamp relates to "now", as a value the presentation layer maps onto a
 * localised string resource.
 */
sealed interface RelativeTime {

    /** Less than a minute ago. */
    data object JustNow : RelativeTime

    /** Under an hour ago. */
    data class MinutesAgo(val minutes: Int) : RelativeTime

    /** Earlier on the same calendar day. */
    data class HoursAgo(val hours: Int) : RelativeTime

    /** On the previous calendar day. */
    data object Yesterday : RelativeTime

    /** Older than yesterday — render with [toFormattedDate] instead. */
    data object Absolute : RelativeTime
}

private fun Long.isYesterdayRelativeTo(now: Long, zone: ZoneId): Boolean =
    toLocalDate(zone) == now.toLocalDate(zone).minusDays(1)

private fun Long.format(pattern: String, zone: ZoneId, locale: Locale): String =
    DateTimeFormatter.ofPattern(pattern, locale)
        .withZone(zone)
        .format(Instant.ofEpochMilli(this))

private const val MILLIS_PER_SECOND = 1_000L
private const val SECONDS_PER_MINUTE = 60L
private const val SECONDS_PER_HOUR = 3_600L
