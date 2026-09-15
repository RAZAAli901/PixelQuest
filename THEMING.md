# PixelQuest — Theming Architecture Specification (`THEMING.md`)

## 1. System Dark Mode Interaction & "Follow System" Decision (Revisiting Day 11)

### 1.1 Background & Day 11 Context
On Day 11, PixelQuest established an explicit design decision: the app was locked to its iconic retro dark arcade palette (`PixelQuestTheme`) regardless of the Android OS system dark/light configuration. At that stage in development, only a single dark pixel art aesthetic existed, and allowing the OS to force Android standard light themes would have broken contrast and degraded the pixel art presentation.

### 1.2 Day 16 Architectural Revisit
With the start of the Days 16–30 extension, PixelQuest introduces a multi-theme architecture:
- **Pixel Mode** (Retro Dark Arcade, canonical Day 1–15 experience)
- **Light Mode** (Clean productivity theme arriving Day 17)
- **Comic Mode** (Bold pop-art theme arriving Days 20–23)

Because a fully-realized Light theme is arriving on Day 17, locking the app exclusively to dark mode is no longer necessary or user-friendly. Modern mobile users expect system-level coordination with sunrise/sunset schedules or battery saver dark-mode triggers.

### 1.3 The Architectural Decision: Adopt "Follow System"
PixelQuest formally adopts a 4th theme configuration: `ThemeMode.System` ("Follow System").

#### Theme Modes:
1. **`ThemeMode.Pixel`**: Explicitly locks to classic retro dark arcade palette and CRT scanlines.
2. **`ThemeMode.Light`**: Explicitly locks to crisp light theme.
3. **`ThemeMode.Comic`**: Explicitly locks to bold comic pop-art theme (coming soon).
4. **`ThemeMode.System`**: Dynamically evaluates the Android OS system night mode:
   - If `isSystemInDarkTheme() == true` -> Resolves to `ThemeMode.Pixel`.
   - If `isSystemInDarkTheme() == false` -> Resolves to `ThemeMode.Light`.

#### Default Policy:
- Default installation preference remains `ThemeMode.Pixel` to preserve the signature arcade branding upon fresh installs.
- Users can switch to `Follow System`, `Pixel`, or `Light` at any time from `Settings -> Theme Selection`.
- Explicit selections (`Pixel`, `Light`, `Comic`) act as strict overrides that completely decouple from OS state.

## 2. Component-Level Theme Compatibility Audit

### 2.1 `PixelButton` Audit
- **Current State**:
  - Backgrounds: Hardcoded 9-patch drawables (`R.drawable.pixel_button_yellow`, `pixel_button_blue`, and pressed variants).
  - Text Color: Hardcoded ternary (`Color.Black` for yellow, `Color.White` for blue).
  - Press Animation: 2.dp downward offset on press with `alpha = 0.5f` on disabled.
  - Interaction: Emits `performLightTap` haptic and `playClickSound()` via `LocalSoundManager`.
- **Theme-Aware Adaptations (Days 17 & 20–23)**:
  - **Light Mode (Day 17)**: Dynamic color tint filter (`ColorFilter.tint(colors.primary)`) applied over the button base shape, and text color resolved dynamically from `LocalAppColorScheme.current.onPrimary` / `onSecondary`.
  - **Comic Mode (Days 20–23)**: Comic theme requires thick 3–4px hard black borders, stark halftone or saturated solid fills, and angled drop-shadow offsets instead of 8-bit stepped pixel corners.
- **Theme-Agnostic Invariants**:
  - Haptic feedback and click SFX triggers remain consistent across all themes.
  - `Role.Button` accessibility semantics and 48dp minimum touch target bounding.

### 2.2 `PixelCard` / `PixelPanel` Audit
- **Current State**:
  - Backgrounds: Hardcoded 9-patch assets (`R.drawable.pixel_panel_border`, `pixel_panel_blue`, `pixel_panel_beige`).
  - Stretches background image via `ContentScale.FillBounds` behind arbitrary composable content.
  - Content padding configurable with 16.dp default.
- **Theme-Aware Adaptations (Days 17 & 20–23)**:
  - **Light Mode (Day 17)**: `pixel_panel_border` has an opaque dark charcoal fill (`#0D0D15`) that severely compromises light mode legibility. Day 17 requires a light card background (`surface = Color(0xFFFFFFFF)`) paired with crisp border outlines (`surfaceVariant = Color(0xFFE1E4E8)`) or tinted pixel-frame assets.
  - **Comic Mode (Days 20–23)**: Comic panels emulate printed comic cells—white/newsprint background fills, thick 3px solid black outlines (`#000000`), angled or dot-screen drop-shadows, and slight skew/tilt variations.
- **Theme-Agnostic Invariants**:
  - Box layout hierarchy, `contentPadding` contract, and composable slot container structure remain constant.

### 2.3 `PixelDialog` Audit
- **Current State**:
  - Implements standard Compose `Dialog(onDismissRequest)`.
  - Container: Delegated entirely to `PixelCard(variant = PixelPanelVariant.BORDER)`.
  - Title: Accurately binds to `MaterialTheme.colorScheme.primary`.
  - Action Buttons: Uses standard `PixelButton` instances (`BLUE` for dismissal/cancel, `YELLOW` for confirmation).
- **Theme-Aware Adaptations (Days 17 & 20–23)**:
  - Inherits container styling directly from `PixelCard`: once `PixelCard` adapts to theme-aware backgrounds in Day 17, `PixelDialog` automatically renders light/comic frames without architectural breakage.
  - In Comic Mode (Days 20–23): Option to render speech-bubble or explosive comic callout headers ("WARNING!", "HEROIC DECISION!").
- **Theme-Agnostic Invariants**:
  - Dialog window scrim, back button dismissal mechanics, title spacing, and button arrangement.

### 2.4 Avatar Display, XP Bar & Progress Ring Audit
- **`PixelAvatarFrame`**:
  - *Current State*: Frame background hardcoded to `PixelSurfaceDark` (`#1A1A2E`). Border color dynamically calculated via `AvatarTierCalculator`.
  - *Theme Adaptations*: In Light mode, container background must read `LocalAppColorScheme.current.surface` (`#FFFFFF`) to avoid dark boxes on light screens. In Comic mode, add high-contrast 3px comic outline with action drop-shadow.
  - *Invariants*: Tier calculation logic and badge emoji positioning.
- **`PixelXpBar`**:
  - *Current State*: Track background hardcoded to `PixelSurfaceDark`, level badge to `PixelGold`, progress fill to `PixelGreen`, and label text to `Color.White`.
  - *Theme Adaptations*: In Light mode, track must read `colors.surfaceVariant` (`#E1E4E8`), fill `colors.tertiary` (`#2E7D32`), level badge `colors.primary`, and label text `colors.onSurface`. In Comic mode, add pop-art striped hatch pattern and bold black borders.
  - *Invariants*: Progress ratio calculation, 600ms easing animation curve, and screen reader percentage announcement semantics.
- **`PixelDailyProgressRing`**:
  - *Current State*: Uses `PixelCard(variant = PixelPanelVariant.BEIGE)`. Title and percentage text hardcoded to `PixelGold`/`PixelTextWhite` and `PixelGreen`/`PixelCyan`.
  - *Theme Adaptations*: Delegate colors to `LocalAppColorScheme.current.primary` / `onSurface`. Card background inherits theme-aware `PixelCard` in Day 17. In Comic mode, "PERFECT DAY!" renders as an action comic sticker/burst.
  - *Invariants*: Milestone evaluation (`progress >= targetThreshold`), column spacing, progress ratio.

## 3. Comprehensive Component Theme-Awareness Audit Matrix

| Component | Currently Theme-Aware? | Hardcoded Dependencies | Day 17 (Light Mode) Scope | Days 20–23 (Comic Mode) Scope |
| :--- | :--- | :--- | :--- | :--- |
| `PixelButton` | Partial | Drawables (`pixel_button_*`), `Color.Black`/`White` text | Dynamic tinting via `ColorFilter.tint(primary)`, text from `onPrimary`/`onSecondary` | Bold 3px black stroke, drop-shadow offset, comic action burst style |
| `PixelCard` / `PixelPanel` | Partial | 9-patch assets with `#0D0D15` background | Light panel variant or tinted border with `#FFFFFF` surface fill | 3px solid black border with newsprint paper fill and angled drop shadow |
| `PixelDialog` | Partial | Uses `PixelCard` & `PixelButton` | Automatic inheritance from `PixelCard` light surface | Comic caption header styling and speech-bubble callouts |
| `PixelAvatarFrame` | Partial | Background hardcoded to `PixelSurfaceDark` | Background to read `colors.surface` (`#FFFFFF`) | High-contrast black outlines and comic tier star badge |
| `PixelXpBar` | Partial | Hardcoded `PixelGold`, `PixelSurfaceDark`, `PixelGreen` | Track reads `surfaceVariant`, fill `tertiary`, badge `primary` | Pop-art striped/diagonal fill with thick black border |
| `PixelDailyProgressRing` | Partial | Hardcoded text colors, uses `PixelCard` | Colors bound to `colors.primary` / `onSurface` | Action comic badge banner ("POW!", "PERFECT DAY!") |
| `PixelBottomNavBar` | Partial | Background `PixelBackgroundDark`, border `PixelSurfaceBorder` | Container `surface`, active tab `primary`, inactive `onSurfaceVariant` | Comic panel navigation strip with divider lines |
| `PixelTextField` | Partial | Border `PixelSurfaceBorder`, text `PixelTextWhite` | Border `surfaceVariant`, text `onBackground`, placeholder `onSurfaceVariant` | Comic dialogue speech input box |
| `PixelDaySelector` | Partial | Selected `PixelGold`, chip `PixelSurfaceDark` | Selected `primaryContainer`, unselected chip `surfaceVariant` | Comic weekday badge strip |
| `PixelFilterChips` | Partial | Hardcoded `PixelSurfaceDark`, `PixelGold` | Theme tokens `primary`, `surfaceVariant`, `onSurface` | Comic category tag badges |
| `PixelHeatmapCell` | Yes | Reads level opacity levels directly | Adapt baseline empty cell color to light slate (`#E1E4E8`) | Halftone dot density matrix |
| `PixelBarChart` | Partial | Grid lines and bar colors | Grid lines `surfaceVariant`, bars `secondary` / `tertiary` | Comic skyscraper bar pillars |
| `TodayQuestCard` | Partial | Uses `PixelCard`, hardcoded status colors | Colors bound to `LocalAppColorScheme.current` | Comic mission briefing card |
| `PixelCrtOverlay` | Yes | Gated strictly to `ThemeMode.Pixel` | Bypassed automatically in Light mode | Bypassed automatically in Comic mode |




