# 📜 PixelQuest Changelog

All notable changes to **PixelQuest** are documented in this file.
PixelQuest follows [Semantic Versioning 2.0.0](https://semver.org/). See [docs/VERSIONING_POLICY.md](docs/VERSIONING_POLICY.md) for full versioning and release lifecycle policies.

---

## [1.8.1] - 2026-10-27 — Notification Banner Dismissal, Circles Tabs & Home Polish

### ⬆️ Upgrading
Install 1.8.1 over 1.8.0 (or any earlier version): 100% backward compatible. All existing quests, streaks, hero stats, and Deen data are preserved.

### 🔔 Notification Banner & Permission Fixes
- **Dismissible Notifications Banner**:
  - Added close/dismiss (`✕`) action to `NotificationsOffBanner`.
  - Dismissal choice is remembered across sessions and screens so the banner does not linger.
  - Automatically resets banner dismissal if notifications are subsequently enabled in settings.
- **Responsive "Allow" Action**:
  - Before requesting `POST_NOTIFICATIONS`, verifies if Android can present the system rationale prompt.
  - If suppressed by Android OS or notifications are disabled in system settings, opens Android's Notification Settings directly so clicking Allow is always responsive.

### 👥 Circles & Home Quest Polish
- **Circles Tabs**:
  - Removed "All" tab in Circles; now dedicated to "Darood" and "Study Circles".
- **Home Screen & Quest Flow**:
  - Reordered cards: AI Coach positioned below Next Prayer and Due Soon items.
  - Clicking "Due in study circle" navigates directly to circle/party pages.
  - Completed quests vanish from the Home checklist.
  - Default quests can now be deleted in edit mode.

---

## [1.8.0] - 2026-10-23 — Pix Mascot, Custom Notifications, Offline Chess Bot & Security Hardening

### ⬆️ Upgrading
Install 1.8.0 over 1.7.2 (or any version from 1.2.0): 100% backward compatible. All existing quests, streaks, hero stats, and Deen data are preserved.

### ♟️ Offline Chess Bot & Performance Optimizations
- **Offline Practice Bot ("Pix AI")**:
  - Play offline chess solo anytime against Pix AI via the new "Play vs Bot" button in the Study Party Chess Tab.
  - Fast heuristic evaluation, checkmate detection, hanging piece protection, and a realistic ~400ms thinking delay.
  - Complete local game lifecycle: instant optimistic piece moves, draw offers, resignations, and rematches.
- **200 Starting Rating (Pawn Tier)**:
  - Standardized chess ladder starting points to 200 Int rating with integer-based delta calculations.
- **Zero-Latency Chess & Touch Optimization**:
  - Eliminated redundant network roundtrips on move dispatch for instant optimistic moves.
  - Clock ticking state updates scoped strictly to clock components, eliminating unnecessary 10 Hz board recompositions.

### 👾 Animated Pix Mascot & Notification Engine
- **Animated Pix Mascot (`PqMascot`)**:
  - 24×24 layered vector grid mascot supporting both `Comic` and `Pixel` aesthetics.
  - 13 reactive moods (`Happy`, `Celebrate`, `LevelUp`, `OnFire`, `Worried`, `Sad`, `Sleepy`, `Focus`, etc.) with real-time micro-animations across the Splash screen, Home header, empty day cards, and level-up celebrations.
- **Moody Launcher App Icons (`MoodyIcon`)**:
  - Android activity-aliases dynamically switching the device home screen icon based on player streak and habit states.
- **Custom Comic Card Notifications (`PqNotify`)**:
  - RemoteViews comic cards with kickers, headers, skin-adaptive backings (`Gold`, `Paper`, `Sky`), and high-DPI mood bitmaps.
  - 10 categorized notification channels, lock-screen privacy guards, and inline action receivers.

### 🔒 Security Hardening (Phase S: S001–S070)
- **Supabase Backend Protection**:
  - Enforced strict default-deny Row Level Security (RLS) across all tables (`cloud_profiles`, `darood_counts`, `study_items`, `chess_games`, `reports`).
  - Storage buckets default to private with short-lived, signed download tokens.
  - Server-authoritative move validation and anti-tamper logic for chess games.
- **Client Security & Manifest Hygiene**:
  - Restricted component exposure: `BootReceiver` marked `exported=false`.
  - Android backup rules configured (`allowBackup=false` / encrypted exclude descriptors).
  - Explicit Network Security Configuration blocking cleartext traffic and disabling debug trust anchors.
  - Zero hardcoded API keys or service tokens in repo, history, or client binary.

### 🏗️ Senior Code Cleanup & Architecture (Phase Q: Q001–Q060)
- **5-Tier Clean Architecture**:
  - Full separation across UI, Presentation, Domain, Data, and Data Sources layers adhering to Unidirectional Data Flow (UDF).
  - Use cases encapsulate core business rules (XP calculation, streak determination, leveling formulas, and difficulty policies).
  - Dedicated model mappers translating Room entities (`*Entity`) and network DTOs (`*Dto`) into clean domain models (`*Model`).
- **State & Lifecycle Resilience**:
  - Integrated `SavedStateHandle` across ViewModels (`TaskFormViewModel`, `QuranSearchViewModel`, `LeaderboardViewModel`) enabling zero data loss across rotation and Android OS process death.
  - Room DAOs verified with 100% indexed queries, transactions, and background dispatcher offloading.
- **Centralized Design System**:
  - Consolidated dimensions (`PqDimens`), colors (`PqColors`), typography (`PqType`), and skin definitions (`PqSkin`), deduplicating dp/sp values across components.
  - Hardcoded strings extracted to localized string resources.
- **Testing Pyramid & Quality Gates**:
  - Domain rules and use cases covered with fast pure Kotlin JVM tests (Q053).
  - In-memory repository fakes verifying offline persistence and error contracts (Q054).
  - ViewModel reactive state transition tests powered by CashApp Turbine (Q055).
  - Compose UI smoke tests under Robolectric validating all primary tabs and screens (Q056).
  - ART Baseline Profile rules expanded for cold start optimization and startup benchmark harness added (Q057).
  - Architecture Decision Records documented in `docs/adr/` (Q058).

---

## [1.7.2] - 2026-10-22 — The app moves

### ⬆️ Upgrading
Install 1.7.2 over 1.7.1 (or any version from 1.2.0): everything is kept. Nothing in the app's data changes.

### ✨ New
- **The app moves.** These are the micro-animations from the owner's animations handoff, in the Comic skin's springy style and the Pixel skin's hard steps:
  - Buttons spring back after a press.
  - A quest's check fills and draws its tick, its title is struck through, and "+N XP" floats up.
  - Home's progress bar and ring sweep to their value, and today's XP rolls up with a "+N XP" chip.
  - The streak flame flickers, and the chip pops when the streak grows.
  - A day that turns perfect gets a "PERFECT!" stamp, a thud and confetti.
  - Hero's level bar fills, and flashes gold on a level up. Hero's numbers roll.
  - The bottom bar's gold pill slides between tabs, and each icon hops.
  - The Deen tab's prayer pips pop in. All five prayed bursts.
  - A short toast confirms a quest done or skipped.
  - Slow lists shimmer while they load.
- **Chess moves too:**
  - Pieces glide, and the selected piece lifts with its legal moves popping in.
  - Captures burst and fly to the captured tray.
  - Check pulses the king's square and shakes the king. Checkmate topples the king and stamps CHECKMATE, and the result follows a moment later.
  - A clock under 10 seconds flashes and shakes.
  - The end card's Int change pops in, and the head-to-head cell rolls up.
  - A challenge opens with the two players clashing and a draining expiry bar. A promoted pawn morphs.
  - Animations never cost clock time.
- **Reduce motion** (Settings, or Android's own Remove animations) turns all of it off: everything shows its end state.

---

## [1.7.1] - 2026-10-22 — Chess challenges as notifications

### ⬆️ Upgrading
Install 1.7.1 over 1.7.0 (or any version from 1.2.0): everything is kept. Nothing in the app's data changes.

### 🔔 New
- **A chess challenge now arrives as a notification,** even with the app closed: "<name> challenges you". Tap it to open the challenge. The lock screen hides who sent it. 1.7.0 had everything for this but its Firebase settings, so challenges showed only in the chat and the Chess tab.
- Notifications for chess have their own channel, so you can turn them off in Android's settings without losing the others.

### 🔧 For developers
- Push uses the Firebase project `pixelqueat`. CI and release builds take the four `FCM_*` settings from repository secrets, and the server's key is the `FCM_SERVICE_ACCOUNT` Supabase secret ([docs/PUSH_SETUP.md](docs/PUSH_SETUP.md)).
- No database or server change: Room 23, migrations through `20261019000108`.

---

## [1.7.0] - 2026-10-22 — Study chat, the Darood weekly ranking, and chess

### ⬆️ Upgrading
Install 1.7.0 over 1.6.0 (or any version from 1.2.0): everything is kept. The app's database moves from version 18 to 23 on first open, adding the new features' tables; nothing you have changes.

### 📚 Study parties
- **Reminders:** for the whole party or just you, once, daily or weekly, with alerts before they're due. Members see the party's; only you see yours.
- **Chat:** each study party has a chat, live for everyone in it, with replies, delete and copy. Long-press a message to report it or block its sender.
- **Files:** share PDFs, images and documents up to 20 MB. Each file is checked on the server before anyone can open it, opens through a link that lasts a minute, and is pinned to the party's Files tab or to an item.

### 📿 Darood Party
- **A weekly ranking:** each week, Monday to Sunday in the party's time zone, ranks the members by Darood. On Sunday night everyone gets their place in a notification, which can be muted for each party, and the party keeps its past weeks.
- **Goals that go on:** when the party reaches its goal, everyone sees it, and the leader can set the next one. The party keeps its goal history.

### ♟️ Chess (new, in study parties)
- **Challenge a party mate,** or open a challenge to the room: 1, 3, 5 or 10 minutes, rated or friendly, your colour or random. A challenge arrives as a notification and as a card in the chat, and expires after 10 minutes.
- **The server referees:** every move is checked, and the clocks are the server's, so a changed app can't cheat and a dropped connection doesn't stop the clock.
- **Int points:** a rating for each time control, with tiers from Pawn to King, a ladder for each party, and your record against each mate. Ten rated games a day per pair; very short games count only when they end in checkmate.
- **Watch live games** from the party's Chess tab, with how many are watching.
- **Made for everyone:** the board in both skins and Simple Mode, TalkBack reads the board and moves, tap-to-move (or drag), and settings for move sounds and haptics, auto-queen, confirming each move, and animation.
- Like the rest of a study party, chess never touches XP, levels, streaks or the leaderboard.

### 🔒 Privacy and safety
- **Export my cloud data** (Settings > Cloud & leaderboard) saves everything the cloud keeps about you to a file: parties, Darood, study, your messages and files' names, chess games with every move, ratings and records.
- **Deleting your cloud account** now removes your messages and files (from storage too). Your games stay for your opponents with your side left empty.
- **Report a chess challenge,** or block its sender, from the challenge screen. Blocked members can't challenge each other.
- Limits on the server for messages, uploads, challenges, moves and watching. Reviewed in [docs/ABUSE_REVIEW.md](docs/ABUSE_REVIEW.md) and [docs/PRIVACY_REVIEW.md](docs/PRIVACY_REVIEW.md).

### 🔧 For developers
- **Room 23** (18 to 23: study reminders, chat, files, Darood week results, chess cache, the chat's `refId`). Supabase migrations through `20261019000108`, all pushed to the hosted project.
- **Edge Functions:** `request-upload`, `finalize-upload`, `get-download-url`, `cleanup-uploads`, `chess-create-challenge`, `chess-respond-challenge`, `chess-move`, `chess-resign`, `chess-draw`, `chess-claim-timeout`, `chess-sweep`, `chess-live-games` and `send-push`. The chess functions use chess.js 1.4.0 through each function's `deno.json`.
- **Push** uses Firebase Cloud Messaging, set up from `FCM_*` build settings (no `google-services.json`); the server's key is the `FCM_SERVICE_ACCOUNT` function secret. With them blank, push is off and challenges still show in the chat and the Chess tab.
- **Logs** carry no personal data (`_shared/log.ts`), and an hourly workflow alerts on error spikes once its secrets are added ([docs/MONITORING.md](docs/MONITORING.md)). Detekt runs in CI with a baseline; lint is clean.
- Release APK: 5.1 MB.

---

## [1.6.0] - 2026-10-21 — A new look: one app, two skins

### ⬆️ Upgrading
Install 1.6.0 over 1.5.0 (or 1.4.1, 1.4.0, 1.3.0): everything is kept. 1.6.0 doesn't change the app's data. If you'd chosen Clean Light or Follow System, PixelQuest opens in Comic; Retro Pixel opens in Pixel.

### 🎨 A new design
- **A bottom bar** in place of the side menu: Home, Quests, the trophy (the leaderboard), Deen and Hero. Settings and Cloud & leaderboard open from Hero's gear.
- **Two skins, Comic and Pixel,** in Settings > Look. Every screen is drawn in either one; Comic is the default. The CRT filter belongs to Pixel and stays off in Simple Mode. Clean Light and Follow System are retired.
- **Every screen rebuilt:**
  - Home shows your quest progress, the next prayer, what's due in your study parties, and what's up next.
  - Quests has Today, All quests and History.
  - Deen has the next prayer, Darood Party and the Quran.
  - Hero has your Profile and Stats.
  - The Quran, parties, the AI Coach, Settings, onboarding and every dialog are rebuilt too.
- **Clearer to use:** one red button per screen, icons in place of emoji, and buttons that look disabled when they are.
- **A new app icon,** with a themed layer for Android 13 and later. Notifications have the new icon and your skin's colour, and a streak at risk gets its own card.
- **Notifications are asked for when you need them:** the first time you turn a reminder on, not at launch. The banner pointing to Android's settings shows only once you've said no.

### ⚡ Smoother and quicker
- **No refresh circle when you open a party.** Opening a study party, Parties or a Darood party used to show the pull-to-refresh circle for a second or more while it checked the server. It now checks quietly, over what's already on screen; the circle shows only when you pull. A study party's update also asks the server for everything at once, so it finishes sooner.
- **Tab switches don't reload.** Each tab keeps its data for a minute after you leave it, the heavy sums behind Hero's stats and quest history run in the background, and Hero's heatmap and the Quests list draw with far less work.
- **A quicker start:** the opening screen takes 0.6 seconds instead of 1.5, and less work happens before the first frame.
- **Faster everywhere:** the app's code is now compiled ahead of time when it's installed (a baseline profile), and the release build is fully optimised. The app is smaller too: 4.6 MB, down from 7.4.

### 🛠️ Improved
- **TalkBack** hears each screen's title as a heading.

### 🔧 For developers
- `ThemeMode` is Comic and Pixel; a stored "light" or "system" reads as Comic. Screens use one component set on the `PqTheme` tokens ([docs/ui-v2.md](docs/ui-v2.md)); the old components and token layer are gone.
- No database or Supabase change: Room 18, migrations through 20261019000044.
- R8 no longer keeps all of Compose and Hilt whole (their libraries ship their own rules); `src/main/baseline-prof.txt` lists the app's code, installed by profileinstaller. `TabSharing` keeps a tab's flows for a minute.

---

## [1.5.0] - 2026-10-21 — The Islamic section, the Quran, and study parties

### ⬆️ Upgrading
Install 1.5.0 over 1.4.1 (or 1.4.0, or 1.3.0): everything is kept. The app's data moves from version 12 to 18, to hold the Quran, your bookmarks and study parties, and a copy of the old data is kept on the phone first. After the update, the Quran loads from the app's own files in the background, once.

### 🌙 The Islamic section (new)
- **One place:** the side menu's Islamic entry opens a hub of the Namaz Tracker, Darood Party and the Quran, in place of their own entries. Darood's invitation dot is on it.
- **Your choice:** Settings has an Islamic card: the Quran's reading defaults, the reminders, and a switch to hide the whole section (its menu entry and the Namaz card on Home). Nothing is deleted when it's hidden.
- **Nothing of it changes your XP, level or streaks,** except prayers in the Namaz Tracker, as before.

### 📖 The Quran (new, works offline)
- **The text:** the Uthmani Arabic from the Tanzil Project, and the Saheeh International translation, bundled with the app, unchanged and checked on every build: 114 surahs, 6,236 ayahs.
- **Reading:** every surah, with its Bismillah; Arabic, translation, or both; text size, spacing, a sepia page, and keeping the screen on.
- **Finding:** search the translation or the Arabic, browse by juz, go to any surah and ayah, and carry on from where you stopped.
- **Each ayah:** tap it to bookmark it, copy its Arabic or translation, or share it with its reference.
- **Sources:** the Quran's own screen credits Tanzil and the translators, as their terms ask.

### 👥 Study parties (new, for signed-in players)
Friends keep each other going through a school or university week. Open them from the side menu's Parties.
- **A party:** 2 to 30 members, invited by username, as Darood Parties are.
- **What's due:** anyone adds an assignment or a quiz, with its subject, notes and due date; the leader pins what matters most.
- **Doing it:** tick an item off (and undo it within 24 hours), or log study time with a timer or by hand. It works offline, and nothing counts twice.
- **The week:** a scoreboard from Monday to Sunday, with medals; last week's winner is announced, and past weeks are kept.
- **Keeping up:** a live activity feed, reminders a day and two hours before something's due, an opt-in Sunday summary, and a card on Home.
- **Points are the party's own:** they never change your XP, level or streaks. In Simple Mode there are no points or scoreboard, only what's due.

### 🛠️ Improved
- **TalkBack** names every text field by its label, across the app; the new screens' buttons are full-size targets.
- **Signing out** sends what's still waiting first, then clears every party from the phone; the Quran and your bookmarks stay.
- **A smaller app:** 7.4 MB, as only the English of the app's libraries is packed.

### 🔧 For developers
- Room 18; Supabase migrations through 20261019000044 (the study tables, their rules, and Realtime).
- CI checks the bundled Quran against Tanzil's files, the APK's size budget, and the parties' privacy with an outsider's reads of every party table.

---

## [1.4.1] - 2026-10-20 — Android 7

### ⬆️ Upgrading
Install 1.4.1 over 1.4.0 (or 1.3.0): everything is kept. Nothing changes on Android 8 and later.

### 🐛 Fixed
- **PixelQuest now opens on Android 7.0 and 7.1.**
  - Every earlier version closed as it started there, before its first screen.
  - The cause: the app works with dates through Java's `java.time`, which Android only has from version 8. 1.4.1 brings its own copy (core library desugaring).
- **Checked on an Android 7.0 emulator** with the release build:
  - 1.4.0 closed at once;
  - 1.4.1, installed over it, went through onboarding to the dashboard;
  - the Namaz Tracker showed the times, the Hijri date and the countdown, and marked a prayer;
  - Darood Party's sign-in screen, Quest History and Stats opened.

### 🔧 For developers
- Every push now runs Android lint. Its NewApi check fails the build on a call Android 7 doesn't have.

---

## [1.4.0] - 2026-10-20 — Darood Party

### ⬆️ Upgrading from 1.3.0
Install 1.4.0 over 1.3.0: everything is kept. The app's data moves from version 9 to 12, to hold Darood Party's copy of your parties, and a copy of the old data is kept on the phone first.

### 📿 Darood Party (new, for signed-in players)
Small groups count Darood together, in a chat where the only thing anyone can send is a number. Open it from the side menu.
- **A party:**
  - Choose a username, then create a party of 2 to 30, with a milestone to aim for if you like.
  - Invite friends by their exact username. Usernames aren't listed anywhere, so ask them for theirs.
  - They accept or decline from their own list.
- **The chat:**
  - Type how many you recited (digits only, at most 100,000 at once) and send.
  - Your number shows at once with a clock, then a tick when it's sent. Offline, it waits and goes later.
  - 10,000 or more asks you to check it first.
  - Quick chips fill in +11, +33, +100 or +313.
  - A day's entries are at most 500,000. A refused entry says why, with Retry.
  - Everyone's numbers arrive live, with days headed Today and Yesterday, and joins, leavings and milestones in between.
- **At the top:** the milestone's progress, and "You today | Party today".
- **My stats (📊):**
  - your total, your share and your place;
  - how steady you are: active days, days in a row, your best run and your average, with a 30-day calendar;
  - everyone's part, kindly ranked: medals for the top three, and no "last place".
- **The leader:**
  - invites (20 an hour), removes, renames, hands over the leadership, and sets or raises the milestone;
  - can delete the party, after typing its name.
  - Reaching the milestone finishes the party, with a celebration card and the time it was reached, for good.
- **The race board (🏁, opt-in per party):** parties ranked by when they finished, then by progress. It shows a party's name and progress only, never anyone's name or numbers.
- **A weekly note (opt-in):** on Friday evenings, how many of the last 7 days you added Darood.
- **Looks:**
  - Pixel, Comic, Light and dark mode each have their own look, and Simple Mode is plain in any theme.
  - TalkBack reads each number as one sentence, and the layout holds at large text sizes.

### 🎮 Kept apart from the game
Darood never adds XP, levels, streaks or anything on the leaderboard, and a test checks that it can't.

### 🔒 Privacy
Your username is seen only by your party's members (and, while you're invited, by the person who invited you). Entries are seen only by the party's members. The race board shows only what its leader chose to share. See [PRIVACY.md](PRIVACY.md).

---

## [1.3.0] - 2026-10-18 — The Namaz Tracker, a Side Menu and a Compact AI Coach

### ⬆️ Upgrading from 1.2.0
Install 1.3.0 over 1.2.0: it's signed with the same key, and your hero, quests, streaks, settings and backups are all kept. The app's data moves from version 7 to 9, and a copy of the old one is kept on the phone first.

### 🕌 Namaz Tracker (new, optional, off until you turn it on)
- **Turning it on:** open the side menu, choose Namaz Tracker and turn it on. A short setup asks for your place: your approximate location, or a city you pick. Without either it uses Lahore.
- **Prayer times are worked out on the phone**, and work offline.
  - Methods: Karachi (the default), Muslim World League, ISNA, Umm al-Qura and Egyptian.
  - Hanafi or Shafi Asr, a high-latitude rule, and minutes to add or take off each prayer.
  - The Hijri date shows under the title.
- **Five prayer buttons, Fajr to Isha.**
  - Each opens at its time. Tap it to mark it prayed, and tap again to undo, the same day.
  - A prayer not marked before the next one starts shows as missed.
  - The screen also has a countdown to the next prayer, the day's progress, the last seven days, and a streak of days with all five prayed.
- **Reminders** at each prayer's start, or 5, 10 or 15 minutes before.
  - Each has Prayed, Snooze 10 min and Dismiss buttons.
  - Options: a nudge 20 minutes before the next prayer, a switch per prayer, and the phone's sound, a short chime or silent.
  - Do Not Disturb is respected, with an opt-in to let Fajr through.
  - Reminders come back after a restart, an update or a time-zone change.
  - The screen warns when exact alarms, notifications or battery saving could hold them back.
- **XP for prayers**, added to the game's own XP:
  - 10 per prayer in its time, or 5 if late, and +30 for all five in a day. At most 80 a day.
  - +100 for every 7 days in a row with all five.
  - Undoing a prayer takes back exactly what it earned. Levels still come from perfect quest days.
  - A Namaz XP switch turns the XP off. Simple Mode hides the numbers but still records your prayers.
- **Celebrations:** the XP pops up as you mark a prayer, and all five prayed shows a card with a burst in your theme's style.
- **Where your prayer XP shows:** today's on the Namaz screen and in the side menu's header. Stats has a Namaz card, and Home has a compact Namaz card while the tracker is on.
- **Fair play:**
  - A prayer can't be marked before its time, or more than a day back.
  - It can't be marked from a phone clock set ahead.
  - Marking from the reminder and the screen at once counts once.
- **Privacy:** your place and your prayer log stay on the phone and in your backups. The leaderboard gets your XP total only, never which prayers you marked or when.
- **Backups** include the Namaz log and settings. Older backups restore as before.

### 🧭 Side menu
- **Opening it:** the three-line icon at the top left of Home, Tasks, Stats, Profile and the Leaderboard opens it. A swipe from the left edge works too.
- **What it holds:** your avatar, name, level and progress, then the Namaz Tracker, History, Leaderboard, Account and Settings.
- **Looks and access:** it's drawn for Comic Pop, Retro Pixel, Light and Simple Mode, and TalkBack reads it as a menu.
- The round Leaderboard button stays in the middle of the bottom bar.

### 🤖 A compact AI Coach
- The coach card takes less room:
  - a one-line header with a small avatar and the refresh button;
  - the latest insight in three lines, with a toggle to expand it;
  - earlier insights as one-line rows.
- Its smaller controls keep full-size touch targets.

---

## [1.2.0] - 2026-10-14 — Comic Pop, Sign-in and the Live Leaderboard, AI Coach and Backups (Days 16–35)

### ⚠️ Upgrading from 1.1.0 or earlier
From 1.2.0 on, PixelQuest is signed with a permanent release key. Earlier releases were each signed with a temporary key, so Android won't install 1.2.0 over them. Before upgrading:
1. In the old app, Settings → **EXPORT QUEST DATA**, and save the file.
2. Uninstall the old app and install 1.2.0.
3. On the welcome screen, tap **I HAVE A BACKUP** and pick the file.

A 1.1.0 backup brings back your hero, level, difficulty, streak and every quest. It has no day-by-day results, level history or settings, because 1.1.0 didn't save them. Backups made from now on include all of these. See docs/UPDATES_AND_BACKUPS.md.

### 🎨 Themes
- **Comic Pop is the default theme** (1.2.0): new players start in it. A theme you already chose is kept, and the Home screen's COMIC MODE button switches to Retro Pixel.
- **Light mode** (Days 16–17): a warm daylight palette, with every screen, chart and dialog adapted and checked for contrast.
- **Comic mode** (Days 20–23, 27–28): a pop-art theme with ink borders, halftone panels and comic versions of every component, unlocked for everyone. Headings use a darker coral ink so they read on paper.

### 🧘 Simple Mode (Days 18–19)
- An un-gamified view: quests become tasks, and XP, levels, flourishes and celebrations are hidden. Streaks, perfect days and levels keep counting in the background with your current difficulty.

### 🤖 AI Coach (Days 24–31)
- Optional, off by default: Google Gemini writes short habit debriefs and daily reminder lines from anonymous stats. Task names never leave the device.
- Cached for 12 hours, at most one live call per 6 hours, and capped at 4 calls a day and 60 a month.
- Released builds reach Gemini through PixelQuest's own server, which holds the key. The key is never inside the APK.
- **For signed-in players only (Day 31).** Sign in with Google or an emailed code to use it. Signed out, the coach card and Settings show a SIGN IN button, and no AI requests are sent. The server checks your sign-in and counts the daily limit per account.

### 🔑 Sign in with email (Day 31)
- Account has **SIGN IN WITH EMAIL** next to Google. Type your address, get a 6-digit code by email, and enter it. There's no password, and the account is made the first time.
- Sign in with Google works against a real Supabase project. The app was dropping a security value (the nonce) that Supabase checks.
- Sharing a phone (Day 32): when a different account signs in, the leaderboard choice, public name and AI Coach consent start fresh. The next person was put on the leaderboard under the previous person's name.
- Signing back in after someone else, after reinstalling, or on a second phone brings back your own leaderboard choice and public name. Such an account used to be shown as off the leaderboard, and the next sync took it off.
- Account no longer asks a player who is already on the leaderboard to join it.
- Signing out with no connection now really signs you out on the phone. The next launch used to sign you back in.
- A failed Google sign-in no longer hides email sign-in.
- Email codes: another address can get a code straight away, and CANCEL stops a code check that's still running.
- After signing out, no AI insight, AI reminder lines or "fresh insight" notice turns up from a request that was still running.
- A signed-in player whose sign-in couldn't be refreshed (offline) sees a connection message with RETRY, not SIGN IN.
- AI calls that failed on PixelQuest's server before reaching Gemini no longer use up the day's limit.
- Today's AI insight has an OPEN AI COACH button to the AI Coach's own screen, which had no way in.

### 🔔 Reminders (Days 26–28, 31, 33)
- Reminder lead times and styles, a "Did you do it?" prompt, missed-quest notices, and snooze.
- One result per quest per day: double taps and late answers no longer count twice.
- Two quests due in the same minute both get their prompt.
- An evening nudge (around 19:00) when your streak is at risk, saying how many more quests keep it (Day 31).
- Reminder fixes (Day 31):
  - Snoozing one quest no longer cancels another's reminder.
  - Answering a late-evening reminder after midnight counts for the right day.
  - A reminder can't ring twice for the same day after the app restarts.
  - Reminders follow you across time zones.
  - A deleted quest's leftover reminder can't award XP.
  - A "Yes" tapped after midnight for a late-evening quest now counts toward your streak. The nightly streak check runs at 02:05, and stays at 02:05 when the clocks change.
  - Paused quests no longer get reminder alarms.
  - Missed-quest notices follow the quest's own settings. A quest with reminders off gets none, and a SILENT quest's notice doesn't vibrate.
- The quest form says when reminders may ring up to 10 minutes late, and offers to allow exact reminder times. Android 14 doesn't allow them by default, and the note used to be only in Settings (Day 33).

### 🗓️ Quests and progress
- Weekly quests on chosen days (Day 27).
- Days per level are 3 / 7 / 14 / 30 for Easy, Medium, Hard and Hardest (Day 29).
- A new quest created after its time of day starts tomorrow (Day 30).
- The task form keeps your edits when the screen rotates (Day 30).
- Backups now include the streak, completion history and level timeline (Days 28, 30).
- Backups are checked when imported (Day 31). A file that isn't a backup says so. Out-of-range values in an edited file are corrected instead of crashing the app. Settings now says whether a backup was saved or restored.
- Leaving the leaderboard while offline is queued until you're back online (Day 30).
- If the nightly streak check is interrupted, the perfect days it had counted aren't counted again. A quest completed while it runs keeps its XP (Day 31).
- RESET ALL PROGRESS keeps your cloud sign-in and leaderboard choice, as its dialog says (Day 31).

### 💾 Updates and backups (Day 34)
- **Updating keeps your progress**, from every earlier version.
  - Players updating from 1.1.0 were sent through the welcome screens again, and finishing them reset their level and XP. Fixed.
  - An update can no longer silently wipe everything if it can't upgrade the app's data.
  - Before an update changes how your game is stored, a copy of the old one is kept on the phone.
- **Backups:**
  - **Include your settings** (sound, haptics, reduce motion, notifications, theme, Simple Mode).
  - **Say which version made them.** A backup from a newer version restores what this one knows.
  - **Are checked after saving**, and get dated file names. Settings shows when you last saved one.
- **Restoring:**
  - The dialog says what the backup holds before anything is replaced.
  - **UNDO LAST RESTORE** puts back what you had before, when there was progress to go back to.
  - Results ticked on a fresh install's starter quests no longer land on the backup's quests.
  - **After a reinstall or on a new phone**, restore straight from the welcome screen: **I HAVE A BACKUP**. The 1.1.0 upgrade no longer needs a throwaway hero first.
- **Automatic backups on the phone:** a daily copy of your progress, the last 3 days, once you've played. It's also updated an hour after you open the app, so it has that day's play. Settings can restore any of them.
  - If the app's data is ever damaged and your game comes back empty, PixelQuest asks **RESTORE YOUR GAME?** with the newest one.
  - **NOT NOW**, a new hero, or RESET ALL PROGRESS on purpose isn't asked again.
- **Android's own backup:** your progress and settings come back after a reinstall or on a new phone. Your sign-in doesn't, so you sign in again.

### 🏆 Leaderboard (Days 31–33)
- LOAD MORE no longer crashes when ranks change between pages.
- Switching tabs while a list loads no longer mixes up the two tabs' lists, LOAD MORE or "around you" ranks.
- Ranks beyond 1,000th are counted correctly.
- Quests completed from a reminder or the full-screen prompt now reach the leaderboard too.
- Privacy: if you're signed in but haven't joined the leaderboard, your level, XP and streaks are no longer uploaded.
- SYNC NOW says when nothing was sent because the leaderboard already has more progress from another device.
- Ordinary leaderboard names like Titan_Slayer, Stitch, Altitude, Scrappy and Dickens are accepted (Day 32). The name filter used to find a rude word inside them; such words now count only as whole words.
- **Fair numbers (Day 33).** The leaderboard checks streaks and levels against the game's own rules: a streak grows by at most one day a day, a level takes at least three perfect days, and both need the XP to match. Impossible numbers are lowered instead of shown. If you catch up after the app missed a few nights, the board shows the rest over the next few days, and SYNC NOW says so.
- **Names are unique (Day 33)**, whatever the capitals, so nobody can appear under your name. Choosing a taken name says "That name is taken". Your name stays yours while you're off the board.
- **One account on two phones (Day 33).** Joining or leaving the leaderboard on one phone now applies to the other, which follows at its next sync. It used to undo the change, whichever phone synced last. A choice made offline is sent once you're back online, and joining offline is queued too.
- **Offline (Day 33).** Account no longer says you're signed out when the server can't be reached. It says the connection is the problem, and you stay signed in.
- If the server refuses a sync outright, the app stops retrying it every few hours (Day 33).
- **Broken streaks show on the board (Day 33).** A streak that dropped to 0, and the fresh numbers after RESET ALL PROGRESS, never reached the leaderboard. It went on showing the old streak.
- **RESET ALL PROGRESS starts your leaderboard numbers over too (Day 33)**, under the same name. Before, the board kept your old streak and level until you passed them again.
- **Phone clock behind (Day 33).** A phone whose clock ran a little behind could lose a sync made soon after another. Those syncs now go through.
- **Waiting choices (Day 33).** Joining or leaving the leaderboard while offline shows "Waiting to reach the server" until it's sent. It's sent as soon as you're back online, and a waiting leave is sent before you sign out.
- **Reports (Day 33).** You can report a player once a day, and reporting again says it's already in. The message no longer blames your connection when the server answered.
- Account's note under your public name says names are unique (Day 33).

### ⚙️ Settings (Day 31)
- SFX: OFF silences the whole app, and HAPTICS: OFF stops every vibration, including the full-screen prompt.
- New REDUCE MOTION switch: looping animations stay still and theme colours change instantly.
- Android's own "Remove animations" setting reduces motion in PixelQuest too (Day 32).
- The sync button is just SYNC NOW; it used to say "(DEBUG)" (Day 32).

### 🧭 Navigation (Day 32)
- Tapping the bottom-bar tab you're already in goes back to that tab's first screen. HOME used to do nothing on a screen opened from Today, such as Account.
- Account fits email sign-in. It no longer invites you to sign in once you are, and the privacy notes say "your email" rather than "your Google email".
- The Sign in with Google button only appears in builds that have a Google client set up.

### ♿ Accessibility (Days 30–31)
- TalkBack reads full day names, which chips, difficulty cards and history filters are selected, heatmap days with their results, and named close, back, delete and refresh buttons.

### ☁️ Builds without cloud or AI (Day 31)
- A build without a Supabase project says sign-in, sync and the leaderboard aren't set up, instead of offering a sign-in that can't work. A build that can't reach Gemini doesn't offer the AI Coach.
- A build without the Google client id still offers email sign-in; only the Google button is hidden.

### Release notes
- Releases built without the signing or Supabase secrets now say so at the top of their notes.
- Tagged releases are checked against the release key's certificate before they're published.

---

## [1.1.0] - 2026-09-12 — The Global Leaderboard Extension

### 🏆 Cloud Leaderboard & Community Competition (Days 13–15)
- **Supabase Cloud Infrastructure & Authentication (Day 13)**:
  - Integrated Supabase backend with Google Sign-In via Android Credential Manager and OAuth Web Client ID.
  - Strict privacy-first design: leaderboard participation defaults to OFF (`false`). Public display names are completely separate pseudonyms from local hero names and Google emails.
  - PostgreSQL Row-Level Security (RLS) policies enforcing write isolation (`auth.uid() = id`), opted-in public visibility (`leaderboard_opt_in = true`), and spectator access.
- **Live Arcade Leaderboard & Background Sync Worker (Day 14)**:
  - New `LeaderboardScreen` featuring dual arcade tabs: **Top Streaks** and **Top Levels**.
  - Gold, Silver, and Bronze podium styling for top 3 ranks, dynamic active player row highlight pulsing, pinned bottom user ranking card, and retro pull-to-refresh.
  - `ProfileSyncWorker` background sync engine executing via WorkManager on habit completion and level-ups, with `ConnectivitySyncObserver` for instant reconnect sync dispatch.
  - Non-intrusive **Spectator Mode** enabling signed-in players to browse rankings without publicly exposing their own stats.
- **Privacy, Moderation, Conflict Resolution & Release Hardening (Day 15)**:
  - **Defense-in-Depth Moderation**: Client-side profanity/offensive-word filter with leetspeak normalization (`DisplayNameModerator`), PostgreSQL trigger function constraint on server, and community report flagging action on leaderboard rows.
  - **Account & Cloud Data Deletion**: Dedicated "Delete My Cloud Data" flow with double-confirmation dialog sequence, removing `public.profiles` row, invoking `delete_user_account()` RPC to erase Supabase auth account, and clearing local cloud references while preserving local quest data.
  - **Leaderboard Opt-Out UX**: Clearly discoverable "Leave Leaderboard" action with lightweight single confirmation dialog and immediate post-opt-out confirmation notice.
  - **Multi-Device Sync Conflict Resolution**: Last-Write-Wins (LWW) by `updated_at` timestamp comparison paired with an anti-regression guard preventing sync from ever pushing lower streak, level, or XP values than what the server holds.
  - **Outage Graceful Degradation**: Zero-crash isolation in `ProfileSyncWorker` with WorkManager exponential backoff, 10-second timeout on leaderboard fetch calls, and subtle non-blocking "cloud sync unavailable" indicator in `AccountScreen`.
  - **Privacy Documentation**: Comprehensive `PRIVACY.md` detailing local-first data storage, cloud sync scope, and deletion rights, linked directly in `AccountScreen` prior to sign-in.

---

## [1.0.0-verified] - 2026-09-02
- Post-release audit and verification pass complete (39 steps, 613 total commits).
- Real-device sideload flow testing verified 100% pass across all 9 core user flows.

---

## [1.0.0] - Initial Public Release

### ⚔️ Core Experience & Architecture (Days 1–3)
- **Day 1**: Project foundation, Jetpack Compose 5-destination navigation scaffold (Home, Tasks, Stats, Profile, Avatar), custom *Press Start 2P* typography, authentic retro UI components (`PixelButton`, `PixelCard`, `PixelProgressBar`, `PixelDialog`).
- **Day 2**: Local data layer powered by Room Database (`TaskEntity`, `UserProfileEntity`, `DifficultySettingsEntity`, `StreakEntity`, `TaskCompletionLogEntity`). Clean architecture repository pattern with reactive `Flow` streams.
- **Day 3**: Interactive Task Management screens (`TasksScreen`, `CreateTaskScreen`, `EditTaskScreen`) with category selection (Fitness, Health, Knowledge, Creative, Mindset, General), priority levels, and recurrence rules.

### ⏰ Notifications, Streaks & Gamification (Days 4–6)
- **Day 4**: Background scheduling engine with `AlarmManager` exact alarms and `WorkManager` periodic background workers for missed task evaluations. "Did You Do It?" prompt dialogs.
- **Day 5**: Dynamic Streak & Multiplier System (`PointsCalculator`, `StreakCalculator`) scaling XP rewards by streak length and customizable difficulty thresholds (Easy 50%, Medium 70%, Hard 90%, Hardest 100%).
- **Day 6**: Leveling & Profile Progression system. Level calculation formula ($Level = 1 + \lfloor \frac{XP}{100} \rfloor$), Level-Up celebration modals, tier frames (Bronze, Silver, Gold), and historical level log history (`PixelLevelHistoryList`).

### 🎨 Retro Audio, Scanlines & Dashboard Polish (Days 7–9)
- **Day 7**: Custom 8-bit Sound Engine powered by `SoundPool` with persisted audio toggle. Retro CRT scanline & vignette shader filter toggleable across all app screens. 6 pixel character class sprites (Hero, Mage, Rogue, Warrior, Paladin, Ranger).
- **Day 8**: Today's Dashboard Screen with live countdown timers, quick-complete drag & tap interactions, daily progress ring, perfect day banners, and dynamic motivation flavor text.
- **Day 9**: Analytics & Stats Dashboard (`StatsScreen`). Interactive 90-day activity heatmap grid (`PixelCalendarHeatmap`), per-task completion rates, and completion rate trend charts.

### 💾 Backup Engine & Polish Pass (Days 10–11)
- **Day 10**: Data Export & Import engine (`DataExportImport`) supporting local JSON backup files. Full app progress reset sequence with confirmation steps.
- **Day 11**: Haptic feedback engine (`PixelHaptics`), snappy retro screen transitions (`PixelTransitions`), keyboard avoidance (`imePadding`), and complete accessibility compliance pass (screen reader semantics, min 48dp touch targets).

### 🚀 Production Release (Day 12)
- **Day 12**: Automated CI/CD release pipeline, R8 minification & resource shrinking, ProGuard keep rules, defensive database & JSON crash guards, end-to-end instrumented UI test suite, and initial v1.0.0 release publication.
