# PixelQuest privacy policy (draft)

*Draft for the owner to review before publishing (S058). It reflects the app as of 2026-10-22 (version 1.7.x). Fill in the bracketed parts. It isn't legal advice; have it checked for the countries the app is offered in.*

**Who we are:** PixelQuest is made by [owner name]. Contact: [contact email].

## The short version

- PixelQuest works without an account. Your quests, streaks, prayer log and Quran bookmarks stay on your phone.
- If you sign in, we keep what the online features need, and nothing else: no ads, no analytics, no tracking, no selling of data.
- You can export your cloud data and delete your account in the app, at any time.

## What stays on your phone

We never receive this. It's kept in the app's private storage until you delete it, reset your progress or uninstall the app:

- your quests (names, notes, times, repeats, reminder styles), completions and misses;
- your hero name, XP, level, streaks and difficulty;
- your Namaz settings and prayer log, and, if you turn it on, your approximate location (city level), used only to work out prayer times on the phone;
- your Quran bookmarks and reader settings;
- the AI Coach's latest insight.

Android's own backup may copy your settings, and a backup file you make in the app, to your Google account. Moving to a new phone with Android's transfer also copies the app's database. These go to your Google account, not to us.

## What we keep when you sign in

| What | Why |
|---|---|
| Your email address, or your Google account's id and email. Google also passes us the name and profile picture link on your Google account; the app doesn't use them | Signing you in |
| A display name, your streaks, level and XP | The leaderboard, only if you choose to join it |
| A username | So party mates can find and invite you |
| Your parties: name, members, roles, invitations | Darood and study parties |
| Darood counts you add, with their date and time zone | Party totals and the weekly ranking |
| Study items, reminders, completions and study sessions you add | Study parties |
| Chat messages and files you send in a party, and when you last read the chat | The party chat. Files are private to the party and opened by short-lived links |
| Chess games and moves, ratings, challenges and records against other players | Chess |
| Reports you make and people you block | Keeping parties safe |
| A push token for your phone | Chess notifications |
| How many AI Coach requests you made today, and recent actions counted by rate limits | Fair-use limits |

Other members of your parties see what you share in those parties: your username, messages, files, counts, study progress and games. The leaderboard shows your display name, streaks and level to everyone, only after you opt in.

## Services we use

- **Supabase** stores the cloud data above, in [region].
- **Google Gemini** writes the AI Coach's insights, only if you turn the coach on and are signed in. It receives numbers only: streak lengths, completion rates, counts per category and the tone you chose. It never receives quest names, notes, usernames or emails. Google handles these requests under its Gemini API terms.
- **Firebase Cloud Messaging** (Google) delivers chess notifications. A notification carries only ids; the words are put together on your phone.
- **Google sign-in**, if you choose it.

## How long we keep it

As long as your account exists, unless you delete something sooner. When you delete a message or a file, its text and file go, and only the fact that a message was deleted stays. Uploads never finished are removed within an hour.

## Your choices

- **Export:** Settings → Account → Export my cloud data saves a copy of everything above as a file.
- **Delete:** Settings → Account → Delete account removes your account and everything that names you. Three things stay, without your name or any link to you: finished chess games and records stay for your opponents, Darood counts stay in their party's total, and study items you added stay in their party. Files you sent are deleted from storage.
- **Leave the leaderboard, a party or the AI Coach** at any time in the app.
- **Location** is asked for only when you choose it, and you can type a city instead.

## Children

[State the app's audience and age rating. If it's offered to children, check the rules that apply, for example COPPA and Google Play's Families policy.]

## Security

Data travels only over encrypted connections, and the app checks the server's certificate. The sign-in session on your phone is sealed with a key in Android's secure hardware. On the server, every table allows only what its rules say. We never ask for your password.

## Changes

We'll update this page and its date when what we collect changes, and say so in the app's release notes.

*Last updated: [date of publishing].*

---

## For the Play data safety form (S068)

| Play category | Collected? | Shared? | Optional? | Purpose |
|---|---|---|---|---|
| Personal info: email address | Yes, when signed in | No | Yes (sign-in is optional) | Account management |
| Personal info: name (Google profile name, unused) | Yes, with Google sign-in | No | Yes | Account management |
| Personal info: user ids (username, account id) | Yes | No | Yes | App functionality, account management |
| Location: approximate | No (used on the phone only) | No | Yes | App functionality |
| Messages: other in-app messages | Yes (party chat) | No | Yes | App functionality |
| Photos and files | Yes (files sent in party chat) | No | Yes | App functionality |
| App activity: other user-generated content | Yes (Darood, study, chess) | No | Yes | App functionality |
| App info and performance: crash logs | No (kept on the phone only) | No | | |
| Device or other ids: push token | Yes | No | Yes | App functionality |

Data is encrypted in transit, and players can ask for it to be deleted (in the app, without contacting us).
