package com.pixelquest.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.ui.components.ComicButton
import com.pixelquest.app.ui.components.ComicButtonVariant
import com.pixelquest.app.ui.components.ComicFloatingActionButton
import com.pixelquest.app.ui.components.ComicPanel
import com.pixelquest.app.ui.components.ComicPanelVariant
import com.pixelquest.app.ui.components.ComicTopAppBar

/**
 * Step 32 (Day 23): Side-by-side visual comparison preview of representative
 * Comic-mode screens against the Nitnode graphic novel design reference.
 *
 * Checks overall cohesion:
 * 1. Consistent 2.5dp black ink contours and 4dp flat offset drop shadows
 * 2. Signature Nitnode color harmony (Paper Canvas, Coral Red, Burnt Orange, Sky Blue, Gold)
 * 3. Comic typography hierarchy (Bangers headers with high-legibility body)
 * 4. Whole-app chrome and in-context components functioning together as a cohesive graphic novel.
 */
@Composable
fun ComicReferenceFidelityComparisonContent() {
    PixelQuestTheme(themeMode = ThemeMode.Comic) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(ComicTokens.PaperBackground)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 96.dp)
            ) {
                ComicTopAppBar(
                    title = "NITNODE REFERENCE FIDELITY",
                    actions = {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(ComicTokens.GoldAccent)
                                .border(1.5.dp, ComicTokens.SolidBlack, RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "POW!",
                                style = ComicTypography.labelMedium,
                                color = ComicTokens.SolidBlack,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                )

                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Fidelity Metric Banner
                    ComicPanel(
                        variant = ComicPanelVariant.BURNT_ORANGE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(
                                text = "COHESION & FIDELITY AUDIT",
                                style = ComicTypography.titleMedium,
                                color = ComicTokens.SolidBlack
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Evaluating in-context visual harmony: ink borders, tactile drop shadows, and balanced color saturation across all screen archetypes.",
                                style = ComicTypography.bodyMedium,
                                color = ComicTokens.SolidBlack
                            )
                        }
                    }

                    // Representative TodayScreen Quest Card
                    Text(
                        text = "TODAY'S HERO QUESTS",
                        style = ComicTypography.titleSmall,
                        color = ComicTokens.SolidBlack
                    )

                    ComicPanel(
                        variant = ComicPanelVariant.SURFACE,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(ComicTokens.SkyBlue)
                                    .border(2.dp, ComicTokens.SolidBlack, RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Star,
                                    contentDescription = null,
                                    tint = ComicTokens.SolidBlack,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Morning Run (5 km)",
                                    style = ComicTypography.titleSmall,
                                    color = ComicTokens.SolidBlack
                                )
                                Text(
                                    text = "FITNESS • +75 XP • 3-DAY STREAK",
                                    style = ComicTypography.labelSmall.copy(fontSize = 10.sp),
                                    color = ComicTokens.CoralRed,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            ComicButton(
                                text = "DONE",
                                onClick = {},
                                variant = ComicButtonVariant.PRIMARY,
                                modifier = Modifier.height(38.dp)
                            )
                        }
                    }

                    // Representative Leaderboard / Stats Multi-Container Row
                    Text(
                        text = "RANKING & PROGRESSION METRICS",
                        style = ComicTypography.titleSmall,
                        color = ComicTokens.SolidBlack
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        ComicPanel(
                            variant = ComicPanelVariant.SKY_BLUE,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "RANK #1",
                                    style = ComicTypography.labelLarge,
                                    color = ComicTokens.SolidBlack
                                )
                                Text(
                                    text = "3,420 XP",
                                    style = ComicTypography.headlineMedium,
                                    color = ComicTokens.SolidBlack
                                )
                            }
                        }

                        ComicPanel(
                            variant = ComicPanelVariant.LAVENDER,
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "STREAK",
                                    style = ComicTypography.labelLarge,
                                    color = ComicTokens.SolidBlack
                                )
                                Text(
                                    text = "14 DAYS",
                                    style = ComicTypography.headlineMedium,
                                    color = ComicTokens.SolidBlack
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
                ComicFloatingActionButton(
                    onClick = {}
                )
            }
        }
    }
}

@Preview(name = "Comic Reference Fidelity Side-by-Side", showBackground = true, widthDp = 380, heightDp = 700)
@Composable
fun ComicReferenceFidelityComparisonPreview() {
    ComicReferenceFidelityComparisonContent()
}
