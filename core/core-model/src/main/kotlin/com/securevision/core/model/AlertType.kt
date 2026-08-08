package com.securevision.core.model

/**
 * The category of event that raised an alert. Determines the notification
 * channel, the overlay colour and whether an alarm sound is played.
 */
enum class AlertType {

    /** An enrolled person was recognised — informational, green overlay. */
    KNOWN_PERSON,

    /** An unrecognised face was seen — red overlay, alarm until they leave frame. */
    UNKNOWN_PERSON,

    /** A weapon was detected — orange overlay, critical alarm. */
    WEAPON,

    /** Movement was detected in an otherwise static scene. */
    MOTION,
}
