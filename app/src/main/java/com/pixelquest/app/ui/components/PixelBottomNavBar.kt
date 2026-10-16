package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.navigation.Screen

data class NavItem(
    /** What TalkBack says, and the tab's label unless [label] is shorter. */
    val title: String,
    val route: String,
    val iconRes: Int,
    /** Drawn as a round icon-only button in the middle of the bar; [title] becomes its spoken label. */
    val isFeatured: Boolean = false,
    /** The visible label, when [title] is too long for a tab (LEADERBOARD is 88dp in the pixel font). */
    val label: String = title
)

val bottomNavItems = listOf(
    NavItem("HOME", Screen.Home.route, R.drawable.ic_home),
    NavItem("TASKS", Screen.Tasks.route, R.drawable.ic_tasks),
    NavItem("LEADERBOARD", Screen.Leaderboard.route, R.drawable.ic_leaderboard, isFeatured = true, label = "RANKS"),
    NavItem("STATS", Screen.Stats.route, R.drawable.ic_stats),
    NavItem("PROFILE", Screen.Profile.route, R.drawable.ic_profile)
)

@Composable
fun PixelBottomNavBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeMode = com.pixelquest.app.ui.theme.PixelTheme.mode
    if (activeMode == com.pixelquest.app.ui.theme.ThemeMode.Comic) {
        ComicBottomNavBar(
            currentRoute = currentRoute,
            onNavigate = onNavigate,
            modifier = modifier
        )
        return
    }

    val soundManager = LocalSoundManager.current
    PixelCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        variant = PixelPanelVariant.BORDER,
        contentPadding = 4.dp
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Every tab the same size: the Leaderboard was a larger round button in the middle.
            bottomNavItems.forEach { item ->
                val isSelected = currentRoute == item.route
                Column(
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clickable(role = Role.Tab) {
                            soundManager?.playNavSound()
                            onNavigate(item.route)
                        }
                        .clearAndSetSemantics {
                            contentDescription = item.title
                            selected = isSelected
                        }
                        .padding(vertical = 4.dp, horizontal = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = item.iconRes),
                        contentDescription = null,
                        // Pixel keeps its gold art; Light tints it so it reads on the white bar.
                        colorFilter = com.pixelquest.app.ui.theme.PixelThemeAssetFilter.forTheme(
                            activeMode,
                            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = item.label,
                        maxLines = 1,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * The round Leaderboard button in the Pixel and Light bars: a primary-coloured circle with the
 * icon tinted to read on it. The selected state thickens the ring in the secondary colour.
 */
@Composable
private fun FeaturedNavButton(
    item: NavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = com.pixelquest.app.ui.theme.PixelTheme.colors
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(colors.primary)
            .border(
                width = if (isSelected) 3.dp else 2.dp,
                color = if (isSelected) colors.secondary else colors.pixelBorder,
                shape = CircleShape
            )
            .clickable(role = Role.Tab, onClickLabel = item.title, onClick = onClick)
            .semantics {
                contentDescription = item.title
                selected = isSelected
            },
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = item.iconRes),
            contentDescription = null,
            colorFilter = ColorFilter.tint(colors.onPrimary),
            modifier = Modifier.size(26.dp)
        )
    }
}
