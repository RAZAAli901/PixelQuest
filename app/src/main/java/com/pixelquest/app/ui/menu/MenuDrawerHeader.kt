package com.pixelquest.app.ui.menu

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.ui.components.PixelAvatarDisplay
import com.pixelquest.app.ui.components.PixelProgressBar
import com.pixelquest.app.ui.theme.PixelTheme

/** What the side menu's header shows about the player. */
data class MenuHeaderState(
    val name: String = "",
    val avatarId: String = "",
    val level: Int = 1,
    val totalXp: Int = 0,
    /** Perfect days toward the next level, out of [daysPerLevel] (levels come from perfect days). */
    val perfectDays: Int = 0,
    val daysPerLevel: Int = 7,
    val isSimpleMode: Boolean = false
) {
    val levelProgress: Float get() = if (daysPerLevel <= 0) 0f else perfectDays.toFloat() / daysPerLevel
}

/**
 * The top of the side menu: the player's avatar and name and, outside Simple Mode, their level,
 * XP and the perfect days toward the next level. Generous padding, so the menu has room to breathe.
 */
@Composable
fun MenuDrawerHeader(state: MenuHeaderState, modifier: Modifier = Modifier) {
    val colors = PixelTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MenuSpacing.Edge, vertical = MenuSpacing.HeaderVertical),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.avatarId.isNotBlank()) {
                PixelAvatarDisplay(avatarId = state.avatarId, size = 56.dp)
                Spacer(modifier = Modifier.width(16.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = state.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.primaryText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
                if (!state.isSimpleMode) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.menu_level_xp, state.level, state.totalXp),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.onSurface
                    )
                }
            }
            LocalMenuDrawer.current?.let { menu ->
                val style = menuStyle(state.isSimpleMode)
                IconButton(onClick = menu::close) {
                    Icon(
                        painter = painterResource(style.closeIcon),
                        contentDescription = stringResource(R.string.menu_close),
                        tint = style.ink,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        if (!state.isSimpleMode) {
            // The line under it says the same in words, so TalkBack reads that instead of the bar.
            PixelProgressBar(progress = state.levelProgress, height = 10.dp, modifier = Modifier.clearAndSetSemantics {})
            Text(
                text = LocalContext.current.resources.getQuantityString(
                    R.plurals.menu_level_progress, state.daysPerLevel, state.perfectDays, state.daysPerLevel, state.level + 1
                ),
                style = MaterialTheme.typography.labelSmall,
                color = colors.onSurface.copy(alpha = 0.75f)
            )
        }
    }
}

/** The side menu's spacing: wider than the screens', so it reads calmly. */
object MenuSpacing {
    /** From the drawer's edges to its content. */
    val Edge = 24.dp
    val HeaderVertical = 28.dp
    /** Entries sit 8dp in from the edge; with their own 16dp padding, their icons line up with the header's 24dp. */
    val ItemInset = 8.dp
    /** Each entry's height: taller than Android's 48dp minimum, for an easy tap and a calm list. */
    val ItemHeight = 56.dp
    val ItemGap = 4.dp
}
