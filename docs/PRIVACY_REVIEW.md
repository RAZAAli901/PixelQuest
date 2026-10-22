# Privacy review: study reminders, chat, files, Darood weekly and chess

Reviewed 2026-10-22 (J173). This lists what each table added for these features stores, who can read it, how long it's kept, and what happens to it on export and on account deletion. `supabase/tests/local/privacy.test.ts` checks the export and deletion; `security.test.ts` checks the reads.

Every table has row-level security on, with nothing allowed unless a policy says so. Nothing here is visible outside the player's own parties. Push carries only ids and a kind; the words are put together on the phone.

## What each table stores

| Table | Stores | Who reads it | Kept |
|---|---|---|---|
| `study_reminders` | A reminder's title, kind, due time, repeat, audience, the alert offsets, the party and who made it | Active members of the party | Until deleted by its maker or the leader, or the party goes |
| `chat_messages` | Text (up to 2,000 characters), the sender, party, reply, the file it carries, and system lines (a chess challenge, a result) | Active members of the party | Until deleted; deleting clears the text and its file, keeping only that a message was deleted |
| `chat_files` | A file's name, type, size, checksum, storage path and status; the bytes are in the private `study-files` bucket | Active members, by a signed link valid 60 seconds | Until deleted; the bytes go too (cleanup-uploads) |
| `chat_reads` | When each member last read each chat | The member | Updated in place |
| `chat_reports`, `chess_reports`, `reports` | Who reported what, the reason, when | Service role only | Until handled by the team |
| `member_blocks` | Who blocked whom | The blocker | Until unblocked |
| `darood_week_results` | Each member's weekly place, total and active days | Party members | Kept as the party's history |
| `party_milestones` | Goals set and reached, by whom | Party members | Kept as the party's history |
| `chess_games`, `chess_moves` | Players, time control, result, every move with its time and the clocks | Active members of the game's party | Kept as the party's history |
| `chess_ratings` | A player's Int points and record per time control | Party mates | Until the account goes |
| `chess_head_to_head` | Two players' wins and draws against each other | Either of the pair; mates of both | Kept (see deletion) |
| `chess_challenges` | Who challenged whom, settings, status | Active members of the party | Expire after 10 minutes; kept as rows |
| `chess_watch_log` | Who joined which game to watch, when | Service role only | **One day** (added in this review) |
| `device_tokens` | Each phone's push token and platform | The owner (write), service role (read to send) | Until the phone signs out or the account goes |
| `private.invite_log` | Who invited, when (for the rate limit) | Nobody but the database | One day |

## Export

**Settings → Account → Export my cloud data** saves one JSON file from `export_my_data()` (migration 20261019000108). It holds:
- the profile and username, and the parties the player is in (role, status, dates);
- their Darood entries and weekly places;
- their study completions, sessions, weekly wins, and the items and reminders they made;
- the chat messages they sent;
- the files they shared: name, type, size and dates, not the bytes, which they can open in the app;
- the reports they made and the people they blocked;
- their chess games with every move, their ratings, challenges and head-to-head records;
- how many phones are registered for push (not the tokens).

Other people's rows aren't included. Their ids appear only as the other side of the player's own games, records and parties.

The phone's own backup (Settings → Backup) is separate. It holds quests, streaks, Namaz and settings, and stays as it was.

## Deletion

`delete_user_account()` deletes the auth user. Every reference to it is set by its foreign key; the privacy test checks the database catalogue, so a new table that references users is covered automatically.

| Goes with the account | Stays, anonymised |
|---|---|
| Profile, username, memberships, study completions and sessions, reminders they made, chat messages and file rows, reads, reports they made, blocks either way, Darood weekly places, chess ratings, challenges, watch log, push tokens | **Chess games**: the player's side becomes empty, so the opponent keeps the game and its moves (live games are aborted first). **Head-to-head**: the pair's row stays for the opponent; the deleted id no longer names anyone. **Darood entries** and **study items**: kept without a name, so the party's totals and list don't change. **Parties they led**: kept, without a leader. **Goals they set**: kept, without a name. |

The bytes of their files become orphans when the file rows go, and `cleanup-uploads` deletes them from storage (J174).

## What changed in this review

- **No way to get a copy of the cloud data.** The phone backup never included parties, chat or chess. Added `export_my_data()` and the Export button.
- **The watch log was kept forever.** It said who watched which game. It's only needed for an hour (the limit) and six hours (the channel), so it now keeps one day.

## Known limits

- A report about a deleted account goes with it (`chess_reports.reported_id` cascades). Deleting an account to escape a report removes the account anyway.
- Messages a member sent stay readable in the party until the member deletes them or the account. That's how group chat is expected to work.
