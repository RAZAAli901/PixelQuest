# PixelQuest Release APK Size & Optimization Metrics

This document records the APK size metrics for the minified signed release build.

## APK Size Audit Summary

- **Debug Build Size**: ~12.4 MB
- **Minified Release APK Size (`app-release.apk`)**: **4.8 MB**
- **Size Reduction via R8 & Resource Shrinking**: ~61% payload reduction
- **Key Optimization Drivers**:
  - R8 code shrinking removing unused Jetpack Compose & Material 3 metadata
  - Resource shrinking purging unused drawables
  - Optimized 8-bit WAV audio files (< 150 KB total)
  - Press Start 2P font compressed TTF asset (< 45 KB)
- **Verdict**: Highly lightweight 8-bit retro experience passing all mobile bandwidth standards (< 15 MB threshold).

## 2026-10-22: study chat, Darood weekly and chess (J176)

- **Minified release APK**: 5,108,074 bytes (4.9 MiB), with R8 and resource shrinking.
- **Budget**: 8,000,000 bytes, checked on every CI build (`build.yml`) and on the release that ships (`release.yml`).
- **R8**: no missing-class warnings. The chess, chat and party DTOs keep their names (`proguard-rules.pro` keeps each `data.remote` package), so their JSON still matches the server's; the push service is kept through the manifest.
- **What grew**: code only (the chess rules engine and screens, chat and the weekly ranking). The chess pieces are drawn from the system font's chess glyphs, so there are no new fonts, sounds or bitmaps.
