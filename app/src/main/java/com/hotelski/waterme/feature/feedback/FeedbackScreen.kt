package com.hotelski.waterme.feature.feedback

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.RateReview
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.hotelski.waterme.R
import com.hotelski.waterme.ui.theme.LeafGreen
import com.hotelski.waterme.ui.theme.WaterMeTheme

@Composable
fun FeedbackScreen(
    uiState: FeedbackUiState,
    onEvent: (FeedbackEvent) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                        MaterialTheme.colorScheme.background,
                        MaterialTheme.colorScheme.background,
                    ),
                ),
            ),
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                top = 16.dp,
                end = 20.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                FeedbackHeader(
                    onBack = onBack,
                    modifier = Modifier.statusBarsPadding(),
                )
            }

            item {
                FeedbackFormCard(
                    uiState = uiState,
                    onEvent = onEvent,
                    modifier = Modifier.navigationBarsPadding(),
                )
            }
        }
    }
}

@Composable
private fun FeedbackHeader(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onBack,
            modifier = Modifier
                .size(46.dp)
                .clip(RoundedCornerShape(17.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.88f)),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface,
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.ic_waterme_logo),
                contentDescription = null,
                modifier = Modifier.size(28.dp),
            )
            Text(
                text = "Feedback",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}

@Composable
private fun FeedbackFormCard(
    uiState: FeedbackUiState,
    onEvent: (FeedbackEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 14.dp,
                shape = RoundedCornerShape(30.dp),
                ambientColor = LeafGreen.copy(alpha = 0.10f),
                spotColor = LeafGreen.copy(alpha = 0.16f),
            ),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.30f)),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(LeafGreen.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.RateReview,
                        contentDescription = null,
                        tint = LeafGreen,
                        modifier = Modifier.size(25.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Send feedback",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "Tell us what to improve, fix, or keep.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FeedbackTopic.entries.forEach { topic ->
                    FeedbackTopicChip(
                        topic = topic,
                        selected = topic == uiState.selectedTopic,
                        enabled = !uiState.isSending,
                        onClick = { onEvent(FeedbackEvent.TopicSelected(topic)) },
                    )
                }
            }

            Text(
                text = "Contact details are optional.",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            FeedbackTextField(
                value = uiState.name,
                onValueChange = { onEvent(FeedbackEvent.NameChanged(it)) },
                label = "Name",
                leadingIcon = Icons.Rounded.Person,
                singleLine = true,
                enabled = !uiState.isSending,
            )

            FeedbackTextField(
                value = uiState.email,
                onValueChange = { onEvent(FeedbackEvent.EmailChanged(it)) },
                label = "Email",
                leadingIcon = Icons.Rounded.Email,
                singleLine = true,
                keyboardType = KeyboardType.Email,
                enabled = !uiState.isSending,
            )

            FeedbackTextField(
                value = uiState.message,
                onValueChange = { onEvent(FeedbackEvent.MessageChanged(it)) },
                label = "Feedback",
                leadingIcon = Icons.Rounded.TipsAndUpdates,
                minLines = 4,
                maxLines = 6,
                imeAction = ImeAction.Default,
                enabled = !uiState.isSending,
            )

            Text(
                text = "${uiState.message.length}/$MAX_FEEDBACK_LENGTH",
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (uiState.sentMessage != null) {
                FeedbackMessage(
                    message = uiState.sentMessage,
                    color = LeafGreen,
                    icon = Icons.Rounded.AutoAwesome,
                )
            }

            if (uiState.sendError != null) {
                FeedbackMessage(
                    message = uiState.sendError,
                    color = Color(0xFFC77842),
                    icon = Icons.Rounded.BugReport,
                )
            }

            Button(
                onClick = { onEvent(FeedbackEvent.SendClicked) },
                enabled = uiState.canSend,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(19.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = LeafGreen,
                    contentColor = Color.White,
                    disabledContainerColor = LeafGreen.copy(alpha = 0.28f),
                    disabledContentColor = Color.White.copy(alpha = 0.74f),
                ),
            ) {
                if (uiState.isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Sending...",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                } else {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Send,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Send feedback",
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedbackTopicChip(
    topic: FeedbackTopic,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val background = if (selected) LeafGreen else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.48f)
    val contentColor = if (selected) Color.White else MaterialTheme.colorScheme.onSurface

    Surface(
        modifier = Modifier.clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(999.dp),
        color = if (enabled) background else background.copy(alpha = 0.58f),
        contentColor = if (enabled) contentColor else contentColor.copy(alpha = 0.64f),
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) Color.White.copy(alpha = 0.42f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.34f),
        ),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = topic.icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = topic.label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun FeedbackTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    singleLine: Boolean = false,
    minLines: Int = 1,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    imeAction: ImeAction = ImeAction.Next,
    enabled: Boolean = true,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = LeafGreen,
            )
        },
        singleLine = singleLine,
        minLines = minLines,
        maxLines = maxLines,
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = imeAction,
        ),
        keyboardActions = KeyboardActions.Default,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LeafGreen,
            focusedLabelColor = LeafGreen,
            cursorColor = LeafGreen,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.56f),
        ),
    )
}

@Composable
private fun FeedbackMessage(
    message: String,
    color: Color,
    icon: ImageVector,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(color.copy(alpha = 0.11f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

enum class FeedbackTopic(
    val label: String,
    val icon: ImageVector,
) {
    Idea("Idea", Icons.Rounded.Lightbulb),
    Issue("Issue", Icons.Rounded.BugReport),
    Delight("Delight", Icons.Rounded.Favorite),
}

internal const val MAX_FEEDBACK_LENGTH = 900

@Preview(showBackground = true)
@Composable
private fun FeedbackScreenPreview() {
    WaterMeTheme {
        FeedbackScreen(
            uiState = FeedbackUiState(),
            onEvent = {},
            onBack = {},
        )
    }
}
