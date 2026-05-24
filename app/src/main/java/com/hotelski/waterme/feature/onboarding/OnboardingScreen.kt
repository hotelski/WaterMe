package com.hotelski.waterme.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.R
import com.hotelski.waterme.feature.common.WaterMeErrorState
import com.hotelski.waterme.feature.common.WaterMeLoadingState
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.ui.theme.CardWhite
import com.hotelski.waterme.ui.theme.GardenBackground
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MutedInk
import com.hotelski.waterme.ui.theme.WaterMeTheme

data class OnboardingUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
)

sealed interface OnboardingEvent {
    data object StartClicked : OnboardingEvent
    data object RetryClicked : OnboardingEvent
}

@Composable
fun OnboardingScreen(
    uiState: OnboardingUiState,
    onEvent: (OnboardingEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        uiState.isLoading -> WaterMeLoadingState(message = "Getting WaterMe ready...", modifier = modifier)
        uiState.errorMessage != null -> WaterMeErrorState(
            message = uiState.errorMessage,
            modifier = modifier,
            onRetryClick = { onEvent(OnboardingEvent.RetryClicked) },
        )

        else -> OnboardingContent(
            onStartClick = { onEvent(OnboardingEvent.StartClicked) },
            modifier = modifier,
        )
    }
}

@Composable
private fun OnboardingContent(
    onStartClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(GardenBackground)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .size(132.dp)
                .clip(RoundedCornerShape(36.dp))
                .background(Color(0xFFDFF2E6)),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_waterme_logo),
                contentDescription = "WaterMe logo",
                modifier = Modifier.size(104.dp),
            )
        }

        Spacer(Modifier.height(28.dp))

        Text(
            text = "WaterMe",
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "A calm plant care assistant for watering, feeding, pruning, repotting, and spotting small health changes before they become big ones.",
            modifier = Modifier.padding(top = 14.dp),
            style = MaterialTheme.typography.bodyLarge,
            color = MutedInk,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OnboardingPill("Care tasks", Modifier.weight(1f))
            OnboardingPill("Health notes", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OnboardingPill("Calendar", Modifier.weight(1f))
            OnboardingPill("Notifications", Modifier.weight(1f))
        }

        Spacer(Modifier.height(36.dp))

        WaterMePrimaryButton(
            label = "Start caring for plants",
            onClick = onStartClick,
            icon = Icons.Rounded.Check,
        )
    }
}

@Composable
private fun OnboardingPill(
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = CardWhite,
        tonalElevation = 1.dp,
        shadowElevation = 3.dp,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            style = MaterialTheme.typography.labelLarge,
            color = LeafGreen,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingScreenPreview() {
    WaterMeTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(),
            onEvent = {},
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun OnboardingLoadingPreview() {
    WaterMeTheme {
        OnboardingScreen(
            uiState = OnboardingUiState(isLoading = true),
            onEvent = {},
        )
    }
}
