# UI v2: the Claude Design redesign

The redesign comes from Claude Design. Its brief is `claude-ui-assets/START_HERE.md` and `compose/HANDOFF.md`.

It makes **one set of screens with two skins, Comic and Pixel**. Every screen draws only with the `PqTheme` tokens (`PqColors`, `PqType`, `pqShape`, `PqCircle`, `.bsp`, `.lsp`), so one setting re-skins the whole app.

## Old screens, and what replaces them

| Today (1.5.0) | Becomes (v2) | Mock |
|---|---|---|
| `MainActivity`'s side menu (`ui/menu`: `MenuDrawerScaffold`, `MenuDrawerContent`, header, items) | **Removed.** `PqRootScaffold` with `PqBottomBar`: Home, Quests, Trophy, Deen, Hero | nav |
| `PixelBottomNavBar` (Home, Tasks, Leaderboard, Stats, Profile) | `PqBottomBar`. The centre trophy keeps its handler: it opens the Leaderboard | nav |
| `ui/screens/HomeScreen` + `today/TodayScreen` (Today's Dashboard and its perfect-day state) | `HomeScreen` (`isPerfectDay` switches 2A and 2B) | 2A, 2B |
| `TodayAiInsightSection` (the Coach card, the COOLDOWN button, OPEN AI COACH) | `CoachCard`, or `AiTeaser` until the coach is unlocked | 4A |
| `TasksScreen` (Quest Log), and the menu's Quest History (`TaskHistoryScreen`) | `QuestsScreen`, with Today, All quests and History | 2C |
| `islamic/ui/IslamicHubScreen` (the Islamic hub) | `DeenScreen` (the Deen tab) | 2D |
| `ui/screens/namaz/NamazScreen` (the Namaz Tracker) | `NamazScreen` (DeenScreens.kt) | 2E |
| `quran/ui/QuranHomeScreen` (the surah list) | `QuranScreen`. The reader, search, bookmarks and sources stay, on the tokens | 2F |
| `ProfileScreen` and `StatsScreen` (two tabs) | `HeroScreen`, with Profile and Stats segments | 2G, 2H |
| `avatar/AvatarSelectionScreen` | `AvatarPickerScreen` | 2I |
| `leaderboard/LeaderboardScreen`, signed out | `LeaderboardLockedScreen`. Signed in, the board stays, on the tokens | 2J |
| `party/ui/PartiesScreen` (study parties) and `darood/ui/DaroodPartyScreen` (Darood Parties) | One `PartiesScreen` listing both kinds, with the Friday note switch | 4B, 4C |
| `darood/ui/create/CreatePartyScreen` (both kinds) | `NewPartyScreen`: the type first, a milestone for Darood, a size of 2 to 30 | 4D |
| `darood/ui/chat/PartyChatScreen` | `DaroodRoomScreen`, with no bottom bar | 4E |
| `darood/ui/chat/MyStats` (the stats sheet) | `DaroodStatsSheet` | 4F |
| `darood/ui/info/PartyInfoScreen` (both kinds) | `PartyInfoScreen`: the leader's actions, or Leave party | 4G |
| `study/ui/StudyPartyScreen` (This week, All items, Scoreboard, People) | `StudyPartyScreen`, with Week, Items, Scores and People tabs | 4H, 4J |
| `study/ui/ItemSheet` (the Add item sheet) | `AddStudyItemSheet`, with a date picker for other days | 4I |
| `settings/ThemeSelectionScreen`, and Home's COMIC MODE / EXIT COMIC button | Settings > Look: Comic or Pixel (`PqSkinState.skin`, persisted) | 3A–3E |
| `NotificationHelper` and the workers' own builders | `PqNotifications`: 7 channels, with Done and Prayed actions | 1D–1F |
| The launcher PNGs | An adaptive icon, logo 1A (Sword Badge), with a themed layer | 1A |

## Where the menu's entries go

| Menu entry | v2 |
|---|---|
| Islamic | The Deen tab |
| Parties | Home's groups button, Deen's Darood Party tile, and Hero > Profile's Parties row (all to `parties`) |
| Quest History | Quests > History |
| Leaderboard | The trophy, and Hero > Profile's Leaderboard row |
| Account, Settings | Hero's gear |

## Kept as they are, re-skinned

These screens have no mock. They keep their layout and take the new tokens:
- Onboarding, Create and Edit quest, Settings, Account, the AI Coach screen, level history and task analytics;
- the Quran reader, search, bookmarks and sources;
- Darood's race board, Add people, username setup, and a study item's screen.
