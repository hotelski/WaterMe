package com.hotelski.waterme.data.aiplantcare

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.GenerativeBackend
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import com.hotelski.waterme.BuildConfig
import com.hotelski.waterme.data.ai.AiModelConfig
import com.hotelski.waterme.model.PlantCareAdvice
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONException
import org.json.JSONObject
import java.util.Locale

interface AiPlantCareRepository {
    suspend fun generateFullPlantCareAdvice(
        plantName: String,
        scientificName: String?,
    ): Result<PlantCareAdvice>

    suspend fun generatePlantCareFollowUpAnswer(
        plantName: String,
        scientificName: String?,
        careProfileSummary: String,
        savedPlantData: String?,
        question: String,
    ): Result<String>
}

sealed class AiPlantCareException(
    message: String,
    cause: Throwable? = null,
) : IllegalStateException(message, cause) {
    class TooManyRequests(cause: Throwable? = null) : AiPlantCareException(
        "The free AI limit has been reached for now. Please try again later.",
        cause,
    )

    class ServiceUnavailable(cause: Throwable? = null) : AiPlantCareException(
        "AI is temporarily busy. Please try again in a moment.",
        cause,
    )

    class NoInternet(cause: Throwable? = null) : AiPlantCareException(
        "No internet connection. Showing saved or basic care advice.",
        cause,
    )

    class Timeout(cause: Throwable? = null) : AiPlantCareException(
        "AI care advice is taking too long. Check your connection and try again.",
        cause,
    )

    class EmptyResponse(cause: Throwable? = null) : AiPlantCareException(
        "AI returned an empty response. Please try again.",
        cause,
    )

    class InvalidJson(cause: Throwable? = null) : AiPlantCareException(
        "AI returned an unexpected response. Please try again.",
        cause,
    )

    class Unknown(message: String, cause: Throwable? = null) : AiPlantCareException(message, cause)
}

class FirebaseAiPlantCareRepository : AiPlantCareRepository {
    private val model by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = AiModelConfig.GeminiFlashLiteModel,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = PlantCareAdviceJsonSchema
            },
        )
    }

    private val followUpModel by lazy {
        Firebase.ai(backend = GenerativeBackend.googleAI()).generativeModel(
            modelName = AiModelConfig.GeminiFlashLiteModel,
        )
    }

    override suspend fun generateFullPlantCareAdvice(
        plantName: String,
        scientificName: String?,
    ): Result<PlantCareAdvice> =
        runCatching {
            val normalizedPlantName = plantName.trim()
            val normalizedScientificName = scientificName?.trim()?.ifBlank { null }
            require(normalizedPlantName.isNotBlank()) { "Plant name is required." }
            if (BuildConfig.DEBUG) {
                Log.d(
                    LogTag,
                    "Requesting Gemini care advice for plant=$normalizedPlantName model=${AiModelConfig.GeminiFlashLiteModel}",
                )
            }

            retryTemporaryAiErrors {
                val response = withTimeout(RequestTimeoutMillis) {
                    model.generateContent(
                        buildPlantCareAdvicePrompt(
                            plantName = normalizedPlantName,
                            scientificName = normalizedScientificName,
                            interpretationHint = resolvePlantInterpretationHint(
                                plantName = normalizedPlantName,
                                scientificName = normalizedScientificName,
                            ),
                        ),
                    )
                }
                val rawResponse = response.text.orEmpty()
                if (BuildConfig.DEBUG) {
                    Log.d(LogTag, "Gemini care advice response received. length=${rawResponse.length}")
                }
                if (rawResponse.isBlank()) throw AiPlantCareException.EmptyResponse()
                parsePlantCareAdviceJson(
                    rawResponse = rawResponse,
                    useBulgarianFallbacks = shouldUseBulgarianResponse(normalizedPlantName),
                )
            }
        }.recoverCatching { error ->
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            if (BuildConfig.DEBUG) {
                Log.d(LogTag, "Gemini care advice request failed: ${error.message}", error)
            }
            if (error is PlantCareAdviceParseException && BuildConfig.DEBUG) {
                Log.d(LogTag, "Raw Gemini plant care response: ${error.rawResponse}", error)
            }
            throw error.toAiPlantCareException()
        }

    override suspend fun generatePlantCareFollowUpAnswer(
        plantName: String,
        scientificName: String?,
        careProfileSummary: String,
        savedPlantData: String?,
        question: String,
    ): Result<String> =
        runCatching {
            val normalizedPlantName = plantName.trim()
            val normalizedScientificName = scientificName?.trim()?.ifBlank { null }
            val normalizedQuestion = question.trim()
            require(normalizedPlantName.isNotBlank()) { "Plant name is required." }
            require(normalizedQuestion.isNotBlank()) { "Ask a question before sending." }
            if (BuildConfig.DEBUG) {
                Log.d(
                    LogTag,
                    "Requesting Gemini care follow-up for plant=$normalizedPlantName model=${AiModelConfig.GeminiFlashLiteModel}",
                )
            }

            retryTemporaryAiErrors {
                val response = withTimeout(RequestTimeoutMillis) {
                    followUpModel.generateContent(
                        buildPlantCareFollowUpPrompt(
                            plantName = normalizedPlantName,
                            scientificName = normalizedScientificName,
                            careProfileSummary = careProfileSummary,
                            savedPlantData = savedPlantData,
                            question = normalizedQuestion,
                        ),
                    )
                }
                val answer = response.text.orEmpty().trim()
                if (BuildConfig.DEBUG) {
                    Log.d(LogTag, "Gemini care follow-up response received. length=${answer.length}")
                }
                answer.ifBlank { throw AiPlantCareException.EmptyResponse() }
            }
        }.recoverCatching { error ->
            if (error is CancellationException && error !is TimeoutCancellationException) throw error
            if (BuildConfig.DEBUG) {
                Log.d(LogTag, "Gemini care follow-up request failed: ${error.message}", error)
            }
            throw error.toAiPlantCareException()
        }

    private fun parsePlantCareAdviceJson(
        rawResponse: String,
        useBulgarianFallbacks: Boolean,
    ): PlantCareAdvice {
        val jsonText = rawResponse.extractJsonObjectText()
        val json = try {
            JSONObject(jsonText)
        } catch (error: JSONException) {
            throw PlantCareAdviceParseException(rawResponse, error)
        }

        return PlantCareAdvice(
            plantName = json.optCleanString("plantName").ifBlank { fallbackPlantName(useBulgarianFallbacks) },
            scientificName = json.optCleanString("scientificName").ifBlank { null },
            shortDescription = json.optCleanString("shortDescription")
                .ifBlank { fallbackShortDescription(useBulgarianFallbacks) },
            careDifficulty = json.optCleanString("careDifficulty").ifBlank { FallbackCareDifficulty },
            matureHeight = json.optCleanString("matureHeight").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            watering = json.optCleanString("watering").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            light = json.optCleanString("light").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            temperature = json.optCleanString("temperature").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            humidity = json.optCleanString("humidity").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            fertilizing = json.optCleanString("fertilizing").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            repotting = json.optCleanString("repotting").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            flowering = json.optCleanString("flowering").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            growth = json.optCleanString("growth").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            toxicity = json.optCleanString("toxicity").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            origin = json.optCleanString("origin").ifBlank { fallbackUnknown(useBulgarianFallbacks) },
            disclaimer = json.optCleanString("disclaimer").ifBlank { fallbackDisclaimer(useBulgarianFallbacks) },
            suggestedWateringIntervalDays = json.optNullableInt("suggestedWateringIntervalDays"),
            suggestedFertilizingIntervalDays = json.optNullableInt("suggestedFertilizingIntervalDays"),
            suggestedLightLevel = json.optCleanString("suggestedLightLevel").ifBlank { null },
            suggestedNote = json.optCleanString("suggestedNote").ifBlank { null },
        )
    }

    private suspend fun <T> retryTemporaryAiErrors(block: suspend () -> T): T {
        var retryCount = 0
        while (true) {
            try {
                return block()
            } catch (error: Throwable) {
                if (error is CancellationException && error !is TimeoutCancellationException) throw error
                if (error is PlantCareAdviceParseException && BuildConfig.DEBUG) {
                    Log.d(LogTag, "Raw Gemini plant care response: ${error.rawResponse}", error)
                }
                val aiError = error.toAiPlantCareException()
                if (!aiError.canRetryWithBackoff || retryCount >= TemporaryRetryCount) {
                    throw aiError
                }
                retryCount += 1
                if (BuildConfig.DEBUG) {
                    Log.d(LogTag, "Retrying temporary AI error after backoff. attempt=$retryCount")
                }
                delay(TemporaryRetryDelayMillis * retryCount)
            }
        }
    }

    private val AiPlantCareException.canRetryWithBackoff: Boolean
        get() = this is AiPlantCareException.ServiceUnavailable || this is AiPlantCareException.Timeout

    private fun Throwable.toAiPlantCareException(): AiPlantCareException {
        if (this is AiPlantCareException) return this
        if (this is PlantCareAdviceParseException) return AiPlantCareException.InvalidJson(this)

        val causeChain = generateSequence(this as Throwable?) { it.cause }.toList()
        val combinedText = causeChain
            .joinToString(separator = " ") { error ->
                "${error::class.qualifiedName.orEmpty()} ${error.message.orEmpty()}"
            }
            .lowercase(Locale.ROOT)

        return when {
            causeChain.any { it is TimeoutCancellationException || it is SocketTimeoutException } ||
                combinedText.containsAny("timeout", "timed out", "deadline exceeded") ->
                AiPlantCareException.Timeout(this)

            causeChain.any { it is UnknownHostException || it is ConnectException } ||
                combinedText.containsAny(
                    "unable to resolve host",
                    "no address associated",
                    "failed to connect",
                    "connection refused",
                    "network error",
                    "no internet",
                ) ->
                AiPlantCareException.NoInternet(this)

            combinedText.containsAny(
                "429",
                "too many requests",
                "quota",
                "resource_exhausted",
                "free_tier",
                "rate limit",
            ) ->
                AiPlantCareException.TooManyRequests(this)

            combinedText.containsAny(
                "503",
                "service unavailable",
                "server unavailable",
                "temporarily unavailable",
                "temporarily busy",
                "model is overloaded",
                "overloaded",
                "unavailable",
            ) ->
                AiPlantCareException.ServiceUnavailable(this)

            causeChain.any { it is IOException } ->
                AiPlantCareException.NoInternet(this)

            else -> AiPlantCareException.Unknown(
                message = message?.takeIf { it.isNotBlank() }
                    ?: "WaterMe could not generate AI care advice. Please try again.",
                cause = this,
            )
        }
    }

    private class PlantCareAdviceParseException(
        val rawResponse: String,
        cause: Throwable,
    ) : IllegalStateException("Failed to parse PlantCareAdvice JSON.", cause)

    private companion object {
        const val LogTag = "AiPlantCareRepository"
        const val RequestTimeoutMillis = 30_000L
        const val TemporaryRetryCount = 1
        const val TemporaryRetryDelayMillis = 700L
        const val FallbackCareDifficulty = "Medium"

        val PlantCareAdviceJsonSchema = Schema.obj(
            mapOf(
                "plantName" to Schema.string(),
                "scientificName" to Schema.string(),
                "shortDescription" to Schema.string(),
                "careDifficulty" to Schema.enumeration(listOf("Easy", "Medium", "Difficult")),
                "matureHeight" to Schema.string(),
                "watering" to Schema.string(),
                "light" to Schema.string(),
                "temperature" to Schema.string(),
                "humidity" to Schema.string(),
                "fertilizing" to Schema.string(),
                "repotting" to Schema.string(),
                "flowering" to Schema.string(),
                "growth" to Schema.string(),
                "toxicity" to Schema.string(),
                "origin" to Schema.string(),
                "disclaimer" to Schema.string(),
                "suggestedWateringIntervalDays" to Schema.integer(nullable = true),
                "suggestedFertilizingIntervalDays" to Schema.integer(nullable = true),
                "suggestedLightLevel" to Schema.string(nullable = true),
                "suggestedNote" to Schema.string(nullable = true),
            ),
        )
    }
}

private fun buildPlantCareAdvicePrompt(
    plantName: String,
    scientificName: String?,
    interpretationHint: String?,
): String {
    val scientificNameLine = scientificName?.let {
        "Scientific name, if known: $it"
    } ?: "Scientific name, if known: unknown"
    val interpretationHintLine = interpretationHint?.let {
        "Interpretation hint: $it"
    } ?: "Interpretation hint: none"
    val responseLanguageInstruction = buildResponseLanguageInstruction(plantName)

    return """
        You are a plant care assistant inside an app called WaterMe.
        Give practical plant care advice for home users.
        Plant name as entered by the user: $plantName
        $scientificNameLine
        $interpretationHintLine
        $responseLanguageInstruction

        Return JSON only.
        Keep every field short and practical.
        Each section should be 1 to 2 sentences maximum.
        Do not use markdown.
        Do not include text before or after the JSON.
        Do not include bullet points.
        Do not claim 100% certainty.
        Keep scientificName as the Latin scientific name when known.
        For plantName, use a common name in the selected response language when known; otherwise use the most recognizable common name.
        The plant name may be in Bulgarian, English, Latin transliteration, or contain a small typo. Infer the most likely plant before answering.
        If the plant name is ambiguous, say that the advice is general for the likely plant.
        Include a safety note in toxicity if the plant may be toxic to pets or children.
        The disclaimer field must use the selected response language and say that AI advice may be inaccurate and the user should check the real plant condition.
        careDifficulty must be exactly one of these English values: Easy, Medium, Difficult.
        suggestedWateringIntervalDays and suggestedFertilizingIntervalDays must be integers or null.
        suggestedLightLevel must be a short practical value or null.
        suggestedNote must be one short practical note or null.
        Return valid JSON only, with no extra text before or after it.

        Return exactly this JSON structure:
        {
          "plantName": "",
          "scientificName": "",
          "shortDescription": "",
          "careDifficulty": "Easy | Medium | Difficult",
          "matureHeight": "",
          "watering": "",
          "light": "",
          "temperature": "",
          "humidity": "",
          "fertilizing": "",
          "repotting": "",
          "flowering": "",
          "growth": "",
          "toxicity": "",
          "origin": "",
          "disclaimer": "",
          "suggestedWateringIntervalDays": null,
          "suggestedFertilizingIntervalDays": null,
          "suggestedLightLevel": null,
          "suggestedNote": null
        }
    """.trimIndent()
}

private fun buildPlantCareFollowUpPrompt(
    plantName: String,
    scientificName: String?,
    careProfileSummary: String,
    savedPlantData: String?,
    question: String,
): String {
    val scientificNameLine = scientificName?.let {
        "Scientific name, if known: $it"
    } ?: "Scientific name, if known: unknown"
    val savedDataText = savedPlantData
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?: "No saved WaterMe plant data is available."
    val responseLanguageInstruction = buildResponseLanguageInstruction(plantName)

    return """
        You are a plant care assistant inside an app called WaterMe.
        Answer one follow-up question about this plant.
        Return short plain text only.
        Do not return JSON.
        Do not use markdown.
        Do not include bullets.
        Keep the answer practical, friendly, and 1 to 3 short sentences.
        Do not claim 100% certainty.
        Include a safety note when toxicity, pets, children, or handling risks are relevant.
        $responseLanguageInstruction

        Plant name: $plantName
        $scientificNameLine

        Cached AI care profile summary:
        $careProfileSummary

        Saved WaterMe plant data:
        $savedDataText

        User question:
        $question
    """.trimIndent()
}

private fun buildResponseLanguageInstruction(plantName: String): String {
    return if (shouldUseBulgarianResponse(plantName)) {
        "Use Bulgarian Cyrillic for all user-facing textual values except scientificName and careDifficulty. If the input is Bulgarian written with Latin letters, answer in Bulgarian Cyrillic."
    } else {
        "Use English for all user-facing textual values except scientificName and careDifficulty. If the input is an English common plant name, answer in English. If the input is only a Latin scientific name and no user language is clear, answer in English."
    }
}

private fun shouldUseBulgarianResponse(plantName: String): Boolean {
    val normalizedName = plantName
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}]"), "")
    val hasCyrillic = plantName.any { it in '\u0400'..'\u04FF' }
    val likelyBulgarianTransliteration = normalizedName in setOf(
        "roza",
        "zdravets",
        "mushkato",
        "temenujka",
        "bosilek",
    )

    return hasCyrillic || likelyBulgarianTransliteration
}

private fun fallbackPlantName(useBulgarian: Boolean): String =
    if (useBulgarian) "\u0420\u0430\u0441\u0442\u0435\u043D\u0438\u0435" else "Plant"

private fun fallbackShortDescription(useBulgarian: Boolean): String =
    if (useBulgarian) {
        "\u041D\u044F\u043C\u0430 \u0434\u043E\u0441\u0442\u0430\u0442\u044A\u0447\u043D\u043E \u0434\u0430\u043D\u043D\u0438 " +
            "\u0437\u0430 \u0442\u043E\u0432\u0430 \u0440\u0430\u0441\u0442\u0435\u043D\u0438\u0435."
    } else {
        "There is not enough information available for this plant."
    }

private fun fallbackUnknown(useBulgarian: Boolean): String =
    if (useBulgarian) {
        "\u041D\u044F\u043C\u0430 \u043D\u0430\u043B\u0438\u0447\u043D\u0430 \u0438\u043D\u0444\u043E\u0440\u043C\u0430\u0446\u0438\u044F."
    } else {
        "No specific information is available."
    }

private fun fallbackDisclaimer(useBulgarian: Boolean): String =
    if (useBulgarian) {
        "AI \u0441\u044A\u0432\u0435\u0442\u0438\u0442\u0435 \u043C\u043E\u0436\u0435 \u0434\u0430 \u0441\u0430 \u043D\u0435\u0442\u043E\u0447\u043D\u0438. " +
            "\u0412\u0438\u043D\u0430\u0433\u0438 \u043F\u0440\u043E\u0432\u0435\u0440\u044F\u0432\u0430\u0439\u0442\u0435 " +
            "\u0440\u0435\u0430\u043B\u043D\u043E\u0442\u043E \u0441\u044A\u0441\u0442\u043E\u044F\u043D\u0438\u0435 " +
            "\u043D\u0430 \u0440\u0430\u0441\u0442\u0435\u043D\u0438\u0435\u0442\u043E."
    } else {
        "AI advice may be inaccurate. Always check your plant's real condition."
    }

private fun resolvePlantInterpretationHint(
    plantName: String,
    scientificName: String?,
): String? {
    if (!scientificName.isNullOrBlank()) return null

    val key = plantName
        .trim()
        .lowercase(Locale.ROOT)
        .replace(Regex("[^\\p{L}]"), "")

    return when (key) {
        "roza", "\u0440\u043E\u0437\u0430" ->
            "The entered name is likely Bulgarian or transliterated Bulgarian for rose. Treat it as rose, genus Rosa, unless the user provides more specific context."
        else -> null
    }
}

private fun String.extractJsonObjectText(): String {
    val trimmed = trim()
    if (trimmed.startsWith("{") && trimmed.endsWith("}")) return trimmed

    val start = trimmed.indexOf('{')
    val end = trimmed.lastIndexOf('}')
    if (start >= 0 && end > start) {
        return trimmed.substring(start, end + 1)
    }
    return trimmed
}

private fun String.containsAny(vararg values: String): Boolean =
    values.any { contains(it) }

private fun JSONObject.optCleanString(name: String): String =
    optString(name, "").trim()

private fun JSONObject.optNullableInt(name: String): Int? {
    if (!has(name) || isNull(name)) return null
    return when (val value = opt(name)) {
        is Number -> value.toInt()
        is String -> value.trim().toIntOrNull()
        else -> null
    }
}
