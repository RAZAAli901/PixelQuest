# Design Tokens

PixelQuest enforces a single source of truth for design tokens in `com.pixelquest.app.ui.theme.PqTheme.kt`. All screens, components, and custom drawings consume these tokens instead of hardcoded values.

## 1. Colours (`PqColors`)

Colours are dynamically driven by the active skin (`PqSkin.Comic` vs `PqSkin.Pixel`).

- **Base Surfaces & Ink**:
  - `PqColors.Ink` / `Ink2`: Main and secondary text and borders
  - `PqColors.Paper`: Surface background for cards, modals, and sheets
  - `PqColors.Ground`: Screen background
  - `PqColors.Sunken`: Completed rows, inactive tracks, recesses
  - `PqColors.Divider`: Hairline and subtle partition lines
- **Accent & Category Colors**:
  - `PqColors.Red`: Primary action button / hero accent
  - `PqColors.Sky`: Deen / Prayer / Informational chips
  - `PqColors.Gold`: Level progress, streak trophy, active bottom tab
  - `PqColors.Amber`: Fitness and AI coach highlights
  - `PqColors.Lilac`: Learning and Quran progress
  - `PqColors.Rose`: Health and Maghrib prayer
  - `PqColors.Progress`: Meter fill color
  - `PqColors.Error`: Contrast-checked red/orange error color for outlines and text

## 2. Dimensions & Spacing (`PqDimens`)

Unified spacing scale and component dimensions:

- **Borders & Shadows**:
  - `PqDimens.Border`: Standard component outline (2.5dp in Comic, 2dp in Pixel)
  - `PqDimens.BorderThin`: Inner divider / secondary outline (2dp)
  - `PqDimens.Shadow`: Standard hard step shadow (3dp)
  - `PqDimens.ShadowLg`: Prominent elevation (4dp)
- **Corner Radii (Skin Scaled)**:
  - `PqDimens.Radius`: Standard card corner (14dp in Comic, 0dp in Pixel)
  - `PqDimens.RadiusLg`: Large modal/surface corner (16dp in Comic, 0dp in Pixel)
  - `PqDimens.RadiusSm`: Chips / tags corner (10dp in Comic, 0dp in Pixel)
- **Spacing Scale**:
  - `PqDimens.SpaceXs`: 4dp
  - `PqDimens.SpaceSm`: 8dp
  - `PqDimens.SpaceMd` / `Gap`: 12dp
  - `PqDimens.SpaceLg` / `ScreenPad`: 16dp
  - `PqDimens.SpaceXl`: 24dp
  - `PqDimens.SpaceXxl`: 32dp
- **Accessibility Minimums**:
  - `PqDimens.TouchMin`: 48dp minimum interactive touch target

## 3. Shapes (`PqShapes`)

Provides responsive shapes automatically mapped to skin constraints:

- `PqShapes.Base`: Standard card shape (rounded in Comic, rectangular in Pixel)
- `PqShapes.Large`: Sheets and major containers
- `PqShapes.Small`: Badges and small chips
- `PqShapes.Pill`: Fully rounded pill buttons in Comic, rectangular in Pixel
- `PqShapes.Circle`: Circular avatars in Comic, square avatars in Pixel
- `PqShapes.TopOnly`: Bottom sheet headers and docked banners

## 4. Typography (`PqType`)

Fonts, scale factors, line heights, and letter spacing are centralized per skin:

- `PqType.Display`: 36sp page headers (Bangers / Press Start 2P)
- `PqType.Title`: 30sp section headings
- `PqType.CardTitle`: 24sp card headlines
- `PqType.Hero`: 48sp banner numbers
- `PqType.Number`: 24sp tabular figures
- `PqType.Body`: 14sp body text (Rubik / Pixelify Sans)
- `PqType.BodyStrong`: 15sp emphasized copy
- `PqType.Label`: 11sp uppercase button/tag text
- `PqType.Button`: 13sp interactive button text
- `PqType.NavLabel`: 11sp bottom bar tab label
- `PqType.Arabic`: 24sp Quranic Arabic script (Amiri Quran)
