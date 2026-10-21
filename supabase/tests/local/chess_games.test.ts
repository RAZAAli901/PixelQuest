// Phase D's "done when" (docs/chess-design.md): two accounts in one study party challenge, play to
// the end through the server's functions, and their ratings and head-to-head update exactly once.
// Needs `functions serve` running (docs/handoff/HOW-TO.md).
import assert from "node:assert/strict";
import { call, localTest, sql, type Session } from "./stack.ts";
import { chessCast, endGames, studyParty, type Player } from "./chess_rls.test.ts";

const fn = (name: string, who: Session, body: unknown) => call(`/functions/v1/${name}`, { token: who.accessToken, body });

/** A challenge from [white] (playing White), accepted by [black]: the new game's id. */
async function newGame(party: string, white: Player, black: Player, timeControl = 180): Promise<string> {
  sql(`DELETE FROM public.chess_challenges WHERE from_user = '${white.userId}'`);
  const made = await fn("chess-create-challenge", white, { party_id: party, to_user: black.userId, time_control: timeControl, colour: "white" });
  assert.equal(made.status, 200, JSON.stringify(made.body));
  const accepted = await fn("chess-respond-challenge", black, { challenge_id: made.body.challenge.id, accept: true });
  assert.equal(accepted.status, 200, JSON.stringify(accepted.body));
  assert.equal(accepted.body.game.white_id, white.userId);
  return accepted.body.game.id;
}

const rating = (who: Session, tc = 180) => sql(`SELECT coalesce((SELECT int_points || '/' || games FROM public.chess_ratings WHERE user_id = '${who.userId}' AND time_control_sec = ${tc}), 'none')`);

localTest("chess, through the functions: scholar's mate, ratings and head-to-head once", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  sql(`DELETE FROM public.chess_ratings WHERE user_id IN ('${white.userId}', '${black.userId}') AND time_control_sec = 300`);
  // Earlier tests' games between this pair were today: moved back, so these count.
  sql(`UPDATE public.chess_games SET ended_at = ended_at - interval '2 days' WHERE ended_at IS NOT NULL AND '${white.userId}' IN (white_id, black_id) AND '${black.userId}' IN (white_id, black_id)`);
  const party = await studyParty(white, [black]);
  const game = await newGame(party, white, black, 300);

  const line = ["e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6", "h5f7"];
  for (const [i, uci] of line.entries()) {
    const mover = i % 2 === 0 ? white : black;
    // People think between moves; at most 5 a second are taken.
    await new Promise((r) => setTimeout(r, 250));
    const res = await fn("chess-move", mover, { game_id: game, ply: i + 1, uci });
    assert.equal(res.status, 200, `${uci}: ${JSON.stringify(res.body)}`);
    if (i === 0) {
      // Sent twice (a lost answer): taken once.
      const again = await fn("chess-move", mover, { game_id: game, ply: 1, uci });
      assert.equal(again.body.repeated, true);
      // The wrong side, and an illegal move, are refused.
      assert.equal((await fn("chess-move", white, { game_id: game, ply: 2, uci: "d2d4" })).body.error, "not_your_turn");
      assert.equal((await fn("chess-move", black, { game_id: game, ply: 2, uci: "e7e4" })).body.error, "illegal_move");
    }
  }
  const row = sql(`SELECT status || ' ' || result || ' ' || result_reason || ' ' || rating_applied FROM public.chess_games WHERE id = '${game}'`);
  assert.equal(row, "finished white checkmate true");
  assert.equal((await fn("chess-move", black, { game_id: game, ply: 8, uci: "e8f7" })).body.error, "game_over");
  assert.equal(rating(white, 300), "820/1");
  assert.equal(rating(black, 300), "780/1");
  // Asked to end again, nothing changes.
  sql(`SELECT private.chess_finish('${game}', 'black', 'resign')`);
  assert.equal(rating(white, 300), "820/1");
});

localTest("chess, through the functions: a resignation, a draw offer, and a game lost on time", async () => {
  const { white, black, third } = await chessCast();
  endGames(white, black, third);
  const party = await studyParty(white, [black, third]);

  // Resigning, after a draw offer is turned down.
  const resigned = await newGame(party, white, black);
  await fn("chess-move", white, { game_id: resigned, ply: 1, uci: "d2d4" });
  assert.equal((await fn("chess-draw", black, { game_id: resigned, action: "offer" })).status, 200);
  assert.equal((await fn("chess-draw", white, { game_id: resigned, action: "decline" })).status, 200);
  assert.equal((await fn("chess-draw", white, { game_id: resigned, action: "accept" })).body.error, "no_draw_offer");
  assert.equal((await fn("chess-resign", third, { game_id: resigned })).status, 403, "not their game");
  const gone = await fn("chess-resign", black, { game_id: resigned });
  assert.equal(gone.body.game.result, "white");

  // Live games, for a member to watch.
  const timed = await newGame(party, black, white);
  const live = await fn("chess-live-games", third, { party_id: party });
  assert.deepEqual(live.body.games.map((g: { id: string }) => g.id), [timed]);

  // On time: White (black's account here) moves, then lets the clock run out.
  await fn("chess-move", black, { game_id: timed, ply: 1, uci: "e2e4" });
  await fn("chess-move", white, { game_id: timed, ply: 2, uci: "e7e5" });
  assert.equal((await fn("chess-claim-timeout", white, { game_id: timed })).body.error, "not_timed_out");
  sql(`UPDATE public.chess_games SET last_move_at = now() - interval '181 seconds' WHERE id = '${timed}'`);
  const late = await fn("chess-move", black, { game_id: timed, ply: 3, uci: "g1f3" });
  assert.equal(late.body.error, "time_out");
  assert.equal(sql(`SELECT result || ' ' || result_reason FROM public.chess_games WHERE id = '${timed}'`), "black timeout");
});
