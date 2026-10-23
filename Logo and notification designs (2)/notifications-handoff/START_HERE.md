# PixelQuest notifications + Pix: instructions for Claude Code

You're implementing the full notification system and the animated mascot **Pix** for the PixelQuest Android app (Kotlin + Compose). This builds on `claude-ui-assets/` (theme, PqSkinState) and **replaces** the old `PqNotifications.kt`.

## Look first
Open `design-reference/PixelQuest Notifications.dc.html` in Chrome. It is the spec:
- §01: Pix's 13 moods and their animations
- §02: Moody app icon rules
- §03: Delivery rules (limits, quiet hours, look)
- §04: The **42 notifications** (also listed in `CATALOG.md`)
- §05: Pixel skin

## Files
```
notifications-handoff/
├─ START_HERE.md            ← you are here
├─ CATALOG.md               ← every notification: ID, mood, channel, copy, actions, trigger, builder
├─ manifest-snippet.xml     ← activity-aliases (moody icon), receiver, permissions
├─ mascot/
│  ├─ PqMascotData.kt       ← GENERATED Pix grid data + layer builder (don't hand-edit)
│  ├─ PqMascot.kt           ← animated Compose PqMascot(mood)
│  ├─ PqMascotBitmap.kt     ← static bitmaps for notification icons/cards
│  └─ MoodyIcon.kt          ← launcher icon switching
├─ notifications/
│  ├─ PqNotify.kt           ← channels + post() + one builder per catalog ID
│  └─ PqNotifyActions.kt    ← action receiver (fill TODOs)
├─ android-res/             ← merge into app/src/main/res/
│  ├─ drawable/ic_pix_<mood>_foreground|monochrome.xml  (5 icon moods)
│  ├─ mipmap-anydpi-v26/ic_launcher_pix_<mood>.xml
│  ├─ drawable/pix_card_*.xml, layout/notif_pix_card.xml, drawable/ic_stat_pixelquest.xml
│  └─ values/pix_colors.xml
└─ design-reference/        ← the HTML spec (open in a browser)
```

## Steps
1. **Package names.** Replace `com.pixelquest` with the real package in every `.kt` file and in `manifest-snippet.xml`.
2. **Resources.** Merge `android-res/` into `res/`. Fonts must exist in `res/font/`: `bangers_regular`, `rubik_extrabold`, `rubik_medium`. They should already be there from `claude-ui-assets`; if the names differ, fix `notif_pix_card.xml`.
3. **Pix.** Add `mascot/`. Use `PqMascot(mood)` in the app where it helps: empty states, the Perfect Day card, streak screens, the chess result. Pix pops whenever the mood changes, so drive it from state.
4. **Notifications.**
   - Delete the old `PqNotifications.kt` and its receiver.
   - Add `notifications/`.
   - In `Application.onCreate()`: call `PqNotify.createChannels(this)` and set `PqNotify.deepLink = { ctx, route -> /* PendingIntent.getActivity to MainActivity with route extra */ }`. Also set `PqNotify.Profile.name` from the user profile.
   - Find every place the app currently posts a notification and replace it with the matching builder from `CATALOG.md`.
   - Add schedulers (WorkManager or AlarmManager) for the time-based triggers in the catalog's "Trigger" column: N01, N06, S01, S02, S05, S06/S07, P03, D01–D06, R03, R06, R07. Prayer reminders need exact alarms (`SCHEDULE_EXACT_ALARM`, or `USE_EXACT_ALARM` if it qualifies).
   - Cancel stale notifications: when a quest is completed in the app, call `PqNotify.cancel` for N02/N03/S01/S02. When a prayer is logged, cancel its D01–D03.
   - Fill the TODOs in `PqNotifyActions`. ✓ Done and ✓ Prayed must award XP exactly like the in-app buttons.
   - Add a WorkManager job at 06:00 that calls `QuietQueue.flush(ctx)`. (Optional: persist the queue so it survives process death.)
   - Ask for `POST_NOTIFICATIONS` the first time the user turns on a reminder, not at launch. Ask for `USE_FULL_SCREEN_INTENT` on Android 14+ the first time they open Chess.
5. **Moody icon.**
   - Apply `manifest-snippet.xml`: remove the LAUNCHER filter from MainActivity and add the 5 aliases. Point `android:icon` at `@mipmap/ic_launcher_pix_happy`.
   - Call `MoodyIcon.install(app) { MoodyIcon.Inputs(...) }` in `Application.onCreate()`, and call `MoodyIcon.apply` from the 20:00 and 00:05 jobs.
   - Add a toggle under Settings › Look: "Moody app icon" (default ON).
   - Test on a real device: some launchers take a few seconds to refresh the icon.
6. **Skins.** Accent, Pix and card backgrounds already follow `PqSkinState.skin`. Check §05 against a Pixel-skin build.

## Done when
- [ ] All 42 builders compile and each one has a caller (or a scheduler) that matches its trigger.
- [ ] Every action button works from the lock screen, and Done and Prayed award XP.
- [ ] Notifications are capped at 5 a day, quiet hours hold non-essential ones until 06:00, and essential ones still arrive.
- [ ] Pix appears as the large icon in the right mood, and celebration cards show the comic card.
- [ ] The moody icon switches according to the §02 rules and the toggle turns it off.
- [ ] Pixel skin: orange accent, square cards, pixel Pix.
