package com.pixelquest.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.view.View
import android.widget.RemoteViews
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.pixelquest.R
import com.pixelquest.ui.theme.PqSkin
import com.pixelquest.ui.theme.PqSkinState

/*
 * PixelQuest notifications — mocks 1D (lock screen), 1E (heads-up), 1F (shade).
 *
 * What Android lets us style: small icon (pure white), accent colour, title/body, actions,
 * progress, BigText/Inbox styles, and custom content via DecoratedCustomViewStyle (the system
 * still draws the app header). The comic/pixel card look lives in res/layout/notif_pq_*.xml.
 *
 * Wiring checklist:
 *  1. Copy android-res/ into app/src/main/res/ (merge folders).
 *  2. Call PqNotifications.createChannels(context) in Application.onCreate().
 *  3. Request POST_NOTIFICATIONS (Android 13+) the first time a reminder is turned on.
 *  4. Register PqNotificationActionReceiver in the manifest (exported=false).
 *  5. Replace the TODO handlers in the receiver with calls into your repositories.
 */
object PqNotifications {

    // ---- Channels (user can mute each one separately in system settings) ----
    const val CH_PRAYER = "pq_prayer"
    const val CH_QUEST = "pq_quest"
    const val CH_STREAK = "pq_streak"
    const val CH_LEVEL = "pq_level"
    const val CH_PARTY = "pq_party"
    const val CH_STUDY = "pq_study"
    const val CH_COACH = "pq_coach"
    private const val GROUP_PARTY = "pq_group_party"

    // ---- Actions ----
    const val ACTION_PRAYED = "com.pixelquest.action.PRAYED"
    const val ACTION_REMIND_5 = "com.pixelquest.action.REMIND_5"
    const val ACTION_QUEST_DONE = "com.pixelquest.action.QUEST_DONE"
    const val ACTION_SNOOZE_10 = "com.pixelquest.action.SNOOZE_10"
    const val ACTION_PARTY_ADD = "com.pixelquest.action.PARTY_ADD"
    const val EXTRA_ID = "id"
    const val EXTRA_NOTIF_ID = "notif_id"
    const val EXTRA_AMOUNT = "amount"

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannelGroup(NotificationChannelGroup("pq_daily", "Daily quests"))
        nm.createNotificationChannelGroup(NotificationChannelGroup("pq_social", "Parties"))
        listOf(
            NotificationChannel(CH_PRAYER, "Prayer reminders", NotificationManager.IMPORTANCE_HIGH).apply { group = "pq_daily"; description = "Before each prayer, with a one-tap Prayed button" },
            NotificationChannel(CH_QUEST, "Quest due", NotificationManager.IMPORTANCE_HIGH).apply { group = "pq_daily"; description = "When a quest's time arrives" },
            NotificationChannel(CH_STREAK, "Streak at risk", NotificationManager.IMPORTANCE_DEFAULT).apply { group = "pq_daily" },
            NotificationChannel(CH_LEVEL, "Level ups & rewards", NotificationManager.IMPORTANCE_DEFAULT).apply { group = "pq_daily" },
            NotificationChannel(CH_COACH, "AI Coach briefs", NotificationManager.IMPORTANCE_LOW).apply { group = "pq_daily" },
            NotificationChannel(CH_PARTY, "Darood parties", NotificationManager.IMPORTANCE_DEFAULT).apply { group = "pq_social"; description = "Friends' counts, milestones and the Friday note" },
            NotificationChannel(CH_STUDY, "Study parties", NotificationManager.IMPORTANCE_DEFAULT).apply { group = "pq_social"; description = "New items, due dates and weekly scores" },
        ).forEach(nm::createNotificationChannel)
    }

    // ---- Look ----
    private val accent get() = if (PqSkinState.skin == PqSkin.Pixel) 0xFFC25E00.toInt() else 0xFFFF5A4E.toInt()

    private fun base(ctx: Context, channel: String, title: String, body: String, tapIntent: PendingIntent) =
        NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(R.drawable.ic_stat_pixelquest)
            .setColor(accent)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(tapIntent)
            .setAutoCancel(true)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))

    /** Comic card via custom RemoteViews (level up, perfect day, milestone). */
    private fun comicCard(ctx: Context, kicker: String, title: String, body: String, progress: Int? = null): Pair<RemoteViews, RemoteViews> {
        val pixel = PqSkinState.skin == PqSkin.Pixel
        val big = RemoteViews(ctx.packageName, R.layout.notif_pq_big).apply {
            setTextViewText(R.id.pq_kicker, kicker)
            setTextViewText(R.id.pq_title, title)
            setTextViewText(R.id.pq_body, body)
            if (pixel) setInt(R.id.pq_card, "setBackgroundResource", R.drawable.notif_card_bg_pixel)
            if (progress != null) { setViewVisibility(R.id.pq_progress, View.VISIBLE); setProgressBar(R.id.pq_progress, 100, progress, false) }
        }
        val small = RemoteViews(ctx.packageName, R.layout.notif_pq_small).apply {
            setTextViewText(R.id.pq_title, title)
            setTextViewText(R.id.pq_body, body)
        }
        return small to big
    }

    private fun action(ctx: Context, action: String, notifId: Int, id: String, amount: Int = 0): PendingIntent {
        val i = Intent(ctx, PqNotificationActionReceiver::class.java).setAction(action)
            .putExtra(EXTRA_ID, id).putExtra(EXTRA_NOTIF_ID, notifId).putExtra(EXTRA_AMOUNT, amount)
        return PendingIntent.getBroadcast(ctx, (action + id + amount).hashCode(), i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    private fun post(ctx: Context, id: Int, b: NotificationCompat.Builder) {
        if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        NotificationManagerCompat.from(ctx).notify(id, b.build())
    }

    // ===================== Builders (one per mock) =====================

    /** 1D top card — "Fajr in 15 minutes · 4:48 AM · 0 / 5 prayed today"  [✓ PRAYED] [REMIND IN 5] */
    fun prayerReminder(ctx: Context, prayer: String, time: String, minutesLeft: Int, prayedToday: Int, tap: PendingIntent) {
        val id = ("prayer_$prayer").hashCode()
        post(ctx, id, base(ctx, CH_PRAYER, "$prayer in $minutesLeft minutes", "$time · $prayedToday / 5 prayed today", tap)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .addAction(0, "✓ Prayed", action(ctx, ACTION_PRAYED, id, prayer))
            .addAction(0, "Remind in 5", action(ctx, ACTION_REMIND_5, id, prayer)))
    }

    /** 1E heads-up — "READ 15 PAGES · 21:00 · Learning · +20 XP on the line"  [✓ DONE] [SNOOZE 10M] */
    fun questDue(ctx: Context, questId: String, title: String, time: String, category: String, xp: Int, tap: PendingIntent) {
        val id = ("quest_$questId").hashCode()
        post(ctx, id, base(ctx, CH_QUEST, title, "$time · $category · +$xp XP on the line", tap)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setSubText("Time's up!")
            .addAction(0, "✓ Done", action(ctx, ACTION_QUEST_DONE, id, questId))
            .addAction(0, "Snooze 10m", action(ctx, ACTION_SNOOZE_10, id, questId)))
    }

    /** 1D gold card — "🔥 STREAK AT RISK!" */
    fun streakAtRisk(ctx: Context, streakDays: Int, tap: PendingIntent) {
        val (small, big) = comicCard(ctx, "Streak", "STREAK AT RISK!", "$streakDays days strong. Finish one quest before midnight to keep it alive.")
        post(ctx, 1001, base(ctx, CH_STREAK, "Streak at risk!", "$streakDays days strong. Finish one quest before midnight.", tap)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).setCustomBigContentView(big))
    }

    /** 1F — "LEVEL UP! 300 XP banked. New title unlocked." with progress bar. */
    fun levelUp(ctx: Context, level: Int, xp: Int, title: String, tap: PendingIntent) {
        val (small, big) = comicCard(ctx, "LVL $level", "LEVEL UP!", "$xp XP banked. New title unlocked: $title.", progress = 100)
        post(ctx, 1002, base(ctx, CH_LEVEL, "Level up! LVL $level", "$xp XP banked. New title: $title", tap)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).setCustomBigContentView(big))
    }

    fun perfectDay(ctx: Context, xp: Int, streak: Int, tap: PendingIntent) {
        val (small, big) = comicCard(ctx, "All quests done", "PERFECT DAY!", "+$xp XP · $streak day streak. Streak protected.")
        post(ctx, 1003, base(ctx, CH_LEVEL, "Perfect day!", "+$xp XP · $streak day streak", tap)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).setCustomBigContentView(big))
    }

    /** 1F orange card — AI Coach daily brief ready. */
    fun coachBrief(ctx: Context, summary: String, tap: PendingIntent) {
        post(ctx, 1004, base(ctx, CH_COACH, "Coach Intel is ready", summary, tap).setCategory(NotificationCompat.CATEGORY_RECOMMENDATION))
    }

    // ---- Darood party ----

    /** "@sara added +313 · Raza party 1,039 / 10,000" with progress + quick-add your usual amount. Grouped per party. */
    fun daroodActivity(ctx: Context, partyId: String, party: String, who: String, amount: Int, total: Int, milestone: Int?, usualAmount: Int, tap: PendingIntent) {
        val id = ("party_$partyId").hashCode()
        val b = base(ctx, CH_PARTY, "$who added +$amount", "$party · ${"%,d".format(total)}" + (milestone?.let { " / ${"%,d".format(it)}" } ?: ""), tap)
            .setGroup(GROUP_PARTY)
            .setOnlyAlertOnce(true)
            .addAction(0, "+$usualAmount from me", action(ctx, ACTION_PARTY_ADD, id, partyId, usualAmount))
        if (milestone != null) b.setProgress(milestone, total.coerceAtMost(milestone), false)
        post(ctx, id, b)
        post(ctx, GROUP_PARTY.hashCode(), NotificationCompat.Builder(ctx, CH_PARTY)
            .setSmallIcon(R.drawable.ic_stat_pixelquest).setColor(accent)
            .setStyle(NotificationCompat.InboxStyle().setSummaryText("Parties"))
            .setGroup(GROUP_PARTY).setGroupSummary(true).setAutoCancel(true))
    }

    fun daroodMilestone(ctx: Context, partyId: String, party: String, milestone: Int, tap: PendingIntent) {
        val (small, big) = comicCard(ctx, party, "MILESTONE!", "Together you reached ${"%,d".format(milestone)}. Set the next goal in Party info.", progress = 100)
        post(ctx, ("milestone_$partyId").hashCode(), base(ctx, CH_PARTY, "$party reached ${"%,d".format(milestone)}!", "Set the next goal in Party info.", tap)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle()).setCustomContentView(small).setCustomBigContentView(big))
    }

    /** Friday note — quiet, only if the user turned it on in Parties. */
    fun fridayNote(ctx: Context, daysAdded: Int, total: Int, tap: PendingIntent) {
        post(ctx, 1005, base(ctx, CH_PARTY, "Your week of Darood", "You added Darood on $daysAdded of 7 days · ${"%,d".format(total)} across your parties.", tap)
            .setPriority(NotificationCompat.PRIORITY_LOW).setSilent(true))
    }

    // ---- Study party ----

    fun studyItemAdded(ctx: Context, partyId: String, party: String, who: String, type: String, title: String, due: String?, tap: PendingIntent) {
        post(ctx, ("study_$partyId$title").hashCode(), base(ctx, CH_STUDY, "$who added a $type", "$title" + (due?.let { " · due $it" } ?: "") + " · $party", tap).setGroup("pq_study_$partyId"))
    }

    fun studyDueTomorrow(ctx: Context, itemId: String, title: String, party: String, tap: PendingIntent) {
        val id = ("due_$itemId").hashCode()
        post(ctx, id, base(ctx, CH_STUDY, "Due tomorrow: $title", "$party · finish today for +5 early bonus", tap)
            .addAction(0, "✓ Done", action(ctx, ACTION_QUEST_DONE, id, itemId)))
    }

    fun studyWeeklyResult(ctx: Context, party: String, place: Int, points: Int, tap: PendingIntent) {
        post(ctx, ("week_$party").hashCode(), base(ctx, CH_STUDY, "$party · week wrapped", "You finished #$place with $points pts. New week starts now.", tap))
    }
}

/** Handles notification action buttons without opening the app. */
class PqNotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getStringExtra(PqNotifications.EXTRA_ID) ?: return
        val notifId = intent.getIntExtra(PqNotifications.EXTRA_NOTIF_ID, 0)
        when (intent.action) {
            PqNotifications.ACTION_PRAYED -> { /* TODO prayerRepository.markPrayed(id, today) */ }
            PqNotifications.ACTION_REMIND_5 -> { /* TODO scheduler.schedulePrayerReminder(id, minutesFromNow = 5) */ }
            PqNotifications.ACTION_QUEST_DONE -> { /* TODO questRepository.complete(id) — must award XP + run perfect-day check */ }
            PqNotifications.ACTION_SNOOZE_10 -> { /* TODO scheduler.scheduleQuestDue(id, minutesFromNow = 10) */ }
            PqNotifications.ACTION_PARTY_ADD -> { /* TODO partyRepository.addCount(id, intent.getIntExtra(PqNotifications.EXTRA_AMOUNT, 0)) */ }
        }
        NotificationManagerCompat.from(ctx).cancel(notifId)
    }
}
