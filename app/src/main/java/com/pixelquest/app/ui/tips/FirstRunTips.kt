package com.pixelquest.app.ui.tips

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.pixelquest.app.ui.components.PixelCard
import com.pixelquest.app.ui.components.PixelPanelVariant
import com.pixelquest.app.ui.theme.PixelTheme
import com.pixelquest.app.ui.theme.inkOnPanel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/** A first-run tip: shown on its screen until the player dismisses it, once, on this phone. */
enum class Tip(@StringRes val title: Int, @StringRes val body: Int) {
    ISLAMIC_HUB(R.string.tip_hub_title, R.string.tip_hub_body),
    QURAN(R.string.tip_quran_title, R.string.tip_quran_body),
    STUDY_PARTIES(R.string.tip_study_title, R.string.tip_study_body)
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
    val colors = PixelTheme.colors
    PixelCard(variant = PixelPanelVariant.BLUE, contentPadding = 14.dp, modifier = modifier.fillMaxWidth().testTag(TipTags.tip(tip))) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(tip.title),
                style = MaterialTheme.typography.titleSmall,
                color = inkOnPanel(colors.primaryText),
                modifier = Modifier.semantics { heading() }
            )
            Text(text = stringResource(tip.body), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = { store.dismiss(tip) }, modifier = Modifier.heightIn(min = 48.dp).testTag(TipTags.dismiss(tip))) {
                    Text(text = stringResource(R.string.tip_got_it), color = colors.primaryText)
                }
            }
        }
    }
}

object TipTags {
    fun tip(tip: Tip) = "tip_${tip.name}"
    fun dismiss(tip: Tip) = "tip_dismiss_${tip.name}"
}
