package com.hotelski.waterme.feature.donate

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LocalFlorist
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.WaterMePremiumCard
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
    onSupportTierClick: (String) -> Unit,
    onRetryClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            WaterMeTopBar(
                title = "Support creator",
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
                WaterMeSectionHeader(title = "Choose an amount")
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

            item {
                DonateCheckoutCard(
                    uiState = uiState,
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
        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.58f),
        accentColor = LeafGreen,
        shape = RoundedCornerShape(32.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            AnimatedSupportBadge()
            Text(
                text = "Support the creator",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "WaterMe is built by an independent creator. Your support helps keep the app maintained.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun AnimatedSupportBadge() {
    val transition = rememberInfiniteTransition(label = "supportCreatorMotion")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = -7f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1450),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "supportCreatorBob",
    )
    val pulse by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "supportCreatorPulse",
    )
    val floatProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2600),
            repeatMode = RepeatMode.Restart,
        ),
        label = "supportCreatorFloat",
    )

    Box(
        modifier = Modifier.size(112.dp),
        contentAlignment = Alignment.Center,
    ) {
        FloatingSupportIcon(
            icon = Icons.Rounded.Favorite,
            color = Clay,
            progress = floatProgress,
            delay = 0f,
            x = (-34).dp,
            y = 22.dp,
        )
        FloatingSupportIcon(
            icon = Icons.Rounded.AutoAwesome,
            color = MistBlue,
            progress = floatProgress,
            delay = 0.32f,
            x = 38.dp,
            y = 14.dp,
        )
        FloatingSupportIcon(
            icon = Icons.Rounded.LocalFlorist,
            color = LeafGreen,
            progress = floatProgress,
            delay = 0.64f,
            x = 2.dp,
            y = (-36).dp,
        )
        Box(
            modifier = Modifier
                .size(78.dp)
                .graphicsLayer {
                    translationY = bob
                    scaleX = pulse
                    scaleY = pulse
                }
                .clip(RoundedCornerShape(30.dp))
                .background(
                    Brush.linearGradient(
                        listOf(
                            LeafGreen.copy(alpha = 0.22f),
                            MistBlue.copy(alpha = 0.16f),
                            MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                        ),
                    ),
                )
                .border(1.dp, LeafGreen.copy(alpha = 0.22f), RoundedCornerShape(30.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Rounded.VolunteerActivism,
                contentDescription = null,
                tint = LeafGreen,
                modifier = Modifier.size(38.dp),
            )
        }
    }
}

@Composable
private fun FloatingSupportIcon(
    icon: ImageVector,
    color: Color,
    progress: Float,
    delay: Float,
    x: Dp,
    y: Dp,
) {
    val delayedProgress = (progress + delay) % 1f
    val alpha = when {
        delayedProgress < 0.16f -> delayedProgress / 0.16f
        delayedProgress > 0.78f -> (1f - delayedProgress) / 0.22f
        else -> 1f
    }.coerceIn(0f, 1f)
    val scale = 0.82f + delayedProgress * 0.22f

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = color,
        modifier = Modifier
            .offset(x = x, y = y - (delayedProgress * 18f).dp)
            .size(22.dp)
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
    )
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
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.015f else 1f,
        animationSpec = tween(durationMillis = 180),
        label = "supportTierScale",
    )
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.78f else 0.34f,
        animationSpec = tween(durationMillis = 180),
        label = "supportTierBorderAlpha",
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(shape)
            .clickable(
                enabled = isEnabled,
                onClick = onClick,
            ),
        shape = shape,
        color = if (isSelected) color.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) color.copy(alpha = borderAlpha) else MaterialTheme.colorScheme.outline.copy(alpha = borderAlpha),
        ),
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
private fun DonateCheckoutCard(
    uiState: DonateUiState,
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
            title = "Thank you"
            message = uiState.successMessage
            color = LeafGreen
            icon = Icons.Rounded.Favorite
        }
        uiState.setupMessage != null -> {
            title = "Play Console setup needed"
            message = uiState.setupMessage
            color = Clay
            icon = Icons.Rounded.Info
        }
        uiState.isLoadingProducts -> {
            title = "Loading support options"
            message = "Checking Google Play."
            color = MistBlue
            icon = Icons.Rounded.Info
        }
        else -> {
            title = "Secure checkout"
            message = "Choose an amount above. Payment is handled by Google Play."
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
            onSupportTierClick = {},
            onRetryClick = {},
        )
    }
}
