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
import com.pixelquest.app.ui.theme.PixelQuestTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TaskPromptActivity : ComponentActivity() {

    private val viewModel: TaskPromptViewModel by viewModels()

    @Inject
    lateinit var soundManager: SoundManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val taskName = intent.getStringExtra("EXTRA_TASK_NAME") ?: "Task"

        setContent {
            val isSimpleMode by viewModel.isSimpleMode.collectAsState()
            PixelQuestTheme {
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
