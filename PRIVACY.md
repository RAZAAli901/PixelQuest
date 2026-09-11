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

If you choose to participate in the global community leaderboard, PixelQuest offers an optional cloud connection powered by Supabase and Google Sign-In.

### What Data is Collected (and What is NOT)
When you sign in and choose to join the leaderboard, the following data is transmitted and stored:

| Data Field | Purpose | Publicly Visible? |
| :--- | :--- | :--- |
| **Supabase User ID (UUID)** | Unique cryptographic identifier for database record association. | No (internal identifier) |
| **Google Email & OAuth Credentials** | Used strictly by Supabase Auth to authenticate your session and issue secure JWTs. | **Never.** Email is never stored in public profile tables or shown on the leaderboard. |
| **Public Display Name** | Pseudonym chosen by you for the leaderboard. Moderated for offensive language. | **Yes**, visible to other authenticated players on rankings. |
| **Current & Longest Streak** | Number of consecutive perfect days completed. | **Yes**, used for "Top Streaks" ranking. |
| **Player Level & Total XP** | Current RPG level and total accumulated experience points. | **Yes**, used for "Top Levels" ranking. |
| **Leaderboard Opt-In Flag** | Boolean flag indicating active participation. | Used by database security policies. |
| **Last Updated Timestamp** | UTC timestamp used for Last-Write-Wins multi-device synchronization. | Used for sync conflict resolution. |

> [!IMPORTANT]
> **Your individual task names, quest descriptions, scheduled times, and private completion logs are NEVER uploaded to the cloud.** Only your aggregate statistics (level, streak, XP) and chosen display name are synchronized.

### When Data is Collected & Synced
Data synchronization occurs strictly when:
1. You explicitly tap "Sign in with Google" in `AccountScreen`.
2. You explicitly choose to opt into the leaderboard and confirm your public display name.
3. Background synchronization (`ProfileSyncWorker`) runs after you complete a quest or level up, pushing updated aggregate stats.

---

## 3. Database Security & Row-Level Security (RLS)

Our cloud database utilizes PostgreSQL with strict **Row-Level Security (RLS)**:
- **Write Isolation**: A player can only create, update, or delete their own profile (`auth.uid() = id`).
- **Read Restrictions**: Profiles are only queryable if `leaderboard_opt_in` is explicitly `true`.
- **Spectator Mode**: Signed-in players who have not opted in can browse the leaderboard in read-only mode without appearing on the leaderboard.
- **Moderation Reports**: Display name abuse reports are stored in an insert-only table accessible strictly to moderation tooling.

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
