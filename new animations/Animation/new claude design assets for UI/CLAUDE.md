# PixelQuest: new UI, instructions for Claude Code

You're implementing a full UI redesign of the PixelQuest Android app (Kotlin + Jetpack Compose). Everything you need is in this folder. Read this file first, then `compose/HANDOFF.md` for the detailed wiring notes on the core screens.

## Folder contents

```
new claude design assets for UI/
├─ CLAUDE.md                     ← you are here
├─ design-reference/             ← open "PixelQuest Screens.dc.html" in a browser = the visual spec
│                                   ("PixelQuest Brand.dc.html" = logo + notification mocks)
├─ compose/
│  ├─ HANDOFF.md                 ← detailed notes for core screens, nav, skins
│  ├─ ui/theme/PqTheme.kt        ← ALL tokens, two skins (Comic / Pixel)
│  ├─ ui/components/             ← PqComponents, PqFormComponents (switch, field, stepper, sheet, CoachCard), PixelAvatar, PqModels
│  ├─ ui/navigation/PqNavigation.kt
│  ├─ ui/screens/                ← Home, Quests, Deen (+Namaz), Quran, Hero (+Stats), Avatar+Leaderboard,
│  │                                Parties (hub, new, Darood room, stats sheet, info), Study party (+add sheet, scores)
│  └─ notifications/PqNotifications.kt
└─ android-res/                  ← merge into app/src/main/res/
   ├─ drawable/ic_stat_pixelquest.xml       (status-bar icon, white pixel sword)
   ├─ drawable/ic_launcher_foreground.xml   (logo 1A "Sword Badge"), ic_launcher_monochrome.xml
   ├─ mipmap-anydpi-v26/ic_launcher*.xml    (adaptive icon)
   ├─ drawable/notif_card_bg*.xml, layout/notif_pq_big.xml, notif_pq_small.xml
   └─ values/pq_colors.xml
```

## Ground rules

1. **Explore the existing app first.** Find the current screens, ViewModels, repositories, nav graph, drawer, theme and font resources. Make a short list of old composable → new composable before you change anything.
2. **The files here are stateless UI.** Each screen takes a `…UiState` and callbacks. Map existing ViewModel state into these types. Don't move business logic into the UI.
3. **Replace, don't duplicate.** Delete the old composables once their replacement is wired. That includes the old separate pixel-mode screens, because one set of screens now serves both skins.
4. **Fix package names.** Every file uses `com.pixelquest…` as a placeholder. Change it to the real package, and change the `com.pixelquest.R` imports to match.
5. **Never hard-code styling in new UI.** Use `PqColors.*`, `PqType.*`, `pqShape(r)`, `PqCircle`, `N.bsp` (body text), `N.lsp` (labels/buttons) and `PqType.display(N)` (titles). That's what keeps both skins working.
6. **One red per screen.** `PqButton` Primary is for the single main action. Secondary actions use `PqButtonStyle.Secondary`. Destructive actions use an outline in `PqColors.RedText`.
7. Build and run after each section. Keep the `@Preview`s compiling.

## Order of work

1. **Theme and resources.** Add the fonts listed in HANDOFF §1: Bangers, Rubik (4 weights), Press Start 2P, Pixelify Sans (3 weights), Amiri. Copy `PqTheme.kt` and the components. Merge `android-res/` into `res/`. If `res/font/rubik_extrabold.ttf` etc. are named differently, update the references in `PqTheme.kt` and `notif_pq_*.xml`.
2. **Navigation.** Remove the drawer. Add `PqRootScaffold` with the bottom bar Home · Quests · Trophy · Deen · Hero. **The centre trophy keeps its current behaviour** (it's connected to the owner's Claude Code feature), so pass the existing handler to `onCenterClick`.
3. **Core screens** (HANDOFF §3): Home, Quests, Deen, Namaz, Quran, Hero/Stats, Avatar, Leaderboard.
4. **Parties and Study** (see below).
5. **Coach Intel card** on Home.
6. **Notifications.**
7. **Skin switch:** Settings > Look: Comic / Pixel → `PqSkinState.skin`, persisted in DataStore and applied before `setContent`.

## Where the drawer items went

| Old drawer item | New home |
|---|---|
| Islamic | **Deen** tab |
| **Parties** | **Home header "groups" button**, **Deen > Darood Party tile**, and a **"Parties" row in Hero > Profile** list (add a `PqListRow(Icons.Rounded.Groups, "Parties", …)`). All open route `parties`. |
| Quest History | Quests tab > History segment |
| Leaderboard | Hero > Profile list |
| Account / Settings | Hero top-bar gear |

## New screens (mocks in design-reference, section "Turn 4")

| Mock | Composable | File | Replaces / notes |
|---|---|---|---|
| 4A | `HomeScreen` + `CoachCard` | HomeScreen.kt, PqFormComponents.kt | Coach Intel card, the disabled COOLDOWN button and the separate "OPEN AI COACH" button become **one** card. Set `HomeUiState.coach` once AI is unlocked; until then `AiTeaser` shows. `nextBriefIn` replaces the cooldown text. |
| 4B / 4C | `PartiesScreen` | PartiesScreens.kt | The Darood Party list and its empty state. The empty state shows automatically when `parties` is empty. Lists both party types. The weekly note toggle lives here as "Friday note". |
| 4D | `NewPartyScreen` | PartiesScreens.kt | Both "New party" forms: type picker first, milestone only for Darood, size 2–30 via `PqStepper`. |
| 4E | `DaroodRoomScreen` | PartiesScreens.kt | The party chat. **Hide the bottom bar on this route.** `events` are newest last (the list is reverse-laid-out). Quick-add chips call `onQuickAdd(n)`. Send is disabled while the input is empty. |
| 4F | `DaroodStatsSheet` | PartiesScreens.kt | The "My stats" bottom sheet. Show it when `onStats` fires. |
| 4G | `PartyInfoScreen` | PartiesScreens.kt | Party info. Leader-only actions (Add people, Rename, Change milestone, Delete) are gated by `youAreLeader`. Non-leaders get "Leave party". |
| 4H | `StudyPartyScreen` (tab Week) | StudyPartyScreens.kt | Study party "This week". Timer start/stop is `onToggleTimer`, the quick minute chips are `onLogMinutes`, and the FAB opens the add sheet. |
| 4I | `AddStudyItemSheet` | StudyPartyScreens.kt | Add item. `dueOptions` = ["No date","Today","Tomorrow", next 2 weekday labels]. The calendar chip opens a M3 `DatePicker` and sets `customDueLabel`. |
| 4J | `StudyPartyScreen` (tab Scores) | StudyPartyScreens.kt | Scoreboard. Points: assignment 10, quiz 15, early (≥1 day) +5, 1 per 10 study minutes up to 12/day. These never touch XP, level or streak. |
| — | tab Items / People | StudyPartyScreens.kt | The empty-state copy matches the old app. |

Routes: `parties`, `parties/new`, `parties/{id}` (render `DaroodRoomScreen` or `StudyPartyScreen` by type), `parties/{id}/info`.

## Pixel skin (mocks 3A–3E and 4K–4Y)

Each mock is drawn in both skins. Nothing extra needs coding: all tokens switch with `PqSkinState.skin`. Check that every new screen looks right in Pixel. That means square corners, the 2dp border, a straight-down 3dp step shadow on tappable items only, orange primary with cream text, green progress, Press Start 2P titles and Pixelify body text. If something looks off in Pixel, fix it with tokens, not with `if (isPixelSkin)` branches, unless the mock shows a different shape. The one existing branch is `ProgressRing`, which becomes a square meter.

## Notifications (mocks 1D–1F in design-reference/PixelQuest Brand.dc.html)

- `PqNotifications.createChannels()` goes in `Application.onCreate()`. There are 7 channels so users can mute each kind.
- Replace the current notification builders with the matching `PqNotifications.*` call: `prayerReminder`, `questDue`, `streakAtRisk`, `levelUp`, `perfectDay`, `coachBrief`, `daroodActivity`, `daroodMilestone`, `fridayNote`, `studyItemAdded`, `studyDueTomorrow`, `studyWeeklyResult`.
- Register `PqNotificationActionReceiver` in the manifest (`android:exported="false"`) and fill in its TODOs with real repository calls. The Done and Prayed buttons must award XP exactly like the in-app buttons.
- Ask for `POST_NOTIFICATIONS` (API 33+) the first time the user turns on a reminder, not at app launch.
- The status-bar icon is `R.drawable.ic_stat_pixelquest`. The accent colour follows the active skin.

## App icon (logo 1A)

`android-res/mipmap-anydpi-v26/ic_launcher.xml` is an adaptive icon: red background, pixel sword foreground, and a monochrome layer for themed icons. Point `android:icon` / `android:roundIcon` in the manifest at it. Delete the old launcher PNGs only after you've checked the new icon on a device.

## Done when

- [ ] No drawer anywhere. The 4 tabs and the trophy work, and Parties is reachable from Home, Deen and Hero.
- [ ] Every mock in design-reference has a matching screen driven by real state.
- [ ] The Darood room posts counts, the milestone bar updates, and the stats sheet opens.
- [ ] Study party: the timer logs minutes, items can be added, ticked and pinned, and the scoreboard ranks members.
- [ ] Notification actions work from the lock screen without opening the app.
- [ ] Switching Comic ↔ Pixel in Settings re-skins every screen, including the new ones, and survives a relaunch.
- [ ] The old screens, the old pixel-mode screens and the drawer code are deleted, and the build has no warnings about unused resources you added.
