package com.securevision.core.data.database.converter

import androidx.room.TypeConverter
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
}
