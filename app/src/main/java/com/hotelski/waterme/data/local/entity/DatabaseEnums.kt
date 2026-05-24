package com.hotelski.waterme.data.local.entity

enum class BackupSyncProvider {
    NONE,
    GOOGLE_DRIVE,
    CLOUD_SYNC,
}

enum class HistoryAction {
    COMPLETED,
    SKIPPED,
    SNOOZED,
    MANUAL_LOG,
    HEALTH_NOTE,
}

enum class MeasurementUnits {
    METRIC,
    IMPERIAL,
}

enum class NotificationPermissionState {
    NOT_REQUESTED,
    GRANTED,
    DENIED,
}

enum class TaskStatus {
    PENDING,
    COMPLETED,
    SKIPPED,
    SNOOZED,
    CANCELED,
}

enum class ThemePreference {
    SYSTEM,
    LIGHT,
    DARK,
}
