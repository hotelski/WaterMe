package com.hotelski.waterme.feature.characters

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CharactersUiState(
    val isLoading: Boolean = false,
    val activeCharacter: PlantCharacterUiModel? = null,
    val characters: List<PlantCharacterUiModel> = emptyList(),
    val achievementSummary: CharacterAchievementSummary = CharacterAchievementSummary(),
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val heartBurstKey: Long = 0L,
)

sealed interface CharactersEvent {
    data object BackClicked : CharactersEvent
    data object RetryClicked : CharactersEvent
    data class CharacterSelected(val characterId: String) : CharactersEvent
}

sealed interface CharactersEffect {
    data object NavigateBack : CharactersEffect
}

private data class CharactersActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val heartBurstKey: Long = 0L,
)

class CharactersViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val careRepository = WaterMeAppContainer.careRepository(appContext)
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val settingsDataStore = WaterMeAppContainer.settingsDataStore(appContext)
    private val actionState = MutableStateFlow(CharactersActionState())
    private val _effects = MutableSharedFlow<CharactersEffect>()
    private var messageDismissJob: Job? = null

    val effects = _effects.asSharedFlow()

    val uiState = combine(
        careRepository.observeCareHistoryForUser(WaterMeAppContainer.LOCAL_USER_ID),
        plantRepository.observePlants(WaterMeAppContainer.LOCAL_USER_ID),
        settingsDataStore.settings,
        actionState,
    ) { careHistory, plants, settings, action ->
        val characters = buildPlantCharacters(
            careHistory = careHistory,
            selectedCharacterId = settings.selectedCharacterId,
            plantsAddedTotal = plants.size,
            appOpenDayStreak = settings.appOpenDayStreak,
        )
        CharactersUiState(
            isLoading = false,
            activeCharacter = characters.firstOrNull { it.isSelected },
            characters = characters,
            achievementSummary = careHistory.toCharacterAchievementSummary(
                plantsAddedTotal = plants.size,
                appOpenDayStreak = settings.appOpenDayStreak,
            ),
            errorMessage = action.errorMessage,
            successMessage = action.successMessage,
            heartBurstKey = action.heartBurstKey,
        )
    }
        .catch { error -> emit(CharactersUiState(errorMessage = error.toUserMessage())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CharactersUiState(isLoading = true),
        )

    init {
        seedDatabase()
    }

    fun onEvent(event: CharactersEvent) {
        when (event) {
            CharactersEvent.BackClicked -> emitEffect(CharactersEffect.NavigateBack)
            CharactersEvent.RetryClicked -> seedDatabase()
            is CharactersEvent.CharacterSelected -> selectCharacter(event.characterId)
        }
    }

    private fun selectCharacter(characterId: String) {
        val character = uiState.value.characters.firstOrNull { it.id == characterId } ?: return
        if (!character.isUnlocked) {
            showMessage(errorMessage = "${character.name} is still locked.")
            return
        }

        viewModelScope.launch {
            runCatching { settingsDataStore.updateSelectedCharacterId(characterId) }
                .onSuccess {
                    actionState.value = CharactersActionState(
                        heartBurstKey = System.nanoTime(),
                    )
                }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            actionState.value = CharactersActionState()
            runCatching { WaterMeAppContainer.seedIfEmpty(appContext) }
                .onFailure { showMessage(errorMessage = it.toUserMessage()) }
        }
    }

    private fun showMessage(
        successMessage: String? = null,
        errorMessage: String? = null,
        heartBurstKey: Long = 0L,
    ) {
        actionState.value = CharactersActionState(
            successMessage = successMessage,
            errorMessage = errorMessage,
            heartBurstKey = heartBurstKey,
        )
        messageDismissJob?.cancel()
        messageDismissJob = viewModelScope.launch {
            delay(MESSAGE_VISIBLE_MILLIS)
            val current = actionState.value
            if (
                current.successMessage == successMessage &&
                current.errorMessage == errorMessage &&
                current.heartBurstKey == heartBurstKey
            ) {
                actionState.value = current.copy(successMessage = null, errorMessage = null, heartBurstKey = 0L)
            }
        }
    }

    private fun emitEffect(effect: CharactersEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not load characters."

    private companion object {
        const val MESSAGE_VISIBLE_MILLIS = 2_400L
    }
}
