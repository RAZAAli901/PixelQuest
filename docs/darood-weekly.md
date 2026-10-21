# Darood Party: weekly ranking and goals that go on

Two additions to Darood Parties ([darood-design.md](darood-design.md)). Neither ever touches XP, levels or streaks.

## The weekly ranking

### The week

- **When it runs:** Monday 00:00 to Sunday 23:59:59 in the party's own time zone, `parties.week_tz`.
- **Where the time zone comes from:** it's set from the leader's zone when the party is made. The leader can change it, and the change counts from the next week.
- **What counts:** an entry belongs to the week its `local_date` falls in. That's the day the member recorded it on, as for every Darood total.

### Positions

- **Who's ranked:** members active in the party when the week closes, by their total for that week, highest first.
- **Ties share a position** (1, 2, 2, 4), ordered for display by most active days, then by username.
- **No entries that week:** the member is still ranked, at the shared last position with a total of 0.
- **Who's left out:** members who left, were removed, or are only invited.
- **Joining mid-week:** that member is ranked on what they added after joining.

### Results

- **Closing a week:** once a week has ended in its zone, it closes and its results are stored once, in `darood_week_results`. They never change after that.
- **Repeats are safe:** closing again changes nothing, so any phone or the server's own schedule can ask.

### The notification

- **When:** each member gets one when the week closes. Each phone checks after the week's end and whenever the app opens.
- **What it says:** "Your position this week: 2nd of 8 with 3,200 Darood". Expanded, it lists every member's position and total.
- **Tone:**
  - Last place gets an encouraging line ("Every Darood counts. This week is a fresh start"), never a shaming one.
  - The leader's notification says exactly what everyone else's does.
  - A member with 0 that week gets the encouraging line too.
- **In the chat:** tapping it opens the party's chat with the Weekly ranking sheet. The week's results also arrive in the chat as a system card.
- **Turning it off:** per party, or everywhere, on this phone.

## Goals that go on

- **Goal history:** a party's goals are a list, `party_milestones`: goal 1, goal 2, and so on, each with its target, who set it and when, and when it was reached.
- **The first goal** is the party's `milestone`, as before. It moves into the list on migration.
- **Reaching a goal:** the first time the total reaches the current goal, it's marked reached. Everyone sees a celebration, and the leader sees **Extend goal**.
- **Extending:** a new goal can be set only once the current one is reached, and only above the party's total now. Quick options are +25%, +50% and double, or any number (digits only, up to the server's maximum).
- **History:** Party info lists the goals ("Goal 1 reached on 21 Oct", "Goal 2 in progress").
- **Notifications:** Goal reached, and New goal set, go to every member on the Darood channel.
- **The race board** shows the current goal, the progress toward it, and when each earlier goal was finished.
