package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.customActions
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.data.local.entity.TaskEntity
import androidx.compose.material3.MaterialTheme

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

/**
 * Isolated Composable for Today's Dashboard quest card.
 * Designed for atomic recomposition so countdown timer ticks do not trigger parent screen re-renders.
 */
@Composable
fun TodayQuestCard(
    task: TaskEntity,
    status: TaskItemStatus,
    onQuickComplete: () -> Unit,
    onQuickSkip: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDone = status == TaskItemStatus.DONE || status == TaskItemStatus.COMPLETED
    val isMissed = status == TaskItemStatus.MISSED
    val isGracePeriod = status == TaskItemStatus.GRACE_PERIOD
    val isActivePending = status == TaskItemStatus.PENDING || isGracePeriod

    var offsetX by remember { mutableStateOf(0f) }

    val cardVariant = when {
        isDone -> PixelPanelVariant.BLUE
        isMissed -> PixelPanelVariant.BORDER
        else -> PixelPanelVariant.BEIGE
    }

    PixelCard(
        variant = cardVariant,
        contentPadding = 12.dp,
        modifier = modifier
            .fillMaxWidth()
            // Swipe right to complete, left to skip (which asks first). The card follows the finger.
            .offset { androidx.compose.ui.unit.IntOffset(offsetX.toInt(), 0) }
            .pointerInput(isDone, isMissed) {
                if (!isDone && !isMissed) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            when {
                                offsetX > 150f -> onQuickComplete()
                                offsetX < -150f -> onQuickSkip()
                            }
                            offsetX = 0f
                        },
                        onDragCancel = { offsetX = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            offsetX = (offsetX + dragAmount).coerceIn(-300f, 300f)
                        }
                    )
                }
            }
            .semantics {
                if (isActivePending) {
                    customActions = listOf(
                        androidx.compose.ui.semantics.CustomAccessibilityAction("Complete") { onQuickComplete(); true },
                        androidx.compose.ui.semantics.CustomAccessibilityAction("Skip") { onQuickSkip(); true }
                    )
                }
            }
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            val colors = com.pixelquest.app.ui.theme.PixelTheme.colors
            val mode = com.pixelquest.app.ui.theme.PixelTheme.mode

            PixelCategoryIcon(
                category = task.category,
                modifier = Modifier.padding(end = 12.dp),
                size = 28.dp
            )

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = task.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        textDecoration = if (isMissed) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                    ),
                    color = when {
                        isDone -> com.pixelquest.app.ui.theme.inkOnPanel(colors.tertiary)
                        isMissed -> colors.error
                        else -> com.pixelquest.app.ui.theme.inkOnPanel(colors.primary)
                    }
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "⏰ ${task.scheduledTime}",
                        style = MaterialTheme.typography.bodySmall,
                        color = com.pixelquest.app.ui.theme.inkOnPanel(colors.secondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = task.category.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = com.pixelquest.app.ui.theme.inkOnPanel(colors.onSurfaceVariant)
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            when {
                isDone -> {
                    PixelCard(
                        variant = PixelPanelVariant.BLUE,
                        contentPadding = 6.dp
                    ) {
                        Text(
                            text = "✓ DONE",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.onSecondary
                        )
                    }
                }
                isMissed -> {
                    // Missed or skipped, but still today: it can be completed late.
                    Column(horizontalAlignment = Alignment.End) {
                        PixelCard(
                            variant = PixelPanelVariant.BORDER,
                            contentPadding = 6.dp
                        ) {
                            Text(
                                text = "✗ MISSED",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.error
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        PixelButton(
                            text = "✓ DONE",
                            onClick = onQuickComplete,
                            variant = PixelButtonVariant.BLUE
                        )
                    }
                }
                else -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        PixelCountdownTimer(
                            scheduledTime = task.scheduledTime,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        PixelButton(
                            text = "✓ DONE",
                            onClick = onQuickComplete,
                            variant = PixelButtonVariant.YELLOW
                        )
                    }
                }
            }
        }
    }
}
