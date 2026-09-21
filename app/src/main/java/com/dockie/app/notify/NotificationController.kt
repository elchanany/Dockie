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
    const val CHANNEL_ID = "dockie_status"
    const val NOTIFICATION_ID = 1001

    const val ACTION_DISABLE = "com.dockie.app.ACTION_DISABLE"
    const val ACTION_OPEN = "com.dockie.app.ACTION_OPEN"

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val existing = manager.getNotificationChannel(CHANNEL_ID)
        if (existing != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.notification_channel_desc)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    fun build(context: Context, docked: Boolean): Notification {
        ensureChannel(context)

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
        val text = if (docked) {
            context.getString(R.string.notification_active)
        } else {
            context.getString(R.string.notification_monitoring)
        }
        val icon = if (docked) R.drawable.ic_notification_active else R.drawable.ic_notification_idle

        return NotificationCompat.Builder(context, CHANNEL_ID)
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
}
