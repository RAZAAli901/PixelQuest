package com.pixelquest.app.security

import android.content.Intent
import com.pixelquest.app.domain.namaz.NamazAlarm
import com.pixelquest.app.namaz.NamazAction
import com.pixelquest.app.notification.TaskActionReceiver
import com.pixelquest.app.scheduling.BootReceiver
import com.pixelquest.app.scheduling.TaskAlarmReceiver
import com.pixelquest.app.scheduling.TaskAlarmScheduler
import com.pixelquest.app.study.alarm.StudyReminderAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * S048: every receiver acts only on the broadcasts the app (or, for BootReceiver, Android) sends
 * it. None is exported (ManifestComponentsTest), so no other app can send to them at all.
 */
class ReceiverActionsTest {

    private val foreign = listOf("android.intent.action.VIEW", "com.evil.ACTION", "", Intent.ACTION_BOOT_COMPLETED)

    @Test
    fun theTaskReceivers_takeTheirOwnActionsOnly() {
        assertTrue(TaskAlarmReceiver.accepts(null))
        assertTrue(TaskAlarmReceiver.accepts(TaskAlarmScheduler.ACTION_SNOOZED_REMINDER))
        assertTrue(TaskActionReceiver.accepts(null))
        assertTrue(TaskActionReceiver.accepts(TaskActionReceiver.ACTION_SNOOZE))
        for (action in foreign) {
            assertFalse(action, TaskAlarmReceiver.accepts(action))
            assertFalse(action, TaskActionReceiver.accepts(action))
        }
        assertFalse(TaskAlarmReceiver.accepts(TaskActionReceiver.ACTION_SNOOZE))
    }

    @Test
    fun theOtherReceivers_matchAnExactAction() {
        for (action in foreign + listOf<String?>(null)) {
            assertNull(NamazAlarm.Kind.fromAction(action))
            assertNull(NamazAction.fromAction(action))
            assertNull(StudyReminderAction.fromAction(action))
        }
        assertFalse(BootReceiver.REARM_ACTIONS.contains("com.evil.ACTION"))
        assertFalse(BootReceiver.REARM_ACTIONS.contains(null as String?))
    }

    @Test
    fun everyReceiver_checksTheActionFirst() {
        val main = listOf(File("src/main/java"), File("app/src/main/java")).first { it.exists() }
        val receivers = main.walkTopDown().filter { it.isFile && it.extension == "kt" && ": BroadcastReceiver()" in it.readText() }.toList()
        assertEquals(7, receivers.size)
        val unchecked = receivers.filterNot { file ->
            val body = file.readText().substringAfter("override fun onReceive(").lines().drop(1).take(2).joinToString("\n")
            "intent.action" in body
        }.map { it.name }
        assertEquals(emptyList<String>(), unchecked)
    }
}
