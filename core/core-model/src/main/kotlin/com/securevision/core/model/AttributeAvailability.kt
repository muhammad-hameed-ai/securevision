package com.securevision.core.model

/**
 * Which attribute classifiers are actually loaded.
 *
 * Reported per attribute rather than as a single "attributes on/off" flag,
 * because they are backed by separate models that can be supplied
 * independently. Without this the UI cannot distinguish "the beard classifier
 * is not installed" from "the beard classifier looked and found none" — and
 * conflating those is precisely what produces a false claim in a notification.
 *
 * @property age Whether age estimation is available.
 * @property gender Whether gender estimation is available.
 * @property emotion Whether emotion classification is available.
 * @property beard Whether beard detection is available.
 * @property mask Whether face-covering detection is available.
 */
data class AttributeAvailability(
    val age: Boolean = false,
    val gender: Boolean = false,
    val emotion: Boolean = false,
    val beard: Boolean = false,
    val mask: Boolean = false,
) {
    /** `true` when at least one classifier can run. */
    val hasAny: Boolean get() = age || gender || emotion || beard || mask

    /** Human-readable summary for the diagnostics log. */
    fun describe(): String = buildList {
        if (age) add("age")
        if (gender) add("gender")
        if (emotion) add("emotion")
        if (beard) add("beard")
        if (mask) add("mask")
    }.joinToString().ifEmpty { "none" }

    companion object {
        /** No classifier is loaded; every attribute reports as not assessed. */
        val NONE = AttributeAvailability()
    }
}
