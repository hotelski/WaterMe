package com.hotelski.waterme.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.waterMePlantScannerHistoryDataStore by preferencesDataStore(
    name = "waterme_plant_scanner_history",
)

data class PlantScannerHistoryEntry(
    val commonName: String,
    val scientificName: String,
    val confidencePercent: Int,
    val photoUri: String?,
    val scannedAtMillis: Long,
)

class PlantScannerHistoryDataStoreManager(
    context: Context,
) {
    private val dataStore: DataStore<Preferences> = context.applicationContext.waterMePlantScannerHistoryDataStore

    fun observeRecentScans(userId: String): Flow<List<PlantScannerHistoryEntry>> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences -> preferences.toRecentScans(userId) }

    suspend fun recordScan(
        userId: String,
        entry: PlantScannerHistoryEntry,
    ) {
        dataStore.edit { preferences ->
            val updatedEntries = (listOf(entry) + preferences.toRecentScans(userId))
                .take(MaxRecentScans)
            preferences.persistRecentScans(userId, updatedEntries)
        }
    }

    suspend fun clearHistory(userId: String) {
        dataStore.edit { preferences ->
            PlantScannerHistoryPreferenceKeys.forUser(userId)
                .forEach { keys -> keys.removeFrom(preferences) }
        }
    }

    private fun Preferences.toRecentScans(userId: String): List<PlantScannerHistoryEntry> =
        PlantScannerHistoryPreferenceKeys.forUser(userId)
            .mapNotNull { keys ->
                val commonName = this[keys.commonName]?.takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val scientificName = this[keys.scientificName].orEmpty()
                val scannedAtMillis = this[keys.scannedAtMillis] ?: return@mapNotNull null
                PlantScannerHistoryEntry(
                    commonName = commonName,
                    scientificName = scientificName,
                    confidencePercent = (this[keys.confidencePercent] ?: 0).coerceIn(0, 100),
                    photoUri = this[keys.photoUri]?.takeIf { it.isNotBlank() },
                    scannedAtMillis = scannedAtMillis,
                )
            }
            .sortedByDescending { it.scannedAtMillis }
            .take(MaxRecentScans)

    private fun androidx.datastore.preferences.core.MutablePreferences.persistRecentScans(
        userId: String,
        entries: List<PlantScannerHistoryEntry>,
    ) {
        PlantScannerHistoryPreferenceKeys.forUser(userId).forEachIndexed { index, keys ->
            val entry = entries.getOrNull(index)
            if (entry == null) {
                keys.removeFrom(this)
            } else {
                this[keys.commonName] = entry.commonName
                this[keys.scientificName] = entry.scientificName
                this[keys.confidencePercent] = entry.confidencePercent.coerceIn(0, 100)
                this[keys.photoUri] = entry.photoUri.orEmpty()
                this[keys.scannedAtMillis] = entry.scannedAtMillis
            }
        }
    }

    private companion object {
        const val MaxRecentScans = 3
    }
}

private data class PlantScannerHistoryPreferenceKeys(
    val commonName: Preferences.Key<String>,
    val scientificName: Preferences.Key<String>,
    val confidencePercent: Preferences.Key<Int>,
    val photoUri: Preferences.Key<String>,
    val scannedAtMillis: Preferences.Key<Long>,
) {
    fun removeFrom(preferences: MutablePreferences) {
        preferences.remove(commonName)
        preferences.remove(scientificName)
        preferences.remove(confidencePercent)
        preferences.remove(photoUri)
        preferences.remove(scannedAtMillis)
    }

    companion object {
        fun forUser(userId: String): List<PlantScannerHistoryPreferenceKeys> {
            val keySegment = userId.toPreferenceKeySegment()
            return List(3) { index ->
                PlantScannerHistoryPreferenceKeys(
                    commonName = stringPreferencesKey("plant_scanner_${keySegment}_history_${index}_common_name"),
                    scientificName = stringPreferencesKey("plant_scanner_${keySegment}_history_${index}_scientific_name"),
                    confidencePercent = intPreferencesKey(
                        "plant_scanner_${keySegment}_history_${index}_confidence_percent",
                    ),
                    photoUri = stringPreferencesKey("plant_scanner_${keySegment}_history_${index}_photo_uri"),
                    scannedAtMillis = longPreferencesKey("plant_scanner_${keySegment}_history_${index}_scanned_at"),
                )
            }
        }
    }
}

private fun String.toPreferenceKeySegment(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') char else '_'
    }.joinToString(separator = "")
