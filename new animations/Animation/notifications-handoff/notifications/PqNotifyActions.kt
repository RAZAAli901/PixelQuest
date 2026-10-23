package com.pixelquest.notifications

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Action names used by PqNotify. */
object A {
    const val OPEN = "pq.OPEN"                       // handled by contentIntent-style deep link
    const val DISMISS = "pq.DISMISS"
    const val QUEST_DONE = "pq.QUEST_DONE"
    const val QUEST_SKIP = "pq.QUEST_SKIP"
    const val SNOOZE_10 = "pq.SNOOZE_10"
    const val OPEN_QUICK_QUEST = "pq.OPEN_QUICK_QUEST"
    const val REMIND_22 = "pq.REMIND_22"
    const val SHARE = "pq.SHARE"
    const val EQUIP_AVATAR = "pq.EQUIP_AVATAR"
    const val PRAYED = "pq.PRAYED"
    const val REMIND_5 = "pq.REMIND_5"
    const val PARTY_ADD = "pq.PARTY_ADD"
    const val INVITE_ACCEPT = "pq.INVITE_ACCEPT"
    const val INVITE_DECLINE = "pq.INVITE_DECLINE"
    const val STUDY_DONE = "pq.STUDY_DONE"
    const val TIMER_STOP = "pq.TIMER_STOP"
    const val CHESS_ACCEPT = "pq.CHESS_ACCEPT"
    const val CHESS_DECLINE = "pq.CHESS_DECLINE"
    const val CHESS_REMATCH = "pq.CHESS_REMATCH"
}

/**
 * Handles notification buttons WITHOUT opening the app (except the OPEN-type ones, which deep link).
 * Fill the TODOs with your repositories. Done/Prayed must award XP exactly like the in-app buttons.
 */
class PqNotifyActions : BroadcastReceiver() {
    companion object {
        private const val X_NOTIF = "notif"; private const val X_SUB = "sub"; private const val X_INT = "int"
        fun intent(ctx: Context, action: String, notifId: Int, subId: String, extraInt: Int): PendingIntent {
            val i = Intent(ctx, PqNotifyActions::class.java).setAction(action)
                .putExtra(X_NOTIF, notifId).putExtra(X_SUB, subId).putExtra(X_INT, extraInt)
            return PendingIntent.getBroadcast(ctx, (action + notifId + subId).hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        }
    }

    override fun onReceive(ctx: Context, intent: Intent) {
        val notifId = intent.getIntExtra(X_NOTIF, 0)
        val sub = intent.getStringExtra(X_SUB).orEmpty()
        val n = intent.getIntExtra(X_INT, 0)
        val pending = goAsync()
        // TODO: launch on your app scope / WorkManager; call pending.finish() when done.
        when (intent.action) {
            A.QUEST_DONE -> { /* questRepository.complete(sub)  → triggers perfect-day / streak-saved checks */ }
            A.QUEST_SKIP -> { /* questRepository.skipToday(sub) */ }
            A.SNOOZE_10 -> { /* scheduler.questDue(sub, inMinutes = 10) */ }
            A.REMIND_22 -> { /* scheduler.streakReminderAt(22, 0) */ }
            A.PRAYED -> { /* prayerRepository.markPrayed(sub, today) */ }
            A.REMIND_5 -> { /* scheduler.prayerReminder(sub, inMinutes = 5) */ }
            A.PARTY_ADD -> { /* partyRepository.addCount(sub, n) */ }
            A.INVITE_ACCEPT -> { /* partyRepository.acceptInvite(sub) */ }
            A.INVITE_DECLINE -> { /* partyRepository.declineInvite(sub) */ }
            A.STUDY_DONE -> { /* studyRepository.complete(sub) */ }
            A.TIMER_STOP -> { /* studyTimer.stop(sub) */ }
            A.CHESS_DECLINE -> { /* chessRepository.decline(sub) */ }
            A.EQUIP_AVATAR -> { /* avatarRepository.equip(sub) */ }
            A.CHESS_ACCEPT, A.CHESS_REMATCH, A.OPEN, A.OPEN_QUICK_QUEST, A.SHARE -> {
                // Needs UI: open the app at the right route (Android 12+ blocks activity starts from receivers,
                // so for these prefer a direct PendingIntent.getActivity in PqNotify if you want zero lag).
            }
        }
        if (intent.action != A.TIMER_STOP || true) PqNotify.cancel(ctx, notifId)
        pending.finish()
    }
}
