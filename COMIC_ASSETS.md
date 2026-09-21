# PixelQuest — Comic Book UI Asset Strategy & Specification (`COMIC_ASSETS.md`)

## 1. Asset Strategy & Rationale (Day 16 Decision Follow-through)

On Day 16, PixelQuest established an explicit architectural rule:
> *Light mode succeeded by dynamically tinting existing 8-bit assets. However, Comic mode represents a distinct comic pop-art visual aesthetic inspired by Nitnode and vintage printed comics. It requires genuinely new art and styling rather than simply tinting retro pixel-art 9-patches.*

This document specifies the exact asset treatments, visual states, and components required for Days 21–23.

---

## 2. Component Asset Specifications (Step 25)

### 2.1 Comic Button States
Classic 8-bit buttons rely on Kenney 9-patch PNGs (`pixel_button_*.png`). Comic buttons require a bold pop-art physical tactile feel:
- **Default State**:
  - Fill: Vibrant solid color fill (`CoralRed` for CTA, `BurntOrange`, `SkyBlue`, `Lavender`, or White).
  - Outline: Solid black ink border (`2.5dp` stroke, `RoundedCornerShape(10.dp)`).
  - Shadow: Hard-edged, flat, solid black drop shadow offset `(4.dp, 4.dp)` down-right.
  - Typography: Heavyweight bold text (Black ink or White on red).
- **Pressed State**:
  - Tactile physical depression: Button surface shifts `+2.dp` or `+3.dp` along the X and Y axes toward the shadow.
  - Shadow clearance collapses from `4.dp` to `1.dp` or `2.dp`, simulating physical comic button press.
  - Haptic feedback: Preserves `PixelHaptics.performLightTap()`.
  - Audio: Preserves `sfx_click.wav`.
- **Disabled State**:
  - Fill: Soft disabled paper gray (`#E0DDD5`).
  - Outline: Subdued black border (`1.5dp` stroke, `alpha = 0.5f`).
  - Shadow: Flat zero-elevation shadow or collapsed `1.dp` shadow.

### 2.2 Panel Backgrounds
Comic panels replace stepped pixel 9-patches (`pixel_panel_border.png`) with clean, graphic comic cells:
- **Surface Panels**: Crisp white background (`#FFFFFF`) with `2.5dp` black ink border and `4dp` offset drop shadow.
- **Container Variants**: Three signature Nitnode reference container fills:
  - Burnt Orange (`#F0A868`)
  - Sky Blue (`#8ECAE6`)
  - Lavender (`#B8A4D4`)
- **Speech Bubbles & Callouts**: Rounded speech balloons with pointer tails for dialogs and tutorial prompts.

### 2.3 Category Icon Treatment
Pixel mode uses 16x16 pixel sprites (`ic_cat_*.png`). Comic mode requires:
- High-contrast black contour silhouettes with saturated cel-shaded fills.
- Rendered inside circular or rounded-corner comic badge frames with solid black borders.

### 2.4 Avatar Frame Accents
Avatar sprites are framed with comic pop-art flair:
- **Tier Framing**:
  - Bronze / Default: 2.5dp solid black comic frame with 10dp corner radius.
  - Silver: Cool cyan/sky-blue inner mat with black outer frame.
  - Gold: Saturated comic gold frame with starburst action badge.
- **Avatar Badges**: High-contrast star or exclamation badge ("POW!", "LVL UP!").

---

## 3. Days 21–23 Component-by-Component Implementation Checklist (Step 26)

This checklist enumerates every component Days 21–23 will adapt to the Comic Book UI Mode:

### 3.1 Core Action & Panel Components (Day 21 Focus)
- [ ] **`ComicButton`**:
  - [ ] Primary CTA variant (Coral Red `#FF5A4E` fill, 2.5dp black border, 4dp flat black drop shadow, black bold text).
  - [ ] Secondary container variants (Burnt Orange, Sky Blue, Lavender).
  - [ ] Outlined / Surface variant (White surface with black border and shadow).
  - [ ] Interactive press state: +2dp XY translation with collapsed 2dp shadow and haptic click.
  - [ ] Disabled state: Desaturated gray `#E0DDD5` fill with 0.5f alpha.
- [ ] **`ComicPanel`** (Foundation created on Day 20):
  - [ ] Full screen integration into `TodayScreen`, `TasksScreen`, `StatsScreen`, `ProfileScreen`.
  - [ ] Cyclical container coloring (`containerForIndex(index)`).
- [ ] **`ComicDialog`**:
  - [ ] Speech-bubble callout header with comic font.
  - [ ] Action buttons styled with `ComicButton`.
  - [ ] Clean white panel surface with 12dp rounded corners and solid black outline.

### 3.2 Progression, Avatars & Feedback (Day 22 Focus)
- [ ] **`ComicAvatarFrame`**:
  - [ ] Solid black 2.5dp outline with 10dp corner radius.
  - [ ] Bronze, Silver, Gold tier badge highlights.
  - [ ] Comic star burst badge for level milestones.
- [ ] **`ComicXpBar`**:
  - [ ] Thick black container border with recessed track.
  - [ ] Diagonal striped comic energy fill pattern.
  - [ ] Level badge rendered as an energetic comic star badge.
- [ ] **`ComicDailyProgressRing`**:
  - [ ] Action comic badge banner ("POW!", "PERFECT DAY!").
  - [ ] High-contrast progress arc with comic border outline.
- [ ] **`ComicQuestCard` (`TodayQuestCard.kt`)**:
  - [ ] Comic mission card with cycling burnt orange / sky blue / lavender container fills.
  - [ ] Quick-complete action stamp ("DONE!", "POW!").

### 3.3 Navigation & Data Displays (Day 23 Focus)
- [ ] **`ComicBottomNavBar`**:
  - [ ] Divided comic strip panel layout with 2dp black divider lines.
  - [ ] Active tab indicated by vibrant coral-red pill chip.
- [ ] **`ComicDaySelector` & `ComicFilterChips`**:
  - [ ] Comic tag stickers with 2dp black borders and 3dp mini drop shadows.
- [ ] **`ComicTextField`**:
  - [ ] Comic dialogue speech input container with clean sans font and black border.
- [ ] **`ComicHeatmapCell` & `ComicBarChart`**:
  - [ ] Pop-art color ramp and skyscraper comic pillars with black borders.
- [ ] **Full Screen Theme Integration & Gate Release**:
  - [ ] Connect `ThemeMode.Comic` across all app screens.
  - [ ] Remove "Coming Soon" gate in `ThemeSelectionCard.kt`.

---

## 4. Architectural Decision: Compose-Drawn Shapes vs. Raster PNG Assets (Step 27)

### 4.1 The Core Decision: Adopt Compose-Drawn Vector Shapes
We formally decide that **Comic mode components are built using Compose-drawn vector shapes (`comicBorder`, `comicDropShadow`, `RoundedCornerShape`) rather than static raster PNG assets**.

### 4.2 Key Rationale
1. **Pristine Geometric Fidelity**:
   The Nitnode reference style features unblurred solid black strokes and sharp offset drop shadows. In Compose, drawing these procedurally ensures mathematical crispness without 9-patch scaling artifacts, blurry corners, or anti-aliasing fuzziness.
2. **Zero APK Bloat**:
   Using procedural modifiers adds **0 bytes of PNG raster weight** to the APK, avoiding multiple density directories (`drawable-xhdpi`, `drawable-xxhdpi`, etc.).
3. **Infinite Palette Flexibility**:
   A single `ComicPanel` or `ComicButton` composable can effortlessly render in Coral Red, Burnt Orange, Sky Blue, Lavender, or White by passing a color token. Raster assets would require dozens of sliced 9-patches for each color and state.
4. **Dynamic Press Physics**:
   Compose enables fluid, responsive press animations (translating the button face `+2dp` into the shadow while collapsing shadow offset) that feel tactile and physical.
5. **Role for Raster Art**:
   Raster PNG assets are reserved strictly for genuinely illustrated elements (e.g. multi-layered character illustrations or complex comic burst stickers), if any are added in later days.

