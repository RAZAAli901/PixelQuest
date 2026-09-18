package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PixelDayDetailDialog(
    date: LocalDate,
    status: DailyStatus,
    isSimpleMode: Boolean = false,
    onDismiss: () -> Unit
) {
    val colors = PixelTheme.colors
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
    val dateText = date.format(formatter)

    val (statusTitle, statusColor) = when (status) {
        DailyStatus.PERFECT -> (if (isSimpleMode) "✓ COMPLETED" else "🌟 PERFECT DAY!") to colors.tertiary
        DailyStatus.PARTIAL -> (if (isSimpleMode) "⚡ PARTIAL" else "⚡ PARTIAL PROGRESS") to colors.gold
        DailyStatus.MISSED -> (if (isSimpleMode) "✗ MISSED" else "💀 MISSED QUESTS") to colors.error
        DailyStatus.NO_TASKS_SCHEDULED -> (if (isSimpleMode) "NO TASKS SCHEDULED" else "🛡️ NO QUESTS SCHEDULED") to colors.secondary
    }

    PixelDialog(
        title = "DAY DETAILS",
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = dateText.uppercase(),
                style = PixelTypography.labelLarge,
                color = colors.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = statusTitle,
                style = PixelTypography.titleMedium,
                color = statusColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            PixelButton(
                text = "CLOSE",
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(0.6f)
            )
        }
    }
}

@Preview(name = "Day Detail Dialog - Dark", showBackground = true)
@Composable
private fun PixelDayDetailDialogDarkPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Pixel) {
        Box(
            modifier = Modifier
                .background(PixelTheme.colors.background)
                .padding(16.dp)
        ) {
            PixelDayDetailDialog(
                date = LocalDate.now(),
                status = DailyStatus.PERFECT,
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "Day Detail Dialog - Light", showBackground = true)
@Composable
private fun PixelDayDetailDialogLightPreview() {
    PixelQuestTheme(themeMode = ThemeMode.Light) {
        Box(
            modifier = Modifier
                .background(PixelTheme.colors.background)
                .padding(16.dp)
        ) {
            PixelDayDetailDialog(
                date = LocalDate.now(),
                status = DailyStatus.PARTIAL,
                onDismiss = {}
            )
        }
    }
}
