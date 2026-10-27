package com.pixelquest.app.study.alarm

import android.app.KeyguardManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pixelquest.app.MainActivity
import com.pixelquest.app.R
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.study.data.StudyRepository
import com.pixelquest.app.study.domain.StudyItemKind
import com.pixelquest.app.ui.components.PqButton
import com.pixelquest.app.ui.components.PqQuietButton
import com.pixelquest.app.ui.theme.PixelQuestTheme
import com.pixelquest.app.ui.theme.PqColors
import com.pixelquest.app.ui.theme.PqType
import com.pixelquest.app.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

object StudyPromptTags {
    const val SCREEN = "study_prompt_screen"
    const val TITLE = "study_prompt_title"
    const val BODY = "study_prompt_body"
    const val DONE = "study_prompt_done"
    const val SNOOZE = "study_prompt_snooze"
    const val OPEN = "study_prompt_open"
}

/**
 * Full-screen prompt displayed when an alarm-style study due alert wakes the device
 * while the screen is off/locked, mirroring the TaskPromptActivity pattern.
 */
@AndroidEntryPoint
class StudyPromptActivity : ComponentActivity() {

    @Inject
    lateinit var studyRepository: StudyRepository

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var dueAlarmScheduler: StudyDueAlarmScheduler

    private var locked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        wakeScreen()

        val itemId = intent.getStringExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID) ?: run {
            finish()
            return
        }
        val partyId = intent.getStringExtra(StudyDueAlarmScheduler.EXTRA_PARTY_ID).orEmpty()
        val title = intent.getStringExtra(StudyDueAlarmScheduler.EXTRA_TITLE).orEmpty()
        val kindStr = intent.getStringExtra(StudyDueAlarmScheduler.EXTRA_KIND).orEmpty()
        val kind = StudyItemKind.entries.firstOrNull { it.name == kindStr } ?: StudyItemKind.ASSIGNMENT
        val dueAtMillis = intent.getLongExtra(StudyDueAlarmScheduler.EXTRA_DUE_AT, 0L)
        val dueAt = if (dueAtMillis > 0) Instant.ofEpochMilli(dueAtMillis) else null

        val promptTitle = formatDuePromptTitle(this, kind, dueAt, title)

        setContent {
            val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.Pixel)
            val isReduceMotion by settingsRepository.isReduceMotionEnabled.collectAsState(initial = false)

            PixelQuestTheme(
                themeMode = themeMode,
                isReduceMotion = com.pixelquest.app.ui.theme.rememberReduceMotion(isReduceMotion),
            ) {
                StudyPromptContent(
                    title = promptTitle,
                    itemName = title,
                    onDone = {
                        dismissNotification(itemId)
                        CoroutineScope(Dispatchers.IO).launch {
                            val item = studyRepository.getItem(itemId)
                            if (item != null) {
                                studyRepository.complete(item)
                            }
                        }
                        finish()
                    },
                    onSnooze = {
                        dismissNotification(itemId)
                        val snoozeIntent = Intent(this, StudyDueAlarmReceiver::class.java).apply {
                            action = StudyDueAlarmReceiver.ACTION_SNOOZE
                            putExtra(StudyDueAlarmScheduler.EXTRA_ITEM_ID, itemId)
                            putExtra(StudyDueAlarmScheduler.EXTRA_PARTY_ID, partyId)
                            putExtra(StudyDueAlarmScheduler.EXTRA_TITLE, title)
                            putExtra(StudyDueAlarmScheduler.EXTRA_KIND, kindStr)
                            putExtra(StudyDueAlarmScheduler.EXTRA_DUE_AT, dueAtMillis)
                        }
                        sendBroadcast(snoozeIntent)
                        finish()
                    },
                    onOpen = {
                        dismissNotification(itemId)
                        val openIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("OPEN_STUDY_PARTY_ID", partyId)
                        }
                        startActivity(openIntent)
                        finish()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        locked = isLocked()
    }

    private fun isLocked(): Boolean =
        (getSystemService(KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

    private fun dismissNotification(itemId: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        manager?.cancel(dueAlarmScheduler.requestCodeForItem(itemId))
    }

    private fun wakeScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                    WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            )
        }
    }

    companion object {
        fun formatDuePromptTitle(context: Context, kind: StudyItemKind, dueAt: Instant?, title: String): String {
            val timeStr = if (dueAt != null) {
                DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault()).format(dueAt)
            } else {
                "10:00"
            }
            val kindLabel = if (kind == StudyItemKind.QUIZ) "Quiz" else "Assignment"
            return "$kindLabel due tomorrow at $timeStr: $title"
        }
    }
}

@Composable
internal fun StudyPromptContent(
    title: String,
    itemName: String,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onOpen: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PqColors.Ground)
            .padding(24.dp)
            .testTag(StudyPromptTags.SCREEN),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        com.pixelquest.app.mascot.PqMascot(
            mood = com.pixelquest.app.mascot.PixMood.Focus,
            size = 96.dp,
        )

        Spacer(Modifier.height(16.dp))

        Text(
            text = title,
            style = PqType.Title,
            textAlign = TextAlign.Center,
            color = PqColors.Ink,
            modifier = Modifier.testTag(StudyPromptTags.TITLE),
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.study_due_alert_prompt_body),
            style = PqType.Body,
            textAlign = TextAlign.Center,
            color = PqColors.Muted,
            modifier = Modifier.testTag(StudyPromptTags.BODY),
        )

        Spacer(Modifier.height(28.dp))

        PqButton(
            text = stringResource(R.string.action_done),
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(StudyPromptTags.DONE),
        )

        Spacer(Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            PqQuietButton(
                text = stringResource(R.string.action_snooze_1h),
                onClick = onSnooze,
                modifier = Modifier
                    .weight(1f)
                    .testTag(StudyPromptTags.SNOOZE),
            )
            Spacer(Modifier.width(12.dp))
            PqQuietButton(
                text = stringResource(R.string.action_open),
                onClick = onOpen,
                modifier = Modifier
                    .weight(1f)
                    .testTag(StudyPromptTags.OPEN),
            )
        }
    }
}
