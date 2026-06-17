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

private val Context.waterMeAiCareQuotaDataStore by preferencesDataStore(
    name = "waterme_ai_care_quota",
)

data class AiCareQuotaSnapshot(
    val requestsUsed: Int,
    val requestLimit: Int,
    val resetAtMillis: Long,
) {
    val remainingRequests: Int
        get() = (requestLimit - requestsUsed).coerceAtLeast(0)

    val isExhausted: Boolean
        get() = remainingRequests <= 0
}

sealed interface AiCareQuotaConsumeResult {
    val snapshot: AiCareQuotaSnapshot

    data class Consumed(
        override val snapshot: AiCareQuotaSnapshot,
    ) : AiCareQuotaConsumeResult

    data class Exhausted(
        override val snapshot: AiCareQuotaSnapshot,
    ) : AiCareQuotaConsumeResult
}

class AiCareQuotaDataStoreManager(
    context: Context,
    private val clock: Clock = Clock.systemDefaultZone(),
) {
    private val dataStore: DataStore<Preferences> = context.applicationContext.waterMeAiCareQuotaDataStore

    suspend fun currentQuota(userId: String): AiCareQuotaSnapshot {
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

    suspend fun consumeRequest(userId: String): AiCareQuotaConsumeResult {
        var result: AiCareQuotaConsumeResult? = null
        dataStore.edit { preferences ->
            val snapshot = preferences.toQuotaSnapshot(userId)
            val activeSnapshot = if (snapshot.resetAtMillis <= nowMillis()) {
                AiCareQuotaSnapshot(
                    requestsUsed = 0,
                    requestLimit = RequestLimit,
                    resetAtMillis = nextResetAtMillis(),
                )
            } else {
                snapshot
            }

            if (activeSnapshot.isExhausted) {
                preferences.persistQuota(userId, activeSnapshot)
                result = AiCareQuotaConsumeResult.Exhausted(activeSnapshot)
                return@edit
            }

            val consumedSnapshot = activeSnapshot.copy(requestsUsed = activeSnapshot.requestsUsed + 1)
            preferences.persistQuota(userId, consumedSnapshot)
            result = AiCareQuotaConsumeResult.Consumed(consumedSnapshot)
        }
        return requireNotNull(result)
    }

    private suspend fun resetQuota(userId: String): AiCareQuotaSnapshot {
        val snapshot = AiCareQuotaSnapshot(
            requestsUsed = 0,
            requestLimit = RequestLimit,
            resetAtMillis = nextResetAtMillis(),
        )
        dataStore.edit { preferences ->
            preferences.persistQuota(userId, snapshot)
        }
        return snapshot
    }

    private fun Preferences.toQuotaSnapshot(userId: String): AiCareQuotaSnapshot {
        val keys = AiCareQuotaPreferenceKeys.forUser(userId)
        return AiCareQuotaSnapshot(
            requestsUsed = (this[keys.requestsUsed] ?: 0).coerceIn(0, RequestLimit),
            requestLimit = RequestLimit,
            resetAtMillis = this[keys.resetAtMillis] ?: nextResetAtMillis(),
        )
    }

    private fun androidx.datastore.preferences.core.MutablePreferences.persistQuota(
        userId: String,
        snapshot: AiCareQuotaSnapshot,
    ) {
        val keys = AiCareQuotaPreferenceKeys.forUser(userId)
        this[keys.requestsUsed] = snapshot.requestsUsed.coerceIn(0, snapshot.requestLimit)
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
        const val RequestLimit = 3
    }
}

private data class AiCareQuotaPreferenceKeys(
    val requestsUsed: Preferences.Key<Int>,
    val resetAtMillis: Preferences.Key<Long>,
) {
    companion object {
        fun forUser(userId: String): AiCareQuotaPreferenceKeys {
            val keySegment = userId.toPreferenceKeySegment()
            return AiCareQuotaPreferenceKeys(
                requestsUsed = intPreferencesKey("ai_care_${keySegment}_requests_used"),
                resetAtMillis = longPreferencesKey("ai_care_${keySegment}_reset_at_millis"),
            )
        }
    }
}

private fun String.toPreferenceKeySegment(): String =
    map { char ->
        if (char.isLetterOrDigit() || char == '_') char else '_'
    }.joinToString(separator = "")
