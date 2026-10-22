package com.pixelquest.app.study.alarm

import com.pixelquest.app.study.data.local.StudyReminderDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Arms study reminders' alarms again whenever the phone's reminders change: one made, changed or
 * deleted here (still waiting to send, too), and every sync that brings the party's. Started with
 * the app; a reboot, an update and a change of time zone or clock come through BootReceiver.
 */
@Singleton
class StudyReminderAlarmSync @Inject constructor(
    private val dao: StudyReminderDao,
    private val scheduler: StudyReminderScheduler,
    private val mutes: StudyReminderMutes
) {
    @OptIn(FlowPreview::class)
    fun start(scope: CoroutineScope) {
        scope.launch {
            // Turning them off or muting a party counts too: its alarms are cancelled, or armed again.
            combine(dao.observeAllReminders(), dao.observeAllPending(), mutes.alertsOn, mutes.muted) { a, b, c, d -> listOf(a, b, c, d) }
                .distinctUntilChanged()
                // A sync writes a party's reminders and its waiting changes in a few steps: armed once they settle.
                .debounce(QUIET_MS)
                .collect {
                    try {
                        scheduler.rescheduleAll()
                    } catch (e: Exception) {
                        com.pixelquest.app.util.AppLog.e("StudyReminderAlarmSync", "Could not arm study reminders", e)
                    }
                }
        }
    }

    internal companion object {
        const val QUIET_MS = 500L
    }
}
