package com.hotelski.waterme.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {
    val permission: String
        get() = Manifest.permission.POST_NOTIFICATIONS

    fun requiresRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun canPostNotifications(context: Context): Boolean =
        !requiresRuntimePermission() ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
