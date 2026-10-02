package com.pixelquest.app.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.navigation.Screen
import com.pixelquest.app.ui.theme.ComicShapeTokens
import com.pixelquest.app.ui.theme.ComicTokens
import com.pixelquest.app.ui.theme.ComicTypography
import com.pixelquest.app.ui.theme.comicBorder
import com.pixelquest.app.ui.theme.comicDropShadow

/**
 * Step 1 (Day 23): Comic-styled Bottom Navigation Bar.
 *
 * Emulates a printed comic strip panel layout:
 * - Solid 2.5dp black ink border with 4dp flat black drop shadow.
 * - Solid black vertical divider lines between comic cell tabs.
 * - Active tab highlighted by vibrant Coral Red pill chip with tactile black outline.
 * - Inactive tabs styled with muted secondary ink.
 */
@Composable
fun ComicBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val soundManager = LocalSoundManager.current
    val haptics = LocalHapticFeedback.current

    ComicPanel(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        variant = ComicPanelVariant.SURFACE,
        borderWidth = ComicShapeTokens.BorderWidthDefault,
        shadowOffset = ComicShapeTokens.ShadowOffsetDefault,
        cornerRadius = ComicShapeTokens.RadiusLarge,
        contentPadding = 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            bottomNavItems.forEachIndexed { index, item ->
                val isSelected = currentRoute == item.route

                if (item.isFeatured) {
                    ComicFeaturedNavButton(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager?.playNavSound()
                                onNavigate(item.route)
                            }
                        },
                        modifier = Modifier.weight(1f)
                    )
                    return@forEachIndexed
                }

                val chipBackground by animateColorAsState(
                    targetValue = if (isSelected) ComicTokens.CoralRed else Color.Transparent,
                    animationSpec = tween(durationMillis = 150),
                    label = "comicNavChipBg"
                )

                val interactionSource = remember { MutableInteractionSource() }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null
                        ) {
                            if (!isSelected) {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                soundManager?.playNavSound()
                                onNavigate(item.route)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val pillShape = RoundedCornerShape(ComicShapeTokens.ChipRadius)
                    val pillModifier = if (isSelected) {
                        Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                            .comicDropShadow(
                                offsetX = ComicShapeTokens.ShadowOffsetSmall,
                                offsetY = ComicShapeTokens.ShadowOffsetSmall,
                                shape = pillShape
                            )
                            .comicBorder(
                                width = ComicShapeTokens.BorderWidthThin,
                                color = ComicTokens.SolidBlack,
                                shape = pillShape
                            )
                            .background(chipBackground, pillShape)
                    } else {
                        Modifier
                            .fillMaxWidth()
                            .height(42.dp)
                    }

                    Box(
                        modifier = pillModifier,
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Image(
                                painter = painterResource(id = item.iconRes),
                                contentDescription = item.title,
                                modifier = Modifier.size(if (isSelected) 20.dp else 18.dp),
                                colorFilter = ColorFilter.tint(
                                    if (isSelected) ComicTokens.SolidBlack else ComicTokens.TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                style = ComicTypography.labelSmall,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Bold,
                                color = if (isSelected) ComicTokens.SolidBlack else ComicTokens.TextSecondary,
                                fontSize = 9.sp
                            )
                        }
                    }
                }

                // Comic panel vertical cell divider between tabs (none beside the round button)
                if (index < bottomNavItems.lastIndex && !bottomNavItems[index + 1].isFeatured) {
                    Box(
                        modifier = Modifier
                            .width(ComicShapeTokens.BorderWidthThin)
                            .height(28.dp)
                            .background(ComicTokens.SolidBlack)
                    )
                }
            }
        }
    }
}

/**
 * The round Leaderboard button: a gold disc with a black ink ring and hard shadow, coral-red while
 * the leaderboard is the open tab, matching the active pill on the other tabs.
 */
@Composable
private fun ComicFeaturedNavButton(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fill by animateColorAsState(
        targetValue = if (isSelected) ComicTokens.CoralRed else ComicTokens.GoldAccent,
        animationSpec = tween(durationMillis = 150),
        label = "comicFeaturedNavFill"
    )
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier.height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .comicDropShadow(
                    offsetX = ComicShapeTokens.ShadowOffsetSmall,
                    offsetY = ComicShapeTokens.ShadowOffsetSmall,
                    shape = CircleShape
                )
                .comicBorder(
                    width = ComicShapeTokens.BorderWidthDefault,
                    color = ComicTokens.SolidBlack,
                    shape = CircleShape
                )
                .background(fill, CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    role = Role.Tab,
                    onClickLabel = item.title,
                    onClick = onClick
                )
                .semantics {
                    contentDescription = item.title
                    selected = isSelected
                },
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = item.iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(ComicTokens.SolidBlack),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Preview(name = "Comic Bottom Nav Bar", showBackground = true)
@Composable
private fun ComicBottomNavBarPreview() {
    ComicBottomNavBar(
        currentRoute = Screen.Home.route,
        onNavigate = {}
    )
}
