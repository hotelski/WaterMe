package com.hotelski.waterme.feature.history

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.CareHistoryRow
import com.hotelski.waterme.feature.common.CareHistoryUiModel
import com.hotelski.waterme.feature.common.WaterMeEmptyState
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePreviewData
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.feature.common.label
import com.hotelski.waterme.model.CareType
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class CareHistoryUiState(
    val isLoading: Boolean = false,
    val selectedFilter: CareType? = null,
    val entries: List<CareHistoryUiModel> = emptyList(),
    val errorMessage: String? = null,
) {
    val isEmpty: Boolean
        get() = !isLoading && entries.isEmpty()
}

sealed interface CareHistoryEvent {
    data object BackClicked : CareHistoryEvent
    data object RetryClicked : CareHistoryEvent
    data class FilterSelected(val careType: CareType?) : CareHistoryEvent
}

@Composable
fun CareHistoryScreen(
    uiState: CareHistoryUiState,
    onEvent: (CareHistoryEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = GardenBackground,
        topBar = {
            WaterMeTopBar(
                title = "Care History",
                navigationIcon = Icons.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = { onEvent(CareHistoryEvent.BackClicked) },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> WaterMeLoadingState("Loading care history...", Modifier.padding(innerPadding))
            uiState.errorMessage != null -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeErrorState(uiState.errorMessage, onRetryClick = { onEvent(CareHistoryEvent.RetryClicked) })
            }

            uiState.isEmpty -> Box(Modifier.padding(innerPadding).padding(20.dp)) {
                WaterMeEmptyState(
                    title = "No care logged",
                    message = "Completed, skipped, and manual care entries will appear here.",
                    icon = Icons.Rounded.History,
                )
            }

            else -> LazyColumn(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize()
                    .background(GardenBackground),
                contentPadding = PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item { WaterMeSectionHeader("All care history") }
                item {
                    androidx.compose.foundation.layout.Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        FilterChip(
                            selected = uiState.selectedFilter == null,
                            onClick = { onEvent(CareHistoryEvent.FilterSelected(null)) },
                            label = { Text("All") },
                        )
                        CareType.entries.forEach { type ->
                            FilterChip(
                                selected = uiState.selectedFilter == type,
                                onClick = { onEvent(CareHistoryEvent.FilterSelected(type)) },
                                label = { Text(type.label()) },
                            )
                        }
                    }
                }
                items(uiState.entries, key = { it.id }) { entry ->
                    CareHistoryRow(entry)
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CareHistoryScreenPreview() {
    WaterMeTheme {
        CareHistoryScreen(
            uiState = CareHistoryUiState(entries = WaterMePreviewData.history),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CareHistoryEmptyPreview() {
    WaterMeTheme {
        CareHistoryScreen(uiState = CareHistoryUiState(), onEvent = {})
    }
}
