package com.pixelquest.app.study.alarm

import com.pixelquest.app.study.data.StudyReminderRepository
import com.pixelquest.app.study.data.local.StudyReminderDao
import com.pixelquest.app.study.domain.ReminderAlert
import com.pixelquest.app.study.domain.ReminderAlerts
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Turns every study reminder on the phone into its local alarms (docs/study-reminders.md, On the
 * phone): each reminder's alerts still to come for its next occurrence, through
 * [StudyReminderAlarms]. Arming again is always safe: what's no longer wanted is cancelled, what
 * fired or was marked done is left out, and the rest replaces itself slot by slot.
 */
@Singleton
class StudyReminderScheduler(
    private val reminders: StudyReminderRepository,
    private val dao: StudyReminderDao,
    private val alarms: StudyReminderAlarms,
    private val ledger: StudyReminderLedger,
    private val clock: () -> Long,
    private val mutes: StudyReminderMutes = MemoryStudyReminderMutes(),
    /** The app's own notifications switch: off, no reminder of any kind is armed. */
    private val notificationsOn: suspend () -> Boolean = { true }
) {
    @Inject
    constructor(
        reminders: StudyReminderRepository,
        dao: StudyReminderDao,
        alarms: StudyReminderAlarms,
        ledger: StudyReminderLedger,
        mutes: StudyReminderMutes,
        settings: com.pixelquest.app.domain.repository.SettingsRepository
    ) : this(reminders, dao, alarms, ledger, System::currentTimeMillis, mutes, { settings.isNotificationsEnabled.first() })

    /** Whether [partyId]'s reminders may alert on this phone now. */
    suspend fun alerts(partyId: String): Boolean = notificationsOn() && mutes.alerts(partyId)

    /** One arming at a time: the ledger is read, then written. */
    private val arming = Mutex()

    /**
     * Arms every reminder's alerts afresh, and returns what's armed. With [catchUp] (a restart), an
     * alert missed in the last hour still comes, once.
     */
    suspend fun rescheduleAll(catchUp: Boolean = false): List<ReminderAlert> = arming.withLock {
        val now = Instant.ofEpochMilli(clock())
        val since = if (catchUp) now.minus(ReminderAlerts.CATCH_UP) else null
        val on = notificationsOn()
        val wanted = reminders.allReminders()
            // Muted (all of them, or the party's), a reminder stays on the phone but nothing's armed.
            .filter { on && mutes.alerts(it.partyId) }
            .flatMap { ReminderAlerts.upcoming(it, now, since) }
            .filterNot { ledger.wasFired(it.firedKey) }
            .filterNot { dao.isDone(it.reminderId, it.occurrence.start.toInstant().toEpochMilli()) }
        val slots = wanted.map { it.slot }.toSet()
        (ledger.armed() - slots).forEach(alarms::cancel)
        wanted.forEach(alarms::schedule)
        ledger.setArmed(slots)
        dao.pruneDone(before = now.minus(DONE_KEPT).toEpochMilli())
        wanted
    }

    fun wasFired(alert: ReminderAlert): Boolean = ledger.wasFired(alert.firedKey)

    /** Records an alert as fired, so it's never fired again. */
    fun markFired(alert: ReminderAlert) {
        val now = clock()
        ledger.markFired(alert.firedKey, now, keepSinceMillis = now - FIRED_KEPT.toMillis())
    }

    private companion object {
        /** A custom alert is at most 14 days early, so a fired one older than 15 days can't come again. */
        val FIRED_KEPT: java.time.Duration = java.time.Duration.ofDays(15)

        /** Done marks for occurrences more than 8 days gone (a week's, and a day over) are let go. */
        val DONE_KEPT: java.time.Duration = java.time.Duration.ofDays(8)
    }
}
