package com.hotelski.waterme.feature.characters

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.WaterMePremiumCard

@Composable
fun PlantCharacterAvatar(
    character: PlantCharacterUiModel,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    animated: Boolean = false,
    heartBurstKey: Any? = null,
    showBackdrop: Boolean = true,
    alwaysShowHearts: Boolean = false,
    heartColor: Color? = null,
) {
    val accent = Color(character.accentColor)
    val resolvedHeartColor = heartColor ?: accent
    val transition = rememberInfiniteTransition(label = "PlantCharacterAvatar")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (animated) -5f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PlantCharacterBob",
    )
    val scale by transition.animateFloat(
        initialValue = 1f,
        targetValue = if (animated) 1.04f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "PlantCharacterScale",
    )

    Box(
        modifier = modifier
            .size(size)
            .offset(y = bob.dp)
            .scale(scale),
        contentAlignment = Alignment.Center,
    ) {
        if (showBackdrop) {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(RoundedCornerShape(size * 0.30f))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(size * 0.76f)
                        .background(accent.copy(alpha = 0.20f), CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    CharacterImage(character = character, size = size * 0.76f)
                }
            }
        } else {
            CharacterImage(character = character, size = size * 0.92f)
        }
        HeartBurstOverlay(
            burstKey = heartBurstKey,
            size = size,
            color = resolvedHeartColor,
            alwaysVisible = alwaysShowHearts,
        )
    }
}

@Composable
private fun CharacterImage(
    character: PlantCharacterUiModel,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    Image(
        painter = painterResource(character.imageResId),
        contentDescription = character.name,
        modifier = modifier.size(size),
        contentScale = ContentScale.Fit,
    )
}

@Composable
fun PlantCharacterCelebrationCard(
    character: PlantCharacterUiModel,
    message: String,
    modifier: Modifier = Modifier,
    heartBurstKey: Any? = null,
) {
    WaterMePremiumCard(
        modifier = modifier,
        containerColor = Color(character.accentColor).copy(alpha = 0.12f),
        accentColor = Color(character.accentColor),
        shape = RoundedCornerShape(28.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlantCharacterAvatar(
                character = character,
                size = 64.dp,
                animated = true,
                heartBurstKey = heartBurstKey,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.celebrationTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message.ifBlank { character.celebrationMessage },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                Icons.Rounded.Check,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = Color(character.accentColor),
            )
        }
    }
}

@Composable
private fun HeartBurstOverlay(
    burstKey: Any?,
    size: Dp,
    color: Color,
    alwaysVisible: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val progress = androidx.compose.runtime.remember { Animatable(1f) }
    val isRunning = androidx.compose.runtime.remember { mutableStateOf(false) }
    val loopTransition = rememberInfiniteTransition(label = "PersistentHeartBurst")
    val loopProgress by loopTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1550),
            repeatMode = RepeatMode.Restart,
        ),
        label = "PersistentHeartProgress",
    )

    LaunchedEffect(burstKey) {
        if (burstKey == null) return@LaunchedEffect
        isRunning.value = true
        progress.snapTo(0f)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950),
        )
        isRunning.value = false
    }

    if (!alwaysVisible && !isRunning.value) return

    val value = progress.value
    val alpha = (1f - value).coerceIn(0f, 1f)
    val heartScale = 0.72f + value * 0.55f
    val hearts = listOf(
        HeartSpec(x = -0.30f, y = -0.36f, driftX = -0.18f, driftY = -0.42f),
        HeartSpec(x = 0.26f, y = -0.34f, driftX = 0.16f, driftY = -0.40f),
        HeartSpec(x = -0.08f, y = -0.42f, driftX = -0.04f, driftY = -0.52f),
        HeartSpec(x = 0.08f, y = -0.25f, driftX = 0.08f, driftY = -0.36f),
    )

    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        if (alwaysVisible) {
            hearts.forEachIndexed { index, heart ->
                val delayedProgress = ((loopProgress + index * 0.18f) % 1f)
                val loopAlpha = when {
                    delayedProgress < 0.18f -> delayedProgress / 0.18f
                    delayedProgress > 0.76f -> (1f - delayedProgress) / 0.24f
                    else -> 1f
                }.coerceIn(0f, 1f)
                val loopScale = 0.78f + delayedProgress * 0.46f
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size * 0.16f)
                        .offset(
                            x = size * (heart.x + heart.driftX * delayedProgress * 0.72f),
                            y = size * (heart.y + heart.driftY * delayedProgress * 0.72f),
                        )
                        .graphicsLayer(
                            alpha = loopAlpha,
                            scaleX = loopScale,
                            scaleY = loopScale,
                            rotationZ = if (index % 2 == 0) -10f * delayedProgress else 10f * delayedProgress,
                        ),
                    tint = color,
                )
            }
        }
        if (isRunning.value) {
            hearts.forEachIndexed { index, heart ->
                Icon(
                    imageVector = Icons.Rounded.Favorite,
                    contentDescription = null,
                    modifier = Modifier
                        .size(size * 0.18f)
                        .offset(
                            x = size * (heart.x + heart.driftX * value),
                            y = size * (heart.y + heart.driftY * value),
                        )
                        .graphicsLayer(
                            alpha = alpha,
                            scaleX = heartScale,
                            scaleY = heartScale,
                            rotationZ = if (index % 2 == 0) -12f * value else 12f * value,
                        ),
                    tint = color,
                )
            }
        }
    }
}

private data class HeartSpec(
    val x: Float,
    val y: Float,
    val driftX: Float,
    val driftY: Float,
)
