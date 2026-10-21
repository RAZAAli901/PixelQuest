package com.pixelquest.app.islamic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pixelquest.app.R
import com.pixelquest.app.darood.data.DaroodReminderSettings
import com.pixelquest.app.quran.data.ReadingSettingsStore
import com.pixelquest.app.quran.ui.reader.ReadingSettings
import com.pixelquest.app.quran.ui.reader.ReadingSettingsContent
import com.pixelquest.app.study.data.StudyReminderSettings
import com.pixelquest.app.study.ui.ToggleRow
import com.pixelquest.app.ui.theme.PixelTheme
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * The Islamic section's settings, in one place: how the Quran reader opens (the same settings its
 * own sheet changes), and the reminders the Islamic side and study parties send.
 */
@HiltViewModel
class IslamicSettingsViewModel @Inject constructor(
    private val reading: ReadingSettingsStore,
    private val darood: DaroodReminderSettings,
    private val study: StudyReminderSettings
) : ViewModel() {
    val readingSettings: StateFlow<ReadingSettings> = reading.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingSettings())
    val daroodWeekly: StateFlow<Boolean> = darood.weeklyReminder
    val studyDue: StateFlow<Boolean> = study.dueReminders
    val studyWeekly: StateFlow<Boolean> = study.weeklySummary

    fun setReading(settings: ReadingSettings) {
        viewModelScope.launch { reading.save(settings) }
    }

    fun setDaroodWeekly(on: Boolean) = darood.setWeeklyReminder(on)
    fun setStudyDue(on: Boolean) = study.setDueReminders(on)
    fun setStudyWeekly(on: Boolean) = study.setWeeklySummary(on)
}

/** The Islamic settings, as the Settings screen shows them. */
@Composable
fun IslamicSettingsSection(viewModel: IslamicSettingsViewModel = hiltViewModel()) {
    val reading by viewModel.readingSettings.collectAsState()
    val daroodWeekly by viewModel.daroodWeekly.collectAsState()
    val studyDue by viewModel.studyDue.collectAsState()
    val studyWeekly by viewModel.studyWeekly.collectAsState()
    IslamicSettingsContent(
        reading, viewModel::setReading,
        IslamicReminders(daroodWeekly, viewModel::setDaroodWeekly, studyDue, viewModel::setStudyDue, studyWeekly, viewModel::setStudyWeekly)
    )
}

/** The reminders the section turns on and off. */
data class IslamicReminders(
    val daroodWeekly: Boolean = false,
    val onDaroodWeekly: (Boolean) -> Unit = {},
    val studyDue: Boolean = true,
    val onStudyDue: (Boolean) -> Unit = {},
    val studyWeekly: Boolean = false,
    val onStudyWeekly: (Boolean) -> Unit = {}
)

@Composable
fun IslamicSettingsContent(reading: ReadingSettings, onReading: (ReadingSettings) -> Unit, reminders: IslamicReminders) {
    val colors = PixelTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().testTag(IslamicTestTags.SETTINGS)) {
        Heading(stringResource(R.string.settings_islamic_quran))
        Text(text = stringResource(R.string.settings_islamic_quran_body), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        ReadingSettingsContent(settings = reading, preview = null, onChange = onReading, embedded = true)

        Heading(stringResource(R.string.settings_islamic_reminders))
        Text(text = stringResource(R.string.settings_islamic_namaz_reminders), style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
        ToggleRow(
            stringResource(R.string.darood_weekly_switch), stringResource(R.string.darood_weekly_explained),
            reminders.daroodWeekly, reminders.onDaroodWeekly, IslamicTestTags.SETTINGS_DAROOD_WEEKLY
        )
        ToggleRow(
            stringResource(R.string.study_reminders_due), stringResource(R.string.study_reminders_due_body),
            reminders.studyDue, reminders.onStudyDue, IslamicTestTags.SETTINGS_STUDY_DUE
        )
        ToggleRow(
            stringResource(R.string.study_reminders_weekly), stringResource(R.string.study_reminders_weekly_body),
            reminders.studyWeekly, reminders.onStudyWeekly, IslamicTestTags.SETTINGS_STUDY_WEEKLY
        )
    }
}

@Composable
private fun Heading(text: String) {
    Text(text = text, style = MaterialTheme.typography.titleSmall, color = PixelTheme.colors.primaryText, modifier = Modifier.semantics { heading() })
}
