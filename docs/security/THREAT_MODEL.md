# Threat model

PixelQuest, as of 1.7.2 (S001 of the security plan). It names what we protect, from whom, and where they could get in. Each threat points to the control that answers it; [VERIFY.md](VERIFY.md) has the proof for each control.

## What we protect

| Asset | Where it lives | Why it matters |
|---|---|---|
| Accounts and sessions | Supabase Auth; the session on the phone | A stolen session is the person |
| Email addresses | Supabase Auth only | Personal data; never shown to other players |
| Usernames, display names | `usernames`, `profiles` | Seen by party mates or (opted in) the leaderboard |
| Party content: Darood entries, study items, ticks, study time, reminders | Supabase, party tables | Private to a party's members |
| Chat messages and shared files | `chat_messages`, `chat_files`, the private `study-files` bucket | Private conversations and documents |
| Chess games, ratings, records | chess tables | Fairness (ratings) and privacy (who played whom) |
| Reports and blocks | `chat_reports`, `chess_reports`, `reports`, `member_blocks` | A reporter must stay anonymous |
| Push tokens | `device_tokens` | Could be used to spam a phone |
| The phone's own data: quests, logs, Namaz, settings | Room database, preferences, backup files | The player's private habits; Namaz holds a rough location |
| Server secrets: service role key, Gemini key, FCM service account | Supabase function secrets only | Full database access, paid API, push to any phone |
| Release signing key | The owner's keystore, GitHub secrets | Anyone holding it can ship an "update" |
| The Gemini budget | Google AI | Money |

## Who might attack

| Attacker | Can do | Wants |
|---|---|---|
| Anyone on the internet | Read the APK, take its Supabase URL and publishable key, call the REST API and the Edge Functions directly | Data, free AI, a working exploit |
| A signed-in stranger | Everything above with a real session | Other people's parties, chats and files; leaderboard cheating |
| A party member gone bad | Read the party; send messages, files, challenges | Harass, spam, upload malware, cheat at chess |
| Someone holding the phone | Lock screen, notifications, a backup, adb on an unlocked phone | Read habits, messages; sign in as the owner |
| Another app on the phone | Send intents and broadcasts; read shared storage | Trigger actions; read files |
| A network attacker | Sit between the phone and the servers (public Wi-Fi, a user-installed CA) | Read or change traffic |
| A compromised dependency or CI | Run code in the build | Steal the signing key or secrets |
| Us (an agent or the owner by mistake) | Push migrations, deploy functions | Drop or open data by accident |

## Entry points and the controls on each

| Entry point | Threats | Controls |
|---|---|---|
| PostgREST (tables and views) with the publishable key | Reading or writing others' rows; open tables | RLS on every table, default deny, policies per row ([RLS matrix](VERIFY.md#s030-rls-matrix)); a CI check for tables without RLS (S029) |
| Postgres functions (RPC) | Calling privileged functions; search_path hijack; trusting a client id | SECURITY DEFINER with a fixed `search_path`; the caller checked inside; EXECUTE granted only to who needs it (S017, S018) |
| Edge Functions | Missing auth; abusing the AI or push; tampered requests | Each checks the caller's token itself; per-user limits; the chess referee checks every move (S050-S052) |
| Storage (`study-files`) | Public files; cross-party reads; malware | Private bucket; path tied to the party; signed links for 60 s; type and size checks on the server (S016, S027) |
| Realtime | Listening to other parties' channels | Private channels authorised by membership; row changes under RLS |
| Sign-in (email code, Google) | Account takeover; code guessing; enumeration | GoTrue rate limits; Google ID token with a nonce; usernames separate from emails (S060, S061) |
| The app's components | Exported receivers or activities called by other apps | Only the launcher activity exported; receivers check their action (S039-S042, S048) |
| Notifications and the lock screen | Habit names, challengers' names on a locked phone | Private visibility; the prompt hides the task until unlocked (S041, S047) |
| Phone storage and backups | Tokens in a backup; data copied off the phone | Tokens encrypted with the Keystore and never backed up; include-only backup rules (S031-S036) |
| The network | Interception | HTTPS only; no user CAs in release; pinned Supabase host with expiry (S037, S038) |
| User text (chat, names, AI) | XSS-like rendering, bidi spoofing, prompt injection | Plain text only; control and bidi characters stripped; fixed system prompt; AI output shown as text (S051, S054) |
| CI and releases | Secret leaks; debuggable builds; unsigned builds | gitleaks; release gate checks; pinned dependencies (S005, S066, S067) |
| Operations | Destructive migrations; unnoticed abuse | Migrations reviewed and tested locally first; abuse alerts; incident plan (S065, S069) |

## Out of scope

- A rooted phone or a debugger on the player's own phone: they can read their own data.
- Denial of service on Supabase itself; Supabase's own platform security.
- Nation-state network attackers who can issue trusted certificates (pinning narrows this; see S038).
