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
