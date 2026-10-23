package com.pixelquest.app.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.pixelquest.app.R
import com.pixelquest.app.mascot.PixMood
import com.pixelquest.app.mascot.PqMascotBitmap
import com.pixelquest.app.ui.theme.PqSkin
import com.pixelquest.app.ui.theme.PqSkinState
import java.time.LocalTime

/*
 * PixelQuest notifications. One builder per catalog ID (see CATALOG.md and design-reference/PixelQuest Notifications.dc.html §04).
 * Replaces the earlier PqNotifications.kt. Every builder goes through post(), which applies:
 *   Pix large icon (mood) · accent per skin · custom comic card · actions · progress · grouping · quiet hours · daily limit.
 *
 * Wiring: PqNotify.createChannels(ctx) in Application.onCreate(); PqNotify.deepLink = { ctx, route -> PendingIntent } once at startup.
 */
object PqNotify {

    // ---------------- Channels ----------------
    enum class Ch(val id: String, val label: String, val importance: Int, val group: String, val essential: Boolean = false, val silent: Boolean = false) {
        Prayer("pq_prayer", "Prayer", NotificationManager.IMPORTANCE_HIGH, "daily", essential = true),
        Quest("pq_quest", "Quests due", NotificationManager.IMPORTANCE_HIGH, "daily"),
        Daily("pq_daily", "Daily plan & recap", NotificationManager.IMPORTANCE_LOW, "daily"),
        Streak("pq_streak", "Streak", NotificationManager.IMPORTANCE_DEFAULT, "daily"),
        Level("pq_level", "Rewards", NotificationManager.IMPORTANCE_DEFAULT, "daily"),
        Coach("pq_coach", "AI Coach", NotificationManager.IMPORTANCE_LOW, "daily"),
        Party("pq_party", "Darood circles", NotificationManager.IMPORTANCE_DEFAULT, "social"),
        Study("pq_study", "Study circles", NotificationManager.IMPORTANCE_DEFAULT, "social"),
        Chess("pq_chess", "Chess", NotificationManager.IMPORTANCE_HIGH, "social", essential = true),
        Ongoing("pq_ongoing", "Live timer & games", NotificationManager.IMPORTANCE_LOW, "social", essential = true, silent = true),
    }

    fun createChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannelGroup(NotificationChannelGroup("daily", "Daily"))
        nm.createNotificationChannelGroup(NotificationChannelGroup("social", "Circles & chess"))
        Ch.entries.forEach { c ->
            nm.createNotificationChannel(
                NotificationChannel(c.id, c.label, c.importance).apply {
                    group = c.group
                    if (c.silent) setSound(null, null)
                },
            )
        }
    }

    /** Set once at startup: build a PendingIntent that opens [route] in your NavHost. */
    lateinit var deepLink: (Context, String) -> PendingIntent

    // ---------------- Spec + post ----------------
    data class Act(val label: String, val action: String, val primary: Boolean = false)
    enum class CardBg { Gold, Paper, Sky }
    data class Card(val kicker: String, val title: String, val body: String, val bg: CardBg, val progress: Int? = null)

    data class Spec(
        val id: Int,
        val ch: Ch,
        val mood: PixMood,
        val title: String,
        val body: String,
        val route: String,
        val acts: List<Act> = emptyList(),
        val card: Card? = null,
        val progress: Pair<Int, Int>? = null, // value, max
        val lines: List<String>? = null, // InboxStyle
        val ongoing: Boolean = false,
        val chronometerBase: Long? = null, // System.currentTimeMillis() of start
        val group: String? = null,
        val alertOnce: Boolean = false,
        val fullScreen: Boolean = false,
        val bypassQuiet: Boolean = false,
        val subId: String = "", // passed to the action receiver
        val extraInt: Int = 0,
    )

    private val pixel get() = PqSkinState.skin == PqSkin.Pixel
    private val accent get() = if (pixel) 0xFFC25E00.toInt() else 0xFFFF5A4E.toInt()

    fun post(ctx: Context, s: Spec) {
        if (Build.VERSION.SDK_INT >= 33 && ActivityCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        if (!s.bypassQuiet && !s.ch.essential && inQuietHours()) {
            QuietQueue.hold(ctx, s)
            return
        }
        if (!s.ch.essential && !Limits.allow(ctx, s.ch)) return

        val largePx = ctx.resources.getDimensionPixelSize(android.R.dimen.notification_large_icon_width)
        val b = NotificationCompat.Builder(ctx, s.ch.id)
            .setSmallIcon(PqNotifications.SMALL_ICON)
            .setColor(accent)
            .setContentTitle(s.title)
            .setContentText(s.body)
            .setLargeIcon(PqMascotBitmap.largeIcon(s.mood, largePx, pixel))
            .setContentIntent(deepLink(ctx, s.route))
            .setAutoCancel(!s.ongoing)
            .setOngoing(s.ongoing)
            .setOnlyAlertOnce(s.alertOnce || s.ongoing)
            .setCategory(if (s.ch == Ch.Chess || s.ch == Ch.Prayer || s.ch == Ch.Quest) NotificationCompat.CATEGORY_REMINDER else NotificationCompat.CATEGORY_RECOMMENDATION)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(
                NotificationCompat.Builder(ctx, s.ch.id)
                    .setSmallIcon(PqNotifications.SMALL_ICON)
                    .setColor(accent)
                    .setContentTitle(ctx.getString(R.string.notif_hidden_title))
                    .build(),
            )

        when {
            s.lines != null -> b.setStyle(NotificationCompat.InboxStyle().also { st -> s.lines.forEach(st::addLine) }.setSummaryText(s.body))
            else -> b.setStyle(NotificationCompat.BigTextStyle().bigText(s.body))
        }
        s.progress?.let { (v, max) -> b.setProgress(max, v.coerceIn(0, max), false) }
        s.chronometerBase?.let { b.setUsesChronometer(true).setWhen(it).setShowWhen(true) }
        s.group?.let { b.setGroup(it) }
        if (s.ch.silent) b.setSilent(true)
        if (s.fullScreen) b.setFullScreenIntent(deepLink(ctx, s.route), true).setPriority(NotificationCompat.PRIORITY_MAX)
        // Max 3 actions; the one marked primary is listed first (Android renders it in the accent).
        s.acts.sortedByDescending { it.primary }.take(3).forEach { a -> b.addAction(0, a.label, PqNotifyActions.intent(ctx, a.action, s.id, s.subId, s.extraInt)) }

        NotificationManagerCompat.from(ctx).notify(s.id, b.build())
        s.group?.let { g ->
            NotificationManagerCompat.from(ctx).notify(
                g.hashCode(),
                NotificationCompat.Builder(ctx, s.ch.id)
                    .setSmallIcon(PqNotifications.SMALL_ICON).setColor(accent)
                    .setStyle(NotificationCompat.InboxStyle()).setGroup(g).setGroupSummary(true).setAutoCancel(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPublicVersion(
                        NotificationCompat.Builder(ctx, s.ch.id)
                            .setSmallIcon(PqNotifications.SMALL_ICON)
                            .setColor(accent)
                            .setContentTitle(ctx.getString(R.string.notif_hidden_title))
                            .build(),
                    )
                    .build(),
            )
        }
    }

    fun cancel(ctx: Context, id: Int) = NotificationManagerCompat.from(ctx).cancel(id)

    private fun inQuietHours(now: LocalTime = LocalTime.now()) = now >= LocalTime.of(23, 30) || now < LocalTime.of(6, 0)

    private fun nid(vararg parts: Any) = parts.joinToString("|").hashCode()
    private fun fmt(n: Int) = "%,d".format(n)

    // ================= DAILY =================
    fun morningPlan(ctx: Context, count: Int, firstTitle: String, firstTime: String) = post(
        ctx,
        Spec(
            nid("N01"),
            Ch.Daily,
            PixMood.Happy,
            "Good morning, ${Profile.name}",
            "$count quests today · first up: $firstTitle at $firstTime",
            "quests",
            listOf(Act("Open quests", A.OPEN)),
        ),
    )
    fun questDue(ctx: Context, questId: String, title: String, time: String, category: String, xp: Int) = post(
        ctx,
        Spec(
            nid("N02", questId),
            Ch.Quest,
            PixMood.Focus,
            "$title · now",
            "$time · $category · +$xp XP on the line",
            "quests",
            listOf(Act("✓ Done", A.QUEST_DONE, true), Act("Snooze 10m", A.SNOOZE_10)),
            subId = questId,
        ),
    )
    fun questOverdue(ctx: Context, questId: String, title: String, xp: Int) = post(
        ctx,
        Spec(
            nid("N02", questId),
            Ch.Quest,
            PixMood.Worried,
            "$title is 1 hour late",
            "Still counts today · +$xp XP",
            "quests",
            listOf(Act("✓ Done", A.QUEST_DONE, true), Act("Skip today", A.QUEST_SKIP)),
            subId = questId,
        ),
    )
    fun halfway(ctx: Context, done: Int, total: Int) = post(
        ctx,
        Spec(
            nid("N04"),
            Ch.Daily,
            PixMood.Happy,
            "$done of $total done",
            "${total - done} more and it's a perfect day.",
            "home",
            progress = done to total,
        ),
    )
    fun perfectDay(ctx: Context, xp: Int, streak: Int) = post(
        ctx,
        Spec(
            nid("N05"),
            Ch.Level,
            PixMood.Celebrate,
            "Perfect day!",
            "+$xp XP · $streak day streak",
            "home",
            card = Card("All quests done", "PERFECT DAY!", "+$xp XP · $streak day streak", CardBg.Gold),
        ),
    )
    fun eveningRecap(ctx: Context, done: Int, total: Int, xp: Int, firstTomorrow: String) = post(
        ctx,
        Spec(
            nid("N06"),
            Ch.Daily,
            PixMood.Sleepy,
            "Day wrapped: $done of $total",
            "+$xp XP today. Tomorrow starts with $firstTomorrow.",
            "home",
        ),
    )

    // ================= STREAK =================
    fun streakAtRisk(ctx: Context, days: Int) = post(
        ctx,
        Spec(
            nid("STREAK"),
            Ch.Streak,
            PixMood.Worried,
            "Your $days-day streak needs one quest",
            "Anything counts. Finish one before midnight.",
            "quests",
            listOf(Act("Quick quest", A.OPEN_QUICK_QUEST, true), Act("Remind at 22:00", A.REMIND_22)),
        ),
    )
    fun streakLastHour(ctx: Context, days: Int) = post(
        ctx,
        Spec(
            nid("STREAK"), Ch.Streak, PixMood.Sad,
            "1 hour left", "Your $days-day streak ends at midnight.", "quests",
            listOf(Act("Save it", A.OPEN_QUICK_QUEST, true)),
            card = Card("Streak · $days days", "1 HOUR LEFT", "Pix is not okay. Do one quest.", CardBg.Paper, progress = 92), bypassQuiet = true,
        ),
    )
    fun streakSaved(ctx: Context, days: Int, best: Int) = post(
        ctx,
        Spec(
            nid("STREAK"),
            Ch.Streak,
            PixMood.OnFire,
            "Streak saved! $days days",
            "Just in time. Your best is $best.",
            "home",
        ),
    )
    fun streakMilestone(ctx: Context, days: Int, bonusXp: Int) = post(
        ctx,
        Spec(
            nid("S04", days),
            Ch.Level,
            PixMood.LevelUp,
            "$days day streak!",
            "New record. +$bonusXp bonus XP.",
            "home",
            listOf(Act("Share", A.SHARE)),
            card = Card("New record", "$days DAY STREAK!", "+$bonusXp bonus XP", CardBg.Gold),
        ),
    )
    fun streakLost(ctx: Context, days: Int, best: Int) = post(
        ctx,
        Spec(
            nid("STREAK"),
            Ch.Streak,
            PixMood.Lost,
            "Your streak ended at $days days",
            "Start a new one today. Your best stays $best.",
            "quests",
            listOf(Act("Start again", A.OPEN_QUICK_QUEST, true)),
        ),
    )
    fun comeback(ctx: Context, daysAway: Int, waiting: Int) = post(
        ctx,
        Spec(
            nid("COMEBACK"),
            Ch.Daily,
            if (daysAway >= 7) PixMood.Sad else PixMood.Sleepy,
            if (daysAway >= 7) "It's quiet without you" else "Pix kept your quests warm",
            if (daysAway >= 7) "One quest today brings Pix back to Happy." else "$waiting quests waiting. 5 minutes to restart.",
            "quests",
            listOf(Act(if (daysAway >= 7) "One quest" else "Open", A.OPEN_QUICK_QUEST)),
        ),
    )

    // ================= PROGRESS =================
    fun levelUp(ctx: Context, level: Int, xp: Int, title: String) = post(
        ctx,
        Spec(
            nid("P01", level),
            Ch.Level,
            PixMood.LevelUp,
            "Level up! LVL $level",
            "${fmt(xp)} XP banked · New title: $title",
            "hero",
            card = Card("LVL $level", "LEVEL UP!", "${fmt(xp)} XP banked · New title: $title", CardBg.Gold, progress = 100),
        ),
    )
    fun avatarUnlocked(ctx: Context, avatarId: String, name: String, level: Int) = post(
        ctx,
        Spec(
            nid("P02", avatarId),
            Ch.Level,
            PixMood.Celebrate,
            "New avatar: $name",
            "Unlocked at LVL $level. Equip it?",
            "avatar",
            listOf(Act("Equip", A.EQUIP_AVATAR, true)),
            subId = avatarId,
        ),
    )
    fun weeklyReport(ctx: Context, week: Int, percent: Int, summary: String, lines: List<String>) = post(
        ctx,
        Spec(
            nid("P03"),
            Ch.Coach,
            PixMood.Wise,
            "Week $week: $percent% complete",
            summary,
            "hero/stats",
            lines = lines,
        ),
    )
    fun leaderboardPassed(ctx: Context, by: String, rank: Int, gapXp: Int) = post(
        ctx,
        Spec(
            nid("P04"),
            Ch.Level,
            PixMood.Challenge,
            "$by passed you",
            "You're #$rank this week, $gapXp XP behind.",
            "leaderboard",
            listOf(Act("Open board", A.OPEN)),
        ),
    )

    // ================= DEEN =================
    fun prayerReminder(ctx: Context, prayer: String, time: String, minutesLeft: Int, prayedToday: Int) = post(
        ctx,
        Spec(
            nid("PRAYER", prayer),
            Ch.Prayer,
            PixMood.Praying,
            "$prayer in $minutesLeft minutes",
            "$time · $prayedToday / 5 prayed today",
            "deen/namaz",
            listOf(Act("✓ Prayed", A.PRAYED, true), Act("Remind in 5", A.REMIND_5)),
            subId = prayer,
        ),
    )
    fun prayerNow(ctx: Context, prayer: String, start: String, end: String) = post(
        ctx,
        Spec(
            nid("PRAYER", prayer),
            Ch.Prayer,
            PixMood.Praying,
            "It's time for $prayer",
            "$start · until $end",
            "deen/namaz",
            listOf(Act("✓ Prayed", A.PRAYED, true)),
            subId = prayer,
        ),
    )
    fun prayerCheck(ctx: Context, prayer: String) = post(
        ctx,
        Spec(
            nid("PRAYER", prayer),
            Ch.Prayer,
            PixMood.Praying,
            "Did you pray $prayer?",
            "Log it so your Deen tracker stays accurate.",
            "deen/namaz",
            listOf(Act("Yes", A.PRAYED, true), Act("Not yet", A.DISMISS)),
            subId = prayer,
        ),
    )
    fun allFivePrayed(ctx: Context, deenStreak: Int) = post(
        ctx,
        Spec(
            nid("D04"),
            Ch.Prayer,
            PixMood.Celebrate,
            "All 5 prayers today",
            "Deen streak: $deenStreak days.",
            "deen",
        ),
    )
    fun jumuah(ctx: Context) = post(
        ctx,
        Spec(
            nid("D05"),
            Ch.Prayer,
            PixMood.Praying,
            "Jumu'ah Mubarak",
            "Read Al-Kahf today · 110 ayahs.",
            "deen/quran/18",
            listOf(Act("Open Al-Kahf", A.OPEN, true)),
        ),
    )
    fun quranContinue(ctx: Context, surah: String, surahNo: Int, ayah: Int) = post(
        ctx,
        Spec(
            nid("D06"),
            Ch.Daily,
            PixMood.Focus,
            "Continue $surah · ayah $ayah",
            "Your 2 pages for today.",
            "deen/quran/$surahNo",
            listOf(Act("Read now", A.OPEN, true)),
        ),
    )

    // ================= PARTIES =================
    fun daroodActivity(ctx: Context, partyId: String, party: String, who: String, amount: Int, total: Int, milestone: Int?, usual: Int) = post(
        ctx,
        Spec(
            nid("PARTY", partyId), Ch.Party, PixMood.Happy,
            "$who added +${fmt(amount)}", "$party · ${fmt(total)}" + (milestone?.let { " / ${fmt(it)}" } ?: ""), "parties/$partyId",
            listOf(Act("+${fmt(usual)} from me", A.PARTY_ADD)), progress = milestone?.let { total to it },
            group = "pq_party_group", alertOnce = true, subId = partyId, extraInt = usual,
        ),
    )
    fun daroodMilestone(ctx: Context, partyId: String, party: String, members: Int, milestone: Int) = post(
        ctx,
        Spec(
            nid("R02", partyId, milestone),
            Ch.Party,
            PixMood.Wow,
            "$party reached ${fmt(milestone)}!",
            "Set the next goal in Party info.",
            "parties/$partyId/info",
            listOf(Act("Set next goal", A.OPEN, true)),
            card = Card("$party · $members members", "MILESTONE!", "Together you reached ${fmt(milestone)}.", CardBg.Sky),
        ),
    )
    fun fridayNote(ctx: Context, days: Int, total: Int) = post(
        ctx,
        Spec(
            nid("R03"),
            Ch.Party,
            PixMood.Praying,
            "Your week of Darood",
            "You added Darood on $days of 7 days · ${fmt(total)} across parties.",
            "parties",
        ),
    )
    fun partyInvite(ctx: Context, inviteId: String, from: String, party: String, type: String, members: Int) = post(
        ctx,
        Spec(
            nid("R04", inviteId),
            Ch.Party,
            PixMood.Happy,
            "$from invited you to $party",
            "$type party · $members members",
            "parties",
            listOf(Act("Join", A.INVITE_ACCEPT, true), Act("Decline", A.INVITE_DECLINE)),
            subId = inviteId,
        ),
    )
    fun studyItemAdded(ctx: Context, partyId: String, party: String, who: String, type: String, title: String, due: String?) = post(
        ctx,
        Spec(
            nid("R05", partyId, title),
            Ch.Study,
            PixMood.Focus,
            "$who added a $type",
            "$title" + (due?.let { " · due $it" } ?: "") + " · $party",
            "parties/$partyId",
            group = "pq_study_$partyId",
        ),
    )
    fun studyDueTomorrow(ctx: Context, itemId: String, title: String, party: String) = post(
        ctx,
        Spec(
            nid("R06", itemId),
            Ch.Study,
            PixMood.Worried,
            "Due tomorrow: $title",
            "$party · finish today for the +5 early bonus.",
            "parties",
            listOf(Act("✓ Done", A.STUDY_DONE, true)),
            subId = itemId,
        ),
    )
    fun studyWeeklyResult(ctx: Context, partyId: String, party: String, place: Int, points: Int, winner: String, winnerPoints: Int) = post(
        ctx,
        Spec(
            nid("R07", partyId),
            Ch.Study,
            PixMood.LevelUp,
            "$party · you finished #$place",
            if (place == 1) "$points pts. You won the week!" else "$points pts. $winner won with $winnerPoints. New week starts now.",
            "parties/$partyId",
            listOf(Act("See scores", A.OPEN)),
        ),
    )
    fun studyTimerOngoing(ctx: Context, partyId: String, party: String, startedAt: Long) = post(
        ctx,
        Spec(
            nid("R08"), Ch.Ongoing, PixMood.Focus,
            "Studying", "$party · every 10 min = +1 pt", "parties/$partyId",
            listOf(Act("Stop", A.TIMER_STOP, true)), ongoing = true, chronometerBase = startedAt, subId = partyId,
        ),
    )

    // ================= CHESS =================
    fun chessChallenge(ctx: Context, challengeId: String, from: String, minutes: Int, rated: Boolean, h2h: String, secondsLeft: Int = 60) = post(
        ctx,
        Spec(
            nid("C01", challengeId), Ch.Chess, PixMood.Challenge,
            "$from challenged you", "$minutes min · ${if (rated) "Rated" else "Casual"} · $h2h", "chess/challenge/$challengeId",
            listOf(Act("Accept", A.CHESS_ACCEPT, true), Act("Decline", A.CHESS_DECLINE)),
            progress = secondsLeft to 60, fullScreen = true, bypassQuiet = true, subId = challengeId,
        ),
    )
    fun chessChallengeExpired(ctx: Context, to: String) = post(
        ctx,
        Spec(
            nid("C02"),
            Ch.Chess,
            PixMood.Sad,
            "$to is busy",
            "Your challenge expired. Try a quick game?",
            "chess",
            listOf(Act("Quick play", A.OPEN)),
        ),
    )
    fun chessYourMove(ctx: Context, gameId: String, opponent: String, minutes: Int, clock: String) = post(
        ctx,
        Spec(
            nid("C03"), Ch.Ongoing, PixMood.Focus,
            "Your move · $clock left", "vs $opponent · $minutes min. Don't flag!", "chess/game/$gameId",
            listOf(Act("Return to game", A.OPEN, true)), ongoing = true, subId = gameId,
        ),
    )
    fun chessLive(ctx: Context, gameId: String, white: String, black: String, party: String, minutes: Int, watchers: Int) = post(
        ctx,
        Spec(
            nid("C04", gameId),
            Ch.Chess,
            PixMood.Challenge,
            "$white vs $black is live",
            "$party · $minutes min · $watchers watching",
            "chess/watch/$gameId",
            listOf(Act("Watch", A.OPEN)),
        ),
    )
    enum class Result { Win, Loss, Draw }
    fun chessResult(ctx: Context, gameId: String, r: Result, opponent: String, intDelta: Int, pool: String, newInt: Int, h2h: String, reason: String? = null) = post(
        ctx,
        Spec(
            nid("C05", gameId),
            Ch.Chess,
            when (r) {
                Result.Win -> PixMood.Celebrate
                Result.Loss -> PixMood.Sad
                Result.Draw -> PixMood.Happy
            },
            when (r) {
                Result.Win -> "You beat $opponent · +$intDelta Int"
                Result.Loss -> "$opponent won this one · $intDelta Int"
                Result.Draw -> "Draw with $opponent · ${if (intDelta >= 0) "+" else ""}$intDelta Int"
            },
            (if (r == Result.Draw && reason != null) "$reason · " else "$pool Int ${fmt(newInt)} · ") + "head to head $h2h",
            "chess/game/$gameId",
            listOfNotNull(Act("Rematch", A.CHESS_REMATCH, r == Result.Win), if (r == Result.Win) Act("Review", A.OPEN) else null),
            subId = gameId,
        ),
    )
    fun chessRematch(ctx: Context, challengeId: String, from: String) = post(
        ctx,
        Spec(
            nid("C08", challengeId),
            Ch.Chess,
            PixMood.Challenge,
            "$from wants a rematch",
            "Same settings, colours swapped.",
            "chess/challenge/$challengeId",
            listOf(Act("Play", A.CHESS_ACCEPT, true), Act("No thanks", A.CHESS_DECLINE)),
            subId = challengeId,
        ),
    )
    fun chessIntMilestone(ctx: Context, pool: String, int: Int, note: String) = post(
        ctx,
        Spec(
            nid("C09", pool, int),
            Ch.Level,
            PixMood.LevelUp,
            "$pool Int ${fmt(int)}!",
            "New personal best.",
            "chess/rating",
            card = Card("$pool · new best", "INT ${fmt(int)}!", note, CardBg.Gold),
        ),
    )

    // ================= COACH =================
    fun coachBrief(ctx: Context, summary: String) = post(
        ctx,
        Spec(
            nid("A01"),
            Ch.Coach,
            PixMood.Wise,
            "Coach Intel is ready",
            summary,
            "coach",
            listOf(Act("Open coach", A.OPEN)),
        ),
    )
    fun coachTip(ctx: Context, title: String, body: String) = post(
        ctx,
        Spec(
            nid("A02"),
            Ch.Coach,
            PixMood.Wise,
            title,
            body,
            "coach",
            listOf(Act("Try it", A.OPEN)),
        ),
    )

    /** Replace with your real profile source. */
    object Profile {
        var name: String = "there"
    }
}

/** Daily cap: 5 non-essential notifications. Essential channels (prayer, chess, ongoing) are exempt. */
internal object Limits {
    private const val MAX = 5
    fun allow(ctx: Context, ch: PqNotify.Ch): Boolean {
        val p = ctx.getSharedPreferences("pq_notify_limits", Context.MODE_PRIVATE)
        val today = java.time.LocalDate.now().toString()
        val count = if (p.getString("day", "") == today) p.getInt("count", 0) else 0
        if (count >= MAX) return false
        p.edit().putString("day", today).putInt("count", count + 1).apply()
        return true
    }
}

/** Holds non-essential notifications posted during quiet hours. Flush from a WorkManager job at 06:00 (keep only the latest per id). */
internal object QuietQueue {
    private val held = LinkedHashMap<Int, PqNotify.Spec>()
    fun hold(ctx: Context, s: PqNotify.Spec) {
        held[s.id] = s
    }
    fun flush(ctx: Context) {
        val all = held.values.toList()
        held.clear()
        all.forEach { PqNotify.post(ctx, it.copy(bypassQuiet = true)) }
    }
}
