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
| **Last Updated Timestamp** | UTC timestamp used for Last-Write-Wins multi-device synchronization. | Used for sync conflict resolution. |

> [!IMPORTANT]
> **Your individual task names, quest descriptions, scheduled times, and private completion logs are NEVER uploaded to the cloud.** Only your aggregate statistics (level, streak, XP) and chosen display name are synchronized.

### When Data is Collected & Synced
Data synchronization occurs strictly when:
1. You explicitly tap "Sign in with Google" or "Sign in with email" in `AccountScreen`.
2. You explicitly choose to opt into the leaderboard and confirm your public display name.
3. Background synchronization (`ProfileSyncWorker`) runs after you complete a quest or level up, pushing updated aggregate stats.

**Sharing a phone:** the leaderboard choice, your public display name and your AI Coach consent belong to your account. If a different account signs in on the same phone, they're reset, so the next person chooses for themselves and is never put on the leaderboard under your name. When you sign in again (also after reinstalling the app, or on another phone), your leaderboard choice and public name come back from your account's own record on the server. The AI Coach consent isn't stored there, so it stays off until you turn it on again.

---

## 3. Database Security & Row-Level Security (RLS)

Our cloud database utilizes PostgreSQL with strict **Row-Level Security (RLS)**:
- **Write Isolation**: A player can only create, update, or delete their own profile (`auth.uid() = id`).
- **Read Restrictions**: Profiles are only queryable if `leaderboard_opt_in` is explicitly `true`.
- **Spectator Mode**: Signed-in players who have not opted in can browse the leaderboard in read-only mode without appearing on the leaderboard.
- **Moderation Reports**: Display name abuse reports are stored in an insert-only table accessible strictly to moderation tooling.

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

## 4. How to Delete Your Data (Full Control)

PixelQuest provides transparent, immediate mechanisms to modify or erase your data at any time:

### A. Leaving the Leaderboard (Opt-Out)
- Go to **Settings > Cloud & Leaderboard**.
- Tap **"LEAVE LEADERBOARD (OPT OUT)"** and confirm.
- **Result**: Your cloud row immediately sets `leaderboard_opt_in = false`. You disappear in real time from public rankings, while keeping your cloud account linked. You can rejoin at any time.

### B. Permanent Cloud Data & Account Deletion
- Go to **Settings > Cloud & Leaderboard**.
- Scroll to **"Cloud Data & Privacy"** and tap **"DELETE MY CLOUD DATA"**.
- Confirm through the double-confirmation prompt.
- **Result**:
  1. Your row in `public.profiles` is permanently deleted.
  2. The `delete_user_account()` RPC purges your Supabase authentication record from `auth.users`.
  3. Local cloud identifiers and credentials on your device are completely wiped, and you are signed out locally.
  4. Local quests, streaks, and levels remain safe on your device.

### C. Complete Local Device Reset
- Go to **Settings > Danger Zone > Reset All Progress**.
- **Result**: Wipes the local Room database, resetting all quests, streaks, and settings back to a fresh install.

---

## 5. Contact & Inquiries

If you have questions regarding this Privacy Policy or your data rights, please open an issue on GitHub:
- **Repository**: [https://github.com/RAZAAli901/PixelQuest](https://github.com/RAZAAli901/PixelQuest)
