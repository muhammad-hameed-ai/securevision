package com.securevision.core.data.database.converter

import androidx.room.TypeConverter
import com.securevision.core.model.AccessLevel
import com.securevision.core.model.AlertType
import com.securevision.core.model.Severity

/**
 * Persists domain enums as their declared names.
 *
 * By `name`, never by `ordinal`. An ordinal column silently reinterprets every
 * stored row the moment someone inserts or reorders an enum constant — the kind
 * of corruption that shows up months later as alerts of the wrong type.
 */
class EnumConverters {

    /** @param type The alert category to store. */
    @TypeConverter
    fun fromAlertType(type: AlertType): String = type.name

    /**
     * @param value Stored name.
     * @throws IllegalArgumentException if the column holds a name no longer
     *   declared by [AlertType] — failing loudly beats mapping it to a default
     *   and mislabelling an alert.
     */
    @TypeConverter
    fun toAlertType(value: String): AlertType = AlertType.valueOf(value)

    /** @param severity The severity to store. */
    @TypeConverter
    fun fromSeverity(severity: Severity): String = severity.name

    /**
     * @param value Stored name.
     * @throws IllegalArgumentException if the column holds a name no longer
     *   declared by [Severity].
     */
    @TypeConverter
    fun toSeverity(value: String): Severity = Severity.valueOf(value)

    /** @param level The operator classification to store. */
    @TypeConverter
    fun fromAccessLevel(level: AccessLevel): String = level.name

    /**
     * @param value Stored name.
     * @return The matching level, or [AccessLevel.DEFAULT].
     *
     * Lenient where the alert enums are strict, and deliberately so: an
     * unrecognised access level costs a mislabelled badge, whereas throwing would
     * make a whole profile — including a face the app can still recognise —
     * unreadable.
     */
    @TypeConverter
    fun toAccessLevel(value: String?): AccessLevel = AccessLevel.fromStorage(value)
}
