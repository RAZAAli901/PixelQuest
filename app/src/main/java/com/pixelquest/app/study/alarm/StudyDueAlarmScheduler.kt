package com.pixelquest.app.study.alarm

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.pixelquest.app.darood.data.remote.DaroodAccount
import com.pixelquest.app.scheduling.AlarmScheduler
import com.pixelquest.app.scheduling.AndroidAlarmScheduler
import com.pixelquest.app.study.data.local.StudyDao
import com.pixelquest.app.study.data.local.StudyItemEntity
import com.pixelquest.app.study.data.toDomain
import com.pixelquest.app.study.domain.StudyItem
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Schedules one alarm per assignment or quiz for the active user at due time minus 24 hours.
 *
 * Adheres to:
 * - One alert per user per item.
 * - Guard against retroactive alerts: if less than 24 hours remain, no alarm is scheduled.
 * - Cancellation on mark done, deletion, or leave.
 * - Unique deterministic request code per item.
 */
@Singleton
class StudyDueAlarmScheduler(
    private val context: Context,
    private val alarmScheduler: AlarmScheduler,
    private val dao: StudyDao,
    private val account: DaroodAccount,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        alarmScheduler: AlarmScheduler,
        dao: StudyDao,
        account: DaroodAccount,
    ) : this(context, alarmScheduler, dao, account, System::currentTimeMillis)

    constructor(
        @ApplicationContext context: Context,
        dao: StudyDao,
        account: DaroodAccount,
    ) : this(context, AndroidAlarmScheduler(context), dao, account, System::currentTimeMillis)

    fun canScheduleExactAlarms(): Boolean = alarmScheduler.canScheduleExactAlarms()

    fun openExactAlarmSettingsIntent(): Intent = alarmScheduler.openExactAlarmSettingsIntent()

    /** Computes a unique, deterministic request code for [itemId]. */
    fun requestCodeForItem(itemId: String): Int =
        REQUEST_CODE_BASE + (itemId.hashCode() and 0x0FFFFFFF)

    /** Trigger time for an item due at [dueAt]: exactly 24 hours prior. */
    fun triggerTimeMillis(dueAt: Instant): Long =
        dueAt.toEpochMilli() - LEAD_TIME.toMillis()

    /**
     * Schedules a 24-hour due alarm for [item].
     * Returns true if scheduled, false if not eligible (no due date, <= 24h remain, or already completed).
     */
    suspend fun schedule(item: StudyItem): Boolean {
        val dueAt = item.dueAt ?: return false
        val triggerAt = triggerTimeMillis(dueAt)
        val now = clock()
        // A06 guard: if less than 24 hours remain, do not alert retroactively
        if (triggerAt <= now) return false

        val me = account.id()
        if (me != null && dao.isItemCompleted(item.id, me)) return false

        val intent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            action = ACTION_DUE_ALARM
            putExtra(EXTRA_ITEM_ID, item.id)
            putExtra(EXTRA_PARTY_ID, item.partyId)
            putExtra(EXTRA_TITLE, item.title)
            putExtra(EXTRA_KIND, item.kind.name)
            putExtra(EXTRA_DUE_AT, dueAt.toEpochMilli())
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForItem(item.id),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return alarmScheduler.schedule(triggerAt, pendingIntent)
    }

    suspend fun schedule(item: StudyItemEntity): Boolean =
        schedule(item.toDomain())

    /** Cancels any scheduled alarm for [itemId]. */
    fun cancel(itemId: String) {
        val intent = Intent(context, StudyDueAlarmReceiver::class.java).apply {
            action = ACTION_DUE_ALARM
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCodeForItem(itemId),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmScheduler.cancel(pendingIntent)
    }

    /** Cancels alarms for all items belonging to [partyId]. */
    suspend fun cancelPartyAlarms(partyId: String) {
        val items = dao.getItemsForParty(partyId)
        for (item in items) {
            cancel(item.id)
        }
    }

    /**
     * Reschedules all unfinished items with a due date for the current user.
     * Arms eligible alarms and returns the list of scheduled item IDs.
     */
    suspend fun rescheduleAll(): List<String> {
        val me = account.id() ?: return emptyList()
        val items = dao.unfinishedItemsWithDue(me)
        val scheduled = mutableListOf<String>()
        for (item in items) {
            if (schedule(item)) {
                scheduled += item.id
            }
        }
        return scheduled
    }

    companion object {
        val LEAD_TIME: Duration = Duration.ofHours(24)
        const val REQUEST_CODE_BASE = 500_000_000
        const val ACTION_DUE_ALARM = "com.pixelquest.app.action.STUDY_DUE_ALARM"
        const val EXTRA_ITEM_ID = "EXTRA_ITEM_ID"
        const val EXTRA_PARTY_ID = "EXTRA_PARTY_ID"
        const val EXTRA_TITLE = "EXTRA_TITLE"
        const val EXTRA_KIND = "EXTRA_KIND"
        const val EXTRA_DUE_AT = "EXTRA_DUE_AT"
    }
}
