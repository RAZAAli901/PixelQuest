package com.pixelquest.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
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
    /** The visible label, when [title] is too long for a tab (LEADERBOARD is 88dp in the pixel font). */
    val label: String = title
)

val bottomNavItems = listOf(
    NavItem("HOME", Screen.Home.route, R.drawable.ic_home),
    NavItem("TASKS", Screen.Tasks.route, R.drawable.ic_tasks),
    NavItem("LEADERBOARD", Screen.Leaderboard.route, R.drawable.ic_leaderboard, label = "RANKS"),
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
