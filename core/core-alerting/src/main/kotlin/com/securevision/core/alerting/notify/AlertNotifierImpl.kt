package com.securevision.core.alerting.notify

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.securevision.core.alerting.R
import com.securevision.core.domain.alerting.AlertNotifier
import com.securevision.core.domain.alerting.NotificationOutcome
import com.securevision.core.model.AlertRecord
import com.securevision.core.model.AlertType
import com.securevision.core.model.Severity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

/**
 * Posts alerts to the system shade.
 *
 * Never throws. A refused permission is an ordinary state this app has to keep
 * working in — the alert is already recorded by the time this runs — so the
 * outcome is returned and the caller decides whether to explain it.
 */
@Singleton
class AlertNotifierImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val channels: NotificationChannels,
) : AlertNotifier {

    private val notificationManager = NotificationManagerCompat.from(context)

    override suspend fun post(alert: AlertRecord, label: String): NotificationOutcome {
        // Checked inline rather than in a helper. Lint traces a permission guard
        // only within the method that needs it, and a guard it cannot see is a
        // guard the next person will assume is missing.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return NotificationOutcome.PERMISSION_DENIED
        }

        // Separate from the permission: a user can leave the permission granted
        // and still switch the app's notifications off in system settings.
        if (!notificationManager.areNotificationsEnabled()) {
            return NotificationOutcome.PERMISSION_DENIED
        }

        channels.register()

        return runCatching {
            notificationManager.notify(notificationIdFor(alert), build(alert, label))
            NotificationOutcome.POSTED
        }.getOrElse { throwable ->
            Log.w(TAG, "posting notification failed", throwable)
            NotificationOutcome.FAILED
        }
    }

    private fun build(alert: AlertRecord, label: String) = NotificationCompat
        .Builder(context, channels.channelFor(alert.severity))
        .setSmallIcon(R.drawable.ic_alert_shield)
        .setContentTitle(titleFor(alert.type))
        .setContentText(bodyFor(alert, label))
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setPriority(priorityFor(alert))
        .setWhen(alert.timestamp)
        .setShowWhen(true)
        .setAutoCancel(true)
        .setContentIntent(launchIntent())
        .apply {
            // The snapshot is the whole point of the notification for a weapon or
            // a stranger: it lets the user decide whether to act without unlocking
            // the phone. Absent or unreadable, the notification still posts.
            snapshotOf(alert)?.let { image ->
                setLargeIcon(image)
                setStyle(
                    NotificationCompat.BigPictureStyle()
                        .bigPicture(image)
                        // Collapsing the large icon avoids showing the same crop
                        // twice once the notification is expanded.
                        .bigLargeIcon(null as Bitmap?),
                )
            }
        }
        .build()

    private fun snapshotOf(alert: AlertRecord): Bitmap? {
        val uri = alert.snapshotUri ?: return null

        return runCatching {
            context.contentResolver.openInputStream(Uri.parse(uri)).use { stream ->
                BitmapFactory.decodeStream(stream)
            }
        }.getOrElse { throwable ->
            Log.w(TAG, "snapshot could not be read for the notification", throwable)
            null
        }
    }

    /**
     * Opens the app.
     *
     * Resolved by package rather than by naming the activity, because
     * `core-alerting` sits below the application module and must not know that
     * `MainActivity` exists. Deep-linking to the individual alert waits for the
     * alerts gallery in Phase 6 — there is currently no destination to land on.
     */
    private fun launchIntent(): PendingIntent? {
        val intent = context.packageManager
            .getLaunchIntentForPackage(context.packageName)
            ?: return null

        return PendingIntent.getActivity(
            context,
            LAUNCH_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /**
     * Pre-channel priority, for API levels where the channel's importance is not
     * what decides whether a notification interrupts.
     *
     * Channels have governed this since API 26, but `NotificationCompat` still
     * reads the priority on older devices, and leaving it at default would demote
     * a weapon alert to a silent line in the shade there.
     */
    private fun priorityFor(alert: AlertRecord): Int = when {
        alert.severity.isAtLeast(Severity.CRITICAL) -> NotificationCompat.PRIORITY_MAX
        alert.severity.isAtLeast(Severity.HIGH) -> NotificationCompat.PRIORITY_DEFAULT
        else -> NotificationCompat.PRIORITY_LOW
    }

    private fun titleFor(type: AlertType): String = context.getString(
        when (type) {
            AlertType.WEAPON -> R.string.notification_weapon_title
            AlertType.UNKNOWN_PERSON -> R.string.notification_unknown_title
            AlertType.MOTION -> R.string.notification_motion_title
            AlertType.KNOWN_PERSON -> R.string.notification_known_title
        },
    )

    private fun bodyFor(alert: AlertRecord, label: String): String {
        val camera = context.getString(
            if (alert.cameraFacing == FRONT) R.string.camera_front else R.string.camera_back,
        )

        return when (alert.type) {
            AlertType.WEAPON ->
                context.getString(R.string.notification_weapon_body, label.weaponName(), camera)
            AlertType.UNKNOWN_PERSON ->
                context.getString(R.string.notification_unknown_body, camera)
            AlertType.MOTION ->
                context.getString(R.string.notification_motion_body, camera)
            AlertType.KNOWN_PERSON ->
                context.getString(R.string.notification_known_body, camera)
        }
    }

    /**
     * The weapon class, capitalised for the notification line.
     *
     * The detector's own label is used rather than a localised lookup: the class
     * set comes from the model, so a table here would fall back to blank for any
     * class a future model adds. Blank when the label is somehow empty, so the
     * sentence never reads "  seen on the back camera".
     */
    private fun String.weaponName(): String = ifBlank { DEFAULT_WEAPON_LABEL }
        .replaceFirstChar { first -> first.uppercase() }

    /**
     * A stable id per alert kind, so a second weapon sighting replaces the first
     * rather than stacking a pile of near-identical notifications.
     *
     * Derived from the type, not the alert id, which is a fresh UUID each time.
     */
    private fun notificationIdFor(alert: AlertRecord): Int =
        BASE_NOTIFICATION_ID + abs(alert.type.ordinal)

    private companion object {
        const val TAG = "AlertNotifier"
        const val FRONT = "front"
        const val LAUNCH_REQUEST_CODE = 1001
        const val BASE_NOTIFICATION_ID = 4200
        const val DEFAULT_WEAPON_LABEL = "Weapon"
    }
}
