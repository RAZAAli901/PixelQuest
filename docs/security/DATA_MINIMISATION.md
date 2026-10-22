# Data minimisation review (S058)

Reviewed 2026-10-22. This covers every piece of data PixelQuest collects, where it lives, why it's needed and how long it's kept. [PRIVACY_REVIEW.md](../PRIVACY_REVIEW.md) (J173) goes table by table through the party, study, chat, Darood weekly and chess tables. This review adds everything else and checks the whole for anything kept that isn't needed. The privacy policy draft built from it is [PRIVACY_POLICY_DRAFT.md](../PRIVACY_POLICY_DRAFT.md).

## The rule

Collect only what a feature needs to work, keep it only as long as the feature needs it, and let the player take it (export) and remove it (delete). Anything the app works out on the phone stays on the phone.

## On the phone only (never sent)

| Data | Why | Kept |
|---|---|---|
| Quests: name, notes, time, repeat, reminder style; completions and misses | The habit tracker | Until the player deletes them or resets progress |
| Hero name, XP, level, streaks, level history, difficulty | The game | Same |
| Namaz settings, prayer log, and the last place used for prayer times (latitude and longitude, or the city picked) | Prayer times, worked out on the phone | Same. The location is asked for only when the player turns it on or taps Use my location, and it is coarse (city-level) |
| Quran bookmarks and reader settings | The reader | Same |
| The AI Coach's last insight | Showing it offline | Replaced by the next one |
| Caches of the player's parties, chat, study and chess | Working offline | Cleared on sign-out (DaroodAccountCleanup) |
| The sign-in session | Staying signed in | Sealed with an Android Keystore key (S034); removed on sign-out |

Cloud backup takes only the settings and a local backup file; device-to-device transfer also takes the database (S031, STORAGE.md). Nothing above leaves the phone unless the player makes a backup file or uses the features below.

## In the cloud (Supabase)

| Data | Where | Why | Kept |
|---|---|---|---|
| Email address | Supabase Auth (`auth.users`) | Signing in with an emailed code, and the account's identity | Until the account is deleted |
| Google account id, email and, from Google's sign-in token, the name and picture link | Supabase Auth (`auth.users.raw_user_meta_data`, `auth.identities`) | Signing in with Google. The app uses only the id and email | Same. See finding 1 |
| Display name, streaks, level, XP, opt-in flag | `profiles` | The public leaderboard, only after opting in | Same; leaving the board stops showing it |
| When the streak and level last changed | `profile_progress_marks` | Stops a leaderboard row being faked | Same |
| Username | `usernames` | Being found by party mates | Same |
| Reports about a profile | `reports` | Moderation | Until handled |
| Parties, Darood, study, chat, files, chess | See PRIVACY_REVIEW.md | Those features | See PRIVACY_REVIEW.md |
| Push token and platform | `device_tokens` | Chess push | Until the account goes. Signing out deletes the token on the phone, so the stored one stops working, and send-push drops tokens Firebase no longer accepts |
| AI Coach calls per account per day | `ai_proxy_usage` ("user:<id>", day, count) | The daily limit (S050) | See finding 2 |
| Recent actions per rate limit | `private.rate_events` (bucket, account id, time) | Rate limits (S020) | A day, pruned when the account acts again. See finding 3 |

## Sent to other services

| Service | What | Why |
|---|---|---|
| Google Gemini, through gemini-proxy | Numbers only: streaks, completion rates, counts per category, the tone picked. No quest names, notes, usernames or emails (`HabitInsightPromptBuilder` redacts them too) | The AI Coach, only when the player turns it on and is signed in |
| Firebase Cloud Messaging | The push token, and a message carrying ids and a kind only. The words are put together on the phone | Chess challenges and moves |
| Google sign-in (Credential Manager) | Handled by Google on the phone | Signing in with Google |

## Findings

1. **Google's name and picture link are stored but unused.** Supabase Auth keeps the claims of Google's sign-in token as they are. The app never reads the name or picture. They aren't exported by `export_my_data` and they go when the account is deleted. *Decision:* leave Supabase Auth's own tables alone (changing them risks breaking sign-in), and say in the privacy policy that Google provides them. Revisit if Supabase adds a way to drop claims.
2. **AI usage counters were kept forever.** Each account had one row per day it used the coach. *Decision:* they're needed only for today's limit. They go with the account now (migration 118), and pruning rows older than 7 days is listed as follow-up F1. It isn't done here, because S058 is a review.
3. **Rate-limit events of players who stop acting stay.** They're pruned only when that player acts again in that bucket. They are small (bucket, id, time) and go with the account (migration 118). Follow-up F2: a scheduled prune of events older than a day.
4. **Logs.** Release builds log no personal data (S055). The Edge Functions log events and statuses only.
5. **Nothing else found.** No advertising id, contacts, precise location, phone number, device id or analytics SDK. The app has no analytics at all.

## Follow-ups

- **F1:** prune `ai_proxy_usage` rows older than 7 days, in `claim_ai_proxy_call` or a schedule.
- **F2:** prune `private.rate_events` older than a day on a schedule.
- **F3:** fill in the Play data safety form from the policy draft's table (S068).
