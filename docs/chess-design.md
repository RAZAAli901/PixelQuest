# Chess in study parties

Members of a study party can play each other at chess, live, with clocks, rated with Int points.
**The server is the referee.** It checks every move, keeps the clocks, ends the game and updates
ratings and head-to-head records. The phone only shows what the server says and sends the
player's moves, so changing the app can't win a game. Chess never changes XP, levels or streaks.

## Architecture

| Part | What it does |
|---|---|
| Postgres tables | `chess_games`, `chess_moves`, `chess_challenges`, `chess_ratings`, `chess_head_to_head`, `chess_config`, `device_tokens`. RLS on every one, default deny. No client inserts or updates games, moves, ratings or records. |
| Database functions | `private.finish_game` is the single place a game ends: it sets the result, then updates ratings and head-to-head in one transaction, and calling it twice changes nothing. Reads go through views for the party leaderboard, head-to-head and profile stats. |
| Edge Functions (Deno) | `chess-challenge`, `chess-respond`, `chess-move`, `chess-resign`, `chess-draw`, `chess-claim-timeout`, `chess-sweep`, `chess-live-games`, `send-push`. Each checks the caller's token itself. The rules come from **chess.js** (no home-made move generator). The rating maths is a pure function with no database access. |
| Realtime | One private channel per game (`chess:game:<id>`) carries moves and clocks. Channel authorisation lets in only active members of the game's party. Presence gives the spectator count. |
| App | `ChessRepository`, `ChessChallengeRepository`, `ChessRatingRepository` and `SpectateRepository`. `ChessClock` shows time from server timestamps. `ChessSessionViewModel` runs the game screen. Room caches ratings, head-to-head and finished games only; a live game is never cached as truth, and moves are never queued offline. |

## The rules (one config)

The values live in the server's `chess_config` and the app's `ChessConfig`, and must match.

| Rule | Value |
|---|---|
| Time controls | 1, 3, 5 and 10 minutes a side, no increment (a table, so increments can come later) |
| Who plays | Active members of the same study party: a challenge to one member, or an open one to the room |
| Games at once | One live game per player (waiting to start, or active) |
| Challenge expiry | 10 minutes |
| Abort | No first move within 30 seconds: aborted, no rating change |
| Rating | Int points, Elo, one rating per time control, start 800, floor 100 |
| K factor | 40 for a player's first 10 games, then 32; 16 above 1600 |
| Anti-farming | At most 10 rated games between the same two players a day. Fewer than 4 moves each means no rating change, unless it ended in checkmate. |
| Head-to-head | Wins, draws and losses for each pair, per time control and in total |
| Tiers | Pawn, Knight, Bishop, Rook, Queen and King, with thresholds in the config |
| Spectators | Any active member of the party can watch live and see how many are watching |
| Rate limits | 10 challenges an hour; 5 moves a second per game; 20 spectator joins an hour |

## Game states

- **challenge:** `open` → `accepted` / `declined` / `cancelled` / `expired`.
- **game:** `waiting` (made on accept; the clocks are set but not running) → `active` (from White's first move) → `finished` or `aborted`.
- **Results:** `white`, `black` or `draw`.
- **Reasons:** `checkmate`, `resign`, `timeout`, `stalemate`, `insufficient_material`, `threefold_repetition`, `fifty_moves`, `draw_agreed`, `aborted`, `timeout_vs_insufficient` (a draw).

## Clocks

- **Kept on the server only.** Each game stores `white_ms`, `black_ms` and `last_move_at`.
- **On a move:** the mover's time used is `now() - last_move_at`, by the server's clock. If that's more than their time left, the move is refused and the game ends on time instead.
- **Running out:** either player can claim a timeout when it's checked against server time. A sweep every minute ends any game whose clock has run out, so an abandoned game still ends.
- **On the phone:** the clock counts down from the server's last numbers. It corrects for the difference between the phone's clock and the server's (measured from each answer) and never decides anything itself.

## Moves

- **Sending:** each move goes with its ply number (`ply` = the number of moves played so far). The server takes it only if it's the caller's turn at that ply, so a retry after a lost answer is safe: the same move at the same ply gets the stored answer back, never a second move.
- **Checking:** the server rebuilds the position from the game's FEN with chess.js and checks the move is legal. It applies promotion, castling and en passant. After the move it detects checkmate, stalemate, insufficient material, threefold repetition (from the move list) and the 50-move rule.
- **Storing:** each move is a `chess_moves` row (`game_id`, `ply` unique together), with the SAN, the UCI, the FEN after it and both clocks after it.

## Ratings and records

- **When:** `finish_game` updates them in the same transaction that sets the result, only for rated games and only once. A second call sees the game finished and does nothing.
- **The maths:** the Elo expected score is `1 / (1 + 10^((Rb - Ra) / 400))`. The new rating is `Ra + K * (S - E)`, rounded, with a floor of 100.
- **Records:** `chess_ratings` keeps games, wins, draws, losses and peak per time control. `chess_head_to_head` keeps one row per pair (`user_low < user_high`) per time control.

## Threats

| Threat | Answer |
|---|---|
| A changed app sends illegal moves or edits the board | Only the functions write games and moves; every move is checked with chess.js against the stored position. |
| A changed app stops its own clock | The clocks are the server's; the phone's are for display. |
| Moving after the game ended, or twice at once | Moves are checked against the game's status and the ply. The unique `(game_id, ply)` key stops a race. |
| Farming rating with a friend, or a second account | 10 rated games a day per pair; short games don't count; ratings need a shared study party. |
| Disconnecting to avoid losing | The clock keeps running on the server; the sweep ends the game on time. |
| Stalling a lost game | The same: the clock runs out. |
| Spamming challenges or spectating | Rate limits; challenges expire after 10 minutes. |
| Harassment | Blocked members can't challenge each other (`member_blocks`). |
| Seeing other parties' games | RLS and channel authorisation: only active members of the game's party. |
| Push notifications leaking content | Push carries only ids and a kind. The text is built on the phone and hidden on the lock screen. Tokens are written only by their owner. |
| Leaked service credentials | The FCM service account lives only in the functions' secrets, never in the app or the repo. |

## Not in this phase

- **The board UI:** Phase E plugs in the owner's design from `/chess-ui`.
- **Increments and correspondence chess:** the time-control table allows them later.
