package com.hotelski.waterme.data.plantnet

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.core.net.toUri
import com.hotelski.waterme.BuildConfig
import com.hotelski.waterme.model.PlantIdentificationResult
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

interface PlantIdentificationRepository {
    suspend fun identifyPlant(imageUri: String): List<PlantIdentificationResult>
}

class PlantNetPlantIdentificationRepository(
    context: Context,
    private val apiService: PlantNetApiService = PlantNetApiService.create(),
    private val apiKey: String = BuildConfig.PLANTNET_API_KEY,
) : PlantIdentificationRepository {
    private val appContext = context.applicationContext

    override suspend fun identifyPlant(imageUri: String): List<PlantIdentificationResult> {
        val trimmedApiKey = apiKey.trim()
        if (trimmedApiKey.isBlank()) {
            throw PlantIdentificationException.ApiError(
                "Plant identification API key is not configured. Add PLANTNET_API_KEY to local.properties.",
            )
        }

        val uri = runCatching { imageUri.toUri() }
            .getOrElse {
                throw PlantIdentificationException.ImageUploadError(
                    "WaterMe could not prepare this image for upload.",
                    it,
                )
            }
        val imagePart = createImagePart(uri)
        val organPart = DefaultOrgan.toRequestBody(TextPlainMediaType)

        val response = try {
            apiService.identifyPlant(
                project = DefaultProject,
                apiKey = trimmedApiKey,
                resultCount = TopResultCount,
                language = DefaultLanguage,
                includeRelatedImages = true,
                image = imagePart,
                organ = organPart,
            )
        } catch (error: UnknownHostException) {
            throw PlantIdentificationException.NoInternet(cause = error)
        } catch (error: ConnectException) {
            throw PlantIdentificationException.NoInternet(cause = error)
        } catch (error: SocketTimeoutException) {
            throw PlantIdentificationException.NoInternet(
                message = "The plant identification request timed out. Check your connection and try again.",
                cause = error,
            )
        } catch (error: IOException) {
            throw PlantIdentificationException.NoInternet(cause = error)
        }

        if (!response.isSuccessful) {
            throw PlantIdentificationException.ApiError(response.toUserMessage())
        }

        val results = response.body()
            ?.results
            .orEmpty()
            .sortedByDescending { it.score ?: 0.0 }
            .take(TopResultCount)
            .mapNotNull { it.toDomain() }

        if (results.isEmpty()) {
            throw PlantIdentificationException.EmptyResult()
        }

        return results
    }

    private fun createImagePart(uri: Uri): MultipartBody.Part {
        val contentResolver = appContext.contentResolver
        val mediaType = contentResolver.resolvePlantNetImageMediaType(uri)
        val bytes = runCatching {
            contentResolver.openInputStream(uri)?.use { input -> input.readBytes() }
        }.getOrNull()

        if (bytes == null || bytes.isEmpty()) {
            throw PlantIdentificationException.ImageUploadError(
                "WaterMe could not read this plant photo for upload.",
            )
        }

        val fileName = uri.displayName(contentResolver, mediaType)
        val requestBody = bytes.toRequestBody(mediaType.toMediaType())
        return MultipartBody.Part.createFormData(ImagePartName, fileName, requestBody)
    }

    private fun ContentResolver.resolvePlantNetImageMediaType(uri: Uri): String {
        val resolvedType = getType(uri)?.lowercase()
        return when {
            resolvedType == JpegMediaType || resolvedType == PngMediaType -> resolvedType
            resolvedType == null && uri.scheme == ContentResolver.SCHEME_FILE -> JpegMediaType
            resolvedType == null -> JpegMediaType
            resolvedType.startsWith("image/") -> throw PlantIdentificationException.ImageUploadError(
                "Plant scanner accepts JPEG or PNG images. Choose another plant photo.",
            )
            else -> throw PlantIdentificationException.ImageUploadError(
                "Choose an image file before scanning.",
            )
        }
    }

    private fun Uri.displayName(
        contentResolver: ContentResolver,
        mediaType: String,
    ): String {
        val contentName = if (scheme == ContentResolver.SCHEME_CONTENT) {
            runCatching {
                contentResolver.query(this, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                    ?.use { cursor ->
                        val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
                    }
            }.getOrNull()
        } else {
            null
        }
        return contentName
            ?.takeIf { it.isNotBlank() }
            ?: lastPathSegment
                ?.substringAfterLast('/')
                ?.takeIf { it.isNotBlank() }
            ?: if (mediaType == PngMediaType) {
                "plant_upload.png"
            } else {
                "plant_upload.jpg"
            }
    }

    private fun PlantNetIdentificationResultDto.toDomain(): PlantIdentificationResult? {
        val species = species ?: return null
        val scientificName = species.scientificNameWithoutAuthor
            ?.takeIf { it.isNotBlank() }
            ?: species.scientificName?.takeIf { it.isNotBlank() }
            ?: return null
        val commonName = species.commonNames
            ?.firstOrNull { it.isNotBlank() }
            ?: scientificName
        val score = score ?: return null
        val relatedImage = images
            ?.firstOrNull { it.bestUrl() != null }

        return PlantIdentificationResult(
            commonName = commonName,
            scientificName = scientificName,
            confidenceScore = score.coerceIn(0.0, 1.0),
            relatedImageUrl = relatedImage?.bestUrl(),
            relatedImageAttribution = relatedImage?.attribution(),
        )
    }

    private fun PlantNetRelatedImageDto.bestUrl(): String? =
        url?.medium?.takeIf { it.isNotBlank() }
            ?: url?.small?.takeIf { it.isNotBlank() }
            ?: url?.original?.takeIf { it.isNotBlank() }

    private fun PlantNetRelatedImageDto.attribution(): String? {
        val citationText = citation?.takeIf { it.isNotBlank() }
        if (citationText != null) {
            return "Photo: $citationText"
        }

        val authorText = author?.takeIf { it.isNotBlank() }
        val licenseText = license?.takeIf { it.isNotBlank() }
        return when {
            authorText != null && licenseText != null -> "Photo: $authorText, $licenseText"
            authorText != null -> "Photo: $authorText"
            licenseText != null -> "Photo license: $licenseText"
            else -> null
        }
    }

    private companion object {
        const val DefaultProject = "all"
        const val DefaultLanguage = "en"
        const val DefaultOrgan = "auto"
        const val TopResultCount = 3
        const val ImagePartName = "images"
        const val JpegMediaType = "image/jpeg"
        const val PngMediaType = "image/png"
        val TextPlainMediaType = "text/plain".toMediaType()
    }
}

sealed class PlantIdentificationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    class NoInternet(
        message: String = "No internet connection. Check your connection and try again.",
        cause: Throwable? = null,
    ) : PlantIdentificationException(message, cause)

    class EmptyResult(
        message: String = "WaterMe could not find a confident plant match for this image.",
    ) : PlantIdentificationException(message)

    class ApiError(
        message: String = "WaterMe could not identify this plant right now.",
    ) : PlantIdentificationException(message)

    class ImageUploadError(
        message: String = "WaterMe could not upload this plant photo.",
        cause: Throwable? = null,
    ) : PlantIdentificationException(message, cause)
}

private fun retrofit2.Response<*>.toUserMessage(): String =
    when (code()) {
        400 -> "WaterMe could not process this image. Try another plant photo."
        401 -> "Plant identification API key was rejected. Check PLANTNET_API_KEY in local.properties."
        404 -> "WaterMe did not find a plant match for this image."
        413 -> "This photo is too large. Try a smaller image."
        415 -> "Plant scanner accepts JPEG or PNG images. Choose another plant photo."
        429 -> "Plant identification is rate limited. Try again later."
        in 500..599 -> "Plant identification is unavailable right now. Try again later."
        else -> "Plant identification returned an error (${code()}). Try again later."
    }
