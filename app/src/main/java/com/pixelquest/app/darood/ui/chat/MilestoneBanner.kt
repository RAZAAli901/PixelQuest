package com.pixelquest.app.darood.ui.chat

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.darood.ui.DaroodTestTags
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.theme.LocalReduceMotion
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import kotlinx.coroutines.delay

/** How long the banner stays before it goes on its own. */
const val MILESTONE_BANNER_MS = 5_000L

/**
 * A new milestone, announced like a notification at the top of the chat: it slides in, TalkBack reads
 * it out, and it goes on a tap or after [MILESTONE_BANNER_MS]. With reduced motion it simply appears.
 *
 * @param target the milestone to announce; null shows nothing.
 */
@Composable
fun MilestoneBanner(target: Long?, onDismiss: () -> Unit, modifier: Modifier = Modifier) {
    val dismiss by rememberUpdatedState(onDismiss)
    LaunchedEffect(target) {
        if (target != null) {
            delay(MILESTONE_BANNER_MS)
            dismiss()
        }
    }
    val still = LocalReduceMotion.current
    AnimatedVisibility(
        visible = target != null,
        enter = if (still) fadeIn() else slideInVertically { -it } + fadeIn(),
        exit = if (still) fadeOut() else slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        val shown = target ?: return@AnimatedVisibility
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp)) {
            Text(
                text = stringResource(R.string.darood_banner_milestone, formatCount(shown)),
                style = PqType.BodyStrong,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(DaroodTestTags.CHAT_BANNER)
                    .comic(bg = PqColors.Gold, radius = PqDimens.Radius, shadow = PqDimens.Shadow)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.darood_banner_dismiss), onClick = onDismiss)
                    .semantics { liveRegion = LiveRegionMode.Polite }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            )
        }
    }
}
