# The Islamic hub

One section of the side menu, "Islamic", opens a landing screen of cards: the Namaz Tracker, Darood Party, and the Quran. Each card is one registered object, so a new item needs no change to the menu or the hub.

This document starts with what the app had before the hub (Phase A's audit), then how the hub is built.

## Before the hub (audit, 1.4.1)

### The side menu
- **Entries** (`ui/menu/MenuItem.kt`, `MenuItems.all`): Namaz Tracker, Darood Party, Quest History, Leaderboard, Account, Settings. Each is a `MenuItem` with an id (`MenuItemId`), a label (a Simple Mode one where it differs), an emoji, optional drawn icons for Pixel and Comic, a route, and three functions of `MenuContext`: whether it shows, a status, and a dot.
- **Namaz Tracker** (🕌): shown while `SettingsRepository.namazAvailable`. Its status says "On" while the tracker's own switch is on.
- **Darood Party** (📿, drawn bead icons in Pixel and Comic): shown while `SettingsRepository.daroodAvailable`. A dot says an invitation is waiting.
- **`MenuViewModel`** builds the header and the `MenuContext`, and keeps the highlighted entry in saved state. `syncWithRoute` highlights the entry whose route is the current one, exactly; any other route clears the highlight.
- **`MainActivity`** opens a chosen entry with `navigate(route) { launchSingleTop = true }`, or as a tab for the Leaderboard. Swiping from the left edge opens the menu on the five tabs only. Screens show the three-line icon with `MenuTopBarAction`, which is empty where there's no menu.

### Routes (`ui/navigation/Routes.kt`, `NavGraph.kt`)
- **Namaz:** `Screen.Namaz` is `namaz`, a single screen with its own sheets.
- **Darood Party:** `Screen.DaroodParty` is `darood` (the party list), with `darood/create`, `darood/race`, `darood/party/{partyId}` (the chat), `darood/party/{partyId}/info` and `darood/party/{partyId}/add`. Leaving a party from its info screen pops back to `darood`, or opens it if it isn't on the back stack.
- **`username_setup`** belongs to Darood but is also opened from Profile and by the one-time prompt after sign-in, so it's not an Islamic screen.
- All of these sit flat in one `NavHost`, with no nested graphs.

### Ways in from outside the menu
- **Home's Namaz card** (`HomeNamazSection`, shown while the tracker is on) calls `onNavigateToNamaz`, which navigates to `namaz` over Home.
- **Notifications:**
  - A Namaz reminder's tap (`NamazNotifier`) and the alarm clock's icon (`NamazAlarmScheduler.openNamaz`) start `MainActivity` with `EXTRA_OPEN_ROUTE = "namaz"`. These are saved in `PendingIntent`s and in alarms already set, so phones updating from 1.4.1 will still deliver `namaz` for a while.
  - `MainActivity` accepts only the routes in `OPENABLE_ROUTES` (just `namaz`), since the activity is exported. It opens the route once the app is past the splash and onboarding, over Home.
  - The Prayed button on a reminder (`NamazActionReceiver`) doesn't open the app.
  - Darood's weekly note opens the app without a route.
- **There are no deep links:** the manifest has only the launcher filter.

### Flags
- `namazAvailable` and `daroodAvailable` (`SettingsRepository`, both on by default) hide an entry and, for Darood, the username prompt. Neither has a switch in Settings.

### Tests that pin the current layout
- `MenuStateTest`: the entries' order and routes, Namaz's and Darood's visibility, status and dot, and the highlight.
- `MenuDrawerUiTest` and `DaroodMenuUiTest`: the drawer opening Namaz and Darood directly.
- `NamazNotifierTest`: the reminder's tap carries `Screen.Namaz.route`.

## What changes

- **One menu entry, Islamic,** in place of Namaz Tracker and Darood Party. It opens the hub, a grid of cards, and keeps Darood's invitation dot.
- **A registry of items:** each card is an `IslamicItemProvider`, bound into a Hilt set. The hub lists whatever is in the set, in order, with each one's on/off flag and badge.
- **A nested route group, `islamic`:** the hub (`islamic/hub`) and every Islamic screen, Namaz's and Darood's among them, under `islamic/`.
- **Old routes redirect:** `namaz` and `darood` still open their screens, for notifications set before the update.
- **Back from any Islamic screen** returns to the hub, then to wherever the player came from, whichever way they came in.
