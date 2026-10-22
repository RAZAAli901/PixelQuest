# Abuse review: study chat, files and chess

Reviewed 2026-10-22 (J172). Every limit below is enforced by the database or an Edge Function. The app's own checks only save a round trip; a changed app gains nothing by skipping them. `supabase/tests/local/abuse.test.ts` checks that the limits in force are the ones listed here.

## Rate limits

| What | Limit | Where | Refusal | Tested in |
|---|---|---|---|---|
| Chat messages | 30 a minute per member, across their parties | trigger on `chat_messages` (20261019000058), `study_chat_config.messages_per_minute` | `too_many_messages` | `study_chat.test.ts` |
| Chat message length | 2,000 characters | `study_chat_config.message_max_chars` | check | `study_chat.test.ts` |
| File uploads | 20 a day per member, rejected ones included | trigger on `chat_files` (058), `uploads_per_day` | `too_many_uploads` | `study_chat.test.ts` |
| File size | 20 MB a file; 500 MB a party | `request-upload` and `chat_request_upload` (059), `chat_storage_cap` (057) | `too_large`, `party_storage_full` | `study_files.test.ts` |
| Invitations | 20 an hour per inviter | `invite_member`, `invite_log` (20261019000026) | `too_many_invites` | `darood_parties.test.ts` |
| Chess challenges | 10 an hour per member; each expires after 10 minutes | `chess_create_challenge` (083), `chess_config.challenges_per_hour` | `rate_limited` | `abuse.test.ts` |
| Chess moves | 5 a second in a game | trigger on `chess_moves` (095) | `rate_limited` | `chess_rls.test.ts` |
| Watching games | 20 joins an hour per member | `chess_watch` (095, 105) | `rate_limited` | `chess_rls.test.ts` |
| Rated games per pair | 10 a day; very short games count only on checkmate | `chess_finish` (092) | the game is played unrated | `chess_rating.test.ts` |
| One live game | one waiting or active game per player | constraint and `chess_create_challenge` | `already_playing` | `chess_rls.test.ts` |

## Report and block

| What | Report | Block |
|---|---|---|
| A chat message | Long press → Report: `report_chat_message` (066). Only active members of the party, not about their own message, once each. | Long press → Block this person: `block_member`. Their messages show as one collapsed line for the blocker only; tapping it unblocks. |
| A shared file | A file is a message from its uploader, so it's reported the same way (reason `unsafe_file` is allowed). Files also open behind a warning, and nothing is previewed from an unknown type. | As for messages. |
| A chess challenge | Challenge screen → Report: `report_chess_challenge` (107). Only active members of the challenge's study party, not the sender. The report names the sender; the sender isn't told who reported it. | Challenge screen → Block @name: `block_member`, and the challenge is declined. Blocked pairs (either way round) can't challenge each other or take each other's open challenges (096). |
| A display name on the leaderboard | Report from the player's card (`reports`, earlier phases). | Not needed: the leaderboard shows no messages. |

Reports (`chat_reports`, `chess_reports`, `reports`) have RLS on and no grants to app users: nobody reads them back but the service role, from the Supabase dashboard. A report doesn't hide anything by itself; the team looks at it.

## What changed in this review

- **Challenges couldn't be reported.** A challenge's chat line is the server's, not the sender's, so `report_chat_message` couldn't name who sent it. Added `chess_reports` and `report_chess_challenge` (migration 20261019000107), with Report and Block on the incoming-challenge screen.
- **The challenge rate limit had no test.** Added one: the 11th challenge in an hour is refused, and an hour later it's allowed again.
- **The incoming-challenge screen now scrolls**, so the new buttons fit on small phones and with large text.

## Known limits

- Blocking is per pair, not per party: a blocked member still sees the blocker's messages (nothing hides them for the blocked person), as in most chat apps.
- Reports aren't triaged in the app. Look at them in the dashboard: `chat_reports`, `chess_reports` and `reports`, newest first.
- A determined member with several accounts can still spam a party they're in. The party's leader can remove them, and invitations are capped at 20 an hour.
