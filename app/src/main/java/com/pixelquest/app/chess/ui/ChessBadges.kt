package com.pixelquest.app.chess.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.chess.data.ChessRatingRepository
import com.pixelquest.app.chess.domain.ChessConfig
import com.pixelquest.app.chess.domain.Rating
import com.pixelquest.app.chess.domain.TimeControl
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.darood.ui.formatCount
import com.pixelquest.app.ui.components.PqChip
import com.pixelquest.app.ui.components.comic
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A member's chess badge in a study party's people list, provided by its screen (J158). */
val LocalChessBadge = staticCompositionLocalOf<(@Composable (userId: String) -> Unit)?> { null }

object ChessBadgeTags {
    fun badge(userId: String) = "chess_badge_$userId"
    const val PROFILE = "chess_profile_card"
}

/** Int points and tier, as a chip: "1,180 · KNIGHT". */
@Composable
fun TierBadge(points: Int, userId: String) {
    val tier = stringResource(tierName(ChessConfig.tierOf(points)))
    val said = stringResource(R.string.chess_badge_said, formatCount(points), tier)
    Box(Modifier.testTag(ChessBadgeTags.badge(userId)).semantics(mergeDescendants = true) { contentDescription = said }) {
        PqChip("${formatCount(points)} · ${tier.uppercase()}", PqColors.Sky)
    }
}

@HiltViewModel
class ChessProfileViewModel @Inject constructor(private val ratings: ChessRatingRepository, account: DaroodAccount) : ViewModel() {
    private val me = account.id()

    /** Chess needs an account (it's played in study parties): signed out, Hero has no chess card. */
    val signedIn: Boolean = me != null
    val ratingsOf: StateFlow<List<Rating>> = (me?.let { ratings.observeRatings(it) } ?: flowOf(emptyList()))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        me?.let { viewModelScope.launch { ratings.refreshRatings(it) } }
    }
}

@Composable
fun ChessProfileRoute(viewModel: ChessProfileViewModel = hiltViewModel()) {
    if (!viewModel.signedIn) return
    val ratings by viewModel.ratingsOf.collectAsState()
    ChessProfileCard(ratings)
}

/**
 * The profile's chess (J158), as the design's hub card: Int points in each time control, the
 * best one's tier, and the record over all of them. 800 where nothing's been played.
 */
@Composable
fun ChessProfileCard(ratings: List<Rating>) {
    val best = ratings.maxOfOrNull { it.points } ?: ChessConfig.START_RATING
    Column(Modifier.fillMaxWidth().testTag(ChessBadgeTags.PROFILE).comic(radius = PqDimens.RadiusLg, shadow = PqDimens.Shadow)) {
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            TimeControl.entries.forEachIndexed { i, tc ->
                val r = ratings.firstOrNull { it.timeControl == tc }
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f).background(if (r != null && r.points == best) PqColors.Gold else PqColors.Paper).padding(horizontal = 8.dp, vertical = 10.dp)
                ) {
                    Text(text = stringResource(R.string.chess_minutes, tc.seconds / 60).uppercase(), style = PqType.Label)
                    Text(text = formatCount(r?.points ?: ChessConfig.START_RATING), style = PqType.Number)
                }
                if (i < TimeControl.entries.lastIndex) Box(Modifier.width(2.dp).fillMaxHeight().background(PqColors.Ink))
            }
        }
        Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Ink))
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = stringResource(R.string.chess_profile_line, stringResource(tierName(ChessConfig.tierOf(best)))), style = PqType.MetaStrong)
            Text(
                text = stringResource(R.string.chess_record_line, ratings.sumOf { it.wins }, ratings.sumOf { it.draws }, ratings.sumOf { it.losses }),
                style = PqType.Meta
            )
        }
    }
}
