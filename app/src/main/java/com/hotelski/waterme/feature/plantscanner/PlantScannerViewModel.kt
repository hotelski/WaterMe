package com.hotelski.waterme.feature.plantscanner

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.preferences.PlantScannerQuotaConsumeResult
import com.hotelski.waterme.data.preferences.PlantScannerQuotaSnapshot
import com.hotelski.waterme.model.PlantIdentificationResult
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PlantScannerResultUiModel(
    val commonName: String,
    val scientificName: String,
    val confidenceScore: Double,
    val relatedImageUrl: String? = null,
    val relatedImageAttribution: String? = null,
) {
    val confidencePercent: Int
        get() = (confidenceScore * 100).toInt().coerceIn(0, 100)
}

private fun PlantIdentificationResult.toUiModel(): PlantScannerResultUiModel =
    PlantScannerResultUiModel(
        commonName = commonName,
        scientificName = scientificName,
        confidenceScore = confidenceScore,
        relatedImageUrl = relatedImageUrl,
        relatedImageAttribution = relatedImageAttribution,
    )

data class PlantScannerUiState(
    val selectedImageUri: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val results: List<PlantScannerResultUiModel> = emptyList(),
    val actionMessage: String? = null,
    val scanLimit: Int = 3,
    val remainingScans: Int = 3,
    val scanQuotaResetCountdown: String? = null,
    val isScanQuotaExhausted: Boolean = false,
) {
    val isEmpty: Boolean
        get() = selectedImageUri == null && !isLoading && errorMessage == null && results.isEmpty()
}

sealed interface PlantScannerEvent {
    data object BackClicked : PlantScannerEvent
    data object TakePhotoClicked : PlantScannerEvent
    data object ChooseFromGalleryClicked : PlantScannerEvent
    data object RetryClicked : PlantScannerEvent
    data object AskCareAdviceClicked : PlantScannerEvent
    data object SaveToPlantsClicked : PlantScannerEvent
    data class ImageSelected(val uri: String?) : PlantScannerEvent
    data class PhotoLaunchFailed(val message: String) : PlantScannerEvent
}

sealed interface PlantScannerEffect {
    data object NavigateBack : PlantScannerEffect
    data object OpenCamera : PlantScannerEffect
    data object OpenGallery : PlantScannerEffect
    data class NavigateToAddPlant(val name: String, val photoUri: String) : PlantScannerEffect
}

class PlantScannerViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantIdentificationRepository =
        WaterMeAppContainer.plantIdentificationRepository(appContext)
    private val plantScannerQuotaDataStore =
        WaterMeAppContainer.plantScannerQuotaDataStore(appContext)
    private val _uiState = MutableStateFlow(PlantScannerUiState())
    private val _effects = MutableSharedFlow<PlantScannerEffect>()
    private var scanJob: Job? = null

    val uiState = _uiState.asStateFlow()
    val effects = _effects.asSharedFlow()

    init {
        observeScanQuota()
    }

    fun onEvent(event: PlantScannerEvent) {
        when (event) {
            PlantScannerEvent.BackClicked -> emitEffect(PlantScannerEffect.NavigateBack)
            PlantScannerEvent.TakePhotoClicked -> emitEffect(PlantScannerEffect.OpenCamera)
            PlantScannerEvent.ChooseFromGalleryClicked -> emitEffect(PlantScannerEffect.OpenGallery)
            is PlantScannerEvent.ImageSelected -> scanSelectedImage(event.uri)
            PlantScannerEvent.RetryClicked -> retryScan()
            PlantScannerEvent.AskCareAdviceClicked -> showActionMessage(
                "AI care advice is mocked for now. The real service will be connected later.",
            )
            PlantScannerEvent.SaveToPlantsClicked -> navigateToAddPlantWithResult()
            is PlantScannerEvent.PhotoLaunchFailed -> {
                scanJob?.cancel()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = event.message,
                        results = emptyList(),
                        actionMessage = null,
                    )
                }
            }
        }
    }

    private fun observeScanQuota() {
        viewModelScope.launch {
            while (isActive) {
                runCatching { plantScannerQuotaDataStore.currentQuota(WaterMeAppContainer.LOCAL_USER_ID) }
                    .onSuccess(::applyQuotaSnapshot)
                delay(QuotaTickMillis)
            }
        }
    }

    private fun navigateToAddPlantWithResult() {
        val state = _uiState.value
        val photoUri = state.selectedImageUri
        val result = state.results.firstOrNull()
        when {
            photoUri.isNullOrBlank() -> showActionMessage("Choose and scan a plant photo before saving.")
            result == null -> showActionMessage("Scan the plant photo before saving it to your plants.")
            else -> emitEffect(
                PlantScannerEffect.NavigateToAddPlant(
                    name = result.commonName.ifBlank { result.scientificName },
                    photoUri = photoUri,
                ),
            )
        }
    }

    private fun scanSelectedImage(uri: String?) {
        val selectedUri = uri?.takeIf { it.isNotBlank() } ?: return
        identifySelectedImage(selectedUri)
    }

    private fun retryScan() {
        val selectedUri = _uiState.value.selectedImageUri
        if (selectedUri == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    errorMessage = "Choose a plant photo before scanning.",
                    actionMessage = null,
                )
            }
            return
        }
        identifySelectedImage(selectedUri)
    }

    private fun identifySelectedImage(selectedUri: String) {
        scanJob?.cancel()
        _uiState.update {
            it.copy(
                selectedImageUri = selectedUri,
                isLoading = true,
                errorMessage = null,
                results = emptyList(),
                actionMessage = null,
            )
        }
        scanJob = viewModelScope.launch {
            val quotaResult = runCatching {
                plantScannerQuotaDataStore.consumeScan(WaterMeAppContainer.LOCAL_USER_ID)
            }.getOrElse { error ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = error.toUserMessage(),
                        results = emptyList(),
                    )
                }
                return@launch
            }
            applyQuotaSnapshot(quotaResult.snapshot)
            if (quotaResult is PlantScannerQuotaConsumeResult.Exhausted) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = ScanQuotaLimitMessage,
                        results = emptyList(),
                        actionMessage = null,
                    )
                }
                return@launch
            }

            runCatching { plantIdentificationRepository.identifyPlant(selectedUri) }
                .onSuccess { results ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = null,
                            results = results.take(1).map { result -> result.toUiModel() },
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage(),
                            results = emptyList(),
                        )
                    }
                }
        }
    }

    private fun showActionMessage(message: String) {
        _uiState.update { it.copy(actionMessage = message, errorMessage = null) }
    }

    private fun applyQuotaSnapshot(snapshot: PlantScannerQuotaSnapshot) {
        val resetCountdown = if (snapshot.isExhausted) {
            snapshot.resetAtMillis.toResetCountdownLabel()
        } else {
            null
        }
        _uiState.update { state ->
            state.copy(
                scanLimit = snapshot.scanLimit,
                remainingScans = snapshot.remainingScans,
                scanQuotaResetCountdown = resetCountdown,
                isScanQuotaExhausted = snapshot.isExhausted,
                errorMessage = if (!snapshot.isExhausted && state.errorMessage == ScanQuotaLimitMessage) {
                    null
                } else {
                    state.errorMessage
                },
            )
        }
    }

    private fun Long.toResetCountdownLabel(): String {
        val totalSeconds = ((this - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        val hours = totalSeconds / SecondsPerHour
        val minutes = (totalSeconds % SecondsPerHour) / SecondsPerMinute
        val seconds = totalSeconds % SecondsPerMinute
        return if (hours > 0) {
            String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    private fun emitEffect(effect: PlantScannerEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not identify this plant."

    private companion object {
        const val QuotaTickMillis = 1_000L
        const val SecondsPerMinute = 60L
        const val SecondsPerHour = 60L * SecondsPerMinute
        const val ScanQuotaLimitMessage = "Daily scan limit reached."
    }
}
