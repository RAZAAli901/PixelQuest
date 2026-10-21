package com.pixelquest.app.quran.ui.list

import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqButtonStyle
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pixelquest.app.R
import com.pixelquest.app.quran.ui.QuranPanel
import com.pixelquest.app.ui.components.PixelTextField

/** Whether a surah and ayah can be gone to: both numbers, and an ayah that surah has. */
sealed interface GoToCheck {
    /** Nothing typed for the surah yet. */
    data object Empty : GoToCheck

    /** There are only [count] surahs. */
    data class NoSuchSurah(val count: Int) : GoToCheck

    /** [surah] has only [ayahs] ayahs. */
    data class NoSuchAyah(val surah: Int, val ayahs: Int) : GoToCheck

    /** Go to [surah], at [ayah] (its first when none was typed). */
    data class Go(val surah: Int, val ayah: Int) : GoToCheck

    companion object {
        /** [surahText] and [ayahText] as typed (digits only), against each surah's ayah count. */
        fun of(surahText: String, ayahText: String, ayahCounts: Map<Int, Int>): GoToCheck {
            val surah = surahText.toIntOrNull() ?: return Empty
            val ayahs = ayahCounts[surah] ?: return NoSuchSurah(ayahCounts.size)
            val ayah = if (ayahText.isEmpty()) 1 else ayahText.toIntOrNull() ?: 0
            return if (ayah in 1..ayahs) Go(surah, ayah) else NoSuchAyah(surah, ayahs)
        }
    }
}

/** Only the digits of what's typed, at most three: no surah or ayah number is longer. */
internal fun numberDigits(text: String): String =
    text.filter { it.isDigit() }.map { Character.getNumericValue(it) }.joinToString("").take(3)

/** The "Go to surah and ayah" dialog, over the surah list. */
@Composable
fun GoToAyahDialog(
    ayahCounts: Map<Int, Int>,
    onGo: (surah: Int, ayah: Int) -> Unit,
    onDismiss: () -> Unit
) {
    var surah by rememberSaveable { mutableStateOf("") }
    var ayah by rememberSaveable { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        GoToAyahContent(
            surah = surah,
            ayah = ayah,
            check = GoToCheck.of(surah, ayah, ayahCounts),
            onSurah = { surah = numberDigits(it) },
            onAyah = { ayah = numberDigits(it) },
            onGo = onGo,
            onCancel = onDismiss
        )
    }
}

/**
 * What the dialog shows: the surah's number and the ayah's (optional: its first), what's wrong with
 * them as they're typed, and Go, which works only for an ayah that's there. Tested outside a Dialog,
 * where Robolectric doesn't hang on a text field.
 */
@Composable
fun GoToAyahContent(
    surah: String,
    ayah: String,
    check: GoToCheck,
    onSurah: (String) -> Unit,
    onAyah: (String) -> Unit,
    onGo: (surah: Int, ayah: Int) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val problem = when (check) {
        is GoToCheck.NoSuchSurah -> pluralStringResource(R.plurals.quran_go_no_surah, check.count, check.count)
        is GoToCheck.NoSuchAyah -> pluralStringResource(R.plurals.quran_go_no_ayah, check.ayahs, check.surah, check.ayahs)
        else -> null
    }
    QuranPanel(modifier = modifier.fillMaxWidth().testTag(GoToTags.DIALOG)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.quran_go_title),
                style = PqType.CardTitle,
                modifier = Modifier.semantics { heading() }
            )
            PixelTextField(
                value = surah,
                onValueChange = onSurah,
                label = stringResource(R.string.quran_go_surah),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag(GoToTags.SURAH)
            )
            PixelTextField(
                value = ayah,
                onValueChange = onAyah,
                label = stringResource(R.string.quran_go_ayah),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth().testTag(GoToTags.AYAH)
            )
            if (problem != null) {
                Text(
                    text = problem,
                    style = PqType.BodyStrong.copy(color = PqColors.Error),
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }.testTag(GoToTags.PROBLEM)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
                PqButton(text = stringResource(R.string.quran_go_cancel), onClick = onCancel, style = PqButtonStyle.Secondary, modifier = Modifier.weight(1f))
                PqButton(
                    text = stringResource(R.string.quran_go_button),
                    onClick = { (check as? GoToCheck.Go)?.let { onGo(it.surah, it.ayah) } },
                    enabled = check is GoToCheck.Go,
                    modifier = Modifier.weight(1f).testTag(GoToTags.GO)
                )
            }
        }
    }
}

/** The dialog's test tags. */
object GoToTags {
    /** The Quran screen's top-bar action that opens the dialog. */
    const val ACTION = "quran_go_action"
    const val DIALOG = "quran_go_dialog"
    const val SURAH = "quran_go_surah"
    const val AYAH = "quran_go_ayah"
    const val PROBLEM = "quran_go_problem"
    const val GO = "quran_go_button"
}
