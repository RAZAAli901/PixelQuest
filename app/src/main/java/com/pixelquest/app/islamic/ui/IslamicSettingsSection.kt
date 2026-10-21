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
    private val study: StudyReminderSettings,
    private val settings: com.pixelquest.app.domain.repository.SettingsRepository? = null,
    private val weekRanking: com.pixelquest.app.darood.sync.WeekRankingMutes? = null
) : ViewModel() {
    /** The Islamic section shows: in the side menu, and the Namaz card on Home. */
    val hubShown: StateFlow<Boolean> = (settings?.islamicHubShown ?: kotlinx.coroutines.flow.flowOf(true))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    fun setHubShown(shown: Boolean) {
        viewModelScope.launch { settings?.setIslamicHubShown(shown) }
    }

    val readingSettings: StateFlow<ReadingSettings> = reading.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReadingSettings())
    val daroodWeekly: StateFlow<Boolean> = darood.weeklyReminder
    val studyDue: StateFlow<Boolean> = study.dueReminders
    val studyWeekly: StateFlow<Boolean> = study.weeklySummary

    fun setReading(settings: ReadingSettings) {
        viewModelScope.launch { reading.save(settings) }
    }

    fun setDaroodWeekly(on: Boolean) = darood.setWeeklyReminder(on)

    /** The weekly ranking's notification, for every Darood Party (each can be muted in its info too). */
    val daroodRanking: StateFlow<Boolean> = weekRanking?.allOn ?: kotlinx.coroutines.flow.MutableStateFlow(true)
    fun setDaroodRanking(on: Boolean) { weekRanking?.setAllOn(on) }
    fun setStudyDue(on: Boolean) = study.setDueReminders(on)
    fun setStudyWeekly(on: Boolean) = study.setWeeklySummary(on)
}

/** The Islamic settings, as the Settings screen shows them. */
@Composable
fun IslamicSettingsSection(viewModel: IslamicSettingsViewModel = hiltViewModel()) {
    val reading by viewModel.readingSettings.collectAsState()
    val daroodWeekly by viewModel.daroodWeekly.collectAsState()
    val daroodRanking by viewModel.daroodRanking.collectAsState()
    val studyDue by viewModel.studyDue.collectAsState()
    val studyWeekly by viewModel.studyWeekly.collectAsState()
    val hubShown by viewModel.hubShown.collectAsState()
    IslamicSettingsContent(
        hubShown, viewModel::setHubShown,
        reading, viewModel::setReading,
        IslamicReminders(daroodWeekly, viewModel::setDaroodWeekly, studyDue, viewModel::setStudyDue, studyWeekly, viewModel::setStudyWeekly, daroodRanking, viewModel::setDaroodRanking)
    )
}

/** The reminders the section turns on and off. */
data class IslamicReminders(
    val daroodWeekly: Boolean = false,
    val onDaroodWeekly: (Boolean) -> Unit = {},
    val studyDue: Boolean = true,
    val onStudyDue: (Boolean) -> Unit = {},
    val studyWeekly: Boolean = false,
    val onStudyWeekly: (Boolean) -> Unit = {},
    val daroodRanking: Boolean = true,
    val onDaroodRanking: (Boolean) -> Unit = {}
)

@Composable
fun IslamicSettingsContent(
    hubShown: Boolean,
    onHubShown: (Boolean) -> Unit,
    reading: ReadingSettings,
    onReading: (ReadingSettings) -> Unit,
    reminders: IslamicReminders
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth().testTag(IslamicTestTags.SETTINGS)) {
        ToggleRow(
            stringResource(R.string.settings_islamic_show), stringResource(R.string.settings_islamic_show_body),
            hubShown, onHubShown, IslamicTestTags.SETTINGS_SHOW_HUB
        )
        Heading(stringResource(R.string.settings_islamic_quran))
        Text(text = stringResource(R.string.settings_islamic_quran_body), style = com.pixelquest.app.ui.theme.PqType.Meta)
        ReadingSettingsContent(settings = reading, preview = null, onChange = onReading, embedded = true)

        Heading(stringResource(R.string.settings_islamic_reminders))
        Text(text = stringResource(R.string.settings_islamic_namaz_reminders), style = com.pixelquest.app.ui.theme.PqType.Meta)
        ToggleRow(
            stringResource(R.string.darood_weekly_switch), stringResource(R.string.darood_weekly_explained),
            reminders.daroodWeekly, reminders.onDaroodWeekly, IslamicTestTags.SETTINGS_DAROOD_WEEKLY
        )
        ToggleRow(
            stringResource(R.string.darood_week_switch), stringResource(R.string.darood_week_switch_body),
            reminders.daroodRanking, reminders.onDaroodRanking, IslamicTestTags.SETTINGS_DAROOD_RANKING
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
    com.pixelquest.app.ui.components.PqFieldLabel(text, Modifier.semantics { heading() })
}
