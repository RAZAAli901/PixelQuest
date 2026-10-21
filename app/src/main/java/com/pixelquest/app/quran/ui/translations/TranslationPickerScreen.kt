package com.pixelquest.app.quran.ui.translations

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import com.pixelquest.app.ui.components.PqCard
import com.pixelquest.app.ui.components.PqTopBar
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqDimens
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.quran.data.BundledTranslation
import com.pixelquest.app.quran.data.QuranLoadRequest
import com.pixelquest.app.quran.data.QuranRepository
import com.pixelquest.app.quran.data.TranslationSwitcher
import com.pixelquest.app.quran.domain.Translation
import com.pixelquest.app.quran.ui.QuranPanel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * The translation picker's state.
 *
 * @param options the translations the app bundles.
 * @param selected the one the player reads (or has just chosen).
 * @param onPhone the translation in the Quran on the phone, with its terms: until a newly chosen
 *   one is loaded, still the last.
 * @param loaded false until the bundled translations have been found.
 */
data class TranslationPickerUiState(
    val options: List<BundledTranslation> = emptyList(),
    val selected: String? = null,
    val onPhone: Translation? = null,
    val loaded: Boolean = false
) {
    /** A newly chosen translation is being put in place of the last. */
    val switching: Boolean get() = onPhone != null && selected != null && onPhone.id != selected
}

/** Which translation the player reads: the bundled ones, and choosing one, which the loader then puts in place. */
@HiltViewModel
class TranslationPickerViewModel @Inject constructor(
    private val switcher: TranslationSwitcher,
    repository: QuranRepository,
    private val load: QuranLoadRequest
) : ViewModel() {

    private val choice = MutableStateFlow<Pair<List<BundledTranslation>, String>?>(null)

    val state: StateFlow<TranslationPickerUiState> = combine(choice, repository.translations()) { found, onPhone ->
        TranslationPickerUiState(
            options = found?.first.orEmpty(),
            selected = found?.second,
            onPhone = onPhone.firstOrNull(),
            loaded = found != null
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TranslationPickerUiState())

    init {
        // The bundled files say who they are at their start: read off the main thread.
        viewModelScope.launch {
            choice.value = withContext(Dispatchers.IO) { switcher.available() to switcher.selected().id }
        }
    }

    /** Chooses translation [id]: kept, and loaded in place of the one on the phone. */
    fun choose(id: String) {
        val (options, selected) = choice.value ?: return
        if (id == selected) return
        viewModelScope.launch {
            if (withContext(Dispatchers.IO) { switcher.select(id) }) {
                choice.value = options to id
                load.request()
            }
        }
    }
}

@Composable
fun TranslationPickerRoute(onNavigateBack: () -> Unit, viewModel: TranslationPickerViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsState()
    TranslationPickerScreen(state = state, onNavigateBack = onNavigateBack, onChoose = viewModel::choose)
}

/**
 * The translations the app bundles, one to choose, each with who made it and its language; then
 * the terms of the one on the phone. They come with the app, so the Quran reads offline.
 */
@Composable
fun TranslationPickerScreen(
    state: TranslationPickerUiState,
    onNavigateBack: () -> Unit,
    onChoose: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(PqDimens.Gap),
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .verticalScroll(rememberScrollState())
            .padding(start = PqDimens.ScreenPad, end = PqDimens.ScreenPad, top = 8.dp, bottom = 40.dp)
            .testTag(TranslationTags.SCREEN)
    ) {
        PqTopBar(stringResource(R.string.quran_translation_title), onBack = onNavigateBack)
        if (state.options.isNotEmpty()) {
            PqCard(Modifier.fillMaxWidth()) {
                Column(Modifier.selectableGroup()) {
                    state.options.forEachIndexed { i, option ->
                        TranslationOption(option = option, selected = option.id == state.selected, onChoose = { onChoose(option.id) })
                        if (i != state.options.lastIndex) Box(Modifier.fillMaxWidth().height(2.dp).background(PqColors.Divider))
                    }
                }
            }
        }
        if (state.switching) {
            Text(
                text = stringResource(R.string.quran_translation_switching),
                style = PqType.BodyStrong,
                modifier = Modifier.testTag(TranslationTags.SWITCHING)
            )
        }
        state.onPhone?.let { Terms(it) }
        Text(text = stringResource(R.string.quran_translation_bundled), style = PqType.Meta)
    }
}

/** One translation to choose: a radio button, its name, and who made it in what language. */
@Composable
private fun TranslationOption(option: BundledTranslation, selected: Boolean, onChoose: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onChoose)
            .testTag(TranslationTags.option(option.id))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        // A v2 radio: an inked ring, filled with ink when chosen.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(22.dp).clip(CircleShape).background(PqColors.Paper).border(PqDimens.Border, PqColors.Ink, CircleShape)
        ) {
            if (selected) Box(Modifier.size(10.dp).clip(CircleShape).background(PqColors.Ink))
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = option.name, style = PqType.BodyStrong)
            Text(text = stringResource(R.string.quran_translation_by, option.translator, option.language), style = PqType.Meta)
        }
    }
}

/** The terms of the translation on the phone, as its source states them, and when it was last updated. */
@Composable
private fun Terms(translation: Translation) {
    QuranPanel(contentPadding = 14.dp, modifier = Modifier.fillMaxWidth().testTag(TranslationTags.TERMS)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = stringResource(R.string.quran_translation_terms, translation.name),
                style = PqType.BodyStrong,
                modifier = Modifier.semantics { heading() }
            )
            Text(text = translation.licenceNote, style = PqType.Body.copy(color = PqColors.Ink2))
            Text(text = stringResource(R.string.quran_translation_updated, translation.lastUpdate), style = PqType.Meta)
        }
    }
}

/** The translation picker's test tags. */
object TranslationTags {
    const val SCREEN = "quran_translations"
    const val SWITCHING = "quran_translation_switching"
    const val TERMS = "quran_translation_terms"

    /** The reading settings' button that opens it. */
    const val OPEN = "quran_translation_open"

    fun option(id: String) = "quran_translation_$id"
}
