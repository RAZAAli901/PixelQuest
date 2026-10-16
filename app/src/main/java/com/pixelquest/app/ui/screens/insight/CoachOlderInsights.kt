package com.pixelquest.app.ui.screens.insight

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.theme.BangersFontFamily
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode

/**
 * Earlier insights under the latest one, each a single line (its date and the start of its summary)
 * until opened; one is open at a time (C12).
 */
@Composable
fun CoachOlderInsights(
    items: List<OlderInsight>,
    expandedId: Long?,
    onToggle: (Long) -> Unit,
    themeMode: ThemeMode,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) return
    val colors = PixelTheme.colors
    val ink = when (themeMode) {
        ThemeMode.Comic -> ComicTokens.SolidBlack
        ThemeMode.Light -> Color(0xFF1E293B)
        else -> colors.onSurface
    }
    val accent = when (themeMode) {
        ThemeMode.Comic -> ComicTokens.CoralInk
        ThemeMode.Light -> Color(0xFF4F46E5)
        else -> colors.primary
    }
    val heading = stringResource(R.string.coach_earlier)
    Column(modifier = modifier.fillMaxWidth()) {
        Spacer(modifier = Modifier.height(CoachDimens.ItemGap))
        Text(
            text = if (themeMode == ThemeMode.Light) heading else heading.uppercase(),
            style = when (themeMode) {
                ThemeMode.Light -> MaterialTheme.typography.labelLarge
                ThemeMode.Comic -> MaterialTheme.typography.labelLarge.copy(fontFamily = BangersFontFamily)
                else -> PixelTypography.labelMedium
            },
            color = accent,
            fontWeight = FontWeight.Bold
        )
        items.forEach { item ->
            Spacer(modifier = Modifier.height(4.dp))
            OlderInsightRow(
                item = item,
                expanded = item.id == expandedId,
                onToggle = { onToggle(item.id) },
                ink = ink,
                accent = accent
            )
        }
    }
}

@Composable
private fun OlderInsightRow(
    item: OlderInsight,
    expanded: Boolean,
    onToggle: () -> Unit,
    ink: Color,
    accent: Color
) {
    val date = rememberDateFormatted(item.insight.generatedAt)
    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = CoachDimens.MinTouchTarget)
            .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
            .clickable(onClickLabel = stringResource(if (expanded) R.string.coach_collapse else R.string.coach_expand), role = Role.Button, onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .coachAnimateSize()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = date,
                style = MaterialTheme.typography.labelSmall,
                color = accent,
                fontWeight = FontWeight.Bold,
                maxLines = 1
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = item.insight.summary,
                style = MaterialTheme.typography.bodySmall,
                color = ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(text = if (expanded) " ▴" else " ▾", color = accent, style = MaterialTheme.typography.labelMedium)
        }
        if (expanded) {
            OlderInsightBody(item = item, ink = ink)
        }
    }
}

@Composable
private fun ColumnScope.OlderInsightBody(item: OlderInsight, ink: Color) {
    listOf(item.insight.summary, item.insight.suggestion, item.insight.encouragement)
        .filter { it.isNotBlank() }
        .forEach { text ->
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = ink)
        }
}
