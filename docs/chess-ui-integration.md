# Chess UI: what the design holds and where it plugs in

The owner's chess design is in `chess-ui/` (copied from `Downloads/chess-design`). This page lists what's in it and maps each screen to the Phase D interfaces (docs/chess-design.md). **The look is kept as designed.** Where the design's mock rules differ from the plan's, the plan wins; those differences are listed at the end.

## What's in it

| File | Format | What it is |
|---|---|---|
| `PixelQuest Chess.dc.html` | Claude Design document | The spec: time controls, Int, head-to-head, spectating, the flow, edge cases and the data model |
| `PQChess.dc.html` | Claude Design component | The phone screens, one block per screen, with Comic and Pixel tokens: hub, challenge, incoming, game, over, spectate, h2h, party, rating, notifs |
| `PQIcon`, `PQNav`, `PQNavPixel` `.dc.html` | Claude Design components | Shared pieces (the app icon, the bottom bar) the app already has in Compose |
| `support.js`, `_ds/` | Runtime and styles | Needed only to open the design in a browser |

## Assets

- **Images:** none.
  - The pieces are Unicode chess glyphs (♚♛♜♝♞♟), drawn solid for both sides. White's are filled with the paper colour and outlined in ink; Black's are filled with ink.
  - Icons are Material Symbols Rounded, which the app already has.
- **Fonts:**
  - Bangers, Rubik, Press Start 2P and Pixelify Sans are already in `app/src/main/res/font`, under their OFL licences in `licenses/`.
  - The design draws the glyphs in Noto Sans Symbols 2. On Android, the system font that covers the chess block is used instead, so no font file is added.
- **Sounds:** none supplied. Move, capture, check and end use the system's own click and haptics (J161).
- **Licences:** nothing new to record.

## The tokens

Each value is listed as Comic / Pixel.

| Token | Value | In the app |
|---|---|---|
| Board, light squares | #fffdf8 / #fffaf0 | `ChessColors.light` (the skin's paper) |
| Board, dark squares | #8ecae6 / #e8a33d | `ChessColors.dark` |
| Last move, selected square | #ffd95a / #fde7a0 | `ChessColors.highlight` |
| King in check | the primary colour | `PqColors.Red` |
| Legal-move dot | a 12 dp circle, ink at 35% | `ChessColors.dot` |
| Clock | 30 sp Bangers / 15 sp Press Start 2P. The side to move is ink with paper text; the other is paper. | `ChessClockBox` |
| Coordinates | 9 sp Rubik bold, ink2: ranks on the left file, files on the bottom rank | `ChessBoard` |

## Screens, mapped to the interfaces

| Design screen | Compose | Data |
|---|---|---|
| hub | `ChessHubScreen`: your Int per time control, a challenge button, live games, rivals | `ChessRatingRepository.observeRatings`, `SpectateRepository.liveGames` |
| challenge | `ChallengeSheet`: a member, the time control, the colour, rated, and the head-to-head at once | `ChessChallengeRepository.challenge`, `ChessRatingRepository.headToHead` |
| incoming | `IncomingChallengeScreen`: a countdown to `expiresAt`, Accept and Decline | `ChessChallengeRepository.respond` |
| game | `ChessGameScreen` | `ChessSessionViewModel` (J133) over `ChessRepository` |
| over | `GameOverCard`: the result, the Int change, the head-to-head with the cell that went up, Rematch | the finished `ChessGame` (whiteChange / blackChange), `headToHead` |
| spectate | `ChessGameScreen` read-only: SPECTATING, Leave, Next live game | `SpectateRepository.watch` |
| h2h | `HeadToHeadScreen`: W·D·L, a split bar, by time control | `ChessRatingRepository.headToHead` |
| party | `StudyPartyScreen` Chess tab: the live game banner, the party ladder, a challenge on each member | `leaderboard`, `liveGames`, `observeChallenges` |
| rating | `ChessRatingScreen`: each time control's Int, record and peak | `observeRatings` |
| notifs | the Chess notification channel (J123) | `ChessPushNotifier` |

## Where the design's mock rules and the plan differ

The plan's rules are the ones the server enforces (chess_config).

| Design says | Built as (the plan) |
|---|---|
| Three pools: Bullet, Blitz (3 and 5 min together) and Rapid | One Int per time control (1, 3, 5 and 10 min). The screens show four. |
| K is 40 for the first 20 games, then 24 | K is 40 for the first 10, then 32, and 16 above 1600 |
| A challenge expires in 60 s | 10 minutes |
| No first move in 20 s aborts | 30 s |
| Quick play with a random opponent within ±150 | Not in the plan: the hub has the challenge button and the party ladder instead |
| Spectators by the challenger's choice, and friends outside the party | Any active member of the party, and only them |
| A draw offer once every 10 moves | One offer standing at a time (the server keeps it) |
