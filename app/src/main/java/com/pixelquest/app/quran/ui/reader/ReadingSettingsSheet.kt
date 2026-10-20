package com.pixelquest.app.quran.ui.reader

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.QuranTypography
import com.pixelquest.app.quran.ui.translations.TranslationTags
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelButtonVariant
import com.pixelquest.app.ui.theme.PixelTheme
import kotlin.math.roundToInt

/**
 * How the player reads: the Arabic's size, the translation's, the space between lines, what each
 * ayah shows, the page it's on, and whether the screen stays on while reading (off unless chosen).
 * Kept on the phone (ReadingSettingsStore).
 */
data class ReadingSettings(
    val arabicSize: Float = QuranTypography.ARABIC_DEFAULT,
    val translationSize: Float = QuranTypography.TRANSLATION_DEFAULT,
    val lineSpacing: Float = QuranTypography.SPACING_DEFAULT,
    val displayMode: DisplayMode = DisplayMode.BOTH,
    val paper: ReaderPaper = ReaderPaper.THEME,
    val keepScreenOn: Boolean = false
) {
    /** The reader's text styles for these settings. */
    fun typography(color: androidx.compose.ui.graphics.Color) =
        QuranTypography.of(arabicSize = arabicSize, translationSize = translationSize, lineSpacing = lineSpacing, color = color)
}

/** The reading settings, in a sheet over the reader. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    settings: ReadingSettings,
    preview: Pair<String, String>?,
    onChange: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit,
    translation: String = "",
    onOpenTranslations: (() -> Unit)? = null
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PixelTheme.colors.background) {
        ReadingSettingsContent(
            settings = settings,
            preview = preview,
            onChange = onChange,
            translation = translation,
            onOpenTranslations = onOpenTranslations
        )
    }
}

/**
 * The sheet's content: what each ayah shows, the page, three sliders, the translation (a button to
 * choose another), and the first ayah of the open surah as a preview, in the chosen sizes. Tested on
 * its own.
 *
 * @param preview the open surah's first ayah (its Arabic and translation, as given), or null.
 * @param translation the translation's name; with [onOpenTranslations], a button to choose another.
 */
@Composable
fun ReadingSettingsContent(
    settings: ReadingSettings,
    preview: Pair<String, String>?,
    onChange: (ReadingSettings) -> Unit,
    modifier: Modifier = Modifier,
    translation: String = "",
    onOpenTranslations: (() -> Unit)? = null
) {
    val colors = PixelTheme.colors
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .testTag(ReaderTags.SETTINGS_SHEET)
    ) {
        Text(
            text = stringResource(R.string.quran_reader_settings),
            style = MaterialTheme.typography.titleMedium,
            color = colors.primaryText,
            modifier = Modifier.semantics { heading() }
        )
        DisplayModeSwitch(selected = settings.displayMode, onSelect = { onChange(settings.copy(displayMode = it)) })
        Text(text = stringResource(R.string.quran_paper), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
        PaperSwitch(selected = settings.paper, onSelect = { onChange(settings.copy(paper = it)) })
        SettingSlider(
            label = stringResource(R.string.quran_setting_arabic_size),
            value = settings.arabicSize,
            range = QuranTypography.ARABIC_MIN..QuranTypography.ARABIC_MAX,
            step = 2f,
            shown = "${settings.arabicSize.roundToInt()}",
            tag = ReaderTags.ARABIC_SIZE,
            onChange = { onChange(settings.copy(arabicSize = it)) }
        )
        SettingSlider(
            label = stringResource(R.string.quran_setting_translation_size),
            value = settings.translationSize,
            range = QuranTypography.TRANSLATION_MIN..QuranTypography.TRANSLATION_MAX,
            step = 1f,
            shown = "${settings.translationSize.roundToInt()}",
            tag = ReaderTags.TRANSLATION_SIZE,
            onChange = { onChange(settings.copy(translationSize = it)) }
        )
        SettingSlider(
            label = stringResource(R.string.quran_setting_line_spacing),
            value = settings.lineSpacing,
            range = QuranTypography.SPACING_MIN..QuranTypography.SPACING_MAX,
            step = 0.1f,
            shown = "${(settings.lineSpacing * 100).roundToInt()}%",
            tag = ReaderTags.LINE_SPACING,
            onChange = { onChange(settings.copy(lineSpacing = it)) }
        )
        KeepScreenOnSwitch(on = settings.keepScreenOn, onChange = { onChange(settings.copy(keepScreenOn = it)) })
        if (onOpenTranslations != null) {
            PixelButton(
                text = stringResource(R.string.quran_translation_open, translation),
                onClick = onOpenTranslations,
                variant = PixelButtonVariant.BLUE,
                modifier = Modifier.fillMaxWidth().testTag(TranslationTags.OPEN)
            )
        }
        if (preview != null) {
            val typography = settings.typography(colors.onBackground)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)) {
                if (settings.displayMode != DisplayMode.TRANSLATION) Text(text = preview.first, style = typography.arabic, modifier = Modifier.fillMaxWidth())
                if (settings.displayMode != DisplayMode.ARABIC) Text(text = preview.second, style = typography.translation, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

/** Whether the screen stays on while reading: the whole row is the switch, read by TalkBack as one. */
@Composable
private fun KeepScreenOnSwitch(on: Boolean, onChange: (Boolean) -> Unit) {
    val colors = PixelTheme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .testTag(ReaderTags.KEEP_SCREEN_ON)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(text = stringResource(R.string.quran_keep_screen_on), style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            Text(text = stringResource(R.string.quran_keep_screen_on_explained), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(12.dp))
        Switch(
            checked = on,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onPrimary,
                checkedTrackColor = colors.primary,
                uncheckedThumbColor = colors.onSurfaceVariant,
                uncheckedTrackColor = colors.surfaceVariant,
                uncheckedBorderColor = colors.pixelBorder
            )
        )
    }
}

/**
 * One setting: its name and value, then a slider over [range] in [step]s. TalkBack reads the value
 * as shown ("28", "120%").
 */
@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    step: Float,
    shown: String,
    tag: String,
    onChange: (Float) -> Unit
) {
    val colors = PixelTheme.colors
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = colors.onSurface)
            Text(text = shown, style = MaterialTheme.typography.labelLarge, color = colors.primaryText)
        }
        Slider(
            value = value.coerceIn(range),
            onValueChange = { onChange(snap(it, range.start, step)) },
            valueRange = range,
            steps = ((range.endInclusive - range.start) / step).roundToInt() - 1,
            colors = SliderDefaults.colors(thumbColor = colors.primary, activeTrackColor = colors.primary),
            modifier = Modifier.fillMaxWidth().semantics { stateDescription = shown }.testTag(tag)
        )
    }
}

/** [value] on the nearest [step] from [start]: no 27.3 sp text. */
internal fun snap(value: Float, start: Float, step: Float): Float =
    start + ((value - start) / step).roundToInt() * step
