package com.pixelquest.app.party.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.pixelquest.app.party.domain.Phrase

/** [phrase], from its resource, in the player's language. */
@Composable
fun phraseText(phrase: Phrase): String = when (phrase) {
    is Phrase.Text -> stringResource(phrase.res, *phrase.args.toTypedArray())
    is Phrase.Plural -> pluralStringResource(phrase.res, phrase.count, *phrase.args.toTypedArray())
}
