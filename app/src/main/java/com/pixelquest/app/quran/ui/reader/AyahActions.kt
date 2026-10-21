package com.pixelquest.app.quran.ui.reader

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.vector.ImageVector
import com.pixelquest.app.ui.components.PqSheet
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.quran.domain.Ayah

/** What can be done with one ayah, from its sheet. */
enum class AyahAction { BOOKMARK, COPY_ARABIC, COPY_TRANSLATION, SHARE }

/** The sheet a tapped ayah opens: its reference, then what can be done with it. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AyahActionsSheet(
    ayah: Ayah,
    surahName: String,
    bookmarked: Boolean,
    onAction: (AyahAction) -> Unit,
    onDismiss: () -> Unit
) {
    PqSheet(onDismiss) {
        AyahActionsContent(ayah = ayah, surahName = surahName, bookmarked = bookmarked, onAction = onAction)
    }
}

/**
 * The sheet's content: "Al-Baqara 2:255", then bookmark (or remove the bookmark), copy the Arabic,
 * copy the translation, and share. Tested on its own.
 */
@Composable
fun AyahActionsContent(
    ayah: Ayah,
    surahName: String,
    bookmarked: Boolean,
    onAction: (AyahAction) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(bottom = 8.dp).testTag(ReaderTags.ACTIONS)) {
        Text(
            text = stringResource(R.string.quran_ayah_reference, surahName, ayah.surah, ayah.number),
            style = PqType.CardTitle,
            modifier = Modifier.padding(vertical = 8.dp).semantics { heading() }
        )
        ActionRow(
            if (bookmarked) Icons.Rounded.BookmarkRemove else Icons.Rounded.BookmarkBorder,
            stringResource(if (bookmarked) R.string.quran_unbookmark else R.string.quran_bookmark),
            AyahAction.BOOKMARK,
            onAction
        )
        ActionRow(Icons.Rounded.ContentCopy, stringResource(R.string.quran_copy_arabic), AyahAction.COPY_ARABIC, onAction)
        ActionRow(Icons.Rounded.Translate, stringResource(R.string.quran_copy_translation), AyahAction.COPY_TRANSLATION, onAction)
        ActionRow(Icons.Rounded.Share, stringResource(R.string.quran_share), AyahAction.SHARE, onAction)
    }
}

/** One action: a full-width row with its icon, a button to TalkBack. */
@Composable
private fun ActionRow(icon: ImageVector, label: String, action: AyahAction, onAction: (AyahAction) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(role = Role.Button) { onAction(action) }
            .testTag(ReaderTags.action(action))
    ) {
        Icon(icon, contentDescription = null, tint = PqColors.Ink, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = label, style = PqType.BodyStrong)
    }
}
