package com.hotelski.waterme.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.hotelski.waterme.model.CareReminder
import com.hotelski.waterme.model.Plant
import java.time.LocalTime
import java.time.ZoneId

object ReminderScheduler {
    private val reminderTime = LocalTime.of(9, 0)

    fun scheduleAll(context: Context, plants: List<Plant>) {
        plants.forEach { plant ->
            plant.reminders
                .filter { it.enabled }
                .forEach { schedule(context, plant, it) }
        }
    }

    fun schedule(context: Context, plant: Plant, reminder: CareReminder) {
        val alarmManager = context.getSystemService(AlarmManager::class.java)
        val pendingIntent = pendingIntent(context, plant, reminder)
        val triggerAt = reminder.nextDueDate
            .atTime(reminderTime)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
            .coerceAtLeast(System.currentTimeMillis() + SAME_DAY_DELAY_MS)

        alarmManager.cancel(pendingIntent)
        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
    }

    private fun pendingIntent(
        context: Context,
        plant: Plant,
        reminder: CareReminder,
    ): PendingIntent {
        val requestCode = requestCode(plant.id, reminder.type.name)
        val intent = Intent(context, CareReminderReceiver::class.java).apply {
            putExtra(CareReminderReceiver.EXTRA_PLANT_NAME, plant.name)
            putExtra(CareReminderReceiver.EXTRA_CARE_TYPE, reminder.type.label.lowercase())
            putExtra(CareReminderReceiver.EXTRA_REQUEST_CODE, requestCode)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun requestCode(plantId: String, type: String): Int =
        "$plantId:$type".hashCode()

    private const val SAME_DAY_DELAY_MS = 15_000L
}
