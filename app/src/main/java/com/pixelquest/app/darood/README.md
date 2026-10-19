# `com.pixelquest.app.darood`: Darood Party

The Darood Party feature lives here, apart from the game's own code, so it's easy to see that it never reaches XP, levels, streaks or the leaderboard (docs/darood-design.md).

| Package | What goes in it |
| --- | --- |
| `domain` | The rules and models, plain Kotlin: usernames, parties, members, entries, stats |
| `data` | Talking to Supabase (DTOs, remote calls) and the Room cache, behind repositories |
| `ui` | The screens and their ViewModels: username setup, the party list, the chat, stats |
| `sync` | Background work: sending entries made offline, live updates |

Server-side rules (tables, row-level security, functions) are in `supabase/migrations`.
