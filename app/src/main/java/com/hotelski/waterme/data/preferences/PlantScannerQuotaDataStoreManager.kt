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
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

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

    private fun androidx.datastore.preferences.core.MutablePreferences.persistQuota(
        userId: String,
        snapshot: PlantScannerQuotaSnapshot,
    ) {
        val keys = PlantScannerQuotaPreferenceKeys.forUser(userId)
        this[keys.scansUsed] = snapshot.scansUsed.coerceIn(0, snapshot.scanLimit)
        this[keys.resetAtMillis] = snapshot.resetAtMillis
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
) {
    companion object {
        fun forUser(userId: String): PlantScannerQuotaPreferenceKeys {
            val keySegment = userId.toPreferenceKeySegment()
            return PlantScannerQuotaPreferenceKeys(
                scansUsed = intPreferencesKey("plant_scanner_${keySegment}_scans_used"),
                resetAtMillis = longPreferencesKey("plant_scanner_${keySegment}_reset_at_millis"),
            )
        }
    }
}

private fun String.toPreferenceKeySegment(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') char else '_'
    }.joinToString(separator = "")
