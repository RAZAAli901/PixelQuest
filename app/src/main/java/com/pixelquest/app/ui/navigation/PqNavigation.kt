package com.pixelquest.app.ui.navigation

import androidx.annotation.StringRes
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape

/**
 * UI v2's bottom bar (docs/ui-v2.md): Home, Quests, the trophy, Deen and Hero. The side menu is gone;
 * its entries live in the tabs. Each tab is the route its back stack starts from.
 *
 * @param simpleLabel what Simple Mode calls it, in plain words.
 */
enum class PqTab(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
    val iconOff: ImageVector,
    @StringRes val simpleLabel: Int = label,
) {
    Home(Screen.Home.route, R.string.pq_tab_home, Icons.Rounded.Home, Icons.Outlined.Home),
    Quests(Screen.Tasks.route, R.string.pq_tab_quests, Icons.Rounded.Checklist, Icons.Rounded.Checklist, R.string.pq_tab_quests_simple),
    Deen(Routes.ISLAMIC, R.string.pq_tab_deen, Icons.Rounded.Brightness3, Icons.Outlined.Brightness3),
    Hero(Screen.Hero.route, R.string.pq_tab_hero, Icons.Rounded.Person, Icons.Outlined.Person);

    companion object {
        /** The tab whose back stack starts at [route], or null (the Leaderboard has the trophy, not a tab). */
        fun forRoute(route: String?): PqTab? = entries.firstOrNull { it.route == route }
    }
}

/** Tags for tests. */
object PqNavTags {
    const val BAR = "pq_bottom_bar"
    const val TROPHY = "pq_nav_trophy"
    fun tab(tab: PqTab) = "pq_nav_${tab.name.lowercase()}"
    fun dot(tab: PqTab) = "pq_nav_dot_${tab.name.lowercase()}"
}

/**
 * @param current the tab the player is in, highlighted; null on the Leaderboard.
 * @param showDeen false while the player has hidden the Islamic section in Settings.
 * @param deenDot a Darood Party invitation is waiting.
 * @param onCenterClick the trophy: it opens the Leaderboard, as the old bar's middle button did.
 */
@Composable
fun PqBottomBar(
    current: PqTab?,
    onSelect: (PqTab) -> Unit,
    onCenterClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDeen: Boolean = true,
    deenDot: Boolean = false,
    isSimpleMode: Boolean = false,
) {
    val sound = LocalSoundManager.current
    val select: (PqTab) -> Unit = { sound?.playNavSound(); onSelect(it) }
    Box(modifier.fillMaxWidth().testTag(PqNavTags.BAR)) {
        Column(Modifier.fillMaxWidth().background(PqColors.Paper)) {
            Box(Modifier.fillMaxWidth().height(PqDimens.Border).background(PqColors.Ink))
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().height(66.dp).padding(horizontal = 6.dp),
                verticalAlignment = Alignment.Top,
            ) {
                NavItem(PqTab.Home, current, isSimpleMode, select, Modifier.weight(1f))
                NavItem(PqTab.Quests, current, isSimpleMode, select, Modifier.weight(1f))
                Spacer(Modifier.weight(1f))
                if (showDeen) NavItem(PqTab.Deen, current, isSimpleMode, select, Modifier.weight(1f), dot = deenDot)
                // Without Deen, Hero takes the right half, so the trophy stays in the middle.
                NavItem(PqTab.Hero, current, isSimpleMode, select, Modifier.weight(if (showDeen) 1f else 2f))
            }
        }
        val trophy = stringResource(R.string.pq_nav_leaderboard)
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-26).dp).size(58.dp)
                .testTag(PqNavTags.TROPHY)
                .comicPressable({ sound?.playNavSound(); onCenterClick() }, bg = PqColors.Trophy, radius = 29.dp)
                .clearAndSetSemantics { contentDescription = trophy; role = Role.Tab; selected = current == null },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.EmojiEvents, null, tint = PqColors.OnTrophy, modifier = Modifier.size(30.dp)) }
    }
}

@Composable
private fun NavItem(
    tab: PqTab,
    current: PqTab?,
    isSimpleMode: Boolean,
    onSelect: (PqTab) -> Unit,
    modifier: Modifier,
    dot: Boolean = false,
) {
    val selected = tab == current
    val label = stringResource(if (isSimpleMode) tab.simpleLabel else tab.label)
    val waiting = stringResource(R.string.pq_nav_something_waiting)
    Box(modifier) {
        Column(
            Modifier
                .fillMaxWidth()
                .testTag(PqNavTags.tab(tab))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(tab) }
                .clearAndSetSemantics {
                    contentDescription = if (dot) "$label, $waiting" else label
                    role = Role.Tab
                    this.selected = selected
                }
                .padding(top = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            val pill = Modifier.width(52.dp).height(30.dp)
            Box(
                if (selected) pill.clip(pqShape(15.dp)).background(PqColors.Gold).border(PqDimens.BorderThin, PqColors.Ink, pqShape(15.dp)) else pill,
                contentAlignment = Alignment.Center,
            ) {
                Icon(if (selected) tab.icon else tab.iconOff, null, tint = if (selected) PqColors.Ink else PqColors.Muted, modifier = Modifier.size(22.dp))
            }
            Text(label, style = PqType.NavLabel, maxLines = 1)
        }
        // Over the pill's top corner; outside the tab's own semantics, which say it in words.
        if (dot) {
            Box(
                Modifier.align(Alignment.TopCenter).offset(x = 20.dp, y = 8.dp).size(10.dp).clip(PqCircle)
                    .background(PqColors.Red).border(PqDimens.BorderThin, PqColors.Ink, PqCircle)
                    .testTag(PqNavTags.dot(tab))
            )
        }
    }
}

/**
 * The app's frame: the screens, with the bottom bar under them. MainActivity puts the NavHost in
 * [content] and pads it by the padding given.
 */
@Composable
fun PqRootScaffold(
    currentTab: PqTab?,
    onTabSelect: (PqTab) -> Unit,
    onCenterClick: () -> Unit,
    showBottomBar: Boolean = true,
    showDeen: Boolean = true,
    deenDot: Boolean = false,
    isSimpleMode: Boolean = false,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        containerColor = PqColors.Ground,
        bottomBar = {
            if (showBottomBar) {
                PqBottomBar(currentTab, onTabSelect, onCenterClick, showDeen = showDeen, deenDot = deenDot, isSimpleMode = isSimpleMode)
            }
        },
    ) { padding -> Box(Modifier.fillMaxSize()) { content(padding) } }
}

/**
 * The frame as MainActivity builds it around [navController]'s screens: the bar's highlight follows
 * the back stack, its tabs and trophy navigate, and it hides on full-screen routes. [root] says which
 * tabs show and their dots.
 */
@Composable
fun PqAppFrame(navController: NavHostController, root: PqRootState, content: @Composable (PaddingValues) -> Unit) {
    val entry by navController.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val currentTab = remember(entry) { navController.currentPqTab() }
    PqRootScaffold(
        currentTab = currentTab,
        onTabSelect = { tab -> navController.navigateToTab(tab.route) },
        onCenterClick = { navController.navigateToTab(Screen.Leaderboard.route) },
        showBottomBar = route != null && !Routes.isFullScreen(route),
        showDeen = root.islamicHubShown,
        deenDot = root.daroodInviteWaiting,
        isSimpleMode = root.isSimpleMode,
        content = content,
    )
}
