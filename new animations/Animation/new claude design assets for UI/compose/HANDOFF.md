# PixelQuest UI v2 — handoff for the coding agent

The visual reference is `PixelQuest Screens.dc.html` (screens 2A–2K). Every file below is **stateless Jetpack Compose UI**. It takes a `UiState` plus callbacks, so your job is to map the existing ViewModels into these states and delete the old UI.

## 1. Drop-in

```
handoff/ui/theme/PqTheme.kt              -> app/src/main/java/<pkg>/ui/theme/
handoff/ui/components/PqComponents.kt    -> app/src/main/java/<pkg>/ui/components/
handoff/ui/components/PqModels.kt        -> app/src/main/java/<pkg>/ui/components/
handoff/ui/components/PixelAvatar.kt     -> app/src/main/java/<pkg>/ui/components/
handoff/ui/navigation/PqNavigation.kt    -> app/src/main/java/<pkg>/ui/navigation/
handoff/ui/screens/*.kt                  -> app/src/main/java/<pkg>/ui/screens/
```

- Replace the `package com.pixelquest...` lines and the `com.pixelquest.R` import with the app's real package.
- If the app already has a `Theme.kt`, rename that theme or merge it into `PixelQuestTheme`. Don't keep two themes.

### Fonts (`res/font/`, lowercase file names)
- `bangers_regular.ttf`: Bangers (the app probably ships it already; reuse it)
- `rubik_medium.ttf`, `rubik_semibold.ttf`, `rubik_bold.ttf`, `rubik_extrabold.ttf`: Rubik
- `amiri_regular.ttf`: Amiri (Quran Arabic)
- `press_start_2p_regular.ttf`: Press Start 2P (pixel skin titles, buttons, labels)
- `pixelify_sans_medium.ttf`, `pixelify_sans_semibold.ttf`, `pixelify_sans_bold.ttf`: Pixelify Sans (pixel skin body)

All are free on Google Fonts (OFL).

## 1b. Two skins: Comic and Pixel (mocks 3A–3E)

There is one set of screens with two looks. Every token in `PqTheme.kt` (`PqColors`, `PqDimens`, `PqType`, `pqShape()`, `PqCircle`) is a getter on `PqSkinState`, so this single line re-skins the whole app live:

```kotlin
PqSkinState.skin = PqSkin.Pixel   // or PqSkin.Comic
```

- Persist the choice in DataStore and set it before `setContent {}` so there's no flash on launch.
- The old "EXIT COMIC / COMIC MODE" button on Home becomes a **Settings > Look: Comic / Pixel** option that writes this value.
- **Delete the old separate pixel-mode screens/composables.** Don't keep two copies of any screen.
- Pixel skin changes: radius 0, 2dp border, a straight-down 3dp step shadow **only on tappable items** (`comicPressable` / `comic(tappable = true)`), orange primary with cream text (`PqColors.OnRed`), green progress (`PqColors.Progress`), Press Start 2P + Pixelify Sans, and a square power meter instead of the round progress ring.
- When writing new UI, never hard-code `RoundedCornerShape`, `CircleShape`, colours or `.sp`. Use `pqShape(r)`, `PqCircle`, `PqColors.*`, and `N.bsp` (body text) / `N.lsp` (buttons/labels) / `PqType.display(N)` (titles) so both skins keep working.
- To preview pixel in Android Studio, temporarily set `PqSkinState.skin = PqSkin.Pixel` at the top of a preview function.

### Dependencies
- `androidx.compose.material:material-icons-extended` (Checklist, Mosque-free moon `Brightness3`, FitnessCenter, WbTwilight, etc.)
- Compose BOM 2024.02+ and Kotlin 1.9+ (uses `enum.entries`). On older Kotlin, replace `.entries` with `.values().toList()`.
- `material3` (already used).

## 2. Navigation change (important)

Old: a drawer plus a bottom bar (Home · Tasks · Trophy · Stats · Profile).
New: **Home · Quests · Trophy · Deen · Hero**. Remove the drawer and the hamburger icon from every screen.

| Old location | New location |
|---|---|
| Drawer > Islamic | **Deen** tab (`DeenScreen`) |
| Drawer > Quest History | Quests tab > "History" segment |
| Drawer > Leaderboard | Hero > Profile list > Leaderboard |
| Drawer > Account / Settings | Hero top-bar gear (`onSettings`) |
| Stats tab | Hero > "Stats" segment |
| Profile tab | Hero > "Profile" segment |
| Drawer header (XP/level) | Home header + Hero card |
| Home "EXIT COMIC" button | Settings screen |
| Home "REFRESH" button | Pull-to-refresh on Home (`PullToRefreshBox`) or remove |
| Centre trophy | **Unchanged.** Pass the existing handler to `onCenterClick` |

Wrap the NavHost in `PqRootScaffold(currentTab, onTabSelect, onCenterClick) { padding -> NavHost(...) }` and pass `padding` into each screen's `contentPadding`. Hide the bar (`showBottomBar = false`) only on full-screen flows such as the Quran reader.

Suggested routes: `home`, `quests`, `deen`, `deen/namaz`, `deen/quran`, `deen/darood`, `hero`, `hero/avatar`, `hero/leaderboard`, `settings`. Back-stack sub-screens (Namaz, Quran, Avatar, Leaderboard) keep the parent tab highlighted.

## 3. Screen map

| Mock | Composable | File | Replaces |
|---|---|---|---|
| 2A / 2B | `HomeScreen` (state.isPerfectDay switches A↔B) | HomeScreen.kt | Today's Dashboard + its completed state |
| 2C | `QuestsScreen` | QuestsScreen.kt | Quest Log |
| 2D | `DeenScreen` | DeenScreens.kt | Islamic hub |
| 2E | `NamazScreen` | DeenScreens.kt | Namaz Tracker |
| 2F | `QuranScreen` | QuranScreen.kt | Quran surah list |
| 2G / 2H | `HeroScreen` (segment) | HeroScreen.kt | Hero Profile + Stats |
| 2I | `AvatarPickerScreen` | AvatarAndLeaderboard.kt | Choose Avatar |
| 2J | `LeaderboardLockedScreen` | AvatarAndLeaderboard.kt | Leaderboard (signed out) |
| 2K | components | PqComponents.kt | all ad-hoc cards/buttons |

## 4. Wiring notes

- **Quest toggle:** `QuestRow`'s circle calls `onToggleQuest`. Hook it to the existing "DONE" logic, including XP and the perfect-day check. Tapping the row body calls `onQuestClick` (edit).
- **Overdue:** `QuestUi.overdue = !done && scheduledTime < now`.
- **Quest grouping (Quests > Today):** before 12:00 is "Morning", 12:00–17:00 "Afternoon", after that "Evening". The label carries a count, e.g. `"Evening · 2 left"`.
- **Category:** `QuestCategory.from(existing.categoryString)`. Unknown values fall back to `Other`.
- **Prayers:** always pass 5 `PrayerUi` in order. Exactly one has `Next` (the first unprayed one whose time hasn't passed). Unprayed ones whose time has passed are `Missed`. `countdown` is like `"in 36 min"`.
- **Home `nextPrayer = null`** means all five are prayed. Pass `nextFajr` for tomorrow.
- **Hero level bar:** `perfectDays / perfectDaysNeeded` (currently 7 per level).
- **Heatmap:** a list of `DayStatus`, oldest first, in Monday-start weeks (length a multiple of 7).
- **Avatars:** `PixelAvatar` draws the six classes in code. If the app already has avatar PNGs, swap the body for `Image(painterResource(..), filterQuality = FilterQuality.None)` and keep the signature.
- **Leaderboard:** show `LeaderboardLockedScreen` when signed out. Both buttons use the same auth flow as the AI Questmaster unlock.

## 5. Design rules (keep these consistent when adding screens)

1. Border: `PqDimens.Border` (2.5dp) ink on every card, button and input.
2. Hard shadow (`Modifier.comic` / `comicPressable`) only on tappable or elevated items. Done rows and list rows are flat.
3. **Red = the single primary action on a screen.** Secondary actions are paper buttons. Never put three red buttons in a stack.
4. Colour cards mark a category or state: Sky = prayer/info, Gold = level/celebration, Amber = fitness/AI, Lilac = learning/Quran. Default cards are Paper.
5. Titles use Bangers (`PqType.Display/Title`). Everything else is Rubik. Red text uses `PqColors.RedText`, not `Red`, for contrast.
6. Root tabs use `PqTopBar(title)` with no back button. Sub-screens use `PqTopBar(title, onBack)`. No boxed headers.
7. Leave ≥4dp of space right/bottom of shadowed items so the shadow isn't clipped (the lists already pad for this).

## 6. Done when

- [ ] The drawer and hamburger are gone, and the 4 tabs plus the trophy work.
- [ ] Every screen in the map renders from real ViewModel state, and `@Preview`s still compile.
- [ ] Old composables for the replaced screens are deleted.
- [ ] Completing a quest from Home and from Quests updates both screens and XP.
- [ ] Marking a prayer on Namaz updates Deen and the Home prayer card.
- [ ] Switching Settings > Look between Comic and Pixel re-skins every screen without a restart, and the choice survives an app relaunch.
