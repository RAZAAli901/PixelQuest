package com.pixelquest.app.ui.prompt

import android.app.KeyguardManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.audio.SoundManager
import com.pixelquest.app.domain.repository.SettingsRepository
import com.pixelquest.app.ui.theme.PixelQuestTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class TaskPromptActivity : ComponentActivity() {

    private val viewModel: TaskPromptViewModel by viewModels()

    @Inject
    lateinit var soundManager: SoundManager

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private var queue by mutableStateOf(PromptQueue())

    /** The phone is locked: the prompt shows no quest until it's unlocked (S041). */
    private var locked by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queue = PromptQueue.restoreFrom(savedInstanceState) ?: (PromptQueue() + PromptQueue.requestFrom(intent))
        if (queue.current == null) {
            finish()
            return
        }

        // Read the saved theme before the first frame (a SharedPreferences read) so the prompt opens
        // in the user's theme instead of flashing Pixel and fading over.
        val initialTheme = com.pixelquest.app.data.repository.SettingsRepositoryImpl.storedThemeMode(this)
        val initialReduceMotion = com.pixelquest.app.data.repository.SettingsRepositoryImpl.storedReduceMotion(this)

        setContent {
            val isSimpleMode by viewModel.isSimpleMode.collectAsState()
            val themeMode by settingsRepository.themeMode.collectAsState(initial = initialTheme)
            val isReduceMotion by settingsRepository.isReduceMotionEnabled.collectAsState(initial = initialReduceMotion)
            PixelQuestTheme(
                themeMode = themeMode,
                isReduceMotion = com.pixelquest.app.ui.theme.rememberReduceMotion(isReduceMotion)
            ) {
                CompositionLocalProvider(LocalSoundManager provides soundManager) {
                    val prompt = queue.current ?: return@CompositionLocalProvider
                    PromptQueueContent(
                        prompt = prompt,
                        isSimpleMode = isSimpleMode,
                        locked = locked,
                        onUnlock = ::askToUnlock,
                        // The save completes even if the activity closes (see onTaskCompleted).
                        onAnswer = { request, done -> viewModel.onTaskCompleted(request.taskId, done, request.occurrenceDate) {} },
                        onNext = { showNext() }
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        locked = isLocked()
    }

    private fun isLocked(): Boolean =
        (getSystemService(KEYGUARD_SERVICE) as? KeyguardManager)?.isKeyguardLocked == true

    /** Asks Android to unlock (8.0 and later); on 7 the player unlocks the phone and comes back. */
    private fun askToUnlock() {
        val keyguard = getSystemService(KEYGUARD_SERVICE) as? KeyguardManager ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            keyguard.requestDismissKeyguard(this, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    locked = false
                }
            })
        }
    }

    /** Another quest's prompt while this one is open (singleTop): it waits its turn. */
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        queue = queue + PromptQueue.requestFrom(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        queue.saveTo(outState)
    }

    private fun showNext() {
        queue = queue.advance()
        if (queue.current == null) finish()
    }
}

/**
 * One queued prompt. The answer buttons call their handler and then onDismiss, so only [onNext]
 * moves the queue on, once per answer. (On Day 30 the activity also advanced when the save
 * finished, which skipped the next queued prompt.)
 */
@Composable
internal fun PromptQueueContent(
    prompt: PromptRequest,
    isSimpleMode: Boolean,
    onAnswer: (prompt: PromptRequest, done: Boolean) -> Unit,
    onNext: () -> Unit,
    locked: Boolean = false,
    onUnlock: () -> Unit = {}
) {
    // Over a locked phone, anyone holding it could read the quest: it waits for the unlock (S041).
    if (locked) {
        LockedPrompt(onUnlock)
        return
    }
    // A fresh screen per quest, so one prompt's state doesn't carry into the next.
    key(prompt.taskId) {
        DidYouDoItScreen(
            taskId = prompt.taskId,
            taskName = prompt.taskName,
            onDismiss = onNext,
            isSimpleMode = isSimpleMode,
            onYesClick = { onAnswer(prompt, true) },
            onNoClick = { onAnswer(prompt, false) }
        )
    }
}

object LockedPromptTags {
    const val SCREEN = "prompt_locked"
    const val UNLOCK = "prompt_unlock"
}

/** The prompt over a locked phone: that a quest is waiting, not which one. */
@Composable
internal fun LockedPrompt(onUnlock: () -> Unit) {
    androidx.compose.foundation.layout.Column(
        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp, androidx.compose.ui.Alignment.CenterVertically),
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(com.pixelquest.app.ui.theme.PqColors.Ground)
            .padding(24.dp)
            .testTag(LockedPromptTags.SCREEN)
    ) {
        androidx.compose.material3.Text(
            androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.prompt_locked_title),
            style = com.pixelquest.app.ui.theme.PqType.Title
        )
        androidx.compose.material3.Text(
            androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.prompt_locked_body),
            style = com.pixelquest.app.ui.theme.PqType.Body,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        com.pixelquest.app.ui.components.PqButton(
            androidx.compose.ui.res.stringResource(com.pixelquest.app.R.string.prompt_unlock),
            onUnlock,
            androidx.compose.ui.Modifier.testTag(LockedPromptTags.UNLOCK)
        )
    }
}
