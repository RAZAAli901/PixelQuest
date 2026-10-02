package com.pixelquest.app.ui.prompt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val taskName = intent.getStringExtra("EXTRA_TASK_NAME") ?: "Task"

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
                    DidYouDoItScreen(
                        taskId = taskId,
                        taskName = taskName,
                        onDismiss = { finish() },
                        isSimpleMode = isSimpleMode,
                        onYesClick = {
                            viewModel.onTaskCompleted(taskId, true) {
                                finish()
                            }
                        },
                        onNoClick = {
                            viewModel.onTaskCompleted(taskId, false) {
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }
}
