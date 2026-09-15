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


