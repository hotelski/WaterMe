package com.hotelski.waterme.feature.donate

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.VolunteerActivism
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.feature.common.WaterMePrimaryButton
import com.hotelski.waterme.feature.common.WaterMeSectionHeader
import com.hotelski.waterme.feature.common.WaterMeTopBar
import com.hotelski.waterme.ui.theme.Clay
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.MistBlue
import com.hotelski.waterme.ui.theme.WaterMeTheme

@Composable
fun DonateScreen(
    uiState: DonateUiState,
    onBack: () -> Unit,
    onShareFeedback: () -> Unit,
    onSupportTierClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "Support WaterMe",
                navigationIcon = Icons.AutoMirrored.Rounded.ArrowBack,
                navigationContentDescription = "Back",
                onNavigationClick = onBack,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item { DonateHeroCard() }

            item {
                WaterMeSectionHeader(title = "Choose a support amount")
            }

            items(
                items = uiState.tiers,
                key = { tier -> tier.productId },
            ) { tier ->
                DonateTierCard(
                    tier = tier,
                    isPurchasing = uiState.isPurchasing,
                    isSelected = uiState.selectedProductId == tier.productId,
                    onClick = { onSupportTierClick(tier.productId) },
                )
            }

            item { DonateImpactCard() }

            item {
                DonateCheckoutCard(
                    uiState = uiState,
                    onShareFeedback = onShareFeedback,
                    onRetryClick = onRetryClick,
                )
            }

            item { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun DonateHeroCard() {
    WaterMePremiumCard(
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.64f),
        accentColor = LeafGreen,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(LeafGreen.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.VolunteerActivism,
                    contentDescription = null,
                    tint = LeafGreen,
                    modifier = Modifier.size(34.dp),
                )
            }
            Text(
                text = "Help WaterMe grow",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Optional support helps fund care reminders, plant characters, and the small details that keep WaterMe useful.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun DonateTierCard(
    tier: DonateTierUiState,
    isPurchasing: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(24.dp)
    val color = tier.accentColor()
    val isEnabled = tier.isAvailable && !isPurchasing

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(
                enabled = isEnabled,
                onClick = onClick,
            ),
        shape = shape,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.34f)),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(color.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = tier.icon(),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(25.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = tier.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = tier.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (isSelected && isPurchasing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = LeafGreen,
                    )
                }
                Text(
                    text = tier.amount,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = if (tier.isAvailable) "Google Play" else "Not active",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (tier.isAvailable) LeafGreen else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun DonateImpactCard() {
    WaterMePremiumCard(
        accentColor = MistBlue,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            DonateCardTitle(
                icon = Icons.Rounded.Favorite,
                title = "What support helps with",
                color = LeafGreen,
            )
            DonateCheckRow("Keep core plant care features available.")
            DonateCheckRow("Polish reminders, history, and calendar workflows.")
            DonateCheckRow("Create more character art and care moments.")
        }
    }
}

@Composable
private fun DonateCheckoutCard(
    uiState: DonateUiState,
    onShareFeedback: () -> Unit,
    onRetryClick: () -> Unit,
) {
    val title: String
    val message: String
    val color: Color
    val icon: ImageVector

    when {
        uiState.errorMessage != null -> {
            title = "Google Play Billing needs attention"
            message = uiState.errorMessage
            color = Clay
            icon = Icons.Rounded.Info
        }
        uiState.successMessage != null -> {
            title = "Support status"
            message = uiState.successMessage
            color = LeafGreen
            icon = Icons.Rounded.CheckCircle
        }
        uiState.setupMessage != null -> {
            title = "Play Console setup needed"
            message = uiState.setupMessage
            color = Clay
            icon = Icons.Rounded.Info
        }
        uiState.isLoadingProducts -> {
            title = "Checking Google Play"
            message = "Loading support products from Google Play."
            color = MistBlue
            icon = Icons.Rounded.Info
        }
        else -> {
            title = "Google Play checkout is ready"
            message = "Tap any active support amount above to continue securely with Google Play."
            color = LeafGreen
            icon = Icons.Rounded.VolunteerActivism
        }
    }

    WaterMePremiumCard(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f),
        accentColor = color,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            DonateCardTitle(
                icon = icon,
                title = title,
                color = color,
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.errorMessage != null || uiState.setupMessage != null) {
                OutlinedButton(
                    onClick = onRetryClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(20.dp),
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Check again")
                }
            }
            WaterMePrimaryButton(
                label = "Share feedback",
                onClick = onShareFeedback,
                icon = Icons.Rounded.RateReview,
            )
        }
    }
}

@Composable
private fun DonateCardTitle(
    icon: ImageVector,
    title: String,
    color: Color,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(color.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DonateCheckRow(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            imageVector = Icons.Rounded.CheckCircle,
            contentDescription = null,
            tint = LeafGreen,
            modifier = Modifier
                .padding(top = 1.dp)
                .size(18.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f),
        )
    }
}

private fun DonateTierUiState.icon(): ImageVector =
    when (productId) {
        SupportTier.WateringCan.productId -> Icons.Rounded.WaterDrop
        SupportTier.FreshGrowth.productId -> Icons.Rounded.LocalFlorist
        SupportTier.GardenBoost.productId -> Icons.Rounded.AutoAwesome
        else -> Icons.Rounded.VolunteerActivism
    }

private fun DonateTierUiState.accentColor(): Color =
    when (productId) {
        SupportTier.WateringCan.productId -> MistBlue
        SupportTier.FreshGrowth.productId -> LeafGreen
        SupportTier.GardenBoost.productId -> Clay
        else -> LeafGreen
    }

@Preview(showBackground = true)
@Composable
private fun DonateScreenPreview() {
    WaterMeTheme {
        DonateScreen(
            uiState = DonateUiState(),
            onBack = {},
            onShareFeedback = {},
            onSupportTierClick = {},
            onRetryClick = {},
        )
    }
}
