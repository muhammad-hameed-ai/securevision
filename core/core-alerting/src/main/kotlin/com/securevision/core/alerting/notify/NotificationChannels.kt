package com.securevision.core.alerting.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import com.securevision.core.alerting.R
import com.securevision.core.model.Severity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Registers the notification channels and maps a severity onto one.
 *
 * Three channels rather than one, because a channel is the only unit Android lets
 * the user tune. Someone who finds motion pings annoying can silence just those
 * without also silencing the weapon alarm — which is what would happen if every
 * alert shared a channel, and would leave the app quiet at the exact moment it
 * matters most.
 *
 * Channel importance is fixed at creation and cannot be raised afterwards, so the
 * critical channel is registered at [NotificationManager.IMPORTANCE_HIGH] from the
 * first launch.
 */
@Singleton
class NotificationChannels @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    private val manager: NotificationManager? by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        context.getSystemService(NotificationManager::class.java)
    }

    /**
     * Creates every channel.
     *
     * Idempotent: `createNotificationChannel` updates the name and description of
     * an existing channel and leaves the user's own importance choice alone, so
     * calling this on every launch is both safe and how a renamed channel reaches
     * an already-installed app.
     */
    fun register() {
        val notificationManager = manager ?: return

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_CRITICAL,
                context.getString(R.string.channel_critical_name),
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = context.getString(R.string.channel_critical_description)
                // The tone comes from AlarmPlayer, which uses the alarm stream and
                // survives Do Not Disturb. A channel sound as well would double it.
                setSound(null, null)
                enableVibration(false)
            },
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_SECURITY,
                context.getString(R.string.channel_security_name),
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = context.getString(R.string.channel_security_description)
                setSound(null, null)
                enableVibration(false)
            },
        )

        notificationManager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ACTIVITY,
                context.getString(R.string.channel_activity_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = context.getString(R.string.channel_activity_description)
                setSound(null, null)
                enableVibration(false)
            },
        )
    }

    /**
     * The channel an alert of this severity belongs to.
     *
     * @param severity Urgency of the alert.
     * @return Channel id.
     */
    fun channelFor(severity: Severity): String = when {
        severity.isAtLeast(Severity.CRITICAL) -> CHANNEL_CRITICAL
        severity.isAtLeast(Severity.HIGH) -> CHANNEL_SECURITY
        else -> CHANNEL_ACTIVITY
    }

    companion object {

        /** Weapons. Heads-up, and never silenced by the other two. */
        const val CHANNEL_CRITICAL = "securevision.critical"

        /** Unrecognised people. Ordinary importance. */
        const val CHANNEL_SECURITY = "securevision.security"

        /** Motion. Quiet, no heads-up. */
        const val CHANNEL_ACTIVITY = "securevision.activity"
    }
}
