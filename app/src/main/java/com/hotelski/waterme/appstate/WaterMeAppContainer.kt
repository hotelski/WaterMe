package com.hotelski.waterme.appstate

import android.content.Context
import com.hotelski.waterme.data.aiplantcare.AiCareCacheRepository
import com.hotelski.waterme.data.aiplantcare.AiPlantCareRepository
import com.hotelski.waterme.data.aiplantcare.FirebaseAiPlantCareRepository
import com.hotelski.waterme.data.aiplantcare.RoomAiCareCacheRepository
import com.hotelski.waterme.data.billing.BillingRepository
import com.hotelski.waterme.data.billing.PlayBillingRepository
import com.hotelski.waterme.data.feedback.FeedbackRepository
import com.hotelski.waterme.data.feedback.HttpFeedbackRepository
import com.hotelski.waterme.data.local.WaterMeDatabase
import com.hotelski.waterme.data.local.WaterMeSeedData
import com.hotelski.waterme.data.plantnet.PlantIdentificationRepository
import com.hotelski.waterme.data.plantnet.PlantNetPlantIdentificationRepository
import com.hotelski.waterme.data.preferences.AiCareQuotaDataStoreManager
import com.hotelski.waterme.data.preferences.PlantScannerQuotaDataStoreManager
import com.hotelski.waterme.data.preferences.SettingsDataStoreManager
import com.hotelski.waterme.data.repository.RoomCareRepository
import com.hotelski.waterme.data.repository.RoomPlantRepository
import com.hotelski.waterme.data.repository.RoomReminderRepository
import com.hotelski.waterme.data.repository.RoomSettingsRepository
import com.hotelski.waterme.notifications.ReminderNotificationCoordinator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object WaterMeAppContainer {
    const val LOCAL_USER_ID = WaterMeSeedData.LOCAL_USER_ID

    @Volatile
    private var databaseInstance: WaterMeDatabase? = null
    @Volatile
    private var settingsDataStoreInstance: SettingsDataStoreManager? = null
    @Volatile
    private var plantScannerQuotaDataStoreInstance: PlantScannerQuotaDataStoreManager? = null
    @Volatile
    private var aiCareQuotaDataStoreInstance: AiCareQuotaDataStoreManager? = null
    @Volatile
    private var feedbackRepositoryInstance: FeedbackRepository? = null
    @Volatile
    private var plantIdentificationRepositoryInstance: PlantIdentificationRepository? = null
    @Volatile
    private var aiPlantCareRepositoryInstance: AiPlantCareRepository? = null
    @Volatile
    private var aiCareCacheRepositoryInstance: AiCareCacheRepository? = null
    @Volatile
    private var billingRepositoryInstance: BillingRepository? = null

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

    fun settingsDataStore(context: Context): SettingsDataStoreManager =
        settingsDataStoreInstance ?: synchronized(this) {
            settingsDataStoreInstance ?: SettingsDataStoreManager(context).also { settingsDataStoreInstance = it }
        }

    fun plantScannerQuotaDataStore(context: Context): PlantScannerQuotaDataStoreManager =
        plantScannerQuotaDataStoreInstance ?: synchronized(this) {
            plantScannerQuotaDataStoreInstance
                ?: PlantScannerQuotaDataStoreManager(context).also { plantScannerQuotaDataStoreInstance = it }
        }

    fun aiCareQuotaDataStore(context: Context): AiCareQuotaDataStoreManager =
        aiCareQuotaDataStoreInstance ?: synchronized(this) {
            aiCareQuotaDataStoreInstance
                ?: AiCareQuotaDataStoreManager(context).also { aiCareQuotaDataStoreInstance = it }
        }

    fun reminderNotificationCoordinator(context: Context): ReminderNotificationCoordinator =
        ReminderNotificationCoordinator(
            context = context.applicationContext,
            database = database(context),
            settingsDataStore = settingsDataStore(context),
        )

    fun feedbackRepository(): FeedbackRepository =
        feedbackRepositoryInstance ?: synchronized(this) {
            feedbackRepositoryInstance ?: HttpFeedbackRepository().also { feedbackRepositoryInstance = it }
        }

    fun plantIdentificationRepository(context: Context): PlantIdentificationRepository =
        plantIdentificationRepositoryInstance ?: synchronized(this) {
            plantIdentificationRepositoryInstance
                ?: PlantNetPlantIdentificationRepository(context).also {
                    plantIdentificationRepositoryInstance = it
                }
        }

    fun aiPlantCareRepository(): AiPlantCareRepository =
        aiPlantCareRepositoryInstance ?: synchronized(this) {
            aiPlantCareRepositoryInstance
                ?: FirebaseAiPlantCareRepository().also { aiPlantCareRepositoryInstance = it }
        }

    fun aiCareCacheRepository(context: Context): AiCareCacheRepository =
        aiCareCacheRepositoryInstance ?: synchronized(this) {
            aiCareCacheRepositoryInstance
                ?: RoomAiCareCacheRepository(database(context)).also { aiCareCacheRepositoryInstance = it }
        }

    fun billingRepository(context: Context): BillingRepository =
        billingRepositoryInstance ?: synchronized(this) {
            billingRepositoryInstance ?: PlayBillingRepository(context).also { billingRepositoryInstance = it }
        }

    suspend fun seedIfEmpty(context: Context) {
        if (settingsDataStore(context).shouldSkipSeedData()) return
        val seeded = WaterMeSeedData.seedIfEmpty(database(context))
        if (seeded) {
            reminderNotificationCoordinator(context).syncScheduledReminders()
        }
    }

    suspend fun deleteAllData(context: Context) {
        reminderNotificationCoordinator(context).cancelScheduledReminders()
        withContext(Dispatchers.IO) {
            val database = database(context)
            database.clearAllTables()
            WaterMeSeedData.ensureLocalUser(database)
        }
    }
}
