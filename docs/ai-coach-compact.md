# Compact AI Coach

Phase 1 of the upgrade plan (*App Upgrade Plan: Compact AI Coach and Namaz Tracker*, commits C01–C20). Goal: the AI Coach takes about half the vertical space, long content collapses instead of being removed, and the bottom bar's oversized round button becomes a normal tab.

## Audit (C01)

The plan's names came from the release APK. This is what the source actually has.

### Where the AI Coach lives

| Plan says | Source |
| --- | --- |
| "the AI Coach tab" | Not a bottom tab. `Screen.AiInsight` (`ai_insight`) is a full screen, opened from the AI card on Home (`TodayAiInsightSection`, its **OPEN AI COACH ▶** button). |
| `AiInsightScreen` | `ui/screens/insight/AiInsightScreen.kt`: `AiInsightScreen` (Hilt entry), `AiInsightScreenContent` (top bar + theme dispatch), `AiInsightPixelDispatch` / `…LightDispatch` / `…ComicDispatch` (scrolling column, 16dp padding). |
| `AiInsightStateRouter` | A composable in `AiInsightScreen.kt`. It renders every state except a theme's own success card. |
| `AiInsightViewModel` | `ui/screens/insight/AiInsightViewModel.kt`. One `uiState: StateFlow<AiInsightUiState>` plus `isSimpleMode`. Used by the screen and by the Home card (each gets its own instance). |
| Header | Inline in each theme view: `PixelAiInsightView` (beige card, 44dp avatar, title + status line), `ComicAiInsightView` (burnt-orange panel, 52dp avatar, title + subtitle + "ISSUE #25" chip), `LightAiInsightView` (white card, 48dp avatar, title + subtitle). |
| Insight card | Per theme: `PixelSuccessCard`, `ComicSuccessView` (three speech bubbles + footer panel), `LightSuccessCard`. The shared `AiInsightCard` is the Home card's version. Each shows all three sections (summary, suggestion, encouragement) in full, then a footer with the time and a REFRESH / COOLDOWN button. |
| Empty, locked, disabled states | Already separate composables in `AiInsightStateComponents.kt` (900 lines): `AiInsightLoadingState`, `AiInsightRateLimitedState`, `AiInsightNotEnoughDataState` (empty), `AiInsightDisabledState` and `AiInsightSignInRequiredState` (both through the private `AiInsightLockedState`), `AiInsightErrorState` (also "not set up"), `AiInsightCapReachedState`. |
| `FeaturedNavButton` | In `PixelBottomNavBar.kt` (Pixel and Light) and `ComicFeaturedNavButton` in `ComicBottomNavBar.kt`. **The featured, round button is the Leaderboard** (`NavItem.isFeatured`), not the AI Coach. 48dp circle in Pixel/Light, 44dp disc with a hard shadow in Comic, icon only, no label. |

### Where the height goes

On the AI Coach screen with an insight (Pixel; the others are similar or taller):

| Part | About |
| --- | --- |
| Header banner (12dp padding, 44dp avatar, title + status line) | 70dp |
| Gap | 14dp (Comic and Light 16dp) |
| Card: title row, three tagged sections each in its own padded box (10–16dp padding, 12–14dp gaps), footer with a 36dp button, cooldown line | 450–650dp, depending on the insight's length |
| Outer padding | 16dp each side |

A 6-inch phone has roughly 560–600dp between the top bar and the bottom bar, so a typical insight needs scrolling. The locked states (signed out, disabled) are a centred 28–32sp icon, a heading, a paragraph and a button with 20–24dp padding, about 220dp for one sentence of information.

### Older insights

Every generated insight is a row in `insight_cache`. `InsightCacheRepository.clearExpired` exists but nothing calls it, so the table holds every insight ever generated (at most 4 a day). Only the newest is shown today. C12 shows a few older ones; trimming the table is left for later.

### Tooling

- No screenshot-testing library (Paparazzi, Roborazzi) is set up. C18 uses Robolectric Compose tests, which measure and assert on the rendered layout in each theme, plus `@Preview`s.
- The app had no `res/values/strings.xml`: all text was inline. New text from this phase goes in string resources, per the plan.
- Reduce motion: `LocalReduceMotion` (theme) is what animations check.

### Tests that pin current behaviour

- `LeaderboardNavButtonTest`: the bar's five tabs, the Leaderboard opening from each theme's bar, its selected state. The featured flag check changes when the round button goes (C20).
- `AiCoachSignInUiTest`, `AiErrorHeadingTest`: the signed-out state's SIGN IN action and the error headings.
- `SimpleModeAiInsightInteractionUiTest`: the Simple Mode wording (it models the strings itself).

## Plan, adapted

| Commit | What it does here |
| --- | --- |
| C02 | `CoachDimens` holds the screen's spacing, padding and font sizes (same values) |
| C03–C05 | The header, the insight card dispatch and the locked and empty states move to their own composables and files |
| C06 | `CoachDisplayState` (compact, default true; what's expanded) in the ViewModel |
| C07–C09 | Smaller padding and gaps, a one-line header with an inline chip, a 40dp avatar |
| C10–C12 | A 3-line preview with an expand toggle (animated), and older insights as one-line rows that open one at a time |
| C13 | The refresh button moves into the header as a compact icon + label |
| C14–C15 | The round Leaderboard button becomes a normal tab in both bars |
| C16 | Signed out, disabled and not set up become one short row with its action |
| C17 | 48dp touch targets on everything that shrank |
| C18–C19 | Compose tests in Pixel, Comic, Light and Simple Mode (including a 6-inch fit check), ViewModel tests |
| C20 | Remove what the compression left unused |

## Phase 2: the three-line menu (C21–C45)

A three-line icon at the top left of the main screens opens a side drawer with Namaz (a placeholder until Phase 4), Account, Settings and shortcuts to History and the Leaderboard. The bottom tabs stay as they are. The drawer is spaced generously, so the screens get room rather than more buttons.

### Map (C21)

| Piece | Where it is | Where the menu attaches |
| --- | --- | --- |
| App frame | `MainActivity`: `Scaffold` with the bottom bar (`PixelBottomNavBar`), the notification banner and `PixelNavHost` in a column | The drawer (`ModalNavigationDrawer`) wraps this `Scaffold`, so it slides over the screen and the bottom bar. It's open to swipes only on the five tabs. |
| Routes | `Screen` in `ui/navigation/NavGraph.kt` (`home`, `tasks`, `leaderboard`, `stats`, `profile`, `settings`, `account`, `task_history`, `ai_insight`, …). No route string is written anywhere else. | C22 moves `Screen` into its own `Routes.kt`; `Screen.Namaz` comes with C33. |
| Home | `TodayScreen`: a `titleLarge` line ("⚔️ TODAY'S DASHBOARD", from `TaskTerminology`) inside the list, then the COMIC MODE and REFRESH buttons | The icon goes left of the title (C28). |
| Tasks | `TasksContent`: no title row at all, just the list and the add button | A title row with the icon (C29). |
| Leaderboard | `PixelTopAppBar("LEADERBOARD")`, whose left slot holds a back arrow only when opened from Stats | The icon takes that slot when there's no back arrow (C30). |
| Stats, Profile | A `titleLarge` line at the top of a scrolling column | Left of the title (C30). |
| AI Coach | `PixelTopAppBar` with a back arrow: it's opened from Home, not a tab | Its left slot is the way back, so the menu isn't added there (the plan lists it; noted in C30). |

The screens reach the drawer through a `CompositionLocal` that `MainActivity` provides, so previews and tests without the drawer simply don't show the icon.

### Result (C22–C45)

- **Where it opens:** the three-line icon (`MenuTopBarAction`) sits left of the title on Home, Tasks (which got a title row: 📜 QUEST LOG, or 📋 ALL TASKS in Simple Mode), the Leaderboard (when it's a tab; opened from Stats it keeps its back arrow), Stats and Profile. On those five tabs a swipe in from the left edge opens it too (`Routes.Tabs`). Back, the close button, a tap outside, a swipe, or choosing an entry closes it.
- **What's in it** (`MenuItems.all`, `ui/menu/`):
  - a header with the player's avatar and name and, outside Simple Mode, LVL · XP and a bar of the perfect days toward the next level;
  - 🕌 Namaz Tracker (a placeholder screen until Phases 3–4; shown while `SettingsRepository.namazAvailable`, on by default);
  - 📜 Quest History (Task History in Simple Mode), 🏆 Leaderboard (opens like its tab), 👤 Account, ⚙️ Settings.
- **Breathing room:** 300dp wide, 24–28dp around the header, 56dp entries (taller than the 48dp minimum) with 16dp between icon and label, icons lined up with the header.
- **Per theme** (`MenuStyle`): Pixel has square corners, a pixel border, the retro font in capitals and a bordered block for the current entry. Comic has a black ink border, rounded corners, the comic font and a coral pill with a flat shadow. Light, and Simple Mode in any theme, are a plain list with a soft tint.
- **Remembered:** the current entry follows the screen the player is on and survives rotation and the app being stopped in the background (`MenuViewModel`, saved state).
- **Accessibility:** TalkBack hears "Menu open" / "Menu closed", entries read as their labels (not their emoji), the name is a heading, and the icons say Open menu and Close menu.
- **Tests:** `MenuStateTest` (entries, visibility, wording, the ViewModel) and `MenuDrawerUiTest` (icon → menu → Namaz in each theme, Back, the close button, Simple Mode, Namaz unavailable).
- **Not changed:** the bottom tabs, Home's cards, and how Settings and Account were reached before (Profile → APP SETTINGS still works).
