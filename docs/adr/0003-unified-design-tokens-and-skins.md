# ADR 0003: Centralized Design Tokens and Dual Skins (Comic & Pixel)

- **Status**: Accepted
- **Date**: 2026-10-22
- **Deciders**: PixelQuest Core Engineering

## Context
PixelQuest features two distinct visual aesthetics:
1. **Comic**: Bold ink borders, rounded card corners, pop shadows, and energetic typography.
2. **Pixel**: Retro 8-bit aesthetic, square corners, stepped shadows, and pixelated retro typography.

Hardcoding styling values, ad-hoc dp padding, or fragmented color values creates visual inconsistencies and breaks re-skinning.

## Decision
1. **Centralized Tokens**: Consolidate all layout dimensions into `PqDimens`, color definitions into `PqColors`, typography into `PqType`, and skin definitions into `PqSkin`.
2. **Dynamic Skinning**: A single setting toggles between `PqSkin.Comic` and `PqSkin.Pixel` dynamically across all screens without reloading or restarting the app.
3. **Component Encapsulation**: Shared UI components (`PqTopBar`, `PqCard`, `PqButton`, `PqListRow`, `PqSegmented`) adapt automatically based on the active `PqSkin`.

## Consequences
- Single point of change for spacing, padding, corner radii, and color palettes.
- Re-skinning affects 100% of the UI deterministically with zero manual overrides per screen.
