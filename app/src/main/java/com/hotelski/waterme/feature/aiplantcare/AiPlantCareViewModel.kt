package com.hotelski.waterme.feature.aiplantcare

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.data.aiplantcare.AiCareCachedAdvice
import com.hotelski.waterme.data.aiplantcare.AiPlantCareException
import com.hotelski.waterme.data.aiplantcare.buildAiCareCacheKey
import com.hotelski.waterme.data.local.entity.HistoryAction
import com.hotelski.waterme.data.local.entity.ReminderEntity
import com.hotelski.waterme.data.local.model.PlantWithDetails
import com.hotelski.waterme.feature.characters.PlantCharacterUiModel
import com.hotelski.waterme.feature.characters.activePlantCharacter
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.model.PlantCareAdvice
import java.text.DateFormat
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AiPlantCareInputMode {
    SAVED_PLANT,
    TEMPORARY_PLANT,
}

data class AiCareLocalCareUiModel(
    val title: String,
    val intervalLabel: String,
    val lastCompletedLabel: String,
    val statusLabel: String,
    val isUrgent: Boolean = false,
)

data class AiCareFallbackAdviceUiModel(
    val title: String,
    val reasonMessage: String,
    val plantName: String,
    val careRows: List<AiCareLocalCareUiModel>,
    val suggestion: String,
)

data class AiPlantCarePlantUiModel(
    val id: String,
    val name: String,
    val scientificName: String?,
    val location: String,
    val photoUri: String?,
    val notes: String,
    val reminderCount: Int,
    val careLogCount: Int,
    val wateringCare: AiCareLocalCareUiModel,
    val fertilizingCare: AiCareLocalCareUiModel,
)

data class AiCareFollowUpUiModel(
    val id: Long,
    val question: String,
    val answer: String,
)

data class AiPlantCareUiState(
    val isLoadingPlants: Boolean = false,
    val savedPlants: List<AiPlantCarePlantUiModel> = emptyList(),
    val inputMode: AiPlantCareInputMode = AiPlantCareInputMode.SAVED_PLANT,
    val selectedPlantId: String? = null,
    val temporaryPlantName: String = "",
    val temporaryScientificName: String = "",
    val isAdviceLoading: Boolean = false,
    val errorMessage: String? = null,
    val advice: PlantCareAdvice? = null,
    val adviceUpdatedLabel: String? = null,
    val aiAvailabilityMessage: String? = null,
    val fallbackAdvice: AiCareFallbackAdviceUiModel? = null,
    val pendingSuggestedAction: AiCarePendingActionUiModel? = null,
    val isApplyingSuggestedAction: Boolean = false,
    val actionMessage: String? = null,
    val actionMessageIsError: Boolean = false,
    val actionHeartBurstKey: Long = 0L,
    val activeCharacter: PlantCharacterUiModel? = null,
    val followUpQuestion: String = "",
    val isFollowUpLoading: Boolean = false,
    val followUpErrorMessage: String? = null,
    val followUpItems: List<AiCareFollowUpUiModel> = emptyList(),
) {
    val selectedPlant: AiPlantCarePlantUiModel?
        get() = savedPlants.firstOrNull { it.id == selectedPlantId }

    val canRequestAdvice: Boolean
        get() = !isAdviceLoading &&
            when (inputMode) {
                AiPlantCareInputMode.SAVED_PLANT -> selectedPlant != null
                AiPlantCareInputMode.TEMPORARY_PLANT -> temporaryPlantName.isNotBlank()
            }

    val shouldShowAdviceButton: Boolean
        get() = isAdviceLoading || canRequestAdvice

    val shouldShowEmptyState: Boolean
        get() = !isLoadingPlants &&
            !isAdviceLoading &&
            errorMessage == null &&
            advice == null &&
            fallbackAdvice == null &&
            !(inputMode == AiPlantCareInputMode.SAVED_PLANT && savedPlants.isEmpty())
}

data class AiCarePendingActionUiModel(
    val title: String,
    val message: String,
    val inputLabel: String,
    val inputValue: String,
    val confirmLabel: String,
    val numericInput: Boolean = false,
)

sealed interface AiPlantCareEvent {
    data object BackClicked : AiPlantCareEvent
    data object SavedPlantModeSelected : AiPlantCareEvent
    data object TemporaryPlantModeSelected : AiPlantCareEvent
    data object GetAdviceClicked : AiPlantCareEvent
    data object RefreshAdviceClicked : AiPlantCareEvent
    data class SetReminderSuggestedActionClicked(val careType: CareType) : AiPlantCareEvent
    data object AddNoteSuggestedActionClicked : AiPlantCareEvent
    data object SaveCareProfileSuggestedActionClicked : AiPlantCareEvent
    data object AddTemporaryPlantClicked : AiPlantCareEvent
    data class SuggestedActionDraftChanged(val value: String) : AiPlantCareEvent
    data object ConfirmSuggestedActionClicked : AiPlantCareEvent
    data object DismissSuggestedActionClicked : AiPlantCareEvent
    data class FollowUpQuestionChanged(val value: String) : AiPlantCareEvent
    data object SendFollowUpClicked : AiPlantCareEvent
    data class SavedPlantSelected(val plantId: String) : AiPlantCareEvent
    data class TemporaryPlantNameChanged(val value: String) : AiPlantCareEvent
    data class TemporaryScientificNameChanged(val value: String) : AiPlantCareEvent
}

sealed interface AiPlantCareEffect {
    data object NavigateBack : AiPlantCareEffect
    data class NavigateToAddPlant(
        val name: String,
        val scientificName: String?,
        val notes: String?,
        val wateringDays: Int?,
        val fertilizingDays: Int?,
    ) : AiPlantCareEffect
}

private data class AiPlantCareFormState(
    val inputMode: AiPlantCareInputMode = AiPlantCareInputMode.SAVED_PLANT,
    val selectedPlantId: String? = null,
    val temporaryPlantName: String = "",
    val temporaryScientificName: String = "",
)

private data class AiPlantCareAdviceState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val advice: PlantCareAdvice? = null,
    val generatedAt: Long? = null,
    val modelName: String? = null,
    val cacheKey: String? = null,
    val aiAvailabilityMessage: String? = null,
    val fallbackAdvice: AiCareFallbackAdviceUiModel? = null,
)

private data class AiCareSuggestedActionState(
    val pendingAction: AiCarePendingAction? = null,
    val draftValue: String = "",
    val isApplying: Boolean = false,
    val message: String? = null,
    val messageIsError: Boolean = false,
    val heartBurstKey: Long = 0L,
) {
    val pendingActionUiModel: AiCarePendingActionUiModel?
        get() = pendingAction?.toUiModel(draftValue)
}

private data class AiCareFollowUpState(
    val question: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val items: List<AiCareFollowUpUiModel> = emptyList(),
)

private sealed interface AiCarePendingAction {
    data class SetReminder(val careType: CareType) : AiCarePendingAction
    data object AddNote : AiCarePendingAction
    data object SaveCareProfile : AiCarePendingAction
}

private fun AiCarePendingAction.toUiModel(draftValue: String): AiCarePendingActionUiModel =
    when (this) {
        is AiCarePendingAction.SetReminder -> AiCarePendingActionUiModel(
            title = "Set ${careType.shortLabel.lowercase()} reminder",
            message = "WaterMe will update this plant's ${careType.label.lowercase()} schedule after you confirm.",
            inputLabel = "Repeat every",
            inputValue = draftValue,
            confirmLabel = "Set reminder",
            numericInput = true,
        )
        AiCarePendingAction.AddNote -> AiCarePendingActionUiModel(
            title = "Add note",
            message = "This note will be added to the selected plant after you confirm.",
            inputLabel = "Note",
            inputValue = draftValue,
            confirmLabel = "Add note",
        )
        AiCarePendingAction.SaveCareProfile -> AiCarePendingActionUiModel(
            title = "Save care profile",
            message = "WaterMe will save this AI care profile after you confirm.",
            inputLabel = "Care profile",
            inputValue = draftValue,
            confirmLabel = "Save profile",
        )
    }

private sealed interface SavedPlantsLoadState {
    data object Loading : SavedPlantsLoadState
    data class Loaded(val plants: List<AiPlantCarePlantUiModel>) : SavedPlantsLoadState
    data class Error(val message: String) : SavedPlantsLoadState
}

private data class AiPlantCareAdviceTarget(
    val plantId: String?,
    val plantName: String,
    val scientificName: String?,
) {
    val cacheKey: String
        get() = buildAiCareCacheKey(
            plantId = plantId,
            plantName = plantName,
            scientificName = scientificName,
        )
}

class AiPlantCareViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val reminderRepository = WaterMeAppContainer.reminderRepository(appContext)
    private val careRepository = WaterMeAppContainer.careRepository(appContext)
    private val settingsDataStore = WaterMeAppContainer.settingsDataStore(appContext)
    private val reminderNotifications = WaterMeAppContainer.reminderNotificationCoordinator(appContext)
    private val aiPlantCareRepository = WaterMeAppContainer.aiPlantCareRepository()
    private val aiCareCacheRepository = WaterMeAppContainer.aiCareCacheRepository(appContext)
    private val formState = MutableStateFlow(AiPlantCareFormState())
    private val adviceState = MutableStateFlow(AiPlantCareAdviceState())
    private val suggestedActionState = MutableStateFlow(AiCareSuggestedActionState())
    private val followUpState = MutableStateFlow(AiCareFollowUpState())
    private val _effects = MutableSharedFlow<AiPlantCareEffect>()
    private var adviceJob: Job? = null
    private var cacheLoadJob: Job? = null
    private var followUpJob: Job? = null
    private var actionMessageDismissJob: Job? = null
    private var runningAdviceCacheKey: String? = null

    private val savedPlantsState = plantRepository
        .observePlantsWithDetails(WaterMeAppContainer.LOCAL_USER_ID)
        .map { plants -> SavedPlantsLoadState.Loaded(plants.map { it.toAiCarePlantUiModel() }) as SavedPlantsLoadState }
        .catch { error -> emit(SavedPlantsLoadState.Error(error.toUserMessage())) }

    private val activeCharacterState = combine(
        careRepository.observeCareHistoryForUser(WaterMeAppContainer.LOCAL_USER_ID),
        plantRepository.observePlants(WaterMeAppContainer.LOCAL_USER_ID),
        settingsDataStore.settings,
    ) { careHistory, plants, settings ->
        activePlantCharacter(
            careHistory = careHistory,
            selectedCharacterId = settings.selectedCharacterId,
            plantsAddedTotal = plants.size,
            appOpenDayStreak = settings.appOpenDayStreak,
        ) as PlantCharacterUiModel?
    }.catch { emit(null) }

    val effects = _effects.asSharedFlow()

    private val baseUiState = combine(
        savedPlantsState,
        formState,
        adviceState,
        suggestedActionState,
        followUpState,
    ) { plantsState, form, advice, suggestedAction, followUp ->
        val savedPlants = when (plantsState) {
            is SavedPlantsLoadState.Loaded -> plantsState.plants
            else -> emptyList()
        }
        val selectedPlantId = form.selectedPlantId?.takeIf { plantId ->
            savedPlants.any { it.id == plantId }
        }
        AiPlantCareUiState(
            isLoadingPlants = plantsState is SavedPlantsLoadState.Loading,
            savedPlants = savedPlants,
            inputMode = form.inputMode,
            selectedPlantId = selectedPlantId,
            temporaryPlantName = form.temporaryPlantName,
            temporaryScientificName = form.temporaryScientificName,
            isAdviceLoading = advice.isLoading,
            errorMessage = advice.errorMessage ?: (plantsState as? SavedPlantsLoadState.Error)?.message,
            advice = advice.advice,
            adviceUpdatedLabel = advice.generatedAt?.let { generatedAt ->
                "Generated by AI \u2022 Last updated: ${generatedAt.formatAdviceGeneratedAt()}"
            },
            aiAvailabilityMessage = advice.aiAvailabilityMessage,
            fallbackAdvice = advice.fallbackAdvice,
            pendingSuggestedAction = suggestedAction.pendingActionUiModel,
            isApplyingSuggestedAction = suggestedAction.isApplying,
            actionMessage = suggestedAction.message,
            actionMessageIsError = suggestedAction.messageIsError,
            actionHeartBurstKey = suggestedAction.heartBurstKey,
            followUpQuestion = followUp.question,
            isFollowUpLoading = followUp.isLoading,
            followUpErrorMessage = followUp.errorMessage,
            followUpItems = followUp.items,
        )
    }

    val uiState = combine(
        baseUiState,
        activeCharacterState,
    ) { state, activeCharacter ->
        state.copy(activeCharacter = activeCharacter)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AiPlantCareUiState(isLoadingPlants = true),
        )

    fun onEvent(event: AiPlantCareEvent) {
        when (event) {
            AiPlantCareEvent.BackClicked -> emitEffect(AiPlantCareEffect.NavigateBack)
            AiPlantCareEvent.SavedPlantModeSelected -> selectInputMode(AiPlantCareInputMode.SAVED_PLANT)
            AiPlantCareEvent.TemporaryPlantModeSelected -> selectInputMode(AiPlantCareInputMode.TEMPORARY_PLANT)
            is AiPlantCareEvent.SavedPlantSelected -> selectSavedPlant(event.plantId)
            is AiPlantCareEvent.TemporaryPlantNameChanged -> updateTemporaryPlantName(event.value)
            is AiPlantCareEvent.TemporaryScientificNameChanged -> updateTemporaryScientificName(event.value)
            AiPlantCareEvent.GetAdviceClicked -> requestAdvice(forceRefresh = false)
            AiPlantCareEvent.RefreshAdviceClicked -> requestAdvice(forceRefresh = true)
            is AiPlantCareEvent.SetReminderSuggestedActionClicked -> openReminderSuggestedAction(event.careType)
            AiPlantCareEvent.AddNoteSuggestedActionClicked -> openAddNoteSuggestedAction()
            AiPlantCareEvent.SaveCareProfileSuggestedActionClicked -> openSaveCareProfileSuggestedAction()
            AiPlantCareEvent.AddTemporaryPlantClicked -> navigateToAddPlantWithAdvice()
            is AiPlantCareEvent.SuggestedActionDraftChanged -> updateSuggestedActionDraft(event.value)
            AiPlantCareEvent.ConfirmSuggestedActionClicked -> confirmSuggestedAction()
            AiPlantCareEvent.DismissSuggestedActionClicked -> suggestedActionState.value = AiCareSuggestedActionState()
            is AiPlantCareEvent.FollowUpQuestionChanged -> updateFollowUpQuestion(event.value)
            AiPlantCareEvent.SendFollowUpClicked -> sendFollowUpQuestion()
        }
    }

    private fun selectInputMode(inputMode: AiPlantCareInputMode) {
        cacheLoadJob?.cancel()
        formState.update { it.copy(inputMode = inputMode) }
        adviceState.value = AiPlantCareAdviceState()
        suggestedActionState.value = AiCareSuggestedActionState()
        resetFollowUp()
    }

    private fun selectSavedPlant(plantId: String) {
        val currentState = uiState.value
        val selectedPlant = currentState.savedPlants.firstOrNull { it.id == plantId }
        val shouldDeselect = currentState.selectedPlantId == plantId
        cacheLoadJob?.cancel()
        formState.update {
            it.copy(
                inputMode = AiPlantCareInputMode.SAVED_PLANT,
                selectedPlantId = if (shouldDeselect) null else plantId,
            )
        }
        adviceState.value = AiPlantCareAdviceState()
        suggestedActionState.value = AiCareSuggestedActionState()
        resetFollowUp()
        if (!shouldDeselect && selectedPlant != null) {
            loadCachedAdvice(
                AiPlantCareAdviceTarget(
                    plantId = selectedPlant.id,
                    plantName = selectedPlant.name,
                    scientificName = selectedPlant.scientificName,
                ),
            )
        }
    }

    private fun updateTemporaryPlantName(value: String) {
        cacheLoadJob?.cancel()
        formState.update {
            it.copy(
                inputMode = AiPlantCareInputMode.TEMPORARY_PLANT,
                temporaryPlantName = value.take(MaxPlantNameLength),
            )
        }
        adviceState.value = AiPlantCareAdviceState()
        suggestedActionState.value = AiCareSuggestedActionState()
        resetFollowUp()
    }

    private fun updateTemporaryScientificName(value: String) {
        cacheLoadJob?.cancel()
        formState.update {
            it.copy(
                inputMode = AiPlantCareInputMode.TEMPORARY_PLANT,
                temporaryScientificName = value.take(MaxScientificNameLength),
            )
        }
        adviceState.value = AiPlantCareAdviceState()
        suggestedActionState.value = AiCareSuggestedActionState()
        resetFollowUp()
    }

    private fun resetFollowUp() {
        followUpJob?.cancel()
        followUpState.value = AiCareFollowUpState()
    }

    private fun requestAdvice(forceRefresh: Boolean) {
        val target = resolveAdviceTarget() ?: return
        if (target.plantName.isObviousNonPlantInput()) {
            adviceJob?.cancel()
            cacheLoadJob?.cancel()
            runningAdviceCacheKey = null
            adviceState.value = AiPlantCareAdviceState(
                cacheKey = target.cacheKey,
                errorMessage = target.plantName.unrecognizedPlantNameMessage(),
            )
            suggestedActionState.value = AiCareSuggestedActionState()
            resetFollowUp()
            return
        }
        if (adviceJob?.isActive == true && runningAdviceCacheKey == target.cacheKey) return
        if (adviceJob?.isActive == true) {
            adviceJob?.cancel()
        }
        adviceJob = viewModelScope.launch {
            runningAdviceCacheKey = target.cacheKey
            try {
                if (!forceRefresh) {
                    val cachedAdvice = runCatching {
                        aiCareCacheRepository.getCachedAdvice(
                            plantId = target.plantId,
                            plantName = target.plantName,
                            scientificName = target.scientificName,
                        )
                    }.getOrNull()
                    if (cachedAdvice != null) {
                        adviceState.value = cachedAdvice.toAdviceState(target.cacheKey)
                        return@launch
                    }
                }

                val previousAdviceState = adviceState.value.takeIf { it.cacheKey == target.cacheKey && it.advice != null }
                adviceState.value = previousAdviceState?.copy(
                    isLoading = true,
                    errorMessage = null,
                    aiAvailabilityMessage = null,
                    fallbackAdvice = null,
                ) ?: AiPlantCareAdviceState(isLoading = true, cacheKey = target.cacheKey)

                val result = aiPlantCareRepository
                    .generateFullPlantCareAdvice(
                        plantName = target.plantName,
                        scientificName = target.scientificName,
                    )

                result
                    .onSuccess { advice ->
                        val cachedAdvice = runCatching {
                            aiCareCacheRepository.saveAdvice(
                                plantId = target.plantId,
                                plantName = target.plantName,
                                scientificName = target.scientificName,
                                advice = advice,
                            )
                        }.getOrNull()
                        adviceState.value = cachedAdvice?.toAdviceState(target.cacheKey)
                            ?: AiPlantCareAdviceState(advice = advice, cacheKey = target.cacheKey)
                        followUpState.value = AiCareFollowUpState()
                    }

                result.exceptionOrNull()?.let { error ->
                    handleAdviceFailure(
                        target = target,
                        error = error,
                    )
                }
            } finally {
                if (runningAdviceCacheKey == target.cacheKey) {
                    runningAdviceCacheKey = null
                }
            }
        }
    }

    private fun loadCachedAdvice(target: AiPlantCareAdviceTarget) {
        if (target.plantName.isObviousNonPlantInput()) return
        cacheLoadJob = viewModelScope.launch {
            val cachedAdvice = runCatching {
                aiCareCacheRepository.getCachedAdvice(
                    plantId = target.plantId,
                    plantName = target.plantName,
                    scientificName = target.scientificName,
                )
            }.getOrNull()
            if (cachedAdvice != null && resolveAdviceTarget()?.cacheKey == target.cacheKey) {
                adviceState.value = cachedAdvice.toAdviceState(target.cacheKey)
            }
        }
    }

    private suspend fun handleAdviceFailure(
        target: AiPlantCareAdviceTarget,
        error: Throwable,
    ) {
        if (error is AiPlantCareException.UnrecognizedPlantName) {
            adviceState.value = AiPlantCareAdviceState(
                cacheKey = target.cacheKey,
                errorMessage = error.toUserMessage(),
            )
            return
        }

        val cachedAdvice = runCatching {
            aiCareCacheRepository.getCachedAdvice(
                plantId = target.plantId,
                plantName = target.plantName,
                scientificName = target.scientificName,
            )
        }.getOrNull()

        if (cachedAdvice != null) {
            adviceState.value = cachedAdvice
                .toAdviceState(target.cacheKey)
                .copy(aiAvailabilityMessage = SavedAdviceUnavailableMessage)
            return
        }

        val selectedPlant = uiState.value.selectedPlant?.takeIf { it.id == target.plantId }
        adviceState.value = AiPlantCareAdviceState(
            cacheKey = target.cacheKey,
            fallbackAdvice = buildFallbackAdvice(
                target = target,
                selectedPlant = selectedPlant,
                reasonMessage = error.toUserMessage(),
            ),
        )
    }

    private fun buildFallbackAdvice(
        target: AiPlantCareAdviceTarget,
        selectedPlant: AiPlantCarePlantUiModel?,
        reasonMessage: String,
    ): AiCareFallbackAdviceUiModel =
        AiCareFallbackAdviceUiModel(
            title = "AI is unavailable right now, but here is a basic WaterMe suggestion.",
            reasonMessage = reasonMessage,
            plantName = selectedPlant?.name ?: target.plantName,
            careRows = selectedPlant?.let { plant ->
                listOf(plant.wateringCare, plant.fertilizingCare)
            }.orEmpty(),
            suggestion = "Check soil moisture before watering, keep the plant in suitable light, and use the real plant condition as the source of truth until AI is available again.",
        )

    private fun updateFollowUpQuestion(value: String) {
        followUpState.update {
            it.copy(
                question = value.take(MaxFollowUpQuestionLength),
                errorMessage = null,
            )
        }
    }

    private fun sendFollowUpQuestion() {
        if (followUpJob?.isActive == true) return

        val state = uiState.value
        val advice = state.advice
        val question = followUpState.value.question.trim()
        if (advice == null) {
            followUpState.update {
                it.copy(errorMessage = "Generate a care profile before asking follow-up questions.")
            }
            return
        }
        if (question.isBlank()) {
            followUpState.update { it.copy(errorMessage = "Ask a question before sending.") }
            return
        }

        val selectedPlant = state.selectedPlant
        val plantName = selectedPlant?.name
            ?: state.temporaryPlantName.trim()
                .ifBlank { advice.plantName }
        if (plantName.isBlank()) {
            followUpState.update {
                it.copy(errorMessage = "WaterMe needs a plant name before asking AI.")
            }
            return
        }
        val scientificName = selectedPlant?.scientificName
            ?: state.temporaryScientificName.trim().takeIf { it.isNotBlank() }
            ?: advice.scientificName

        followUpJob = viewModelScope.launch {
            followUpState.update { it.copy(isLoading = true, errorMessage = null) }
            aiPlantCareRepository
                .generatePlantCareFollowUpAnswer(
                    plantName = plantName,
                    scientificName = scientificName,
                    careProfileSummary = advice.toFollowUpProfileSummary(),
                    savedPlantData = selectedPlant?.toFollowUpSavedPlantData(),
                    question = question,
                )
                .onSuccess { answer ->
                    followUpState.update {
                        it.copy(
                            question = "",
                            isLoading = false,
                            errorMessage = null,
                            items = it.items + AiCareFollowUpUiModel(
                                id = System.currentTimeMillis(),
                                question = question,
                                answer = answer,
                            ),
                        )
                    }
                }
                .onFailure { error ->
                    followUpState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.toUserMessage(),
                        )
                    }
                }
        }
    }

    private fun openReminderSuggestedAction(careType: CareType) {
        val advice = uiState.value.advice ?: return
        val intervalDays = when (careType) {
            CareType.WATERING -> advice.suggestedWateringIntervalDays
            CareType.FERTILIZING -> advice.suggestedFertilizingIntervalDays
            else -> null
        }?.coerceIn(MinReminderDays, MaxReminderDays) ?: careType.defaultFrequencyDays
        suggestedActionState.value = AiCareSuggestedActionState(
            pendingAction = AiCarePendingAction.SetReminder(careType),
            draftValue = intervalDays.toString(),
        )
    }

    private fun openAddNoteSuggestedAction() {
        val advice = uiState.value.advice ?: return
        suggestedActionState.value = AiCareSuggestedActionState(
            pendingAction = AiCarePendingAction.AddNote,
            draftValue = advice.suggestedNote
                ?.takeIf { it.isNotBlank() }
                ?: advice.shortDescription
                    .takeIf { it.isNotBlank() }
                    .orEmpty(),
        )
    }

    private fun openSaveCareProfileSuggestedAction() {
        val advice = uiState.value.advice ?: return
        suggestedActionState.value = AiCareSuggestedActionState(
            pendingAction = AiCarePendingAction.SaveCareProfile,
            draftValue = advice.toPlantNotesProfile(),
        )
    }

    private fun navigateToAddPlantWithAdvice() {
        val state = uiState.value
        if (state.selectedPlant != null) return

        val advice = state.advice ?: return
        val plantName = advice.plantName
            .trim()
            .ifBlank { state.temporaryPlantName.trim() }
        if (plantName.isBlank()) {
            showSuggestedActionMessage(
                message = "Generate advice with a plant name before adding it.",
                isError = true,
            )
            return
        }

        val scientificName = advice.scientificName
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: state.temporaryScientificName.trim().takeIf { it.isNotBlank() }
        emitEffect(
            AiPlantCareEffect.NavigateToAddPlant(
                name = plantName,
                scientificName = scientificName,
                notes = advice.toAddPlantPrefillNotes(scientificName),
                wateringDays = advice.suggestedWateringIntervalDays?.coerceIn(MinReminderDays, MaxReminderDays),
                fertilizingDays = advice.suggestedFertilizingIntervalDays?.coerceIn(MinReminderDays, MaxReminderDays),
            ),
        )
    }

    private fun updateSuggestedActionDraft(value: String) {
        suggestedActionState.update { state ->
            val pendingAction = state.pendingAction
            val nextValue = when (pendingAction) {
                is AiCarePendingAction.SetReminder -> value.filter { it.isDigit() }.take(MaxReminderInputDigits)
                AiCarePendingAction.AddNote -> value.take(MaxSuggestedNoteLength)
                AiCarePendingAction.SaveCareProfile -> value.take(MaxCareProfileNoteLength)
                null -> value
            }
            state.copy(draftValue = nextValue, message = null)
        }
    }

    private fun confirmSuggestedAction() {
        val state = suggestedActionState.value
        val pendingAction = state.pendingAction ?: return
        val selectedPlant = uiState.value.selectedPlant
        val advice = uiState.value.advice ?: return

        viewModelScope.launch {
            suggestedActionState.update { it.copy(isApplying = true, message = null) }
            when (pendingAction) {
                is AiCarePendingAction.SetReminder -> applySuggestedReminder(
                    plant = selectedPlant,
                    careType = pendingAction.careType,
                    intervalValue = state.draftValue,
                )
                AiCarePendingAction.AddNote -> applySuggestedNote(
                    plant = selectedPlant,
                    note = state.draftValue,
                )
                AiCarePendingAction.SaveCareProfile -> applySaveCareProfile(
                    plant = selectedPlant,
                    profile = state.draftValue,
                    advice = advice,
                )
            }
        }
    }

    private suspend fun applySuggestedReminder(
        plant: AiPlantCarePlantUiModel?,
        careType: CareType,
        intervalValue: String,
    ) {
        if (plant == null) {
            showSuggestedActionMessage(
                message = "Choose a saved plant before setting reminders.",
                isError = true,
            )
            return
        }
        val intervalDays = intervalValue.toIntOrNull()
        if (intervalDays == null || intervalDays !in MinReminderDays..MaxReminderDays) {
            showSuggestedActionMessage(
                message = "Use a reminder interval between $MinReminderDays and $MaxReminderDays days.",
                isError = true,
                keepPendingAction = true,
            )
            return
        }

        runCatching {
            reminderRepository.saveReminderForPlant(
                plantId = plant.id,
                careType = careType,
                enabled = true,
                frequencyDays = intervalDays,
                nextDueAt = intervalDays.toDefaultReminderDueAt(),
                preferredHour = DefaultReminderHour,
                preferredMinute = DefaultReminderMinute,
                notificationsEnabled = true,
            )
            reminderNotifications.syncScheduledReminders()
        }
            .onSuccess {
                showSuggestedActionMessage(
                    message = "${careType.label} reminder set for every $intervalDays days.",
                )
            }
            .onFailure { error ->
                showSuggestedActionMessage(
                    message = error.toUserMessage(),
                    isError = true,
                    keepPendingAction = true,
                )
            }
    }

    private suspend fun applySuggestedNote(
        plant: AiPlantCarePlantUiModel?,
        note: String,
    ) {
        if (plant == null) {
            showSuggestedActionMessage(
                message = "Choose a saved plant before adding notes.",
                isError = true,
            )
            return
        }
        val cleanNote = note.trim()
        if (cleanNote.isBlank()) {
            showSuggestedActionMessage(
                message = "Add note text before saving.",
                isError = true,
                keepPendingAction = true,
            )
            return
        }

        runCatching {
            plantRepository.updatePlantNotes(
                plantId = plant.id,
                notes = plant.notes.appendAiCareNote(cleanNote),
            )
        }
            .onSuccess {
                showSuggestedActionMessage(
                    message = "AI suggested note added to ${plant.name}.",
                )
            }
            .onFailure { error ->
                showSuggestedActionMessage(
                    message = error.toUserMessage(),
                    isError = true,
                    keepPendingAction = true,
                )
            }
    }

    private suspend fun applySaveCareProfile(
        plant: AiPlantCarePlantUiModel?,
        profile: String,
        advice: PlantCareAdvice,
    ) {
        if (plant == null) {
            showSuggestedActionMessage(
                message = "Care profile is saved in AI Care cache only. Choose a saved plant to attach it to plant notes.",
            )
            return
        }
        val cleanProfile = profile.trim().ifBlank { advice.toPlantNotesProfile() }

        runCatching {
            plantRepository.updatePlantNotes(
                plantId = plant.id,
                notes = plant.notes.appendAiCareNote(cleanProfile),
            )
        }
            .onSuccess {
                showSuggestedActionMessage(
                    message = "AI care profile saved to ${plant.name} notes.",
                )
            }
            .onFailure { error ->
                showSuggestedActionMessage(
                    message = error.toUserMessage(),
                    isError = true,
                    keepPendingAction = true,
                )
            }
    }

    private fun showSuggestedActionMessage(
        message: String,
        isError: Boolean = false,
        keepPendingAction: Boolean = false,
    ) {
        val heartBurstKey = if (isError) 0L else System.nanoTime()
        if (keepPendingAction) {
            suggestedActionState.update {
                it.copy(
                    isApplying = false,
                    message = message,
                    messageIsError = isError,
                    heartBurstKey = heartBurstKey,
                )
            }
        } else {
            suggestedActionState.value = AiCareSuggestedActionState(
                message = message,
                messageIsError = isError,
                heartBurstKey = heartBurstKey,
            )
        }
        actionMessageDismissJob?.cancel()
        actionMessageDismissJob = viewModelScope.launch {
            delay(ActionMessageVisibleMillis)
            suggestedActionState.update { current ->
                if (
                    current.message == message &&
                    current.messageIsError == isError &&
                    current.heartBurstKey == heartBurstKey
                ) {
                    current.copy(
                        message = null,
                        messageIsError = false,
                        heartBurstKey = 0L,
                    )
                } else {
                    current
                }
            }
        }
    }

    private fun resolveAdviceTarget(): AiPlantCareAdviceTarget? {
        val state = uiState.value
        return when (state.inputMode) {
            AiPlantCareInputMode.SAVED_PLANT -> {
                val plant = state.selectedPlant
                if (plant == null) {
                    showError("Choose one saved plant before asking for care advice.")
                    null
                } else {
                    AiPlantCareAdviceTarget(
                        plantId = plant.id,
                        plantName = plant.name,
                        scientificName = plant.scientificName,
                    )
                }
            }

            AiPlantCareInputMode.TEMPORARY_PLANT -> {
                val plantName = state.temporaryPlantName.trim()
                if (plantName.isBlank()) {
                    showError("Enter a plant name before asking for care advice.")
                    null
                } else {
                    AiPlantCareAdviceTarget(
                        plantId = null,
                        plantName = plantName,
                        scientificName = state.temporaryScientificName.trim().ifBlank { null },
                    )
                }
            }
        }
    }

    private fun showError(message: String) {
        adviceState.value = AiPlantCareAdviceState(errorMessage = message)
    }

    private fun emitEffect(effect: AiPlantCareEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun PlantWithDetails.toAiCarePlantUiModel(): AiPlantCarePlantUiModel {
        val entity = this.plant
        val primaryPhoto = photos.firstOrNull { it.isPrimary } ?: photos.firstOrNull()
        val clock = Clock.systemDefaultZone()
        return AiPlantCarePlantUiModel(
            id = entity.plantId,
            name = entity.name,
            scientificName = entity.plantType.trim().takeIf { it.isNotBlank() },
            location = entity.location,
            photoUri = primaryPhoto?.localUri,
            notes = entity.notes,
            reminderCount = reminders.size,
            careLogCount = careHistory.size,
            wateringCare = toLocalCareSummary(CareType.WATERING, clock),
            fertilizingCare = toLocalCareSummary(CareType.FERTILIZING, clock),
        )
    }

    private fun PlantWithDetails.toLocalCareSummary(
        careType: CareType,
        clock: Clock,
    ): AiCareLocalCareUiModel {
        val reminder = reminders
            .filter { it.careType == careType && it.deletedAt == null }
            .sortedWith(compareByDescending<ReminderEntity> { it.isEnabled }
                .thenBy { it.nextDueAt })
            .firstOrNull()
        val lastCompleted = careHistory
            .filter { entry ->
                entry.careType == careType &&
                    entry.action in setOf(HistoryAction.COMPLETED, HistoryAction.MANUAL_LOG)
            }
            .maxByOrNull { it.performedAt }
        val status = reminder?.toFallbackStatus(clock)
        return AiCareLocalCareUiModel(
            title = careType.label,
            intervalLabel = reminder?.let { reminderEntity ->
                val interval = if (reminderEntity.frequencyDays == 1) {
                    "Every day"
                } else {
                    "Every ${reminderEntity.frequencyDays} days"
                }
                if (reminderEntity.isEnabled) interval else "$interval • reminder off"
            } ?: "No reminder set",
            lastCompletedLabel = lastCompleted?.performedAt?.let { performedAt ->
                "${careType.lastCompletedLabelPrefix()}: ${performedAt.toShortDateLabel(clock)}"
            } ?: "${careType.lastCompletedLabelPrefix()}: not logged yet",
            statusLabel = status?.label ?: "No due date",
            isUrgent = status?.isUrgent ?: false,
        )
    }

    private data class FallbackDueStatus(
        val label: String,
        val isUrgent: Boolean,
    )

    private fun ReminderEntity.toFallbackStatus(clock: Clock): FallbackDueStatus {
        if (!isEnabled) return FallbackDueStatus(label = "Reminder off", isUrgent = false)
        val today = LocalDate.now(clock)
        val dueDate = nextDueAt.toLocalDate(clock.zone)
        return when {
            dueDate.isBefore(today) -> FallbackDueStatus(label = "Overdue", isUrgent = true)
            dueDate == today -> FallbackDueStatus(label = "Due today", isUrgent = true)
            else -> FallbackDueStatus(label = "Upcoming: ${dueDate.format(ShortDateFormatter)}", isUrgent = false)
        }
    }

    private fun CareType.lastCompletedLabelPrefix(): String =
        when (this) {
            CareType.WATERING -> "Last watered"
            CareType.FERTILIZING -> "Last fertilized"
            else -> "Last completed"
        }

    private fun Long.toShortDateLabel(clock: Clock): String =
        toLocalDate(clock.zone).format(ShortDateFormatter)

    private fun Long.toLocalDate(zoneId: ZoneId): LocalDate =
        Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not generate AI care advice. Please try again."

    private fun AiCareCachedAdvice.toAdviceState(cacheKey: String): AiPlantCareAdviceState =
        AiPlantCareAdviceState(
            advice = advice,
            generatedAt = generatedAt,
            modelName = modelName,
            cacheKey = cacheKey,
        )

    private fun Long.formatAdviceGeneratedAt(): String =
        DateFormat
            .getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(this))

    private fun String.appendAiCareNote(note: String): String {
        val cleanExisting = trim()
        val cleanNote = note.trim()
        return if (cleanExisting.isBlank()) {
            cleanNote
        } else {
            "$cleanExisting\n\n$cleanNote"
        }
    }

    private fun String.isObviousNonPlantInput(): Boolean {
        val normalized = trim()
            .lowercase(Locale.ROOT)
            .replace(Regex("[^\\p{L}\\p{N}]"), "")
        if (normalized.isBlank()) return true
        if (normalized.all { it.isDigit() }) return true
        return normalized in setOf(
            "test",
            "testing",
            "\u0442\u0435\u0441\u0442",
            "\u0442\u0435\u0441\u0442\u043E\u0432\u043E",
            "asdf",
            "qwerty",
            "abc",
            "abcd",
            "none",
            "null",
            "unknown",
            "plant",
            "\u0440\u0430\u0441\u0442\u0435\u043D\u0438\u0435",
        )
    }

    private fun String.unrecognizedPlantNameMessage(): String =
        if (any { it in '\u0400'..'\u04FF' }) {
            "WaterMe \u043D\u0435 \u0440\u0430\u0437\u043F\u043E\u0437\u043D\u0430 \u0442\u043E\u0432\u0430 \u043A\u0430\u0442\u043E \u0440\u0430\u0441\u0442\u0435\u043D\u0438\u0435. " +
                "\u041F\u0440\u043E\u0432\u0435\u0440\u0435\u0442\u0435 \u0438\u043C\u0435\u0442\u043E \u0438 \u043E\u043F\u0438\u0442\u0430\u0439\u0442\u0435 \u043E\u0442\u043D\u043E\u0432\u043E."
        } else {
            "WaterMe couldn't recognize this as a plant. Please check the plant name and try again."
        }

    private fun PlantCareAdvice.toFollowUpProfileSummary(): String =
        buildString {
            plantName.takeIf { it.isNotBlank() }?.let { appendLine("Profile plant name: $it") }
            scientificName?.takeIf { it.isNotBlank() }?.let { appendLine("Scientific name: $it") }
            careDifficulty.takeIf { it.isNotBlank() }?.let { appendLine("Care difficulty: $it") }
            shortDescription.takeIf { it.isNotBlank() }?.let { appendLine("Description: $it") }
            watering.takeIf { it.isNotBlank() }?.let { appendLine("Watering: $it") }
            light.takeIf { it.isNotBlank() }?.let { appendLine("Light: $it") }
            temperature.takeIf { it.isNotBlank() }?.let { appendLine("Temperature: $it") }
            humidity.takeIf { it.isNotBlank() }?.let { appendLine("Humidity: $it") }
            fertilizing.takeIf { it.isNotBlank() }?.let { appendLine("Fertilizing: $it") }
            repotting.takeIf { it.isNotBlank() }?.let { appendLine("Repotting: $it") }
            toxicity.takeIf { it.isNotBlank() }?.let { appendLine("Toxicity: $it") }
            suggestedWateringIntervalDays?.let { appendLine("Suggested watering interval: every $it days") }
            suggestedFertilizingIntervalDays?.let { appendLine("Suggested fertilizing interval: every $it days") }
            suggestedLightLevel?.takeIf { it.isNotBlank() }?.let { appendLine("Suggested light: $it") }
            suggestedNote?.takeIf { it.isNotBlank() }?.let { appendLine("Suggested note: $it") }
        }.trim().ifBlank { "No cached profile details are available." }

    private fun AiPlantCarePlantUiModel.toFollowUpSavedPlantData(): String =
        buildString {
            appendLine("Saved plant id: $id")
            appendLine("Saved plant name: $name")
            scientificName?.takeIf { it.isNotBlank() }?.let { appendLine("Saved scientific name: $it") }
            location.takeIf { it.isNotBlank() }?.let { appendLine("Location: $it") }
            notes.takeIf { it.isNotBlank() }?.let { appendLine("Notes: $it") }
            appendLine("Reminder count: $reminderCount")
            appendLine("Care log count: $careLogCount")
            appendLine("Watering status: ${wateringCare.toPromptSummary()}")
            appendLine("Fertilizing status: ${fertilizingCare.toPromptSummary()}")
        }.trim()

    private fun AiCareLocalCareUiModel.toPromptSummary(): String =
        "$intervalLabel; $lastCompletedLabel; $statusLabel"

    private fun PlantCareAdvice.toPlantNotesProfile(): String =
        buildString {
            appendLine("AI Care Profile: ${plantName.ifBlank { "Plant" }}")
            scientificName?.takeIf { it.isNotBlank() }?.let { appendLine("Scientific name: $it") }
            careDifficulty.takeIf { it.isNotBlank() }?.let { appendLine("Difficulty: $it") }
            shortDescription.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            suggestedWateringIntervalDays?.let { appendLine("Suggested watering: every $it days") }
            suggestedFertilizingIntervalDays?.let { appendLine("Suggested fertilizing: every $it days") }
            suggestedLightLevel?.takeIf { it.isNotBlank() }?.let { appendLine("Light: $it") }
            suggestedNote?.takeIf { it.isNotBlank() }?.let { appendLine("Note: $it") }
            disclaimer.takeIf { it.isNotBlank() }?.let { appendLine(it) }
        }.trim()

    private fun PlantCareAdvice.toAddPlantPrefillNotes(scientificNameOverride: String?): String =
        buildString {
            appendLine("AI Care Profile: ${plantName.ifBlank { "Plant" }}")
            scientificNameOverride?.takeIf { it.isNotBlank() }?.let { appendLine("Scientific name: $it") }
            shortDescription.takeIf { it.isNotBlank() }?.let { appendLine(it) }
            suggestedWateringIntervalDays?.let { appendLine("Water: every $it days") }
            suggestedFertilizingIntervalDays?.let { appendLine("Fertilize: every $it days") }
            suggestedLightLevel?.takeIf { it.isNotBlank() }?.let { appendLine("Light: $it") }
            suggestedNote?.takeIf { it.isNotBlank() }?.let { appendLine("Note: $it") }
        }
            .trim()
            .take(MaxAddPlantPrefillNotesLength)

    private fun Int.toDefaultReminderDueAt(): Long =
        LocalDate.now()
            .plusDays(toLong())
            .atTime(DefaultReminderHour, DefaultReminderMinute)
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

    private companion object {
        const val MaxPlantNameLength = 80
        const val MaxScientificNameLength = 120
        const val DefaultReminderHour = 9
        const val DefaultReminderMinute = 0
        const val MinReminderDays = 1
        const val MaxReminderDays = 365
        const val MaxReminderInputDigits = 3
        const val MaxSuggestedNoteLength = 700
        const val MaxCareProfileNoteLength = 2_400
        const val MaxAddPlantPrefillNotesLength = 320
        const val MaxFollowUpQuestionLength = 240
        const val ActionMessageVisibleMillis = 2_400L
        const val SavedAdviceUnavailableMessage = "Showing saved AI advice. Refresh is unavailable right now."
        val ShortDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
    }
}
