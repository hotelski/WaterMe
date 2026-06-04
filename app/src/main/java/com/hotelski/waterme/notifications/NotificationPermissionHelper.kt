package com.hotelski.waterme.notifications

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat

object NotificationPermissionHelper {
    val permission: String
        get() = POST_NOTIFICATIONS_PERMISSION

    fun requiresRuntimePermission(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    fun canPostNotifications(context: Context): Boolean =
        !requiresRuntimePermission() ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    private const val POST_NOTIFICATIONS_PERMISSION = "android.permission.POST_NOTIFICATIONS"
}
