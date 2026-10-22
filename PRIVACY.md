# PixelQuest Privacy Policy

*Last updated: September 12, 2026*  
*App Version: 1.1.0*

PixelQuest is committed to protecting your privacy and giving you complete control over your personal habit data. We believe your daily habits and self-improvement journeys are deeply personal.

---

## 1. Local-First Philosophy (Default Operation)

By default, PixelQuest operates **100% locally and offline**:
- **All Core Gameplay Stored On-Device**: Your quests, tasks, schedule times, recurrence rules, completion logs, streaks, experience points (XP), level-up history, avatar preferences, and notification settings are stored strictly in a local SQLite database on your device using Android Room.
- **Zero Third-Party Trackers**: PixelQuest contains **zero** commercial advertising SDKs, zero behavioral analytics, zero telemetry frameworks, and zero device fingerprinting trackers.
- **No Cloud Account Required**: You can play and enjoy the complete PixelQuest experience without ever creating an account, signing in, or connecting to the internet.
- **Copies Kept on the Phone**: PixelQuest keeps three kinds of copy of your progress in its own private storage, which other apps can't read:
  - a daily automatic backup, for the last 3 days, once you've played;
  - what you had just before your last restore, so the restore can be undone (only when you had progress to go back to);
  - the saved game as it was before an update changed how it's stored, until the next such update.

  They never leave the phone, aren't part of Android's backup, and go when you uninstall the app or use **Clear storage**.
- **Android's Own Backup**: If backup is on in your phone's settings, Android saves PixelQuest's data with the rest of your phone's backup, and restores it after a reinstall or on a new phone (also when moving phones directly).
  - **What it copies:** only your progress and settings (the quest database, settings, a pending level-up, and which account last signed in).
  - **What it never copies:** your sign-in session, so a restored phone asks you to sign in again. Nor AI Coach data, reminder bookkeeping or leaderboard choices still waiting to sync.
  - **Your own file:** PixelQuest's export (Settings → EXPORT QUEST DATA) is a separate file you control. See docs/UPDATES_AND_BACKUPS.md.

---

## 2. Optional Cloud Leaderboard & Authentication

If you choose to participate in the global community leaderboard, or to use the AI Coach, PixelQuest offers an optional cloud account powered by Supabase. You can sign in with Google, or with your email address and a one-time code we email you (no password). Signing in on its own uploads nothing to the leaderboard.

### What Data is Collected (and What is NOT)
When you sign in and choose to join the leaderboard, the following data is transmitted and stored:

| Data Field | Purpose | Publicly Visible? |
| :--- | :--- | :--- |
| **Supabase User ID (UUID)** | Unique cryptographic identifier for database record association. | No (internal identifier) |
| **Email Address (and Google OAuth credentials, if you use Google)** | Used strictly by Supabase Auth to sign you in: to send your one-time code by email, or to check your Google sign-in, and to issue secure session tokens. | **Never.** Email is never stored in public profile tables or shown on the leaderboard. |
| **Public Display Name** | Pseudonym chosen by you for the leaderboard. Moderated for offensive language. | **Yes**, visible to other authenticated players on rankings. |
| **Current & Longest Streak** | Number of consecutive perfect days completed. | **Yes**, used for "Top Streaks" ranking. |
| **Player Level & Total XP** | Current RPG level and total accumulated experience points. | **Yes**, used for "Top Levels" ranking. |
| **Leaderboard Opt-In Flag** | Boolean flag indicating active participation. | Used by database security policies. |
| **Last Updated Timestamp** | UTC timestamp of the last write, from the server's clock. | Stored with the row; not shown in rankings. |
| **When your streak and level last changed** | Two server-side timestamps used only to check that leaderboard numbers grow at a pace the game allows (a streak by at most one day a day). Kept in a separate table that players, including you, can't read through the app. | **No.** |

> [!IMPORTANT]
> **Your individual task names, quest descriptions, scheduled times, and private completion logs are NEVER uploaded to the cloud.** Only your aggregate statistics (level, streak, XP) and chosen display name are synchronized.

### When Data is Collected & Synced
Data synchronization occurs strictly when:
1. You explicitly tap "Sign in with Google" or "Sign in with email" in `AccountScreen`.
2. You explicitly choose to opt into the leaderboard and confirm your public display name.
3. Background synchronization (`ProfileSyncWorker`) runs after you complete a quest or level up, pushing updated aggregate stats.

**Sharing a phone:** the leaderboard choice, your public display name and your AI Coach consent belong to your account. If a different account signs in on the same phone, they're reset, so the next person chooses for themselves and is never put on the leaderboard under your name. When you sign in again (also after reinstalling the app, or on another phone), your leaderboard choice and public name come back from your account's own record on the server. With one account on two phones, joining or leaving on either one applies to both: the other phone follows at its next sync instead of undoing it. The AI Coach consent isn't stored there, so it stays off until you turn it on again.

---

## 3. Database Security & Row-Level Security (RLS)

Our cloud database utilizes PostgreSQL with strict **Row-Level Security (RLS)**:
- **Write Isolation**: A player can only create, update, or delete their own profile (`auth.uid() = id`).
- **Read Restrictions**: Profiles are only queryable if `leaderboard_opt_in` is explicitly `true`.
- **Spectator Mode**: Signed-in players who have not opted in can browse the leaderboard in read-only mode without appearing on the leaderboard.
- **Moderation Reports**: Display name abuse reports are stored in an insert-only table accessible strictly to moderation tooling.
  - Each report holds your account ID as the reporter, the reported player and a reason.
  - You can report a player once a day and can't report yourself.
  - The time recorded is the server's.

---

## 3. Optional AI Habit Insights (Google Gemini Integration)

PixelQuest offers AI-powered habit analysis and coaching powered by Google's Gemini API. This is a **completely distinct third-party data flow from Supabase** and operates under strict data-minimization safeguards.

### A. Strict Opt-In Architecture
- **Disabled by Default**: AI Habit Insights are turned **OFF** by default on all installations.
- **Player Consent Required**: No network requests are ever sent to Google Gemini unless you explicitly navigate to **Settings > AI Habit Insights** and enable the feature.
- **Signed-In Players Only**: The AI Coach works only while you're signed in (Google or an emailed code). Signed out, the app sends no AI requests at all and doesn't show insights or AI-written reminder lines saved earlier.

### B. What Habit-Summary Data is Transmitted
When you request or receive an AI insight, PixelQuest transmits strictly anonymized, high-level summary metrics:
- **Streak Totals**: Current streak count, longest streak count, and total perfect days.
- **Category Ratios**: Predefined category tags (e.g. `FITNESS`, `LEARNING`, `HEALTH`) paired with completion percentages and counts (e.g. `80% (4/5 completed)`).
- **Recent Momentum**: Aggregate 7-day completion percentage and missed task count.
- **Progression Tier**: Your numeric player level (e.g. `Level 5`).

### C. What is NEVER Transmitted to Google Gemini
- **Verbatim Task Names & Notes**: We **never** transmit individual task titles (such as "Take heart medication" or "Call counselor") or notes. Raw task descriptions remain 100% on your device.
- **Personal Identifiers**: No usernames, real names, emails, Google OAuth credentials, or Supabase user IDs are ever included in AI prompts.
- **Device & Location Data**: Zero device telemetry, GPS coordinates, advertising IDs or hardware identifiers.
- **Active Sanitization Defense**: PixelQuest executes an active client-side regex and blacklist sanitization pass prior to prompt dispatch, actively scrubbing emails, phone numbers, and names even in accidental edge cases.

### D. How AI Requests Reach Gemini
- The released app doesn't contain the Gemini API key. Its AI requests go to PixelQuest's own server, a Supabase Edge Function called `gemini-proxy`. That server holds the key and passes each request on to Google Gemini.
- Along with the anonymized summary above, the app sends your **sign-in session token** (in a request header, never in the prompt). The server uses it only to check with Supabase Auth that you're a signed-in player, and to learn your account ID. Nothing from it is passed to Google.
- The server stores only how many AI calls each account ID made on each day, so it can enforce daily limits. Those counts are deleted after 30 days. It doesn't store your prompts or Gemini's answers.
- Earlier versions sent a random install ID instead; the app no longer makes or sends one.

---

## Namaz Tracker and your location

The Namaz Tracker (off unless you turn it on) works out prayer times on your phone. It needs your rough location for that, about the city you're in:
- **When it asks:** only when you turn the tracker on or tap *Use my location*, and it says why first. You can pick a city from a list instead, or keep the default (Lahore).
- **How precise:** coarse (city-level) location only. The app never asks for your precise position.
- **Where it goes:** the place is saved in the app's database on your phone, so the times keep working offline. It's never sent anywhere: not to PixelQuest's server, the leaderboard or the AI Coach.
- **Backups:** like the rest of your progress, it's in your exported backup files and in Android's own backup of the app.
- **Reminders:** prayer reminders are worked out and scheduled on your phone, with Android's own alarms. Nothing about your prayers or reminders is sent anywhere.
- **XP and the leaderboard:** a prayer's XP joins your XP total, as a quest's does. If you've joined the leaderboard, only that total is synced, with your level and streaks. Which prayers you marked, and when, never leave your phone.

---

## Darood Parties

- **Your username:** to join Darood Parties, a signed-in player chooses a username (3 to 20 letters, numbers or `_`). It's stored on PixelQuest's server with your account, and on your phone so the app can show it offline.
  - It isn't listed anywhere and isn't on the leaderboard. Others can only check whether a name they type is taken, which doesn't say whose it is.
  - The members of a party you're in see it, and while you're invited, so do the party's leader and whoever invited you. Nobody else can read it.
  - You can change it once every 30 days. Deleting your cloud account deletes it.
- **What you post:** each number you add, the day it's for and your time zone. These are stored on the server with the party, and seen only by the party's active members.
  - Entries can't be edited or deleted, by you or anyone. A party's total is its members' work together.
  - If you leave or are removed, what you posted stays in the party's total.
  - If you delete your cloud account, your entries stay in the total without your name.
  - A deleted party is hidden from everyone, its members and entries with it.
- **Your numbers:** your total, share, place and how steady you are come from your party's entries. They're shown to you, and in "Everyone's part" to the party's members.
- **The race board (off unless a party's leader turns it on):** other signed-in players see the party's name, how many are in it, its total, how far it is to its milestone and when it finished. Never a member's name, username or numbers.
- **The weekly note (off unless you turn it on):** a notification on your phone on Friday evenings, worked out from your own days. Nothing about it is sent anywhere.
- **The weekly ranking:** at the end of each week the server records each member's place, total and active days for that week. Only the party's members see them. A notification tells you your place; you can mute it for a party.
- **Goals:** each goal a party sets is kept in its history, with who set it and when. If you delete your cloud account, the goals you set stay without your name.

## Study parties

- **Membership** works as for Darood Parties: the same username, invitations and leader, seen by the same people.
- **What's stored on the server, with the party:** the items members add (a title, a subject, notes and when it's due), who finished each and when, and study time (minutes and a day). Only the party's active members read any of it; nobody else, and nothing of it is on any public board.
  - Your ticks can be undone for 24 hours, then they're final. Study time can't be edited.
  - If you delete your cloud account, your ticks and study time go with it; items you added stay in the party, without your name.
  - A deleted party is gone for everyone, with everything in it.
- **Points** are worked out on the server from those rows, for each member's own week. They never change your XP, level or streaks, and never reach the leaderboard.
- **On your phone:** a copy of your parties, for offline use, cleared when you sign out. The study timer, reminders (on unless you turn them off) and the Sunday summary (off unless you turn it on) are worked out on the phone; nothing about them is sent anywhere.

## Study chat and files

- **Messages:** what you write, when, and what it replies to. Only the party's active members read them. You can delete your own; the leader can delete any. A deleted message keeps only that it was deleted.
- **Files:** the name, type, size and a checksum, and the file itself in private storage. Members open a file through a link that works for 60 seconds, and the phone always asks before opening one. A file that isn't what its name says is refused. A deleted file is removed from storage.
- **Reports and blocks:** a report goes to the PixelQuest team and is never shown to anyone in the party. Blocking someone hides their messages for you only, and stops chess challenges between you.
- **Limits:** 30 messages a minute, 20 uploads a day, 20 MB a file and 500 MB a party, counted on the server.
- **If you delete your cloud account,** your messages and files go with it, and the files are removed from storage.

## Chess

- **What's stored, with the study party:** each game's players, time control, result and every move with its time; your Int points and record in each time control; your record against each party mate; and the challenges you send and get. Only the party's active members read them.
- **Watching:** who joined a game to watch is kept for one day, for the limits, and isn't shown to anyone.
- **Notifications:** to tell you about a challenge, your phone's push token is stored with your account. A notification carries only ids; its words are put together on your phone, and it's hidden on the lock screen. Signing out removes the token.
- **If you delete your cloud account,** your ratings, challenges and tokens go. Games you played stay for your opponents with your side left empty, and their record against you stays without your name.

## 4. How to Delete Your Data (Full Control)

PixelQuest provides transparent, immediate mechanisms to modify or erase your data at any time:

### A. Leaving the Leaderboard (Opt-Out)
- Go to **Settings > Cloud & Leaderboard**.
- Tap **"LEAVE LEADERBOARD (OPT OUT)"** and confirm.
- **Result**: Your cloud row immediately sets `leaderboard_opt_in = false`. You disappear in real time from public rankings, while keeping your cloud account linked. You can rejoin at any time.
- **Offline:** Account says the leave is waiting, and it's sent as soon as you're back online.
- **Signing out:** if you sign out first, the app sends the leave before signing out.
- **Other phones:** your account's other phones follow the leave instead of undoing it.

### B. Permanent Cloud Data & Account Deletion
- Go to **Settings > Cloud & Leaderboard**.
- Scroll to **"Cloud Data & Privacy"** and tap **"DELETE MY CLOUD DATA"**.
- Confirm through the double-confirmation prompt.
- **Result**:
  1. Your row in `public.profiles` is permanently deleted, with the record of when your streak and level last changed.
  2. The `delete_user_account()` RPC purges your Supabase authentication record from `auth.users`.
  3. Local cloud identifiers and credentials on your device are completely wiped, and you are signed out locally.
  4. Local quests, streaks, and levels remain safe on your device.

- **Parties, chat, files and chess:** your memberships, messages, files (from storage too), study ticks and time, ratings and challenges go. What stays is what your party mates need, without your name: your Darood entries in the party's total, items you added, and your games for your opponents.

### C. Exporting Your Cloud Data
- On the same screen, tap **"Export my cloud data"** and choose where to save the file.
- **Result**: one JSON file with your profile and username, your parties, Darood entries and weekly places, study progress, the messages you sent, the files you shared (names and sizes), your chess games with every move, your ratings and records, and the reports and blocks you made. Nobody else's messages or details are in it.

### D. Complete Local Device Reset
- Go to **Settings > Danger Zone > Reset All Progress**.
- **Result**: Wipes the local Room database, resetting all quests, streaks, and settings back to a fresh install.
- **On the leaderboard**, your numbers start over too (sent with the next sync). Your name and your choice to be on the board are kept.

---

## 5. Contact & Inquiries

If you have questions regarding this Privacy Policy or your data rights, please open an issue on GitHub:
- **Repository**: [https://github.com/RAZAAli901/PixelQuest](https://github.com/RAZAAli901/PixelQuest)
