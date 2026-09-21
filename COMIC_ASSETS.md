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
