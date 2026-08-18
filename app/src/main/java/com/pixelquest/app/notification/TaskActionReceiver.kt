package com.pixelquest.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.domain.repository.TaskCompletionRepository
import com.pixelquest.app.domain.repository.UserProfileRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TaskActionReceiver : BroadcastReceiver() {

    @Inject
    lateinit var taskCompletionRepository: TaskCompletionRepository

    @Inject
    lateinit var userProfileRepository: UserProfileRepository

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra("EXTRA_TASK_ID", -1L)
        val wasCompleted = intent.getBooleanExtra("EXTRA_WAS_COMPLETED", false)
        if (taskId == -1L) return

        NotificationManagerCompat.from(context).cancel(taskId.toInt())
    }
}
