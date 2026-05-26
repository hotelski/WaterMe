package com.hotelski.waterme.feature.characters

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.material.icons.rounded.LocalFlorist
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.feature.common.WaterMePremiumCard
import com.hotelski.waterme.ui.theme.Ink
import com.hotelski.waterme.ui.theme.MutedInk

@Composable
fun PlantCharacterAvatar(
    character: PlantCharacterUiModel,
    modifier: Modifier = Modifier,
    size: Dp = 96.dp,
    animated: Boolean = false,
) {
    val accent = Color(character.accentColor)
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
            .scale(scale)
            .clip(RoundedCornerShape(size * 0.30f))
            .background(accent.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.72f)
                .clip(CircleShape)
                .background(accent.copy(alpha = 0.20f)),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(width = size * 0.42f, height = size * 0.34f)
                    .align(Alignment.BottomCenter)
                    .clip(RoundedCornerShape(size * 0.10f))
                    .background(accent.copy(alpha = 0.82f)),
            )
            Icon(
                imageVector = Icons.Rounded.LocalFlorist,
                contentDescription = character.name,
                modifier = Modifier
                    .size(size * 0.46f)
                    .align(Alignment.Center),
                tint = accent,
            )
            Box(
                modifier = Modifier
                    .size(size * 0.08f)
                    .align(Alignment.Center)
                    .offset(x = -(size * 0.10f), y = size * 0.08f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.86f)),
            )
            Box(
                modifier = Modifier
                    .size(size * 0.08f)
                    .align(Alignment.Center)
                    .offset(x = size * 0.10f, y = size * 0.08f)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.86f)),
            )
        }
    }
}

@Composable
fun PlantCharacterCelebrationCard(
    character: PlantCharacterUiModel,
    message: String,
    modifier: Modifier = Modifier,
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
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = character.celebrationTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = message.ifBlank { character.celebrationMessage },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedInk,
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
