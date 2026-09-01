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
