package com.securevision.feature.alerts

/**
 * Navigation identity of the alerts feature.
 */
object AlertsRoutes {

    /** Gallery of raised alerts. */
    const val ALERTS = "alerts"

    /** Argument carrying which alert is open. */
    const val ARG_ALERT_ID = "alertId"

    /** One alert, with its full snapshot and metadata. */
    const val DETAIL = "alerts/{$ARG_ALERT_ID}"

    /**
     * Builds the detail route for one alert.
     *
     * @param alertId Which alert to open.
     */
    fun detail(alertId: String): String = "alerts/$alertId"
}
