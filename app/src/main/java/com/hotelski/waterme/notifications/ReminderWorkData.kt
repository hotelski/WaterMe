package com.hotelski.waterme.notifications

import androidx.work.Data
import androidx.work.workDataOf
import com.hotelski.waterme.model.CareType

data class CareReminderSchedule(
    val plantId: String,
    val plantName: String,
    val reminderId: String,
    val taskId: String,
    val careType: CareType,
    val dueAtMillis: Long,
    val frequencyDays: Int,
    val notificationsEnabled: Boolean = true,
) {
    val notificationId: Int
        get() = "$plantId:$reminderId:$taskId".hashCode()
}

enum class ReminderNotificationAction(val intentAction: String) {
    COMPLETE("com.hotelski.waterme.notifications.COMPLETE"),
    SNOOZE("com.hotelski.waterme.notifications.SNOOZE"),
    SKIP("com.hotelski.waterme.notifications.SKIP"),
}

fun CareReminderSchedule.toWorkData(): Data =
    workDataOf(
        KEY_PLANT_ID to plantId,
        KEY_PLANT_NAME to plantName,
        KEY_REMINDER_ID to reminderId,
        KEY_TASK_ID to taskId,
        KEY_CARE_TYPE to careType.name,
        KEY_DUE_AT_MILLIS to dueAtMillis,
        KEY_FREQUENCY_DAYS to frequencyDays,
        KEY_NOTIFICATIONS_ENABLED to notificationsEnabled,
    )

fun Data.toCareReminderSchedule(): CareReminderSchedule? {
    val plantId = getString(KEY_PLANT_ID) ?: return null
    val plantName = getString(KEY_PLANT_NAME) ?: return null
    val reminderId = getString(KEY_REMINDER_ID) ?: return null
    val taskId = getString(KEY_TASK_ID) ?: return null
    val careType = getString(KEY_CARE_TYPE)?.let(CareType::valueOf) ?: return null
    val dueAtMillis = getLong(KEY_DUE_AT_MILLIS, INVALID_LONG)
    val frequencyDays = getInt(KEY_FREQUENCY_DAYS, INVALID_INT)

    if (dueAtMillis == INVALID_LONG || frequencyDays == INVALID_INT) return null

    return CareReminderSchedule(
        plantId = plantId,
        plantName = plantName,
        reminderId = reminderId,
        taskId = taskId,
        careType = careType,
        dueAtMillis = dueAtMillis,
        frequencyDays = frequencyDays,
        notificationsEnabled = getBoolean(KEY_NOTIFICATIONS_ENABLED, true),
    )
}

const val KEY_PLANT_ID = "plant_id"
const val KEY_PLANT_NAME = "plant_name"
const val KEY_REMINDER_ID = "reminder_id"
const val KEY_TASK_ID = "task_id"
const val KEY_CARE_TYPE = "care_type"
const val KEY_DUE_AT_MILLIS = "due_at_millis"
const val KEY_FREQUENCY_DAYS = "frequency_days"
const val KEY_NOTIFICATIONS_ENABLED = "notifications_enabled"
const val KEY_REMINDER_ACTION = "reminder_action"
const val KEY_SNOOZE_MINUTES = "snooze_minutes"

private const val INVALID_LONG = Long.MIN_VALUE
private const val INVALID_INT = Int.MIN_VALUE
