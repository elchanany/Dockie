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

object NotificationController {
    const val CHANNEL_STATUS = "dockie_status"
    const val CHANNEL_ALERT = "dockie_alert"
    const val NOTIFICATION_ID = 1001
    const val ALERT_NOTIFICATION_ID = 1002

    const val ACTION_DISABLE = "com.dockie.app.ACTION_DISABLE"
    const val ACTION_OPEN = "com.dockie.app.ACTION_OPEN"

    /**
     * Ongoing status channel. [withIcon] controls IMPORTANCE_LOW (status-bar
     * icon visible) vs IMPORTANCE_MIN (notification lives only in the shade).
     * Android keeps the importance once a channel exists, so a changed
     * preference deletes and recreates the channel.
     */
    fun syncStatusChannel(context: Context, withIcon: Boolean) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val want = if (withIcon) {
            NotificationManager.IMPORTANCE_LOW
        } else {
            NotificationManager.IMPORTANCE_MIN
        }
        val existing = manager.getNotificationChannel(CHANNEL_STATUS)
        if (existing != null && existing.importance == want) return
        if (existing != null) manager.deleteNotificationChannel(CHANNEL_STATUS)
        val channel = NotificationChannel(
            CHANNEL_STATUS,
            context.getString(R.string.notification_channel_name),
            want,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
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

    /** Backwards-compatible entry point used by DockieApp. */
    fun ensureChannel(context: Context) {
        syncStatusChannel(context, withIcon = true)
        ensureAlertChannel(context)
    }

    fun build(
        context: Context,
        docked: Boolean,
        withIcon: Boolean = true,
        statusExtra: String? = null,
    ): Notification {
        syncStatusChannel(context, withIcon)

        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val disableIntent = Intent(context, DockMonitoringService.DisableActionReceiver::class.java)
            .setAction(ACTION_DISABLE)
        val disablePending = PendingIntent.getBroadcast(
            context, 1, disableIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = context.getString(R.string.app_name)
        var text = if (docked) {
            context.getString(R.string.notification_active)
        } else {
            context.getString(R.string.notification_monitoring)
        }
        if (!statusExtra.isNullOrBlank()) text += " · $statusExtra"
        val icon = if (docked) R.drawable.ic_notification_active else R.drawable.ic_notification_idle

        return NotificationCompat.Builder(context, CHANNEL_STATUS)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(icon)
            .setContentIntent(openPending)
            .setOngoing(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .addAction(
                R.drawable.ic_notification_idle,
                context.getString(R.string.notification_action_disable),
                disablePending,
            )
            .build()
    }

    /** One-time heads-up shown when a dock session starts keeping the screen awake. */
    fun postDockedAlert(context: Context) {
        ensureAlertChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openPending = PendingIntent.getActivity(
            context, 2, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ALERT)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notification_alert_text))
            .setSmallIcon(R.drawable.ic_notification_active)
            .setContentIntent(openPending)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .build()
        try {
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.notify(ALERT_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {
            // POST_NOTIFICATIONS denied on Android 13+: the quiet status
            // notification still works; the one-time alert is simply skipped.
        } catch (_: Exception) {
        }
    }
}
