# Accessibility

What TalkBack users get, and the helpers to use so new screens stay consistent. Colour contrast is covered in `THEMING.md` (every listed ratio is recomputed by `ThemingDocContrastTest`).

## Helpers (`ui/components`)

| Helper | Use it for | TalkBack reads |
| --- | --- | --- |
| `Modifier.dayChip(day, selected, onToggle)` | A weekday chip in a multi-select row. Also hide the chip's letter with `Modifier.clearAndSetSemantics {}`. | "Tuesday, checkbox, checked" (not a lone "T" that Thursday shares) |
| `Modifier.choiceChip(selected, onSelect)` | One option of a single-choice row (recurrence, category). | "Weekly, radio button, selected" |
| `DismissButton(contentDescription, onClick, color)` | The small ✕ that closes a tip card. It has a 48dp touch target. | "Dismiss the Simple Mode tip, button" (not "✕") |
| `SymbolIcon(symbol, contentDescription, style)` | An emoji or symbol inside an `IconButton` (◀, 🗑️, 🔄). | "Back, button", "Delete Morning Run, button" (not "black left-pointing triangle" or "wastebasket") |
| `HeatmapCellLabels.describe(date, status, today)` | A heatmap square, which on screen is only a colour. | "Friday 2 October: perfect day", "Tuesday 6 October, today: nothing done yet", "Thursday 8 October: upcoming" |

An icon next to a text label is decorative: give it `contentDescription = null`, or TalkBack reads the name twice.

## What's covered (Day 30)

- **Quest form**: day chips (Pixel and Comic), recurrence and category chips.
- **Settings**: the Comic mode and Simple Mode tip cards' close buttons.
- **Symbol buttons**: Back on New/Edit Quest, AI Coach and Leaderboard; Delete on Edit Quest and on each quest in the list ("Delete Morning Run"); Leaderboard refresh.
- **Stats**: heatmap squares. Days after today aren't tappable, and today isn't called "missed" before it ends. The quest mini-history reads "done" / "missed" with the day.

Compose exposes a chip as a focusable checkable node whose name comes from a child node, the same shape as a Compose `Button` with a `Text` inside. TalkBack reads them together.

## Tests

`ChipAccessibilityTest` (Pixel and Comic), `StatsAndTipAccessibilityTest` and `SymbolButtonAccessibilityTest` check names, roles, states, the 48dp target and that the bare symbols aren't read on their own. They run under Robolectric with every unit test.

## Not covered yet

- A full TalkBack pass on a device. Day 30 checked the labels with `uiautomator dump` on the emulator, not by listening.
- Buttons drawn with `IconButton` were audited on Day 30; clickable rows and cards built from plain `Modifier.clickable` have not been audited one by one.
