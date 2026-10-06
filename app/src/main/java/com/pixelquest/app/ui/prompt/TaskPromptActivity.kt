package com.pixelquest.app.ui.prompt

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        queue = PromptQueue.restoreFrom(savedInstanceState) ?: (PromptQueue() + PromptQueue.requestFrom(intent))
        if (queue.current == null) {
            finish()
            return
        }

        // Read the saved theme before the first frame (a SharedPreferences read) so the prompt opens
        // in the user's theme instead of flashing Pixel and fading over.
        val initialTheme = runBlocking { settingsRepository.themeMode.first() }
        val initialReduceMotion = runBlocking { settingsRepository.isReduceMotionEnabled.first() }

        setContent {
            val isSimpleMode by viewModel.isSimpleMode.collectAsState()
            val themeMode by settingsRepository.themeMode.collectAsState(initial = initialTheme)
            val isReduceMotion by settingsRepository.isReduceMotionEnabled.collectAsState(initial = initialReduceMotion)
            PixelQuestTheme(themeMode = themeMode, isReduceMotion = isReduceMotion) {
                CompositionLocalProvider(LocalSoundManager provides soundManager) {
                    val prompt = queue.current ?: return@CompositionLocalProvider
                    // A fresh screen per quest, so one prompt's state doesn't carry into the next.
                    key(prompt.taskId) {
                        DidYouDoItScreen(
                            taskId = prompt.taskId,
                            taskName = prompt.taskName,
                            onDismiss = { showNext() },
                            isSimpleMode = isSimpleMode,
                            onYesClick = {
                                viewModel.onTaskCompleted(prompt.taskId, true) { showNext() }
                            },
                            onNoClick = {
                                viewModel.onTaskCompleted(prompt.taskId, false) { showNext() }
                            }
                        )
                    }
                }
            }
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
