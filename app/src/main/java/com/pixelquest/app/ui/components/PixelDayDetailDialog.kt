package com.pixelquest.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pixelquest.app.domain.model.DailyStatus
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelRed
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography
import com.pixelquest.app.ui.theme.PixelYellow
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun PixelDayDetailDialog(
    date: LocalDate,
    status: DailyStatus,
    onDismiss: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
    val dateText = date.format(formatter)

    val (statusTitle, statusColor) = when (status) {
        DailyStatus.PERFECT -> "🌟 PERFECT DAY!" to PixelGreen
        DailyStatus.PARTIAL -> "⚡ PARTIAL PROGRESS" to PixelYellow
        DailyStatus.MISSED -> "💀 MISSED QUESTS" to PixelRed
        DailyStatus.NO_TASKS_SCHEDULED -> "🛡️ NO QUESTS SCHEDULED" to PixelCyan
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
                color = PixelGold
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
