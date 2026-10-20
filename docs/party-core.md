# Party core: one party system, two kinds of party

Darood Parties (1.4) and Study parties (Phase E of the Islamic hub plan) share one party system. Both have the same members, invitations, leader and limits; each kind adds its own data and screens. This page maps what Darood Party built, and says which part becomes shared and which stays Darood's.

**The rule:** everything about *who is in a party* is shared. Everything about *what the party does* belongs to its kind.

## On the server (Supabase)

| What | Today | Becomes |
|---|---|---|
| `parties` | Darood Parties: name, leader, member limit, milestone, completion, race board | **Shared**, with a `party_type` column (`darood` or `study`, default `darood`). `milestone`, `milestone_set_at`, `completed_at` and `is_public_race` are Darood's columns on the shared row; they stay empty for a study party. |
| `party_members` | Memberships: invited, active, declined, left, removed; leader or member | **Shared**, unchanged. |
| `party_member_cap` trigger | At most `max_members` (2-30) invited or active | **Shared**, unchanged. |
| `create_party(name, max_members, milestone)` | Starts a Darood Party | **Shared** as `create_party(name, max_members, party_type)`. The old form stays, making a Darood Party, for the 1.4 apps already installed. |
| `invite_member`, `respond_invite`, `leave_party`, `remove_member`, `transfer_leadership`, `delete_party` | The membership rules | **Shared.** They never looked at what the party does, except that a finished party (`completed_at`, Darood's milestone reached) takes no new members. A study party never finishes that way. |
| `party_leader_gone` trigger | Hands the leadership on when a leader's row goes | **Shared**, unchanged. |
| `private.invite_log` | The 20-invitations-an-hour limit | **Shared**, unchanged: one limit across both kinds. |
| `private.is_active_member`, `is_invited`, `can_see_username` | Who may read what | **Shared**, unchanged. |
| Leader may update `name` | Column grant and policy | **Shared.** |
| `darood_entries`, `add_darood_entry`, `darood_entry_guard`, `darood_entry_limits`, `darood_entry_completes` | Posting numbers | **Darood's.** Only a Darood Party takes entries (the guard checks the party's type). |
| `set_milestone`, `party_milestone_rules`, `party_completion_is_final` | Milestones and finishing | **Darood's.** Only a Darood Party has a milestone. |
| `party_stats`, `party_member_stats`, `party_day_stats` | Totals from entries | **Darood's** (a study party has no entries, so it has no rows). |
| `race_board`, `is_public_race` | The public race | **Darood's.** Only Darood Parties with a milestone appear. |
| Realtime on `party_members` | Members change live | **Shared.** Realtime on `darood_entries` stays Darood's. |

Study party data (items, completions, sessions, weekly scores) gets its own tables in Phase E, keyed by `party_id`, read by active members of a `study` party only.

## On the phone

| What | Today (package `darood`) | Becomes |
|---|---|---|
| `Party`, `PartyMember`, `PartyRole`, `MemberStatus` | `darood.domain` | **Shared** (`party.domain`). `Party` gains its `type`. Its Darood fields (milestone, total, progress, race board) stay on it, empty for a study party. |
| `DaroodRules` (name 2-40, members 2-30, default 10) | `darood.domain` | The name and member limits become **shared** `PartyRules`. The entry limits stay Darood's. |
| `RaceLine`, `DaroodEntry`, `EntryState`, `PartyStats`, `MemberStats`, `DayTotal(s)`, `Consistency` | `darood.domain` | **Darood's.** |
| `PartyDto`, `PartyMemberDto`, `MemberNameDto`, `PartyStatusReply`, `RemoteParty`, `InviteOutcome` | `darood.data.remote` | **Shared** (`party.data.remote`). |
| Entry, stats, race and day DTOs | `darood.data.remote` | **Darood's.** |
| `DaroodPartyRemoteDataSource` | Every call | Split: membership calls go to a **shared** `PartyRemoteDataSource`; entries, stats, milestone and race stay Darood's. |
| `DaroodPartyRepository` | Every call, and the cache | Split: **shared** `PartyRepository` (parties, members, invitations, leader actions, the cache of both) and `DaroodRepository` (entries, the queue, stats, milestone, race). |
| Room `darood_party`, `darood_party_member` | The cache of parties and members | **Shared** in what they hold: `darood_party` gains `partyType`. The tables keep their names, so nothing is copied. |
| Room `darood_entry`, `darood_pending_entry` | Entries and the send queue | **Darood's**, unchanged. |
| `DaroodDao` | All of it | Split: the party and member queries move to a **shared** `PartyDao`; entries stay in `DaroodDao`. |
| `DaroodAccount`, `UsernameRepository`, `UsernameCache`, username screens | The account and its username | **Shared**: a study party needs the same username. They stay where they are, used by both. |
| `DaroodLiveSource` | Realtime for a party | Split: a **shared** helper for members and invitations; entries stay Darood's. |
| `DaroodSyncWorker`, `DaroodWeeklyWorker`, `DaroodAccountCleanup`, reminders, `MilestoneSeenStore` | Background work | **Darood's.** The cleanup also clears the shared cache, as it does now. |

### Screens

| Screen | Today | Becomes |
|---|---|---|
| `DaroodPartyScreen` (the list), `PartyRow`, `InviteRow` | Darood Parties and invitations | **Shared** list, shown for one kind: the Islamic hub shows Darood Parties, the new Parties screen shows study parties. |
| `CreatePartyScreen` | Name, member limit, milestone | **Shared**; the milestone is Darood's part. |
| `AddPeopleScreen` | Invite by username | **Shared**, unchanged. |
| `PartyInfoScreen` | Members, leader actions, milestone, race board | **Shared** members and leader actions; milestone and race board are Darood's parts. |
| Member limit selector, username search, member list | Inside those screens | **Shared** composables. |
| `PartyChatScreen`, the `chat` package, `RaceBoardScreen`, `MyStats` | Posting and following numbers | **Darood's.** |

A kind says what's its own through `PartyKind`: its name and icon, what its list rows summarise, the extra fields when it's created, and the tabs and rules its party screen shows. `PartyType` is the kind's name on the server and in Room.

## What doesn't change

- Darood Parties work as they do in 1.4: the same rules, screens and routes (`islamic/darood/...`).
- Neither kind touches XP, levels, streaks or the leaderboard. Study party points (Phase E) are their own.
- Every rule stays enforced on the server; the app mirrors it.
