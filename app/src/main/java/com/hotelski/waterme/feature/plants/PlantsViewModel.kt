package com.hotelski.waterme.feature.plants

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotelski.waterme.appstate.WaterMeAppContainer
import com.hotelski.waterme.feature.common.PlantCardUiModel
import com.hotelski.waterme.feature.common.endOfTodayMillis
import com.hotelski.waterme.feature.common.toPlantCardUiModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface PlantsEffect {
    data object NavigateToAddPlant : PlantsEffect
    data class NavigateToEditPlant(val plantId: String) : PlantsEffect
}

private data class PlantsActionState(
    val errorMessage: String? = null,
    val successMessage: String? = null,
)

private data class PlantsFilterState(
    val searchQuery: String = "",
    val showFavoritesOnly: Boolean = false,
)

class PlantsViewModel(
    application: Application,
) : AndroidViewModel(application) {
    private val appContext = application.applicationContext
    private val plantRepository = WaterMeAppContainer.plantRepository(appContext)
    private val careRepository = WaterMeAppContainer.careRepository(appContext)

    private val searchQuery = MutableStateFlow("")
    private val showFavoritesOnly = MutableStateFlow(false)
    private val selectedPlantPanels = MutableStateFlow<Map<String, PlantCardPanel>>(emptyMap())
    private val actionState = MutableStateFlow(PlantsActionState())
    private val _effects = MutableSharedFlow<PlantsEffect>()

    val effects = _effects.asSharedFlow()

    private val filters = combine(searchQuery, showFavoritesOnly) { query, favoritesOnly ->
        PlantsFilterState(searchQuery = query, showFavoritesOnly = favoritesOnly)
    }

    val uiState = combine(
        plantRepository.observePlantsWithDetails(WaterMeAppContainer.LOCAL_USER_ID),
        careRepository.observeTasksDueBy(endOfTodayMillis()),
        filters,
        selectedPlantPanels,
        actionState,
    ) { plants, tasksDueToday, filters, selectedPanels, action ->
        val query = filters.searchQuery
        val favoritesOnly = filters.showFavoritesOnly
        val normalizedQuery = query.trim()
        val dueCountsByPlantId = tasksDueToday.groupingBy { it.plantId }.eachCount()
        val plantCards = plants
            .map { plant -> plant.toPlantCardUiModel(dueTaskCount = dueCountsByPlantId[plant.plant.plantId] ?: 0) }
            .sortedWith(compareByDescending<PlantCardUiModel> { it.isFavorite }.thenBy { it.name.lowercase() })
            .filter { plant ->
                normalizedQuery.isBlank() ||
                    plant.name.contains(normalizedQuery, ignoreCase = true)
            }
            .filter { plant -> !favoritesOnly || plant.isFavorite }
        val favoriteCount = plants.count { it.plant.isFavorite }

        PlantsUiState(
            isLoading = false,
            plants = plantCards,
            searchQuery = query,
            showFavoritesOnly = favoritesOnly,
            favoriteCount = favoriteCount,
            selectedPlantPanels = selectedPanels,
            errorMessage = action.errorMessage,
            successMessage = action.successMessage,
        )
    }
        .catch { error -> emit(PlantsUiState(errorMessage = error.toUserMessage())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = PlantsUiState(isLoading = true),
        )

    init {
        seedDatabase()
    }

    fun onEvent(event: PlantsEvent) {
        when (event) {
            PlantsEvent.AddPlantClicked -> emitEffect(PlantsEffect.NavigateToAddPlant)
            is PlantsEvent.EditPlantClicked -> emitEffect(PlantsEffect.NavigateToEditPlant(event.plantId))
            is PlantsEvent.FavoriteToggled -> toggleFavorite(event.plantId, event.isFavorite)
            PlantsEvent.FavoriteFilterToggled -> toggleFavoriteFilter()
            is PlantsEvent.PlantPanelClicked -> togglePlantPanel(event.plantId, event.panel)
            is PlantsEvent.SearchQueryChanged -> updateSearchQuery(event.value)
            PlantsEvent.RetryClicked -> seedDatabase()
        }
    }

    private fun togglePlantPanel(plantId: String, panel: PlantCardPanel) {
        selectedPlantPanels.value = if (selectedPlantPanels.value[plantId] == panel) {
            selectedPlantPanels.value - plantId
        } else {
            selectedPlantPanels.value + (plantId to panel)
        }
    }

    private fun toggleFavorite(plantId: String, isFavorite: Boolean) {
        viewModelScope.launch {
            actionState.value = PlantsActionState()
            runCatching { plantRepository.setPlantFavorite(plantId, !isFavorite) }
                .onFailure { actionState.value = PlantsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun toggleFavoriteFilter() {
        showFavoritesOnly.value = !showFavoritesOnly.value
        actionState.value = PlantsActionState()
    }

    private fun updateSearchQuery(value: String) {
        searchQuery.value = value.take(MAX_SEARCH_LENGTH)
        actionState.value = if (value.length > MAX_SEARCH_LENGTH) {
            PlantsActionState(errorMessage = "Search is limited to $MAX_SEARCH_LENGTH characters.")
        } else {
            PlantsActionState()
        }
    }

    private fun seedDatabase() {
        viewModelScope.launch {
            actionState.value = PlantsActionState()
            runCatching { WaterMeAppContainer.seedIfEmpty(appContext) }
                .onFailure { actionState.value = PlantsActionState(errorMessage = it.toUserMessage()) }
        }
    }

    private fun emitEffect(effect: PlantsEffect) {
        viewModelScope.launch { _effects.emit(effect) }
    }

    private fun Throwable.toUserMessage(): String =
        message?.takeIf { it.isNotBlank() } ?: "WaterMe could not load your plants."

    private companion object {
        const val MAX_SEARCH_LENGTH = 80
    }
}
