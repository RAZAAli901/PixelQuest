package com.pixelquest.app.study.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pixelquest.app.R
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.ui.components.PixelButton
import com.pixelquest.app.ui.components.PixelTextField
import com.pixelquest.app.ui.theme.PixelTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** The Add item (or Edit item) sheet, over the party's screen. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemSheet(
    form: ItemForm,
    today: LocalDate,
    isLeader: Boolean,
    onChange: (ItemForm) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss, containerColor = PixelTheme.colors.background) {
        ItemFormContent(form, today, isLeader, onChange, onSave)
    }
}

/**
 * The item's fields: kind, title, subject, notes, the day and time it's due, and for the leader, a
 * pin. A day is picked from the next four weeks, and a time from the hours of the day: the simplest
 * choice that works in every theme.
 */
@Composable
fun ItemFormContent(form: ItemForm, today: LocalDate, isLeader: Boolean, onChange: (ItemForm) -> Unit, onSave: () -> Unit) {
    val colors = PixelTheme.colors
    val locale = LocalConfiguration.current.locales[0]
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
            .padding(bottom = 16.dp)
            .navigationBarsPadding()
            .testTag(StudyTags.FORM)
    ) {
        Text(
            text = stringResource(if (form.editingId == null) R.string.study_add_item else R.string.study_edit_item),
            style = MaterialTheme.typography.titleMedium,
            color = colors.primaryText,
            modifier = Modifier.semantics { heading() }
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            StudyItemKind.entries.forEach { kind ->
                ChoiceChip(kindLabel(kind), form.kind == kind, Modifier.testTag(StudyTags.formKind(kind))) { onChange(form.copy(kind = kind)) }
            }
        }
        val showProblem = form.triedToSave
        PixelTextField(
            value = form.title,
            onValueChange = { onChange(form.withTitle(it)) },
            label = stringResource(R.string.study_field_title),
            errorText = if (showProblem && form.problem == ItemFormProblem.TITLE_SHORT) stringResource(R.string.study_title_short) else null,
            modifier = Modifier.fillMaxWidth().testTag(StudyTags.FORM_TITLE)
        )
        PixelTextField(
            value = form.subject,
            onValueChange = { onChange(form.withSubject(it)) },
            label = stringResource(R.string.study_field_subject),
            modifier = Modifier.fillMaxWidth().testTag(StudyTags.FORM_SUBJECT)
        )
        PixelTextField(
            value = form.notes,
            onValueChange = { onChange(form.withNotes(it)) },
            label = stringResource(R.string.study_field_notes),
            singleLine = false,
            modifier = Modifier.fillMaxWidth().testTag(StudyTags.FORM_NOTES)
        )

        FieldLabel(stringResource(R.string.study_field_due))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
            item(key = "none") {
                ChoiceChip(stringResource(R.string.study_section_no_date), form.dueDay == null, Modifier.testTag(StudyTags.FORM_NO_DATE)) { onChange(form.copy(dueDay = null)) }
            }
            items(ItemForm.days(today, form.dueDay), key = { it.toString() }) { day ->
                val label = when (day) {
                    today -> stringResource(R.string.study_section_today)
                    today.plusDays(1) -> stringResource(R.string.study_section_tomorrow)
                    else -> day.format(DateTimeFormatter.ofPattern("EEE d MMM", locale))
                }
                ChoiceChip(label, form.dueDay == day, Modifier.testTag(StudyTags.formDay(day))) { onChange(form.copy(dueDay = day)) }
            }
        }
        if (showProblem && form.problem == ItemFormProblem.QUIZ_NEEDS_DATE) {
            Text(text = stringResource(R.string.study_quiz_needs_date), style = MaterialTheme.typography.bodySmall, color = colors.error, modifier = Modifier.testTag(StudyTags.FORM_PROBLEM))
        }
        if (form.dueDay != null) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.selectableGroup()) {
                items((ItemForm.TIMES + form.dueTime).distinct().sorted(), key = { it.toString() }) { time ->
                    ChoiceChip(time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)), form.dueTime == time, Modifier.testTag(StudyTags.formTime(time))) {
                        onChange(form.copy(dueTime = time))
                    }
                }
            }
        }
        if (isLeader) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .toggleable(value = form.pinned, role = Role.Switch, onValueChange = { onChange(form.copy(pinned = it)) })
                    .testTag(StudyTags.FORM_PIN)
            ) {
                Text(text = stringResource(R.string.study_field_pin), style = MaterialTheme.typography.bodyLarge, color = colors.onSurface, modifier = Modifier.weight(1f))
                Switch(checked = form.pinned, onCheckedChange = null)
            }
        }
        PixelButton(
            text = stringResource(if (form.editingId == null) R.string.study_add_item_save else R.string.study_edit_item_save),
            onClick = onSave,
            enabled = !form.saving,
            modifier = Modifier.fillMaxWidth().testTag(StudyTags.FORM_SAVE)
        )
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(text = text, style = MaterialTheme.typography.labelLarge, color = PixelTheme.colors.onSurface)
}

/** One choice among several: kind, day or time. */
@Composable
fun ChoiceChip(label: String, selected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = PixelTheme.colors
    val shape = RoundedCornerShape(8.dp)
    Text(
        text = label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) colors.onPrimary else colors.onSurface,
        modifier = modifier
            .clip(shape)
            .background(if (selected) colors.primary else colors.surface)
            .border(1.dp, colors.pixelBorder, shape)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .heightIn(min = 48.dp)
            .padding(horizontal = 14.dp, vertical = 13.dp)
    )
}
