package com.hotelski.waterme.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.time.Clock
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.waterMePlantScannerQuotaDataStore by preferencesDataStore(
    name = "waterme_plant_scanner_quota",
)

data class PlantScannerQuotaSnapshot(
    val scansUsed: Int,
    val scanLimit: Int,
    val resetAtMillis: Long,
) {
    val remainingScans: Int
        get() = (scanLimit - scansUsed).coerceAtLeast(0)

    val isExhausted: Boolean
        get() = remainingScans <= 0
}

sealed interface PlantScannerQuotaConsumeResult {
    val snapshot: PlantScannerQuotaSnapshot

    data class Consumed(
        override val snapshot: PlantScannerQuotaSnapshot,
    ) : PlantScannerQuotaConsumeResult

    data class Exhausted(
        override val snapshot: PlantScannerQuotaSnapshot,
    ) : PlantScannerQuotaConsumeResult
}

class PlantScannerQuotaDataStoreManager(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val dataStore: DataStore<Preferences> = context.applicationContext.waterMePlantScannerQuotaDataStore

    fun observeTotalScans(userId: String): Flow<Int> =
        dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .map { preferences -> preferences.toTotalScans(userId) }

    suspend fun currentQuota(userId: String): PlantScannerQuotaSnapshot {
        val preferences = dataStore.data
            .catch { error ->
                if (error is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw error
                }
            }
            .first()

        val snapshot = preferences.toQuotaSnapshot(userId)
        if (snapshot.resetAtMillis > nowMillis()) {
            return snapshot
        }

        return resetQuota(userId)
    }

    suspend fun consumeScan(userId: String): PlantScannerQuotaConsumeResult {
        var result: PlantScannerQuotaConsumeResult? = null
        dataStore.edit { preferences ->
            val snapshot = preferences.toQuotaSnapshot(userId)
            val activeSnapshot = if (snapshot.resetAtMillis <= nowMillis()) {
                PlantScannerQuotaSnapshot(
                    scansUsed = 0,
                    scanLimit = ScanLimit,
                    resetAtMillis = nextResetAtMillis(),
                )
            } else {
                snapshot
            }

            if (activeSnapshot.isExhausted) {
                preferences.persistQuota(userId, activeSnapshot)
                result = PlantScannerQuotaConsumeResult.Exhausted(activeSnapshot)
                return@edit
            }

            val consumedSnapshot = activeSnapshot.copy(scansUsed = activeSnapshot.scansUsed + 1)
            preferences.persistQuota(userId, consumedSnapshot)
            preferences.incrementTotalScans(userId, activeSnapshot)
            result = PlantScannerQuotaConsumeResult.Consumed(consumedSnapshot)
        }
        return requireNotNull(result)
    }

    private suspend fun resetQuota(userId: String): PlantScannerQuotaSnapshot {
        val snapshot = PlantScannerQuotaSnapshot(
            scansUsed = 0,
            scanLimit = ScanLimit,
            resetAtMillis = nextResetAtMillis(),
        )
        dataStore.edit { preferences ->
            preferences.persistQuota(userId, snapshot)
        }
        return snapshot
    }

    private fun Preferences.toQuotaSnapshot(userId: String): PlantScannerQuotaSnapshot {
        val keys = PlantScannerQuotaPreferenceKeys.forUser(userId)
        return PlantScannerQuotaSnapshot(
            scansUsed = (this[keys.scansUsed] ?: 0).coerceIn(0, ScanLimit),
            scanLimit = ScanLimit,
            resetAtMillis = this[keys.resetAtMillis] ?: nextResetAtMillis(),
        )
    }

    private fun Preferences.toTotalScans(userId: String): Int {
        val keys = PlantScannerQuotaPreferenceKeys.forUser(userId)
        val persistedTotal = this[keys.totalScans]
        if (persistedTotal != null) return persistedTotal.coerceAtLeast(0)

        val resetAtMillis = this[keys.resetAtMillis] ?: nextResetAtMillis()
        return if (resetAtMillis > nowMillis()) {
            (this[keys.scansUsed] ?: 0).coerceIn(0, ScanLimit)
        } else {
            0
        }
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.persistQuota(
        userId: String,
        snapshot: PlantScannerQuotaSnapshot,
    ) {
        val keys = PlantScannerQuotaPreferenceKeys.forUser(userId)
        this[keys.scansUsed] = snapshot.scansUsed.coerceIn(0, snapshot.scanLimit)
        this[keys.resetAtMillis] = snapshot.resetAtMillis
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.incrementTotalScans(
        userId: String,
        activeSnapshot: PlantScannerQuotaSnapshot,
    ) {
        val keys = PlantScannerQuotaPreferenceKeys.forUser(userId)
        val existingTotal = this[keys.totalScans] ?: activeSnapshot.scansUsed
        this[keys.totalScans] = (existingTotal + 1).coerceAtLeast(0)
    }

    private fun nowMillis(): Long = clock.millis()

    private fun nextResetAtMillis(): Long {
        val zone = clock.zone
        return Instant.ofEpochMilli(nowMillis())
            .atZone(zone)
            .toLocalDate()
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
            .toEpochMilli()
    }

    private companion object {
        const val ScanLimit = 3
    }
}

private data class PlantScannerQuotaPreferenceKeys(
    val scansUsed: Preferences.Key<Int>,
    val resetAtMillis: Preferences.Key<Long>,
    val totalScans: Preferences.Key<Int>,
) {
    companion object {
        fun forUser(userId: String): PlantScannerQuotaPreferenceKeys {
            val keySegment = userId.toPreferenceKeySegment()
            return PlantScannerQuotaPreferenceKeys(
                scansUsed = intPreferencesKey("plant_scanner_${keySegment}_scans_used"),
                resetAtMillis = longPreferencesKey("plant_scanner_${keySegment}_reset_at_millis"),
                totalScans = intPreferencesKey("plant_scanner_${keySegment}_total_scans"),
            )
        }
    }
}

private fun String.toPreferenceKeySegment(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') char else '_'
    }.joinToString(separator = "")
