# PixelQuest — Asset Attribution & Inventory

## Asset Pack Details
- **Pack Name**: Kenney UI Pack — Pixel
- **Author**: Kenney (Kenney.nl)
- **Source URL**: https://kenney.nl/assets/ui-pack-pixel
- **License**: Creative Commons Zero (CC0 1.0 Universal / Public Domain)

## Imported Files Inventory

### Buttons (`res/drawable/`)
- `pixel_button_blue.png`: Normal state 8-bit blue button
- `pixel_button_blue_pressed.png`: Pressed state 8-bit blue button
- `pixel_button_yellow.png`: Normal state 8-bit yellow/gold button
- `pixel_button_yellow_pressed.png`: Pressed state 8-bit yellow/gold button

### Panels (`res/drawable/`)
- `pixel_panel_blue.png`: Dark blue retro panel container background
- `pixel_panel_beige.png`: Parchment beige retro panel container background
- `pixel_panel_border.png`: Retro 8-bit gold/slate bordered panel container

### Progress Bars (`res/drawable/`)
- `pixel_bar_background.png`: Recessed dark progress bar track container
- `pixel_bar_green_fill.png`: Bright 8-bit green progress bar fill indicator

### Navigation Icons (`res/drawable/`)
16x16 pixel-art vector drawables in Pixel gold (`#F4C430`), drawn at 24dp:
- `ic_home.xml`: house
- `ic_tasks.xml`: checklist (also the notification small icon)
- `ic_stats.xml`: bar chart
- `ic_profile.xml`: person

### App and Notification Icons (UI v2)
Made for PixelQuest in Claude Design (docs/ui-v2.md):
- `mipmap-anydpi-v26/ic_launcher.xml`, `ic_launcher_round.xml`: the adaptive launcher icon, logo 1A (Sword Badge): `drawable/ic_launcher_foreground.xml`, a pixel sword, on `@color/pq_icon_bg`, with `drawable/ic_launcher_monochrome.xml` for themed icons. `mipmap/ic_launcher.xml` and `ic_launcher_round.xml` draw the same badge for Android 7.x, which has no adaptive icons
- `drawable/ic_stat_pixelquest.xml`: the status-bar icon, a white pixel sword
- `layout/notif_pq_big.xml`, `notif_pq_small.xml` and `drawable/notif_card_bg*.xml`: the notifications' card, in the Comic, gold and Pixel looks; their colours are in `values/pq_colors.xml`

### Task Category Icons (`res/drawable/`)
16x16 pixel-art vector drawables in Pixel gold (`#F4C430`):
- `ic_cat_fitness.xml`: dumbbell
- `ic_cat_health.xml`: heart with a medical cross
- `ic_cat_learning.xml`: open book
- `ic_cat_chores.xml`: broom
- `ic_cat_other.xml`: star

### Difficulty Tier Icons (`res/drawable/`)
16x16 pixel-art skulls that get scarier with difficulty:
- `ic_diff_easy.xml`: small green skull
- `ic_diff_medium.xml`: blue skull
- `ic_diff_hard.xml`: orange skull and crossbones
- `ic_diff_hardest.xml`: red flaming skull and crossbones

### Fonts (`res/font/`)
- `press_start_2p.ttf`: Open-source 8-bit retro font by CodeMan38 (SIL Open Font License 1.1) via Google Fonts
- `bangers_regular.ttf`: Bold heavyweight comic display font designed by Vernon Adams (SIL Open Font License 1.1) via Google Fonts / GitHub (`google/fonts`)
- `kalam_regular.ttf`: Handwritten/marker-style accent font designed by Indian Type Foundry (Lipi Raval, Jonny Pinhorn) (SIL Open Font License 1.1) via Google Fonts
- `kalam_bold.ttf`: Bold handwritten/marker-style accent font designed by Indian Type Foundry (SIL Open Font License 1.1) via Google Fonts
- `rubik_medium.ttf`, `rubik_semibold.ttf`, `rubik_bold.ttf`, `rubik_extrabold.ttf`: Rubik, by The Rubik Project Authors (SIL Open Font License 1.1), the Comic skin's text in UI v2 (docs/ui-v2.md). Google Fonts publishes it as one variable font (`google/fonts`, `ofl/rubik/Rubik[wght].ttf`, SHA-256 `1b3a7437...4e88`); each weight here is a static instance of it, cut with fontTools to Latin, Latin-1 and common punctuation, so Android 7 shows each weight. Its licence is in `licenses/rubik-OFL.txt`
- `pixelify_sans_medium.ttf`, `pixelify_sans_semibold.ttf`, `pixelify_sans_bold.ttf`: Pixelify Sans, by The Pixelify Sans Project Authors (SIL Open Font License 1.1), the Pixel skin's text in UI v2. Made the same way from `ofl/pixelifysans/PixelifySans[wght].ttf` (SHA-256 `9ba86cd0...89e8a5`). Its licence is in `licenses/pixelify-sans-OFL.txt`
- `amiri_quran.ttf`: Amiri Quran 1.003, the Quran reader's Arabic typeface, by The Amiri Project Authors (Khaled Hosny, Sebastian Kosch) (SIL Open Font License 1.1) via Google Fonts (`google/fonts`, `ofl/amiriquran`); its licence is in `licenses/amiri-quran-OFL.txt`

### Sound Effects (`res/raw/`)
- **Pack Name**: Kenney Interface Sounds / Retro Audio (Kenney.nl)
- **Source URL**: https://kenney.nl/assets/interface-sounds
- **License**: Creative Commons Zero (CC0 1.0 Universal / Public Domain)
- `sfx_click.wav`: 8-bit short button tap sound
- `sfx_complete.wav`: Positive 8-bit task completion chime
- `sfx_missed.wav`: Negative 8-bit task missed buzz
- `sfx_levelup.wav`: 8-bit level-up fanfare chime

### Avatar Sprites (`res/drawable/`)
- **Pack Name**: Kenney Toon Characters / Custom Pixel Avatars
- **Source URL**: https://kenney.nl/assets
- **License**: Creative Commons Zero (CC0 1.0 Universal / Public Domain)
- `avatar_hero.png`: 32x32 8-bit hero avatar sprite
- `avatar_mage.png`: 32x32 8-bit mage avatar sprite
- `avatar_rogue.png`: 32x32 8-bit rogue avatar sprite
- `avatar_warrior.png`: 32x32 8-bit warrior avatar sprite
- `avatar_paladin.png`: 32x32 8-bit paladin avatar sprite
- `avatar_ranger.png`: 32x32 8-bit ranger avatar sprite

## Day 7 Consistency Audit Summary
- **Visual Audit**: Confirmed 100% compliance across pixel grid resolution (16x16 / 32x32) and retro color palette. Zero default Material icons remain in active UI flows.

## Day 11 Audio & Visual Polish Inventory
- **Navigation SFX**: `playNavSound()` (0.35f volume, 1.2f pitch shift using `sfx_click.wav`) gated behind sound settings toggle.
- **Quest Begin Chime**: `playQuestBeginSound()` (1.0f volume, 1.25f pitch fanfare using `sfx_levelup.wav`) for hero onboarding completion.
- **Streak Broken Audio**: `sfx_missed.wav` audio trigger connected to Day 5 `StreakBrokenBanner`.
- **Haptic Maps**: `PixelHaptics` mapping (Light Tap for buttons, Medium Warning for delete/reset confirm, Success Pattern for quick-complete & level-up celebration).
## Day 12 Final Asset Audit
- **Final Inventory Pass**: Verified all 30+ visual drawable PNG/XML assets, Press Start 2P font, 8-bit WAV sound effects, and launcher icons.
- **Verification Status**: 100% compliant with CC0 1.0 Universal / SIL Open Font License.

## Day 16 Multi-Theme Asset Plan (Days 17 & 20–23 Roadmap)
- **Day 17 (Light Mode) — Reused & Tinted Strategy**:
  - *No new raster PNG assets required*.
  - Existing pixel 9-patches and icons are dynamically tinted at runtime via `PixelThemeAssetFilter` and Compose `ColorFilter.tint(colors.primary)`.
  - Avoids APK size bloat while preserving retro 8-bit silhouettes against light `#F6F8FA` surfaces.
- **Days 20–23 (Comic Mode) — Genuinely New Art Strategy**:
  - *Requires dedicated new comic pop-art assets*:
    - 6 new comic-book style hero/villain avatar illustrations (halftone ink/cel-shaded style).
    - Comic card panel assets with thick 3px solid black outlines and 4px diagonal drop shadows.
    - Action burst button frames ("POW!", "LEVEL UP!", "QUEST DONE!").
## Day 17 Light Mode Runtime Tinting Architecture (Step 15)
- **Tinting Engine**: `PixelThemeAssetFilter` provides runtime `ColorFilter.tint(color, BlendMode.SrcAtop)` and theme-gated filters:
  1. **Category Icons (`ic_cat_*`)**:
     - *In Pixel Mode*: Shows the icons in Pixel gold (null filter).
     - *In Light Mode*: Dynamically tinted with `colors.primary` (`#B45309`) or `colors.onSurfaceVariant` (`#57534E`) in lists and selectors to achieve >= 4.5:1 contrast against light card surfaces.
  2. **Difficulty Tier Icons (`ic_diff_*`)**:
     - *In Pixel Mode*: Preserves saturated vector colors (Green/Blue/Orange/Red).
     - *In Light Mode*: Tinted with `colors.primary` (`#B45309`) when selected and `colors.onSurfaceVariant` (`#57534E`) when inactive, avoiding washed-out pastel fills.
  3. **Progress Bar Fill (`pixel_bar_green_fill.png`)**:
     - *In Pixel Mode*: Uses canonical arcade neon green (`#00E676`).
     - *In Light Mode*: Tinted with `colors.tertiary` (`#15803D`) providing rich meadow green contrast.
  4. **Avatar Sprites (`avatar_*.png`)**:
     - *Readability Confirmation*: Avatar sprites retain high-contrast 1px black contour silhouettes. When placed over `PixelTheme.colors.surface` (`#FFFFFF`), sprites remain crisp and legible without requiring artificial tinting.
  5. **Card Panels (`PixelCard`)**:
     - Light mode bypasses the dark-filled `#0D0D15` 9-patch `pixel_panel_border.png`, procedurally rendering an 8-bit stepped pixel border using `colors.pixelBorder` (`#292524`) with clean 2dp drop shadow.
- **APK Footprint Impact**: Zero additional bytes added to the APK package; 100% runtime procedural shading and Compose vector tinting.

## Day 20 Comic Mode Vector Shape Architecture (Steps 27–29)
- **Architectural Decision**: Comic Mode components (buttons, panels, cards, dialogue frames) are built 100% using **procedural Compose-drawn vector shapes** rather than static raster PNG assets:
  - `comicBorder`: Solid black ink line (`2.5dp`), `RoundedCornerShape(10.dp)`.
  - `comicDropShadow`: Flat, hard-edged solid black duplicated offset shape (`4.dp` offset down-right).
  - `ComicPanel`: Layout-safe box container reserving shadow clearance (`4.dp`).
  - `ComicButton`: Tactile +3dp XY translation with collapsible shadow and integrated haptics/audio.
- **Proof-of-Concept Validation**: `ComicButton.kt` successfully replicates the Nitnode reference button with zero raster 9-patch assets, infinite color flexibility, and 0 APK bloat.
- **Raster Scope**: Raster PNGs are reserved strictly for future complex multi-layer character illustrations, if desired.

<!-- Audit Status: Fully verified for v1.1.0, Day 16 multi-theme architecture, Day 17 Light Mode, and Day 20 Comic Mode Foundation -->


