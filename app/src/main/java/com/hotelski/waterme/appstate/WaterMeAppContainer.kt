package com.hotelski.waterme.appstate

import android.content.Context
import com.hotelski.waterme.data.local.WaterMeDatabase
import com.hotelski.waterme.data.local.WaterMeSeedData
import com.hotelski.waterme.data.repository.RoomCareRepository
import com.hotelski.waterme.data.repository.RoomPlantRepository
import com.hotelski.waterme.data.repository.RoomReminderRepository
import com.hotelski.waterme.data.repository.RoomSettingsRepository

object WaterMeAppContainer {
    const val LOCAL_USER_ID = WaterMeSeedData.LOCAL_USER_ID

    @Volatile
    private var databaseInstance: WaterMeDatabase? = null

    fun database(context: Context): WaterMeDatabase =
        databaseInstance ?: synchronized(this) {
            databaseInstance ?: WaterMeDatabase.build(context).also { databaseInstance = it }
        }

    fun plantRepository(context: Context): RoomPlantRepository =
        RoomPlantRepository(database(context))

    fun reminderRepository(context: Context): RoomReminderRepository =
        RoomReminderRepository(database(context))

    fun careRepository(context: Context): RoomCareRepository =
        RoomCareRepository(database(context))

    fun settingsRepository(context: Context): RoomSettingsRepository =
        RoomSettingsRepository(database(context))

    suspend fun seedIfEmpty(context: Context) {
        WaterMeSeedData.seedIfEmpty(database(context))
    }
}
