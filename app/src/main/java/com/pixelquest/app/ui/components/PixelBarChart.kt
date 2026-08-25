package com.pixelquest.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.theme.PixelBackgroundCard
import com.pixelquest.app.ui.theme.PixelCyan
import com.pixelquest.app.ui.theme.PixelGold
import com.pixelquest.app.ui.theme.PixelGreen
import com.pixelquest.app.ui.theme.PixelTextWhite
import com.pixelquest.app.ui.theme.PixelTypography

@Composable
fun PixelBarChart(
    weeklyData: List<Pair<String, Float>>,
    modifier: Modifier = Modifier,
    maxHeightDp: Int = 100
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "📈 WEEKLY TREND",
            style = PixelTypography.titleMedium,
            color = PixelGold
        )

        Spacer(modifier = Modifier.height(12.dp))

        PixelCard(
            variant = PixelPanelVariant.BEIGE,
            contentPadding = 12.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.Bottom,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(maxHeightDp.dp + 32.dp)
            ) {
                weeklyData.forEach { (weekLabel, rate) ->
                    val percentageInt = (rate * 100).toInt()
                    val barHeight = (rate.coerceIn(0.05f, 1f) * maxHeightDp).dp

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Text(
                            text = "$percentageInt%",
                            style = PixelTypography.labelSmall.copy(fontSize = 7.sp),
                            color = PixelGreen
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Box(
                            modifier = Modifier
                                .width(20.dp)
                                .height(barHeight)
                                .background(PixelGreen, shape = RoundedCornerShape(2.dp))
                                .border(1.dp, Color(0xFF1C7139), shape = RoundedCornerShape(2.dp))
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = weekLabel,
                            style = PixelTypography.labelSmall.copy(fontSize = 8.sp),
                            color = PixelCyan
                        )
                    }
                }
            }
        }
    }
}
