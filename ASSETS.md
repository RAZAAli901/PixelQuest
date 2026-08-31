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
- `ic_home.png`: 16x16 pixel-art home icon
- `ic_tasks.png`: 16x16 pixel-art quest log icon
- `ic_stats.png`: 16x16 pixel-art level stats icon
- `ic_profile.png`: 16x16 pixel-art hero avatar icon

### Task Category Icons (`res/drawable/`)
- `ic_cat_fitness.png`: 16x16 pixel-art fitness category icon
- `ic_cat_health.png`: 16x16 pixel-art health category icon
- `ic_cat_learning.png`: 16x16 pixel-art learning category icon
- `ic_cat_chores.png`: 16x16 pixel-art chores category icon
- `ic_cat_other.png`: 16x16 pixel-art custom/other category icon

### Difficulty Tier Icons (`res/drawable/`)
- `ic_diff_easy.xml`: Green shield icon for Easy difficulty
- `ic_diff_medium.xml`: Blue shield icon for Medium difficulty
- `ic_diff_hard.xml`: Orange shield icon for Hard difficulty
- `ic_diff_hardest.xml`: Red shield icon for Hardest difficulty

### Fonts (`res/font/`)
- `press_start_2p.ttf`: Open-source 8-bit retro font by CodeMan38 (SIL Open Font License) via Google Fonts

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
- **Haptics Toggle**: `isHapticsEnabled` setting in `SettingsRepository` and `SettingsScreen`.

