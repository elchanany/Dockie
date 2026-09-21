package com.dockie.app.power

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

object PermissionManager {
    fun hasWriteSettings(context: Context): Boolean =
        Settings.System.canWrite(context)

    fun writeSettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_WRITE_SETTINGS,
            Uri.parse("package:${context.packageName}"),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
