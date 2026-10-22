# PixelQuest Versioning Policy

PixelQuest strictly adheres to [Semantic Versioning 2.0.0 (SemVer)](https://semver.org/) and Android Google Play `versionCode` conventions.

---

## 1. Semantic Version Format (`MAJOR.MINOR.PATCH`)

Given a version number `MAJOR.MINOR.PATCH` (e.g., `1.8.0`):

1. **MAJOR version** increments when incompatible API or breaking database/storage changes are made that require manual data migration or breaking communication changes.
   - *Example*: Upgrades requiring account reset or incompatible wire protocols.
2. **MINOR version** increments when functionality is added in a backwards-compatible manner, or major architectural overhauls (such as Phase S Security Hardening and Phase Q Clean Architecture) are merged.
   - *Example*: Adding Deen hub, Study parties, Chess, or Clean Architecture refactoring.
3. **PATCH version** increments when backwards-compatible bug fixes, minor performance improvements, or theme tweaks are released.
   - *Example*: Quick bug fix for notification delivery or typography alignment.

---

## 2. Android `versionCode` & Build Numbers

- Every release tag increments `versionCode` monotonically in `app/build.gradle.kts`.
- `versionCode` must never be reused across published builds.
- Formula / Pattern:
  - Form: `MAJOR * 10000 + MINOR * 100 + PATCH` or sequential milestone counter.

---

## 3. Database Schema Versioning & Migrations

- Every modification to Room SQLite entities must increment `Room` version number (currently at version `23`).
- **Zero Destructive Migrations**: `fallbackToDestructiveMigration()` is forbidden in production release builds.
- Automated migration tests (`app/src/androidTest/java/com/pixelquest/app/data/local/MigrationTest.kt`) must verify automated data preservation from `version 1` to `current version`.

---

## 4. Release Cadence & Tagging

- Release tags must follow Git annotated tags: `v<MAJOR>.<MINOR>.<PATCH>` (e.g. `v1.8.0`).
- Every release must have corresponding release notes in [CHANGELOG.md](../CHANGELOG.md) covering:
  - ⬆️ Upgrading instructions and database migration behavior.
  - ✨ New features and enhancements.
  - 🔒 Security & Privacy hardening notes.
  - 🛠️ Developer and architecture updates.
