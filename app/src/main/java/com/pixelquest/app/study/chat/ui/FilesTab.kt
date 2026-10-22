package com.pixelquest.app.study.chat.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.chat.domain.ChatFile
import com.pixelquest.app.study.chat.domain.ChatFileType
import com.pixelquest.app.study.ui.ChoiceChip
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.theme.PqType

/** How the Files tab is narrowed and ordered. */
data class FilesQuery(val search: String = "", val type: ChatFileType? = null, val newestFirst: Boolean = true)

/** The files [query] keeps, in its order: names containing its search, of its type. Pinned ones first. */
fun filesFor(files: List<ChatFile>, query: FilesQuery): List<ChatFile> {
    val words = query.search.trim()
    val kept = files.filter { (words.isEmpty() || it.displayName.contains(words, ignoreCase = true)) && (query.type == null || it.type == query.type) }
    val dated = if (query.newestFirst) kept.sortedByDescending { it.createdAt } else kept.sortedBy { it.createdAt }
    return dated.sortedByDescending { it.pinned }
}

/** The Files tab's search, kind filter and order, over its list. */
@Composable
fun FilesControls(query: FilesQuery, onChange: (FilesQuery) -> Unit, modifier: Modifier = Modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = modifier.fillMaxWidth()) {
        PixelTextField(
            value = query.search,
            onValueChange = { onChange(query.copy(search = com.pixelquest.app.domain.validation.InputRules.typed(it, com.pixelquest.app.domain.validation.InputRules.Field.SEARCH))) },
            label = stringResource(R.string.study_files_search),
            modifier = Modifier.fillMaxWidth().testTag(StudyChatTags.FILES_SEARCH)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            item { ChoiceChip(stringResource(R.string.study_files_all), query.type == null, Modifier.testTag(StudyChatTags.filesType(null))) { onChange(query.copy(type = null)) } }
            items(ChatFileType.entries) { type ->
                ChoiceChip(stringResource(type.label()), query.type == type, Modifier.testTag(StudyChatTags.filesType(type))) { onChange(query.copy(type = type)) }
            }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            item { ChoiceChip(stringResource(R.string.study_files_newest), query.newestFirst, Modifier.testTag(StudyChatTags.FILES_NEWEST)) { onChange(query.copy(newestFirst = true)) } }
            item { ChoiceChip(stringResource(R.string.study_files_oldest), !query.newestFirst, Modifier.testTag(StudyChatTags.FILES_OLDEST)) { onChange(query.copy(newestFirst = false)) } }
        }
    }
}

@Composable
fun NoFilesFound() = Text(stringResource(R.string.study_files_none_found), style = PqType.Meta, modifier = Modifier.testTag(StudyChatTags.FILES_NONE))
