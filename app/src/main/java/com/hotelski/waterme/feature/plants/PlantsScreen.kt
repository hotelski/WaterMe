package com.hotelski.waterme.feature.plants

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.PlantCard
import com.hotelski.waterme.feature.common.PlantCardUiModel
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class PlantsUiState(
    val isLoading: Boolean = false,
    val plants: List<PlantCardUiModel> = emptyList(),
    val searchQuery: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && plants.isEmpty() && searchQuery.isBlank()
}

sealed interface PlantsEvent {
    data object AddPlantClicked : PlantsEvent
    data object RetryClicked : PlantsEvent
    data class PlantClicked(val plantId: String) : PlantsEvent
    data class SearchQueryChanged(val value: String) : PlantsEvent
}

@Composable
fun PlantsScreen(
    uiState: PlantsUiState,
    onEvent: (PlantsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = { WaterMeTopBar(title = "Plants") },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { onEvent(PlantsEvent.AddPlantClicked) },
                containerColor = LeafGreen,
                contentColor = Color.White,
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add plant")
            }
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading your plants...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(PlantsEvent.RetryClicked) })
            }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(GardenBackground),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    WaterMeSectionHeader(
                        title = "${uiState.plants.size} plants",
                        actionLabel = "Add",
                        onActionClick = { onEvent(PlantsEvent.AddPlantClicked) },
                    )
                }
                item {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = { onEvent(PlantsEvent.SearchQueryChanged(it)) },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Search plants") },
                        singleLine = true,
                    )
                }

                when {
                    uiState.isEmpty -> item {
                        WaterMeEmptyState(
                            title = "Add your first plant",
                            message = "Create a care schedule with reminders and notes.",
                            actionLabel = "Add plant",
                            onActionClick = { onEvent(PlantsEvent.AddPlantClicked) },
                        )
                    }

                    uiState.plants.isEmpty() -> item {
                        WaterMeEmptyState(
                            title = "No matching plants",
                            message = "Try another name, type, or room.",
                        )
                    }

                    else -> items(uiState.plants, key = { it.id }) { plant ->
                        PlantCard(
                            plant = plant,
                            onClick = { onEvent(PlantsEvent.PlantClicked(plant.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantsScreenPreview() {
    WaterMeTheme {
        PlantsScreen(
            uiState = PlantsUiState(plants = WaterMePreviewData.plants),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun PlantsEmptyPreview() {
    WaterMeTheme {
        PlantsScreen(
            uiState = PlantsUiState(),
            onEvent = {},
        )
    }
}
