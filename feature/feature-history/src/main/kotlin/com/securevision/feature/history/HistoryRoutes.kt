package com.securevision.feature.history

/**
 * Navigation identity of the detection history feature.
 *
 * Phase 6 adds the date-grouped audit trail of every detection the pipeline
 * observed — the superset of the alerts list, which shows only what the user
 * was interrupted for.
 */
object HistoryRoutes {

    /** Chronological detection log. */
    const val HISTORY = "history"
}
