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
| `quran/ui/QuranHomeScreen` (the surah list) | `QuranScreen`. The reader, search, bookmarks and sources follow it in v2 | 2F |
| `ProfileScreen` and `StatsScreen` (two tabs) | `HeroScreen`, with Profile and Stats segments | 2G, 2H |
| `avatar/AvatarSelectionScreen` | `AvatarPickerScreen` | 2I |
| `leaderboard/LeaderboardScreen`, signed out | `LeaderboardLockedScreen`. Signed in, the board in v2 | 2J |
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

## Screens without a mock

These have no mock of their own. Each is built from v2's components in the mocks' language:
- Onboarding, the splash, level-up, level history, difficulty, quest stats, Create and Edit quest, Settings, Account (Cloud & leaderboard), and the AI Coach screen;
- the Quran reader (its top bar, settings sheet, actions sheet and display switch), search, the translation picker, Go to, bookmarks and sources;
- Darood's race board, Add people, username setup, and a study item's screen;
- the Namaz screen's cards and sheets (setup, settings, city picker), its snackbar, All five prayed, and the +XP badge;
- every dialog, the "did you do it?" prompt, the notifications-off banner, the exact-alarm prompt and the first-run tips.

## The building blocks

Screens use these (in `ui/components/PqComponents.kt` and `PqFormComponents.kt`), never Material's own look:
- **Surfaces:** `PqCard`, `Modifier.comic`, `comicPressable`, `PqSheet`, `PqDialog`, `PqConfirmDialog`.
- **Buttons:** `PqButton` (Primary is the screen's one red button; Secondary is paper; disabled draws at half strength), `PqQuietButton`, `PqDangerButton`, `PqIconButton`, `PqBackButton`.
- **Structure:** `PqTopBar` (with an optional line under the title), `PqGroupLabel`, `PqFieldLabel`, `PqSectionHeader`, `PqBullets`.
- **Choices:** `PqSegmented`, `PqChoiceChip`, `PqSwitch`, `PqRadio`, `CheckCircle`.
- **Small things:** `PqTag`, `PqChip`, `CategoryTile`, `PqProgressBar`, `InitialAvatar`, `PixelAvatar` (the hero's sprite).

The labelled text field (`PixelTextField`), the quest form's selectors, and the CRT overlay keep their old names; they draw as v2.

## The skin

`ThemeMode` is the choice in Settings > Look, stored by id: `comic` or `pixel`. Before v2 there were also Clean Light and Follow System; a phone that kept one of those reads as Comic, which it already wore. `MainActivity` reads it before `setContent`, so the first frame is in the right skin, and follows changes after. The CRT filter is only in Pixel, when it's switched on, and never in Simple Mode.

The Quran reader's sepia page is the one place that still swaps colours through `AppColorScheme` (`PixelTheme.colors`), so its text and ribbon follow the page.

## What's gone

The side menu, the old bottom bar, `ThemeSelectionScreen`, Home's Comic toggle, and these components, each once nothing used it: `PixelButton`, `ComicButton`, `PixelCard`, `ComicPanel`, `PixelDialog`, `PixelConfirmDialog`, `PixelTopAppBar`, `PixelSnackbar`, `ComicSnackbar`, the old XP, progress and avatar-frame components, `SymbolIcon`, `PixelLoadingState`, and the old token layer (`ComicTokens`, `ComicShapeTokens`, the arcade colour names, `ComicTypography`). Emoji in buttons and titles became icons.
