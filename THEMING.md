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
