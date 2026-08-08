package com.securevision.core.common

/**
 * Cross-module constants that are not user-facing text and not tunable settings.
 *
 * Anything a user can read belongs in `strings.xml`; anything a user can change
 * belongs in `AppSettings`. What is left — storage names, channel ids, format
 * patterns — lives here so that two modules can never disagree about it.
 */
object Constants {

    /** Names of on-device stores. Changing one is a migration, not a rename. */
    object Storage {
        const val DATABASE_NAME = "securevision.db"
        const val SETTINGS_DATASTORE_NAME = "securevision_settings"

        /** Internal-storage directory holding enrolment photos. */
        const val PROFILE_PHOTO_DIRECTORY = "profiles"

        /** Internal-storage directory holding alert snapshots. */
        const val SNAPSHOT_DIRECTORY = "snapshots"

        /** Internal-storage directory holding recorded video. */
        const val RECORDING_DIRECTORY = "recordings"

        /**
         * JPEG quality for snapshots and enrolment photos.
         *
         * High rather than maximum: an enrolment photo is re-encoded once and then
         * only ever displayed, because recognition runs against the stored
         * embedding, never against the saved image.
         */
        const val JPEG_QUALITY = 90

        /** Extension used for stills written by [PROFILE_PHOTO_DIRECTORY] and [SNAPSHOT_DIRECTORY]. */
        const val IMAGE_EXTENSION = "jpg"

        /** Extension used for clips written to [RECORDING_DIRECTORY]. */
        const val VIDEO_EXTENSION = "mp4"
    }

    /** Notification channel identifiers, one per alert class. */
    object Notification {
        const val CHANNEL_KNOWN_PERSON = "securevision.known_person"
        const val CHANNEL_UNKNOWN_PERSON = "securevision.unknown_person"
        const val CHANNEL_WEAPON = "securevision.weapon"
        const val CHANNEL_MOTION = "securevision.motion"
        const val CHANNEL_MONITORING_SERVICE = "securevision.monitoring"
    }

    /** `DateTimeFormatter` patterns used by the formatting extensions. */
    object DateTime {
        const val DATE_TIME_PATTERN = "dd MMM yyyy, HH:mm:ss"
        const val DATE_PATTERN = "dd MMM yyyy"
        const val TIME_PATTERN = "HH:mm:ss"

        /** Sortable, filesystem-safe stamp used in generated file names. */
        const val FILE_STAMP_PATTERN = "yyyyMMdd_HHmmss"
    }

    /** Fixed characteristics of the recognition pipeline. */
    object Recognition {
        /** FaceNet-512 output dimensionality. */
        const val EMBEDDING_DIMENSIONS = 512

        /** Square input edge, in pixels, expected by the FaceNet model. */
        const val FACE_INPUT_SIZE = 160

        /**
         * Landmarks used for affine alignment before embedding: both eyes, nose
         * tip and both mouth corners. Alignment is mandatory — skipping it is what
         * collapses similarity scores toward a constant for every face.
         */
        const val ALIGNMENT_LANDMARK_COUNT = 5
    }

    /** Minimum spacing between repeated user-visible alerts of the same kind. */
    object Alerting {
        const val DUPLICATE_ALERT_WINDOW_MILLIS = 8_000L
    }
}
