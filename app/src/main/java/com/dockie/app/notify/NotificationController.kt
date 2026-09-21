package com.dockie.app.notify

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dockie.app.MainActivity
import com.dockie.app.R
import com.dockie.app.service.DockMonitoringService

/**
 * Three quiet channels:
 * - STATUS (LOW): ongoing notification shown ONLY while docked. This is the
 *   only one that may show a status-bar icon.
 * - MONITOR (MIN): ongoing notification while enabled but undocked. Shade-only,
 *   never a status-bar icon — so the icon reliably disappears on undock.
 * - ALERT (DEFAULT): the one-time "screen will stay awake" heads-up. It is
 *   always cancelled when the session ends (undock / disable / expiry), so
 *   no stale message ever lingers.
 */
object NotificationController {
    const val CHANNEL_STATUS = "dockie_status"
    const val CHANNEL_MONITOR = "dockie_monitor"
    const val CHANNEL_ALERT = "dockie_alert"
    const val NOTIFICATION_ID = 1001
    const val ALERT_NOTIFICATION_ID = 1002

    const val ACTION_DISABLE = "com.dockie.app.ACTION_DISABLE"
    const val ACTION_OPEN = "com.dockie.app.ACTION_OPEN"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_STATUS) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_STATUS,
                    context.getString(R.string.notification_channel_name),
                    NotificationManager.IMPORTANCE_LOW,
                ).apply {
                    description = context.getString(R.string.notification_channel_desc)
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                },
            )
        }
        if (manager.getNotificationChannel(CHANNEL_MONITOR) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_MONITOR,
                    context.getString(R.string.notification_channel_monitor_name),
                    NotificationManager.IMPORTANCE_MIN,
                ).apply {
                    description = context.getString(R.string.notification_channel_monitor_desc)
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                },
            )
        }
        ensureAlertChannel(context)
    }

    fun ensureAlertChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ALERT) != null) return
        val channel = NotificationChannel(
            CHANNEL_ALERT,
            context.getString(R.string.notification_channel_alert_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = context.getString(R.string.notification_channel_alert_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    private fun openPending(context: Context, requestCode: Int): PendingIntent {
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context, requestCode, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun disablePending(context: Context): PendingIntent {
        val disableIntent = Intent(context, DockMonitoringService.DisableActionReceiver::class.java)
            .setAction(ACTION_DISABLE)
        return PendingIntent.getBroadcast(
            context, 1, disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    /** Quiet shade-only entry while enabled but undocked. Never shows an icon. */
    fun buildMonitoring(context: Context): Notification {
        ensureChannel(context)
        return NotificationCompat.Builder(context, CHANNEL_MONITOR)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_monitoring))
            .setSmallIcon(R.drawable.ic_notification_idle)
            .setContentIntent(openPending(context, 0))
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(
                R.drawable.ic_notification_idle,
                context.getString(R.string.notification_action_disable),
                disablePending(context),
            )
            .build()
    }

    /**
     * Active entry while docked. The status-bar icon appears only here, and
     * only when [withIcon] is true — otherwise it posts shade-only too.
     */
    fun buildDocked(
        context: Context,
        withIcon: Boolean = true,
        statusExtra: String? = null,
    ): Notification {
        ensureChannel(context)
        var text = context.getString(R.string.notification_active)
        if (!statusExtra.isNullOrBlank()) text += " · $statusExtra"
        val channel = if (withIcon) CHANNEL_STATUS else CHANNEL_MONITOR
        val icon = if (withIcon) {
            R.drawable.ic_notification_active
        } else {
            R.drawable.ic_notification_idle
        }
        return NotificationCompat.Builder(context, channel)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(text)
            .setSmallIcon(icon)
            .setContentIntent(openPending(context, 0))
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(
                R.drawable.ic_notification_idle,
                context.getString(R.string.notification_action_disable),
                disablePending(context),
            )
            .build()
    }

    /** One-time heads-up shown when a dock session starts keeping the screen awake. */
    fun postDockedAlert(context: Context) {
        ensureAlertChannel(context)
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_alert_text))
            .setSmallIcon(R.drawable.ic_notification_active)
            .setContentIntent(openPending(context, 2))
            .setAutoCancel(true)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        try {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.notify(ALERT_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied on Android 13+: the quiet status entry
            // still works; the one-time alert is simply skipped.
        } catch (_: Exception) {
        }
    }

    /** Removes the one-time alert so no stale message lingers after undock. */
    fun cancelAlert(context: Context) {
        try {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.cancel(ALERT_NOTIFICATION_ID)
        } catch (_: Exception) {
        }
    }
}
