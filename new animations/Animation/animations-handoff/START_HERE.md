# PixelQuest micro-animations: instructions for Claude Code

Add the 12 micro-animations from the design spec to the PixelQuest app (Kotlin + Compose). This builds on `claude-ui-assets/` (PqTheme, PqSkinState, PqComponents).

## Look first
Open `design-reference/PixelQuest Animations.dc.html` in Chrome. Every animation (M01–M12) loops twice side by side: Comic on the left, Pixel on the right. Each one lists its trigger, sequence, haptic and Compose API, and §03 says which screen uses which.

## Files
```
animations-handoff/
├─ START_HERE.md
├─ compose/PqMotion.kt      ← tokens + every animation composable/modifier (M01–M12)
├─ compose/PqChessMotion.kt ← chess animations (C01–C09); depends on PqMotion.kt
└─ design-reference/        ← open "PixelQuest Animations.dc.html"
```

## Steps
1. Copy `compose/PqMotion.kt` into `ui/motion/`. Fix the package name and the `com.pixelquest.ui.theme.*` import.
2. **Press (M04).** Inside `comicPressable` in PqComponents, swap the hard-coded press offset for `Modifier.pqPress(interactionSource)` so every button gets the spring back.
3. **XP (M01/M02).** Replace the static XP/progress bar on Home and Hero with `XpBar(xp, max, level)`. When XP is awarded, show `XpGainChip(amount, trigger++)` above the bar. Use `RollingNumber` for every XP, points, Int and streak number that changes.
4. **Quests (M03).** In the quest row use `QuestCheck(checked, onToggle)`, add `Modifier.questRowDone(done)` to the row, draw the title strike with `strikeProgress(done)`, and show `FloatingReward("+20 XP", trigger)` above the circle.
5. **Streak (M05).** Replace the header streak chip with `StreakCounter(days)`.
6. **Burst (M06).** Wrap the reward icon in `RewardBurst(trigger)` for an avatar unlock, a chess win and all five prayers.
7. **Progress (M07).** On Home use `ProgressRingAnimated` in Comic and `PixelMeter` in Pixel.
8. **Perfect day (M08).** On the Perfect Day card, add `PerfectDayStamp(visible)` + `Modifier.thud(trigger)` + `ConfettiFall(trigger)`.
9. **Toast (M09).** Put one `PqToastHost` in the root scaffold and expose `PqToastState` via a CompositionLocal. Show it after undoable actions (complete or skip a quest, delete an item).
10. **Nav (M10).** In PqNav add `Modifier.navHop(selected)` to each tab icon and `NavIndicator(index, 4, tabWidth)` behind the row.
11. **Prayer (M11).** Replace the Deen/Namaz pip row with `PrayerPips(done)`.
12. **Loading (M12).** Add `Modifier.pqShimmer()` to skeleton blocks on lists that take more than 250ms to load.

## Chess (§03, C01–C09). Add `compose/PqChessMotion.kt` next to PqMotion.kt.
- **C01 Move.** Render each piece with `AnimatedPiece(col, row, square, bullet = timeControl == 1)`, keyed by a stable piece id so moves animate. Add `LastMoveHighlight(from, to)` under the pieces.
- **C02 Capture.** On capture, show `CaptureEffect(trigger, col, row, square, trayOffset)` with the captured glyph. Add `Modifier.trayBump(count)` to the captured tray.
- **C03 Check.** Add `Modifier.checkPulse(inCheck)` to the king's square, `Modifier.kingShake(checkCount)` to the king, and `CheckTag(visible)` near the board.
- **C04 Checkmate.** Add `Modifier.kingTopple(mated)` to the mated king, then `BoardDim(true)` and `ResultStamp("CHECKMATE", true)`. Open the Result sheet about 1.2s later.
- **C05 Clock.** Replace both clocks with `ChessClock(millis, active)`. It switches to tenths under 20s and flashes, ticks and shakes under 10s.
- **C06 Select.** Add `Modifier.pieceLift(selected)` to the selected piece and show `LegalMoveDots(targets, captures, square)`. Captures get rings instead of dots.
- **C07 Result.** On the result sheet, show `IntDeltaChip(delta)`, `RollingNumber(int)` and `H2HCounter(w, d, l, changed)`.
- **C08 Challenge.** On the incoming challenge screen, use `ChallengeClash(me, them)` and `ExpiryBar(secondsLeft)`.
- **C09 Promotion.** When a pawn promotes, use `PromotionMorph(trigger, square, pawn, promoted)`.
- Chess timing rule: a move animation must never delay the clock. Start the opponent's clock as soon as the move is sent, not when the animation ends.

## Rules
- Celebrations queue: XP, then level up, then perfect day. Never show two at once. A simple `Channel` in the Home ViewModel is enough.
- When system animations are off, `reducedMotion()` returns true and everything jumps to its end state. Keep that behaviour.
- Pixel skin = `PqMotion.steps(4)` everywhere. That's already handled through `PqMotion.snap()` and `PqMotion.out()`. Don't hard-code easings.
- Animations must never block taps.

## Done when
- [ ] Each M01–M12 and C01–C09 matches the design reference in both skins.
- [ ] Completing a quest from a notification still animates the bar and number the next time Home opens (animate from the last shown value).
- [ ] No jank on a mid-range device. Check with Layout Inspector / the JankStats overlay.
