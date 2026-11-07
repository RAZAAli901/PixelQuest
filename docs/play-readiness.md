# Google Play Readiness Audit: PixelQuest

**Audit Date:** 5 Nov 2026 · **Auditor:** @Raza Ali Murtaza  
**Audited Artifact:** `app-release_2.apk` (PixelQuest 1.2.0, version code 102)  
**Release Context:** First Play release (New app enrollment)  
**Overall Verdict:** Cannot be published as it stands; 3 hard blockers, 3 policy risks, 3 content/safety gaps, 4 Play Console requirements.

---

## 1. Executive Summary

| Category | Count | Status Description |
|---|---|---|
| **Fail (Blockers)** | 3 | Blocks upload or store automated review outright |
| **Risk** | 3 | Likely to cause rejection or policy strike without declarations/fallbacks |
| **Gap** | 3 | Required Play policy mechanisms missing from code or UI |
| **Console** | 4 | Declarations, forms, and closed-test requirements on Play Console |
| **Info** | 2 | Security and resilience improvements |
| **Pass** | 11 | Meets current Google Play technical requirements |

---

## 2. Play Readiness Checklist (26 Items)

### Blockers (Must Fix for Upload / Initial Processing)
- [x] **#1 Package format:** Build Android App Bundle (`.aab` via `bundleRelease`) instead of `.apk`. Enroll in Play App Signing with upload key.
- [x] **#2 Target API level:** Raise `targetSdk` and `compileSdk` to **API 36** (Android 16, mandated since 31 Aug 2026). Test Android 15 & 16 runtime behavior changes.
- [x] **#3 Launcher icon:** Add adaptive launcher icon (`android:icon` and `android:roundIcon` with foreground, background, monochrome layers) and export 512×512 px store icon.

### Policy Risks & System Interactions
- [x] **#4 Exact alarms:** Permission `SCHEDULE_EXACT_ALARM` restricted on Android 14+. Check `canScheduleExactAlarms()` before scheduling; fall back to window alarms with user notice.
- [x] **#5 Full-screen intent:** `USE_FULL_SCREEN_INTENT` given by default only to calling/alarm apps. Check `canUseFullScreenIntent()` on Android 14+, provide system settings redirect, and fall back to heads-up high-priority notification.
- [x] **#6 Foreground service:** Declare foreground service type in manifest and code (`FOREGROUND_SERVICE` requires specific types on Android 14+); audit WorkManager and prepare study timer service type.

### Policy Gaps (User Safety & AI Content)
- [x] **#8 AI-generated content:** In-app reporting for Gemini AI Coach output ("Report this insight" button and report persistence).
- [x] **#9 Terms for user content:** Terms of Use acceptance requirement before leaderboard opt-in and public name publishing (banning offensive names).
- [x] **#10 Report and block:** In-app reporting retained, plus blocking feature for leaderboard/chat interactions (hiding blocked users).

### Play Console Actions
- [ ] **#7 Developer account closed test:** Personal accounts created after 13 Nov 2023 require 12 testers for 14 consecutive days before production release.
- [x] **#11 Account deletion web link:** Publish public web page for account and data deletion requests (hosted on GitHub Pages).
- [x] **#12 Data safety form:** Disclose collection/sharing for email, display name, level, streak, XP, habit completion patterns (Gemini), transit encryption, and deletion.
- [x] **#15 Store content questions:** Complete questionnaires for content rating, target audience (13+), no ads, and declarations (see `docs/store/listing.md`).

### Information & Hardening
- [x] **#16 Boot receiver:** Review `RECEIVE_BOOT_COMPLETED` receiver; keep `android:exported="false"` where possible.
- [x] **#17 Backup rules:** Tighten `allowBackup` and XML backup rules to protect sensitive cached data.

### Passed Checks (Maintain Compliance)
- [x] **#13 Privacy policy:** Public URL link present in app and store listing.
- [x] **#14 Account deletion in app:** "Delete my cloud data" calls backend `delete_user_account`.
- [x] **#18 Minimum version:** `minSdk 24` (Android 7.0) supported.
- [x] **#19 Signing key:** Valid release certificate (PixelQuest valid to 2054, signature scheme v2).
- [x] **#20 Version code:** Monotonically increasing version code.
- [x] **#21 Native libraries:** No unapproved native `.so` binaries.
- [x] **#22 Debuggable:** `android:debuggable="false"` in release builds.
- [x] **#23 Network security:** HTTPS only, cleartext traffic disabled.
- [x] **#24 Notifications:** `POST_NOTIFICATIONS` runtime permission requested properly.
- [x] **#25 Permissions & SDKs:** Minimal permissions, disclosed SDKs, no ad SDKs.
- [x] **#26 App size:** Optimized bundle size (approx. 6 MB).

---

## 3. Implementation Plan (P001–P030)

### Phase 1: Build & Target (P001–P007)
- [x] **P001**: docs: copy the audit summary into docs/play-readiness.md as a checklist
- [x] **P002**: chore: configure bundleRelease for release build type & document Play App Signing
- [x] **P003**: chore: raise compileSdk and targetSdk to 36
- [x] **P004**: fix: edge-to-edge and window insets on every screen
- [x] **P005**: fix: review Android 15 & 16 behavior changes and apply necessary fixes
- [x] **P006**: test: run test suite under API 36 environment
- [x] **P007**: fix: resolve any regressions from P006

### Phase 2: Icon & Store Assets (P008–P009)
- [x] **P008**: feat: adaptive launcher icon (foreground, background, monochrome layers)
- [x] **P009**: chore: export 512×512 store icon and 1024×500 feature graphic into docs/store/

### Phase 3: Foreground Services, Alarms & Full-Screen Alerts (P010–P018)
- [x] **P010**: docs: list every foreground work location
- [x] **P011**: fix: declare foreground service type and permissions or remove unused declarations
- [x] **P012**: feat: study timer foreground service type and permissions for target SDK 36
- [x] **P013**: docs: write docs/store/fgs-declaration.md with user impact & demo steps
- [x] **P014**: feat: check canScheduleExactAlarms() before exact alarms with window alarm fallback
- [x] **P015**: feat: settings row opening Alarms & Reminders system settings
- [x] **P016**: feat: check canUseFullScreenIntent() on Android 14+ with permission grant flow
- [x] **P017**: fix: high-priority heads-up notification fallback when full-screen intent disallowed
- [x] **P018**: docs: write docs/store/full-screen-intent-declaration.md

### Phase 4: AI Reporting, Terms & Blocking (P019–P023)
- [x] **P019**: feat: "Report this insight" action on AI Coach answers
- [x] **P020**: feat(db): allow report type ai_insight in reports schema with RLS
- [x] **P021**: feat: terms of use acceptance screen before leaderboard opt-in & name publishing
- [x] **P022**: feat: block user functionality for leaderboard entries
- [x] **P023**: docs: draft terms of use and privacy policy text in docs/legal/

### Phase 5: Deletion, Privacy & Data Safety (P024–P026)
- [x] **P024**: docs: static account deletion webpage in docs/site/delete-account.html
- [x] **P025**: docs: comprehensive privacy policy in docs/legal/privacy-policy.md
- [x] **P026**: docs: docs/store/data-safety.md field-by-field worksheet

### Phase 6: Release & Validation (P027–P030)
- [x] **P027**: ci: bundle build & bundletool validation check in CI
- [x] **P028**: chore: bump version code & release notes
- [x] **P029**: docs: store listing text in docs/store/listing.md & screenshot checklist
- [x] **P030**: test: bundle smoke test via bundletool & final readiness sign-off

