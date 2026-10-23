package com.pixelquest.ui.navigation

import com.pixelquest.ui.theme.*

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Brightness3
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.rounded.Brightness3
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.EmojiEvents
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pixelquest.ui.components.comicPressable
import com.pixelquest.ui.theme.PqColors
import com.pixelquest.ui.theme.PqDimens
import com.pixelquest.ui.theme.PqType

/**
 * New IA: Home · Quests · [Trophy] · Deen · Hero. The drawer is removed.
 *  - Islamic        -> Deen tab
 *  - Quest history  -> Quests tab, "History" segment
 *  - Leaderboard    -> Hero > Profile list
 *  - Account/Settings -> Hero top-bar gear
 *  - Stats          -> Hero, "Stats" segment
 * The centre trophy keeps its existing action (wire your current handler to onCenterClick).
 */
enum class PqTab(val route: String, val label: String, val icon: ImageVector, val iconOff: ImageVector) {
    Home("home", "Home", Icons.Rounded.Home, Icons.Outlined.Home),
    Quests("quests", "Quests", Icons.Rounded.Checklist, Icons.Rounded.Checklist),
    Deen("deen", "Deen", Icons.Rounded.Brightness3, Icons.Outlined.Brightness3),
    Hero("hero", "Hero", Icons.Rounded.Person, Icons.Outlined.Person),
}

@Composable
fun PqBottomBar(current: PqTab?, onSelect: (PqTab) -> Unit, onCenterClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxWidth()) {
        Column(Modifier.fillMaxWidth().background(PqColors.Paper)) {
            Box(Modifier.fillMaxWidth().height(PqDimens.Border).background(PqColors.Ink))
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().height(66.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                NavItem(PqTab.Home, current == PqTab.Home, onSelect, Modifier.weight(1f))
                NavItem(PqTab.Quests, current == PqTab.Quests, onSelect, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
                NavItem(PqTab.Deen, current == PqTab.Deen, onSelect, Modifier.weight(1f))
                NavItem(PqTab.Hero, current == PqTab.Hero, onSelect, Modifier.weight(1f))
            }
        }
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-26).dp).size(58.dp)
                .comicPressable(onCenterClick, bg = PqColors.Trophy, radius = 29.dp),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.EmojiEvents, "Trophy", tint = PqColors.OnTrophy, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun NavItem(tab: PqTab, selected: Boolean, onSelect: (PqTab) -> Unit, modifier: Modifier) {
    Column(
        modifier.clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(tab) }.padding(top = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        val pill = Modifier.width(52.dp).height(30.dp)
        Box(
            if (selected) pill.clip(pqShape(15.dp)).background(PqColors.Gold).border(PqDimens.BorderThin, PqColors.Ink, pqShape(15.dp)) else pill,
            contentAlignment = Alignment.Center,
        ) {
            Icon(if (selected) tab.icon else tab.iconOff, tab.label, tint = if (selected) PqColors.Ink else PqColors.Muted, modifier = Modifier.size(22.dp))
        }
        Text(tab.label, style = PqType.NavLabel)
    }
}

/** Root scaffold. Put your NavHost inside `content` and pass the padding through. */
@Composable
fun PqRootScaffold(
    currentTab: PqTab?,
    onTabSelect: (PqTab) -> Unit,
    onCenterClick: () -> Unit,
    showBottomBar: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = PqColors.Ground,
        bottomBar = { if (showBottomBar) PqBottomBar(currentTab, onTabSelect, onCenterClick) },
    ) { padding -> Box(Modifier.fillMaxSize()) { content(padding) } }
}
