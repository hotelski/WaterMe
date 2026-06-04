package com.hotelski.waterme.data.feedback

import com.hotelski.waterme.BuildConfig
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class FeedbackRequest(
    val topic: String,
    val name: String?,
    val email: String?,
    val message: String,
    val appVersion: String,
    val androidSdk: Int,
)

interface FeedbackRepository {
    suspend fun sendFeedback(request: FeedbackRequest)
}

class HttpFeedbackRepository(
    private val endpointUrl: String = BuildConfig.FEEDBACK_ENDPOINT_URL,
) : FeedbackRepository {
    override suspend fun sendFeedback(request: FeedbackRequest) {
        withContext(Dispatchers.IO) {
            val endpoint = endpointUrl.trim()
            if (endpoint.isBlank()) {
                throw IOException("Feedback endpoint is not configured for this build.")
            }

            val url = runCatching { URL(endpoint) }
                .getOrElse { throw IOException("Feedback endpoint is invalid.") }
            if (url.protocol != "https") {
                throw IOException("Feedback endpoint must use HTTPS.")
            }

            val connection = (url.openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = CONNECT_TIMEOUT_MILLIS
                readTimeout = READ_TIMEOUT_MILLIS
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "WaterMe/${BuildConfig.VERSION_NAME}")
            }

            try {
                connection.outputStream.use { output ->
                    output.write(request.toJson().toString().toByteArray(Charsets.UTF_8))
                }

                val responseCode = connection.responseCode
                if (responseCode !in HTTP_SUCCESS_RANGE) {
                    val backendMessage = connection.readResponseBody(error = true).toBackendMessage()
                    throw IOException(backendMessage ?: "WaterMe could not send feedback. Try again later.")
                }
            } finally {
                connection.disconnect()
            }
        }
    }

    private fun FeedbackRequest.toJson(): JSONObject =
        JSONObject()
            .put("topic", topic)
            .put("name", name.orEmpty())
            .put("email", email.orEmpty())
            .put("message", message)
            .put("appVersion", appVersion)
            .put("androidSdk", androidSdk)
            .put("source", "android")

    private fun HttpURLConnection.readResponseBody(error: Boolean): String? {
        val stream = if (error) errorStream else inputStream
        return stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
    }

    private fun String?.toBackendMessage(): String? =
        this
            ?.takeIf { it.isNotBlank() }
            ?.let { body ->
                runCatching {
                    val json = JSONObject(body)
                    json.optString("message")
                        .takeIf { it.isNotBlank() }
                        ?: json.optString("error").takeIf { it.isNotBlank() }
                }.getOrNull()
            }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
        val HTTP_SUCCESS_RANGE = 200..299
    }
}
