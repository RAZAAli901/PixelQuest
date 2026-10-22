package com.pixelquest.app.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.pixelquest.app.R
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.ui.components.comicPressable
import com.pixelquest.app.ui.motion.LocalPqToast
import com.pixelquest.app.ui.motion.PqMotion
import com.pixelquest.app.ui.motion.PqToastHost
import com.pixelquest.app.ui.motion.PqToastState
import com.pixelquest.app.ui.motion.navHop
import com.pixelquest.app.ui.motion.reducedMotion
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.pqShape
import kotlin.math.roundToInt

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
    Hero(Screen.Hero.route, R.string.pq_tab_hero, Icons.Rounded.Person, Icons.Outlined.Person),
    ;

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
    val select: (PqTab) -> Unit = {
        sound?.playNavSound()
        onSelect(it)
    }
    // M10: the gold pill slides to the selected tab. The slots aren't all the same width (the
    // trophy's gap, and Hero's double slot without Deen), so each tab says where its middle is.
    val middles = remember { mutableStateMapOf<PqTab, Float>() }
    var barX by remember { mutableFloatStateOf(0f) }
    val place: (PqTab, Float) -> Unit = { tab, x -> middles[tab] = x - barX }
    Box(modifier.fillMaxWidth().testTag(PqNavTags.BAR)) {
        Column(Modifier.fillMaxWidth().background(PqColors.Paper)) {
            Box(Modifier.fillMaxWidth().height(PqDimens.Border).background(PqColors.Ink))
            Box(Modifier.fillMaxWidth().navigationBarsPadding().height(66.dp).onGloballyPositioned { barX = it.positionInRoot().x }) {
                SlidingPill(current?.let { middles[it] })
                Row(
                    Modifier.fillMaxWidth().height(66.dp).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    NavItem(PqTab.Home, current, isSimpleMode, select, Modifier.weight(1f), place = place)
                    NavItem(PqTab.Quests, current, isSimpleMode, select, Modifier.weight(1f), place = place)
                    Spacer(Modifier.weight(1f))
                    if (showDeen) NavItem(PqTab.Deen, current, isSimpleMode, select, Modifier.weight(1f), dot = deenDot, place = place)
                    // Without Deen, Hero takes the right half, so the trophy stays in the middle.
                    NavItem(PqTab.Hero, current, isSimpleMode, select, Modifier.weight(if (showDeen) 1f else 2f), place = place)
                }
            }
        }
        val trophy = stringResource(R.string.pq_nav_leaderboard)
        Box(
            Modifier.align(Alignment.TopCenter).offset(y = (-26).dp).size(58.dp)
                .testTag(PqNavTags.TROPHY)
                .comicPressable({
                    sound?.playNavSound()
                    onCenterClick()
                }, bg = PqColors.Trophy, radius = 29.dp)
                .clearAndSetSemantics {
                    contentDescription = trophy
                    role = Role.Tab
                    selected = current == null
                },
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Rounded.EmojiEvents, null, tint = PqColors.OnTrophy, modifier = Modifier.size(30.dp)) }
    }
}

/** The selected tab's gold pill, sliding to the tab whose middle is [middle] (px from the bar's left). */
@Composable
private fun SlidingPill(middle: Float?) {
    if (middle == null) return
    val reduced = reducedMotion()
    val x = remember { Animatable(middle) }
    LaunchedEffect(middle) {
        if (reduced) {
            x.snapTo(middle)
        } else {
            x.animateTo(middle, tween(PqMotion.Slow, easing = PqMotion.nav()))
        }
    }
    val density = LocalDensity.current
    val half = with(density) { 26.dp.toPx() }
    val top = with(density) { 8.dp.roundToPx() }
    Box(
        Modifier.offset { IntOffset((x.value - half).roundToInt(), top) }.width(52.dp).height(30.dp)
            .clip(pqShape(15.dp)).background(PqColors.Gold).border(PqDimens.BorderThin, PqColors.Ink, pqShape(15.dp)),
    )
}

@Composable
private fun NavItem(
    tab: PqTab,
    current: PqTab?,
    isSimpleMode: Boolean,
    onSelect: (PqTab) -> Unit,
    modifier: Modifier,
    dot: Boolean = false,
    place: (PqTab, Float) -> Unit = { _, _ -> },
) {
    val selected = tab == current
    val label = stringResource(if (isSimpleMode) tab.simpleLabel else tab.label)
    val waiting = stringResource(R.string.pq_nav_something_waiting)
    Box(modifier.onGloballyPositioned { place(tab, it.positionInRoot().x + it.size.width / 2f) }) {
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
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            // The gold pill behind the selected icon is the bar's sliding one; the icon hops (M10).
            Box(Modifier.width(52.dp).height(30.dp), contentAlignment = Alignment.Center) {
                Icon(if (selected) tab.icon else tab.iconOff, null, tint = if (selected) PqColors.Ink else PqColors.Muted, modifier = Modifier.size(22.dp).navHop(selected))
            }
            Text(label, style = PqType.NavLabel, maxLines = 1)
        }
        // Over the pill's top corner; outside the tab's own semantics, which say it in words.
        if (dot) {
            Box(
                Modifier.align(Alignment.TopCenter).offset(x = 20.dp, y = 8.dp).size(10.dp).clip(PqCircle)
                    .background(PqColors.Red).border(PqDimens.BorderThin, PqColors.Ink, PqCircle)
                    .testTag(PqNavTags.dot(tab)),
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
    // M09: one toast for the whole app, above the bottom bar; screens reach it through LocalPqToast.
    val toast = remember { PqToastState() }
    CompositionLocalProvider(LocalPqToast provides toast) {
        Scaffold(
            containerColor = PqColors.Ground,
            bottomBar = {
                if (showBottomBar) {
                    PqBottomBar(currentTab, onTabSelect, onCenterClick, showDeen = showDeen, deenDot = deenDot, isSimpleMode = isSimpleMode)
                }
            },
        ) { padding ->
            Box(Modifier.fillMaxSize()) {
                content(padding)
                PqToastHost(toast, Modifier.align(Alignment.BottomCenter).padding(bottom = padding.calculateBottomPadding()))
            }
        }
    }
}

/** The frame's state, for the screens inside it: Home's parties button wears the invitation dot. */
val LocalPqRootState = compositionLocalOf { PqRootState() }

/**
 * The frame as MainActivity builds it around [navController]'s screens: the bar's highlight follows
 * the back stack, its tabs and trophy navigate, and it hides on full-screen routes. [root] says which
 * tabs show and their dots, and is the screens' [LocalPqRootState].
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
    ) { padding -> CompositionLocalProvider(LocalPqRootState provides root) { content(padding) } }
}
