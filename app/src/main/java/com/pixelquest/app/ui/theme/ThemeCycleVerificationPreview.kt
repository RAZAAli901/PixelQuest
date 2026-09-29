package com.pixelquest.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelFloatingActionButton
import com.pixelquest.app.ui.components.PixelStatCard
import com.pixelquest.app.ui.components.PixelTopAppBar

/**
 * Preview harness exercising the full theme cycle:
 * Pixel -> Comic -> Light -> Comic -> Pixel
 *
 * Verifies live theme switching across major screen elements:
 * - TopAppBar chrome
 * - Quest cards & actions
 * - Stat metric cards
 * - Floating action button
 * - No crashes, no stale rendering, and smooth 300ms color cross-fade.
 */
@Composable
fun ThemeCycleVerificationContent(
    initialMode: ThemeMode = ThemeMode.Pixel
) {
    val cycleSequence = remember {
        listOf(
            ThemeMode.Pixel,
            ThemeMode.Comic,
            ThemeMode.Light,
            ThemeMode.Comic,
            ThemeMode.Pixel
        )
    }
    var cycleIndex by remember { mutableIntStateOf(cycleSequence.indexOf(initialMode).coerceAtLeast(0)) }
    val currentMode = cycleSequence[cycleIndex % cycleSequence.size]

    PixelQuestTheme(themeMode = currentMode) {
        val appColorScheme = LocalAppColorScheme.current
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(appColorScheme.background)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 80.dp)
            ) {
                PixelTopAppBar(
                    title = "THEME: ${currentMode.name.uppercase()}"
                )

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    PixelCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "CYCLE STEP ${cycleIndex + 1}/${cycleSequence.size}: ${currentMode.name}",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = appColorScheme.primary
                                )
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = appColorScheme.gold
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Cross-fade active. Verifying dynamic token resolution, typography switches, and border adaptations.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = appColorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            PixelButton(
                                text = "Next Theme in Cycle >>",
                                onClick = {
                                    cycleIndex = (cycleIndex + 1) % cycleSequence.size
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PixelStatCard(
                            label = "XP EARNED",
                            value = "1,450",
                            icon = "⭐",
                            modifier = Modifier.weight(1f)
                        )
                        PixelStatCard(
                            label = "STREAK",
                            value = "7 DAYS",
                            icon = "🔥",
                            modifier = Modifier.weight(1f)
                        )
                    }

                    PixelCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = appColorScheme.gold,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Daily Hero Quest",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = appColorScheme.onSurface
                                )
                                Text(
                                    text = "+50 XP on completion",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = appColorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
            ) {
                PixelFloatingActionButton(
                    onClick = {
                        cycleIndex = (cycleIndex + 1) % cycleSequence.size
                    }
                )
            }
        }
    }
}

@Preview(name = "Cycle Step 1 - Pixel", showBackground = true)
@Composable
fun ThemeCycleStep1PixelPreview() {
    ThemeCycleVerificationContent(initialMode = ThemeMode.Pixel)
}

@Preview(name = "Cycle Step 2 - Comic", showBackground = true)
@Composable
fun ThemeCycleStep2ComicPreview() {
    ThemeCycleVerificationContent(initialMode = ThemeMode.Comic)
}

@Preview(name = "Cycle Step 3 - Light", showBackground = true)
@Composable
fun ThemeCycleStep3LightPreview() {
    ThemeCycleVerificationContent(initialMode = ThemeMode.Light)
}
