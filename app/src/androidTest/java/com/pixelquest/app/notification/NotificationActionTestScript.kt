package com.pixelquest.app.notification

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NotificationActionTestScript {

    @Test
    fun verifyNotificationActionIntents() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val yesIntent = Intent(context, TaskActionReceiver::class.java).apply {
            putExtra("EXTRA_TASK_ID", 42L)
            putExtra("EXTRA_WAS_COMPLETED", true)
        }

        assertEquals(42L, yesIntent.getLongExtra("EXTRA_TASK_ID", -1L))
        assertTrue(yesIntent.getBooleanExtra("EXTRA_WAS_COMPLETED", false))
    }
}
