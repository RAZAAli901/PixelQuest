# QA checklist: 1.7.0 on real phones

Run this on the **signed release APK** before tagging v1.7.0 (J179, J180). What the automated tests already cover is listed in [VERIFICATION.md](../VERIFICATION.md); this is what only a person with phones can check. Write the result in each box (✅, ❌ with a note, or "skipped: why") and the date, then copy the table into VERIFICATION.md.

**You need:**
- two Android phones (or one phone and the `PixelQuest_API24` emulator), each signed in to a different account;
- a third account for watching, if you can (a second emulator or a friend's phone);
- the hosted Supabase project (migrations through `20261019000108`, every function deployed);
- for notifications: the `FCM_*` settings in `local.properties` when building, and the `FCM_SERVICE_ACCOUNT` function secret.

Mark what you can't run as "skipped" and say why. Push without the Firebase settings is one such case.

Tested on: ____________ (phone, Android version) and ____________ · build: `app-release.apk` 1.7.0 (108) · date: ________

## 1. Update without losing anything

| # | Check | Result |
|---|---|---|
| 1.1 | On a phone with **1.6.0** and real use (quests done today, a streak, Namaz on, a Darood Party, a study party), install 1.7.0 over it. It opens without the welcome steps. | |
| 1.2 | Quests, today's ticks, XP, level, streak, Namaz log, settings and the skin are all as they were. | |
| 1.3 | Parties, Darood entries and the study party's items are there, also with the network off. | |
| 1.4 | Settings → Backup → export a file, then restore it: the summary matches, and nothing is lost. | |

## 2. Study reminders

| # | Check | Result |
|---|---|---|
| 2.1 | Add a party reminder due in 10 minutes with a 5-minute alert. The alert arrives on **both** phones on time, also with the app closed. | |
| 2.2 | Add a reminder for "just me": only your phone shows it and alerts. | |
| 2.3 | A weekly reminder shows its next date after the first passes. Deleting it stops its alerts. | |

## 3. Study chat and files

| # | Check | Result |
|---|---|---|
| 3.1 | A message from phone A appears on phone B within a couple of seconds, with B on the chat. | |
| 3.2 | Reply, copy and delete your own message. As leader, delete the other's. | |
| 3.3 | Share a PDF and a photo from A. They show on B as ready; opening one asks first, then opens in the phone's viewer. | |
| 3.4 | Try a file over 20 MB, and an `.exe` renamed to `.pdf`: both refused, with a clear reason. | |
| 3.5 | Report B's message from A (long press → Report). Then block B: B's messages collapse to one line on A only. Tap the line to unblock. | |
| 3.6 | Airplane mode on A: a message waits with a clock. Back online, it sends (at the latest when the chat is opened or pulled to refresh), once. | |

## 4. Darood weekly ranking and goals

| # | Check | Result |
|---|---|---|
| 4.1 | Darood Party → This week: each member's place, total and active days, also in Simple Mode. | |
| 4.2 | After the week closes (Sunday night in the party's time zone), each member gets their place as a notification. Past weeks show the result. Muting the party stops the next one. | |
| 4.3 | Reach a goal: everyone sees it reached, and the leader can **Extend goal**. Party info lists the goal history. | |
| 4.4 | Nothing in Darood changes XP, level or streak. | |

## 5. Chess: two phones

| # | Check | Result |
|---|---|---|
| 5.1 | On A, study party → Chess → Challenge → pick B, 3 min, rated, White. B gets a notification (with push set up) and a card in the chat. | |
| 5.2 | B opens it: who, the game, your record, and a countdown. **Accept**: both phones open the board, A as White at the bottom, B as Black. | |
| 5.3 | Play 10 moves each, tapping and dragging. Each move appears on the other phone at once. An illegal move is not taken. Both clocks match within a second. | |
| 5.4 | Promote a pawn: the picker offers queen, rook, bishop and knight. With Settings → Chess → Auto-queen on, it doesn't ask. | |
| 5.5 | Offer a draw from A; B declines, then B offers and A accepts. The end card shows the draw and both ratings. | |
| 5.6 | New game: let A's clock run out with A's app closed. B sees A lose on time within a minute. | |
| 5.7 | New game: A turns on airplane mode for 20 seconds mid-game. A reconnects, and the board and clocks catch up. | |
| 5.8 | A resigns: it asks first. The end card: who won, how, the Int change, and the record between you. **Rematch** sends a new challenge. | |
| 5.9 | Back from a game asks first. Leaving and returning from Home resumes the game. The screen stays on during a game. | |
| 5.10 | On the incoming-challenge screen, **Report** asks first and says it's done. **Block** declines the challenge, and then neither of you can challenge the other. Unblock from the chat. | |
| 5.11 | The Chess tab's ladder and each player's tier match the end cards. The Hero card shows your best rating. | |

## 6. Chess: watching

| # | Check | Result |
|---|---|---|
| 6.1 | With a game on between A and B, a third member opens the Chess tab: the live game is listed with its watcher count. | |
| 6.2 | Watching: the board updates with every move. Tapping squares does nothing. The players see "1 watching". | |
| 6.3 | Leaving drops the count on the players' phones. | |

## 7. Looks and access

| # | Check | Result |
|---|---|---|
| 7.1 | The board, end card, Chess tab, chat and weekly ranking in **Comic** and **Pixel**, and with Simple Mode on (no ratings or ladder in chess). | |
| 7.2 | TalkBack: the board is read square by square ("e4, white pawn"), moves are announced, and a move can be made by double-tapping two squares. | |
| 7.3 | Largest font size and display size: nothing is cut off on the board, the clocks, the challenge screen or the chat. | |
| 7.4 | Dark wallpapers and the phone's dark mode don't make anything unreadable. | |

## 8. Privacy

| # | Check | Result |
|---|---|---|
| 8.1 | Settings → Cloud & leaderboard → **Export my cloud data** saves a file. It holds your messages and games and nobody else's messages. | |
| 8.2 | With a throwaway account that played a game and shared a file: **Delete my cloud data**. On the opponent's phone, the game is still in their history with the other side unnamed. | |
| 8.3 | A notification on the lock screen doesn't show who challenged you. | |

## 9. Android 7

| # | Check | Result |
|---|---|---|
| 9.1 | On the `PixelQuest_API24` emulator (port 5556): install 1.7.0 over 1.6.0, open a study party's chat and the Chess tab, and play a few moves against another phone. | |

## Sign-off

- [ ] Every row is ✅ or explained.
- [ ] The results are copied into VERIFICATION.md (Section S).
- [ ] Then: J180, the signed build is tagged `v1.7.0` (docs/RELEASE_PIPELINE.md), and the README's download links move to v1.7.0.
