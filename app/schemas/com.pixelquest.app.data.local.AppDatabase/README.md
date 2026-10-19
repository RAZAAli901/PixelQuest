# Database layouts

Room writes `N.json` for each database version when the app is built (`exportSchema = true`, Day 34). `DatabaseUpgradeTest` builds each earlier version's database from these files and checks that updating keeps a player's progress.

Before Day 34 the app didn't export layouts. The earlier ones were recovered by building Room's export at an old commit:

| Version | Shipped in | Recovered from |
| --- | --- | --- |
| 1 | Early debug builds only | Derived from version 2 by undoing `MIGRATION_1_2`: no `perfectDaysTowardNextLevel` column and no `level_history` table. The commits with version 1 don't build on their own. |
| 2 | v1.0.0 to v1.0.3 | Tag `v1.0.3` |
| 3 | v1.1.0 | Tag `v1.1.0`. Identical to the last version-3 commit, `930197e2` (same identity hash) |
| 4 | Debug builds, Day 24 | `78b81fc0`, the last version-4 commit |
| 5 | Debug builds, Day 26 | `b511a4d2` |
| 6 | Debug builds, Day 27 | `68ad1856` |
| 7 | Day 28 to v1.2.0 | Exported by the build |
| 8 | Since the Namaz Tracker (adds `namaz_log` and `namaz_settings`, docs/namaz-design.md) | Exported by the build |
| 9 | Since Namaz XP (adds `namaz_log.bonusXp`) | Exported by the build |
| 10 | Since Darood Party (adds `darood_party`, `darood_party_member`, `darood_entry` and `darood_pending_entry`, docs/darood-design.md) | Exported by the build |

A version's layout must not change after it ships. Bump the version and add a migration instead. Room exports the new file, and the test fails until both exist.
