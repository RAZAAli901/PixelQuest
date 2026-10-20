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

The plan for Phase A, in short. The next section says how it was built.

- **One menu entry, Islamic,** in place of Namaz Tracker and Darood Party. It opens the hub, a grid of cards, and keeps Darood's invitation dot.
- **A registry of items:** each card is an `IslamicItemProvider`, bound into a Hilt set. The hub lists whatever is in the set, in order, with each one's on/off flag and badge.
- **A nested route group, `islamic`:** the hub (`islamic/hub`) and every Islamic screen, Namaz's and Darood's among them, under `islamic/`.
- **Old routes redirect:** `namaz` and `darood` still open their screens, for notifications set before the update.
- **Back from any Islamic screen** returns to the hub, then to wherever the player came from, whichever way they came in.

## As built (Phase A)

### The items
| Item | Order | Shown while | Badge | Its class |
|---|---|---|---|---|
| Namaz Tracker | 10 | Namaz is available (on or off: its screen is where it's switched on) | Prayers of the current prayer day whose time has come and aren't marked prayed, while the tracker is on | `ui/screens/namaz/NamazIslamicItem` |
| Darood Party | 20 | Darood's flag is on | Invitations waiting for an answer | `darood/ui/DaroodIslamicItem` |
| Quran | 30 | Always | None | `quran/QuranIslamicItem` |

- **The registry** (`islamic/IslamicRegistry`) combines the items' cards into one list, in order. Two items with the same id or route fail there, before the hub.
- **The hub** (`islamic/ui/IslamicHubScreen`, `islamic/hub`) lists the items that are on, two to a row. Its top bar has the side menu's three-line icon.
- **Debug builds** add a "More coming soon" card after the items (`IslamicHubFlags`), to show where a new one will sit. A release never shows it.

### Routes and Back
- **The routes:**
  - `islamic` is a nested graph, built by `islamicGraph()` (`ui/navigation/IslamicGraph.kt`).
  - Namaz is `islamic/namaz`. Darood Party's screens are under `islamic/darood`. The Quran is `islamic/quran`.
  - `Routes.isIslamic(route)` is true for all of them. The menu's Islamic entry uses it to stay highlighted (`MenuItem.owns`).
- **Redirects:** 1.4's `namaz` and `darood` stay as redirects (`redirect()`).
  - Namaz reminders and alarms set by 1.4 still carry `namaz`, so `MainActivity` still accepts it.
  - Darood's inner routes couldn't be reached from outside the app, so they moved without redirects.
- **Back:** every way in from outside goes through `NavController.navigateInIslamic(route)`, which puts the hub beneath the screen. So Back goes to the hub, then to wherever the player was. The ways in are:
  - the menu entry;
  - Home's Namaz card;
  - a notification's route;
  - the redirects;
  - leaving a party whose list isn't open.

  Choosing Islamic from inside the section goes back to the hub instead of opening a second one.

### Looks
- **Pixel and Comic:** the app's panels (`PixelCard`: Pixel's frame, Comic's ink and flat shadow), with drawn icons.
  - The icons are a crescent and star (`ic_islamic_*`, the menu entry), a mosque (`ic_namaz_*`), Darood's beads, and an open book (`ic_quran_*`).
  - The badge is a chamfered chip in Pixel and an inked pill in Comic.
- **Light:** the daylight panels Home's cards use, with emoji.
- **Simple Mode, any theme:** the plain look its side menu has, one item a row: a soft rounded block with the emoji, the title, a description in a plain font, and the badge.
- **TalkBack:**
  - Each card is one button, read as its title, description and badge in words: "Namaz Tracker. Prayer times, reminders and your week. 2 prayers to mark".
  - The emoji and the badge's bare number aren't read.
- **Large text:** from 1.5x, the hub is one card a row.

### Tests
- `IslamicRegistryTest`, `IslamicHubViewModelTest`, and each item's own test.
- `IslamicRoutesTest`: the graph, the redirects and Back.
- `IslamicHubScreenTest`, `IslamicItemCardTest` and `IslamicHubAccessibilityTest`.
- `IslamicHubScreenshotTest` draws the hub in every look to `app/build/outputs/islamic-screenshots`.
- `IslamicHubStoryTest`: from the side menu to each screen and back, and a 1.4 reminder's route.
- `MenuStateTest`, `MenuDrawerUiTest` and `DaroodMenuUiTest` reach Namaz and Darood through the hub.

## Add a new Islamic item in 3 steps

Say the item is a tasbeeh counter. Nothing in the side menu, the hub or `NavGraph.kt` changes.

**1. Write the item:** one class that implements `IslamicItemProvider`, with an `@Inject` constructor. It gives:
- **`order`:** its place on the hub. Namaz is 10, Darood 20 and the Quran 30, so 40 comes after them.
- **`item()`:** its card, as a `Flow<IslamicItem>`:
  - an id and a route, both unique; the route must be under `islamic/`;
  - its title and one-line description;
  - its icon;
  - `enabled` from its feature flag, if it has one;
  - a badge count and its plural (`badgeLabel`), if something can wait there.
- **`screens()`:** its own screens, added to the Islamic graph. Back from them goes to the hub by itself.

```kotlin
class TasbeehIslamicItem @Inject constructor() : IslamicItemProvider {
    override val order: Int = 40

    override fun item(): Flow<IslamicItem> = flowOf(
        IslamicItem(
            id = "tasbeeh",
            titleRes = R.string.tasbeeh_title,
            descriptionRes = R.string.tasbeeh_description,
            icon = IslamicIcon("📿"),
            route = "${Routes.ISLAMIC}/tasbeeh"
        )
    )

    override fun NavGraphBuilder.screens(navController: NavHostController) {
        composable("${Routes.ISLAMIC}/tasbeeh") { TasbeehScreen(onNavigateBack = { navController.popBackStack() }) }
    }
}
```

**2. Bind it:** add one line to `di/IslamicModule.kt`.

```kotlin
@Binds
@IntoSet
abstract fun tasbeeh(item: TasbeehIslamicItem): IslamicItemProvider
```

**3. Give it words and an icon:**
- **Words:** its title, description and any badge plural go in its own `res/values/strings_<feature>.xml`, with notes for translators.
- **An icon:** an emoji is enough. For drawn icons in Pixel and Comic, add `ic_<feature>_pixel.xml` (2-unit blocks on the 24-unit grid) and `ic_<feature>_comic.xml` (ink), and pass them to `IslamicIcon`.

**Then test it:**
- Write a provider test like `QuranIslamicItemTest`: the card's id, route and flag, and its badge.
- Add the card to `IslamicHubScreenshotTest`'s items to see it in every look.
- The registry fails a duplicate id or route, and `IslamicHubStoryTest` shows how to drive its screen from the hub.

