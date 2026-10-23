# Notification catalog (42 notifications, generated from the design doc)

Every row = one builder in `PqNotify.kt`. Pix = large-icon mood. ★ = primary (filled) action.

## Daily quests
Plan, nudge and wrap the day.

| ID | Name | Pix | Channel | Title · Body | Actions | Trigger | Builder |
|---|---|---|---|---|---|---|---|
| N01 | Morning plan | `happy` | `pq_daily` | **Good morning, Raza**<br>4 quests today. First up: Read 15 Pages at 21:00. | OPEN QUESTS | 07:30 local, or 10 min after Fajr is logged. Skipped if the app opened today. | `morningPlan(ctx, count, firstTitle, firstTime)` |
| N02 | Quest due | `focus` | `pq_quest` | **Read 15 Pages · now**<br>21:00 · Learning · +20 XP on the line | ★ ✓ DONE / SNOOZE 10M | At the quest time. Heads-up. | `questDue(ctx, questId, title, time, category, xp)` |
| N03 | Quest overdue | `worried` | `pq_quest` | **Read 15 Pages is 1 hour late**<br>It still counts today. +20 XP. | ★ ✓ DONE / SKIP TODAY | 60 min after the quest time if it isn't done. Once per quest. | `questOverdue(ctx, questId, title, xp)` |
| N04 | Halfway there | `happy` | `pq_daily` | **2 of 4 done**<br>Two more and it's a perfect day.<br>_progress 50% (2/4)_ | — | When the 2nd of 4 (or half) completes, if before 18:00. | `halfway(ctx, done, total)` |
| N05 | Perfect day | `celebrate` | `pq_level` | **Perfect day!**<br>+120 XP · 5 day streak<br>_custom card: ALL QUESTS DONE · **PERFECT DAY!** · +120 XP · 5 day streak_ | — | The last quest of the day completes. | `perfectDay(ctx, xp, streak)` |
| N06 | Evening recap | `sleepy` | `pq_daily` | **Day wrapped: 3 of 4**<br>+60 XP today. Tomorrow starts with Fajr at 4:48 AM. | — | 22:30, silent, only if there was activity today and no perfect day. | `eveningRecap(ctx, done, total, xp, firstTomorrow)` |

## Streak
Pix gets more anxious as midnight gets closer. That's the Duolingo pull, kept kind.

| ID | Name | Pix | Channel | Title · Body | Actions | Trigger | Builder |
|---|---|---|---|---|---|---|---|
| S01 | Streak at risk | `worried` | `pq_streak` | **Your 4-day streak needs one quest**<br>Anything counts. Finish one before midnight. | ★ QUICK QUEST / REMIND AT 22:00 | 20:00 if no quest is done today and streak ≥ 2. | `streakAtRisk(ctx, days)` |
| S02 | Last hour | `sad` | `pq_streak` | **1 hour left**<br>Your 4-day streak ends at midnight.<br>_custom card: STREAK · 4 DAYS · **1 HOUR LEFT** · Pix is not okay. Do one quest._<br>_progress 92% (23:00)_ | ★ SAVE IT | 23:00 if still nothing done. Ignores quiet hours. | `streakLastHour(ctx, days)` |
| S03 | Streak saved | `onfire` | `pq_streak` | **Streak saved! 5 days**<br>Just in time. Your best is 7. | — | A quest completes after S01 or S02 was shown. | `streakSaved(ctx, days, best)` |
| S04 | Streak milestone | `levelup` | `pq_level` | **7 day streak!**<br>A full week. Pix is wearing the crown.<br>_custom card: NEW RECORD · **7 DAY STREAK!** · A full week. +50 bonus XP._ | SHARE | Streak reaches 7, 14, 30, 50, 100 or 365. | `streakMilestone(ctx, days)` |
| S05 | Streak lost | `lost` | `pq_streak` | **Your streak ended at 4 days**<br>Start a new one today. Your best stays 7. | ★ START AGAIN | 08:00 the morning after a streak breaks. | `streakLost(ctx, days, best)` |
| S06 | Comeback · 3 days | `sleepy` | `pq_daily` | **Pix kept your quests warm**<br>3 quests waiting. 5 minutes to restart. | OPEN | No app open for 3 days, at the user's usual open time. | `comeback(ctx, daysAway = 3)` |
| S07 | Comeback · 7 days | `sad` | `pq_daily` | **It's quiet without you**<br>One quest today brings Pix back to Happy. | ONE QUEST | No app open for 7 days. Then stop. Never more than 2 comebacks. | `comeback(ctx, daysAway = 7)` |

## Progress
Rewards and reports.

| ID | Name | Pix | Channel | Title · Body | Actions | Trigger | Builder |
|---|---|---|---|---|---|---|---|
| P01 | Level up | `levelup` | `pq_level` | **Level up! LVL 5**<br>1,500 XP banked. New title: Seeker.<br>_custom card: LVL 5 · **LEVEL UP!** · 1,500 XP banked · New title: Seeker_<br>_progress 100%_ | — | XP crosses a level threshold. | `levelUp(ctx, level, xp, title)` |
| P02 | Avatar unlocked | `celebrate` | `pq_level` | **New avatar: Mage**<br>Unlocked at LVL 5. Equip it? | EQUIP | An avatar unlocks. | `avatarUnlocked(ctx, name)` |
| P03 | Weekly report | `wise` | `pq_coach` | **Week 42: 82% complete**<br>Best day Tuesday · +410 XP<br>_Inbox: Quests 23 of 28 · Prayers 31 of 35 · Chess 4W 1D 2L_ | — | Sunday 19:00. | `weeklyReport(ctx, week, percent, lines)` |
| P04 | Passed on the board | `challenge` | `pq_level` | **@sara passed you**<br>You're #4 this week, 40 XP behind. | OPEN BOARD | Signed-in users only. Once a day at most. | `leaderboardPassed(ctx, by, rank, gap)` |

## Deen
Calm tone. Pix never guilts about prayer.

| ID | Name | Pix | Channel | Title · Body | Actions | Trigger | Builder |
|---|---|---|---|---|---|---|---|
| D01 | Prayer reminder | `praying` | `pq_prayer` | **Fajr in 15 minutes**<br>4:48 AM · 0 / 5 prayed today | ★ ✓ PRAYED / REMIND IN 5 | X min before each prayer (user setting, default 15). Ignores quiet hours. | `prayerReminder(ctx, prayer, time, minutesLeft, prayedToday)` |
| D02 | Prayer time | `praying` | `pq_prayer` | **It's time for Dhuhr**<br>11:48 AM · until 3:12 PM | ★ ✓ PRAYED | At the prayer time, if D01 is turned off. | `prayerNow(ctx, prayer, start, end)` |
| D03 | Prayer check-in | `praying` | `pq_prayer` | **Did you pray Isha?**<br>Log it so your Deen tracker stays accurate. | YES / NOT YET | 45 min before the next prayer, if the previous one isn't logged. | `prayerCheck(ctx, prayer)` |
| D04 | All five prayed | `celebrate` | `pq_prayer` | **All 5 prayers today**<br>Deen streak: 3 days. | — | The fifth prayer is logged. | `allFivePrayed(ctx, deenStreak)` |
| D05 | Jumu'ah | `praying` | `pq_prayer` | **Jumu'ah Mubarak**<br>Read Al-Kahf today · 110 ayahs. | OPEN AL-KAHF | Friday, 2h before Dhuhr. | `jumuah(ctx)` |
| D06 | Quran · continue | `focus` | `pq_daily` | **Continue Al-Baqara · ayah 142**<br>Your 2 pages for today. | READ NOW | Daily at the user's chosen time, if no reading was logged. | `quranContinue(ctx, surah, ayah)` |

## Parties
Darood + Study. Grouped per party so the shade stays clean.

| ID | Name | Pix | Channel | Title · Body | Actions | Trigger | Builder |
|---|---|---|---|---|---|---|---|
| R01 | Darood activity | `happy` | `pq_party` | **@sara added +313**<br>Raza · 1,039 / 10,000<br>_progress 10% (10%)_ | +313 FROM ME | A member posts. Batched: one update per party per 15 min. | `daroodActivity(ctx, partyId, party, who, amount, total, milestone, usual)` |
| R02 | Darood milestone | `wow` | `pq_party` | **Raza reached 10,000!**<br>Set the next goal in Party info.<br>_custom card: RAZA · 1 MEMBER · **MILESTONE!** · Together you reached 10,000._ | SET NEXT GOAL | The party total crosses its milestone. | `daroodMilestone(ctx, partyId, party, milestone)` |
| R03 | Friday note | `praying` | `pq_party` | **Your week of Darood**<br>You added Darood on 5 of 7 days · 2,310 across parties. | — | Friday after Maghrib, silent, if turned on. | `fridayNote(ctx, days, total)` |
| R04 | Party invite | `happy` | `pq_party` | **@ali invited you to Bros**<br>Study party · 3 members | ★ JOIN / DECLINE | An invite arrives. | `partyInvite(ctx, inviteId, from, party, type, members)` |
| R05 | Study item added | `focus` | `pq_study` | **@sara added a quiz**<br>Calc quiz 3 · due tomorrow · Bros | — | A member adds an item. Grouped per party. | `studyItemAdded(ctx, partyId, party, who, type, title, due)` |
| R06 | Due tomorrow | `worried` | `pq_study` | **Due tomorrow: Physics lab report**<br>Finish today for the +5 early bonus. | ★ ✓ DONE | 18:00 the day before. Pinned items also 2 days before. | `studyDueTomorrow(ctx, itemId, title, party)` |
| R07 | Week wrapped | `levelup` | `pq_study` | **Bros · you finished #2**<br>33 pts. @sara won with 48. New week starts now. | SEE SCORES | Monday 00:05 when the board resets. | `studyWeeklyResult(ctx, party, place, points, winner, winnerPoints)` |
| R08 | Study timer | `focus` | `pq_ongoing` | **Studying · 24:13**<br>Bros · +2 pts so far | STOP | Ongoing while the timer runs. Uses a chronometer. | `studyTimerOngoing(ctx, party, startedAt)` |

## Chess
Fast and time-critical. Challenges use full-screen intent when the phone is locked.

| ID | Name | Pix | Channel | Title · Body | Actions | Trigger | Builder |
|---|---|---|---|---|---|---|---|
| C01 | Challenge | `challenge` | `pq_chess` | **@sara challenged you**<br>3 min · Rated · you lead 4–2–1<br>_progress 70% (0:42)_ | ★ ACCEPT / DECLINE | An incoming challenge. Expires in 60s, and the bar counts down. | `chessChallenge(ctx, challengeId, from, minutes, rated, h2h)` |
| C02 | Challenge expired | `sad` | `pq_chess` | **@sara is busy**<br>Your challenge expired. Try a quick game? | QUICK PLAY | Your outgoing challenge times out or is declined. | `chessChallengeExpired(ctx, to)` |
| C03 | Your move | `focus` | `pq_ongoing` | **Your move · 2:12 left**<br>vs @sara · 3 min. Don't flag! | ★ RETURN TO GAME | Ongoing while a game is live and the app is in the background. | `chessYourMove(ctx, gameId, opponent, clock)` |
| C04 | Party game live | `challenge` | `pq_chess` | **@ali vs @sara is live**<br>Bros · 5 min · 5 watching | WATCH | A game starts in your party with spectators allowed. Off by default. | `chessLive(ctx, gameId, white, black, party, watchers)` |
| C05 | You won | `celebrate` | `pq_chess` | **You beat @sara · +14 Int**<br>Blitz Int 1,060 · head to head 5–2–1 | ★ REMATCH / REVIEW | A game ends while the app is in the background. | `chessResult(ctx, gameId, Result.Win, …)` |
| C06 | You lost | `sad` | `pq_chess` | **@sara won this one · −10 Int**<br>Blitz Int 1,036 · head to head 4–2–2 | REMATCH | As C05, for a loss. | `chessResult(ctx, gameId, Result.Loss, …)` |
| C07 | Draw | `happy` | `pq_chess` | **Draw with @sara · +2 Int**<br>Threefold repetition · head to head 4–3–1 | REMATCH | As C05, for a draw. | `chessResult(ctx, gameId, Result.Draw, …)` |
| C08 | Rematch offer | `challenge` | `pq_chess` | **@sara wants a rematch**<br>Same settings, colours swapped. | ★ PLAY / NO THANKS | The opponent taps Rematch after you've left the result screen. | `chessRematch(ctx, challengeId, from)` |
| C09 | Int milestone | `levelup` | `pq_level` | **Blitz Int 1,100!**<br>New personal best.<br>_custom card: BLITZ · NEW BEST · **INT 1,100!** · Top of Bros this week._ | — | Int crosses a 100 boundary for the first time. | `chessIntMilestone(ctx, pool, int)` |

## Coach
Low priority. Pix wears the glasses.

| ID | Name | Pix | Channel | Title · Body | Actions | Trigger | Builder |
|---|---|---|---|---|---|---|---|
| A01 | Coach brief ready | `wise` | `pq_coach` | **Coach Intel is ready**<br>4-day streak, matching your best. Finish Read 15 Pages before 21:00 for a new record. | OPEN COACH | A new brief is generated (about every 6h, while AI is unlocked). | `coachBrief(ctx, summary)` |
| A02 | Coach tip | `wise` | `pq_coach` | **Tip: stack reading after Isha**<br>You finish Learning quests 3× more often after a prayer. | TRY IT | At most 2 a week, from coach insights. | `coachTip(ctx, title, body)` |
