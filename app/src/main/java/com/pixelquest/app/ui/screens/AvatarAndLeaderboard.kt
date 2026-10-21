package com.pixelquest.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pixelquest.app.R
import com.pixelquest.app.auth.AuthUiState
import com.pixelquest.app.auth.AuthViewModel
import com.pixelquest.app.domain.CloudAvailability
import com.pixelquest.app.ui.components.AvatarClass
import com.pixelquest.app.ui.components.CategoryTile
import com.pixelquest.app.ui.components.PixelAvatar
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.screens.avatar.AvatarSelectionViewModel
import com.pixelquest.app.ui.theme.PqCircle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.bsp
import com.pixelquest.app.ui.theme.lsp
import com.pixelquest.app.ui.theme.pqShape

/** Tags for tests. */
object AvatarTags {
    const val SCREEN = "avatar_screen"
    const val EQUIP = "avatar_equip"
    fun tile(avatar: AvatarClass) = "avatar_${avatar.name.lowercase()}"
}

object LockedBoardTags {
    const val SCREEN = "leaderboard_locked"
    const val GOOGLE = "leaderboard_locked_google"
    const val EMAIL = "leaderboard_locked_email"
    const val ERROR = "leaderboard_locked_error"
}

// =====================  CHOOSE AVATAR (mock 2I)  =====================

/** Choosing the hero's avatar: from Hero. The equipped one comes from the profile. */
@Composable
fun AvatarPickerRoute(onBack: () -> Unit, viewModel: AvatarSelectionViewModel = hiltViewModel()) {
    val equippedId by viewModel.currentAvatarId.collectAsState()
    val equipped = AvatarClass.from(equippedId)
    // The one being looked at, until equipped; it starts on the one the hero wears.
    var selected by rememberSaveable(equippedId) { mutableStateOf(equipped) }
    AvatarPickerScreen(
        equipped = equipped,
        selected = selected,
        onBack = onBack,
        onSelect = { selected = it },
        onEquip = { viewModel.selectAvatar(it.id) },
    )
}

/** A tap previews an avatar; Equip makes it the hero's. */
@Composable
fun AvatarPickerScreen(
    equipped: AvatarClass,
    selected: AvatarClass,
    onBack: () -> Unit,
    onSelect: (AvatarClass) -> Unit,
    onEquip: (AvatarClass) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().background(PqColors.Ground).testTag(AvatarTags.SCREEN)
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PqTopBar(stringResource(R.string.pq_avatar_title), onBack = onBack)
        PqCard(bg = PqColors.Gold, shadow = PqDimens.ShadowLg, radius = 18.dp) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                PixelAvatar(selected, 104.dp)
                Column(Modifier.padding(start = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(stringResource(R.string.pq_avatar_preview), style = PqType.Label)
                    Text(stringResource(selected.label).uppercase(), style = PqType.display(40))
                    Text(
                        if (selected == equipped) stringResource(R.string.pq_avatar_equipped)
                        else stringResource(R.string.pq_avatar_current, stringResource(equipped.label)),
                        style = PqType.Body.copy(fontSize = 13.bsp),
                    )
                }
            }
        }
        AvatarChoiceGrid(selected = selected, onSelect = onSelect, equipped = equipped, modifier = Modifier.weight(1f))
        PqButton(
            if (selected == equipped) stringResource(R.string.pq_avatar_equipped) else stringResource(R.string.pq_avatar_equip, stringResource(selected.label)),
            { onEquip(selected) },
            Modifier.fillMaxWidth().testTag(AvatarTags.EQUIP), enabled = selected != equipped, height = 54.dp,
        )
    }
}

// =====================  LEADERBOARD · LOCKED (mock 2J)  =====================

/**
 * The Leaderboard while signed out: Google sign-in right here; the emailed code on the Account
 * screen, which has its form. In a build without a cloud project, only why there's nothing.
 */
@Composable
fun LeaderboardLockedRoute(
    onEmailCode: () -> Unit,
    onBack: (() -> Unit)? = null,
    viewModel: AuthViewModel = hiltViewModel(),
) {
    val auth by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LeaderboardLockedScreen(
        onBack = onBack,
        onGoogleSignIn = { viewModel.signInWithGoogle(context) },
        onEmailCode = onEmailCode,
        signingIn = auth is AuthUiState.SigningIn,
        error = (auth as? AuthUiState.Error)?.message,
    )
}

@Composable
fun LeaderboardLockedScreen(
    onBack: (() -> Unit)?,
    onGoogleSignIn: () -> Unit,
    onEmailCode: () -> Unit,
    modifier: Modifier = Modifier,
    signingIn: Boolean = false,
    error: String? = null,
    cloudAvailable: Boolean = CloudAvailability.inThisBuild,
) {
    Box(
        modifier.fillMaxSize().background(PqColors.Ground).testTag(LockedBoardTags.SCREEN)
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            PqTopBar(stringResource(R.string.pq_lb_title), onBack = onBack)
            // A faded glimpse of what signing in opens.
            Column(Modifier.alpha(0.4f).clearAndSetSemantics {}, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { r ->
                    Row(
                        Modifier.fillMaxWidth().comic(radius = 12.dp, border = PqDimens.BorderThin, shadow = 0.dp).padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text("$r", style = PqType.number(22), modifier = Modifier.width(22.dp))
                        Box(Modifier.size(34.dp).clip(pqShape(8.dp)).background(PqColors.Ink))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            Box(Modifier.fillMaxWidth(0.8f - r * 0.09f).height(10.dp).clip(PqCircle).background(PqColors.Ink))
                            Box(Modifier.fillMaxWidth(0.4f).height(8.dp).clip(PqCircle).background(PqColors.Muted))
                        }
                        Box(Modifier.width(56.dp).height(12.dp).clip(PqCircle).background(PqColors.Ink))
                    }
                }
            }
        }
        PqCard(Modifier.align(Alignment.Center).fillMaxWidth(), shadow = 5.dp, radius = 18.dp) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                CategoryTile(Icons.Rounded.Lock, PqColors.Gold, size = 52.dp, iconSize = 28.dp, radius = 14.dp)
                Text(stringResource(R.string.pq_lb_locked_title), style = PqType.display(32, 32))
                Text(
                    if (cloudAvailable) stringResource(R.string.pq_lb_locked_body) else CloudAvailability.NOT_IN_THIS_BUILD,
                    style = PqType.Body.copy(color = PqColors.Ink2),
                )
                if (cloudAvailable) {
                    Spacer(Modifier.height(2.dp))
                    PqButton(
                        stringResource(if (signingIn) R.string.pq_lb_signing_in else R.string.pq_lb_google), onGoogleSignIn,
                        Modifier.fillMaxWidth().testTag(LockedBoardTags.GOOGLE), enabled = !signingIn, height = 50.dp,
                    )
                    PqButton(
                        stringResource(R.string.pq_lb_email), onEmailCode,
                        Modifier.fillMaxWidth().testTag(LockedBoardTags.EMAIL), style = PqButtonStyle.Secondary, height = 46.dp,
                    )
                    error?.let { Text(it, style = PqType.MetaStrong.copy(color = PqColors.RedText), modifier = Modifier.testTag(LockedBoardTags.ERROR)) }
                }
            }
        }
    }
}

/**
 * The avatars three to a row (mock 2I): a tap chooses one (it fills gold and gets a check); the one the
 * hero wears now, if any, is marked. The avatar picker and onboarding both use it.
 */
@Composable
fun AvatarChoiceGrid(selected: AvatarClass, onSelect: (AvatarClass) -> Unit, modifier: Modifier = Modifier, equipped: AvatarClass? = null) {
    LazyVerticalGrid(
        GridCells.Fixed(3), modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(top = 8.dp, end = 4.dp, bottom = 4.dp),
    ) {
        items(AvatarClass.entries) { a ->
            val sel = a == selected
            Box {
                Column(
                    Modifier.fillMaxWidth()
                        .comic(bg = if (sel) PqColors.Gold else PqColors.Paper, shadow = if (sel) PqDimens.Shadow else 0.dp, tappable = true)
                        .selectable(selected = sel, role = Role.RadioButton) { onSelect(a) }
                        .testTag(AvatarTags.tile(a))
                        .padding(top = 12.dp, bottom = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    PixelAvatar(a, 60.dp, Modifier.clearAndSetSemantics {})
                    Text(stringResource(a.label).uppercase(), style = PqType.Button.copy(fontSize = 12.lsp))
                }
                if (sel) Box(
                    Modifier.align(Alignment.TopEnd).offset(8.dp, (-8).dp).size(26.dp).clip(PqCircle).background(PqColors.Ink),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Rounded.Check, null, tint = PqColors.Gold, modifier = Modifier.size(18.dp)) }
                if (a == equipped) Text(
                    stringResource(R.string.pq_avatar_on), style = PqType.Label.copy(fontSize = 8.lsp, color = PqColors.Paper),
                    modifier = Modifier.padding(6.dp).clip(pqShape(4.dp)).background(PqColors.Ink).padding(horizontal = 4.dp, vertical = 2.dp),
                )
            }
        }
    }
}
