package com.pixelquest.app.ui.prompt

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.pixelquest.app.ui.theme.PixelQuestTheme
import dagger.hilt.android.AndroidEntryPoint

import androidx.compose.runtime.CompositionLocalProvider
import com.pixelquest.app.audio.LocalSoundManager
import com.pixelquest.app.audio.SoundManager
import javax.inject.Inject

@AndroidEntryPoint
class TaskPromptActivity : ComponentActivity() {

    private val viewModel: TaskPromptViewModel by viewModels()

    @Inject
    lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val taskName = intent.getStringExtra("EXTRA_TASK_NAME") ?: "Quest"

        setContent {
            PixelQuestTheme {
                CompositionLocalProvider(LocalSoundManager provides soundManager) {
                    DidYouDoItScreen(
                        taskId = taskId,
                        taskName = taskName,
                        onDismiss = { finish() },
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
