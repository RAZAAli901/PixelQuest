package com.pixelquest.app.ui.tips

import androidx.compose.foundation.layout.padding
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A first-run tip: shown on its screen until the player dismisses it, once, on this phone. */
enum class Tip(@StringRes val title: Int, @StringRes val body: Int) {
    ISLAMIC_HUB(R.string.tip_hub_title, R.string.tip_hub_body),
    QURAN(R.string.tip_quran_title, R.string.tip_quran_body)
}

/** Which tips the player has dismissed. */
interface TipsStore {
    val dismissed: StateFlow<Set<Tip>>
    fun dismiss(tip: Tip)
}

@Singleton
class PrefsTipsStore @Inject constructor(@ApplicationContext context: Context) : TipsStore {
    private val prefs = context.getSharedPreferences("pixelquest_tips", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(Tip.entries.filter { prefs.getBoolean(it.name, false) }.toSet())
    override val dismissed: StateFlow<Set<Tip>> = state.asStateFlow()

    override fun dismiss(tip: Tip) {
        prefs.edit().putBoolean(tip.name, true).apply()
        state.value = state.value + tip
    }
}

/** In memory, for tests and previews. */
class MemoryTipsStore(dismissed: Set<Tip> = emptySet()) : TipsStore {
    private val state = MutableStateFlow(dismissed)
    override val dismissed: StateFlow<Set<Tip>> = state.asStateFlow()
    override fun dismiss(tip: Tip) {
        state.value = state.value + tip
    }
}

/** The tips' store, given by MainActivity; none (no tips) in tests and previews unless they give one. */
val LocalTipsStore = staticCompositionLocalOf<TipsStore?> { null }

/** [tip] as a card, until it's dismissed with Got it. Nothing without a store, or once dismissed. */
@Composable
fun FirstRunTip(tip: Tip, modifier: Modifier = Modifier) {
    val store = LocalTipsStore.current ?: return
    val dismissed by store.dismissed.collectAsState()
    if (tip in dismissed) return
    PqCard(modifier.fillMaxWidth().testTag(TipTags.tip(tip)), bg = PqColors.Sky) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(text = stringResource(tip.title), style = PqType.BodyStrong, modifier = Modifier.semantics { heading() })
            Text(text = stringResource(tip.body), style = PqType.Body.copy(color = PqColors.Ink2))
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                PqButton(
                    text = stringResource(R.string.tip_got_it),
                    onClick = { store.dismiss(tip) },
                    style = PqButtonStyle.Secondary,
                    modifier = Modifier.testTag(TipTags.dismiss(tip))
                )
            }
        }
    }
}

object TipTags {
    fun tip(tip: Tip) = "tip_${tip.name}"
    fun dismiss(tip: Tip) = "tip_dismiss_${tip.name}"
}
