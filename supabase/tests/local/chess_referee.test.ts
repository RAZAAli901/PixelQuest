// The referee's database half (migrations 20261019000085 to 089): turn, ply, a game that's over,
// a move sent twice, and the clocks by the database's own time.
import assert from "node:assert/strict";
import { localTest, sql } from "./stack.ts";
import { chessCast, endGames, gameRow, studyParty } from "./chess_rls.test.ts";

const commit = (user: string, game: string, ply: number, uci: string, san: string, end = "NULL, NULL", flag = "'black', 'timeout'") =>
  sql(`SELECT public.chess_commit_move('${user}', '${game}', ${ply}, '${uci}', '${san}', 'fen-${ply}', ${end}, ${flag})`);
const field = (game: string, column: string) => sql(`SELECT ${column} FROM public.chess_games WHERE id = '${game}'`);

localTest("chess rules: the right turn and ply only, once each, and nothing after the end", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  const game = gameRow(party, white, black, "waiting");
  sql(`UPDATE public.chess_games SET started_at = NULL WHERE id = '${game}'`);

  assert.throws(() => commit(black.userId, game, 1, "e7e5", "e5"), /not_your_turn/);
  const first = JSON.parse(commit(white.userId, game, 1, "e2e4", "e4"));
  assert.equal(first.move.ply, 1);
  assert.equal(field(game, "status"), "active");
  // Sent again after a lost answer: the same move back, once.
  const again = JSON.parse(commit(white.userId, game, 1, "e2e4", "e4"));
  assert.equal(again.repeated, true);
  assert.equal(sql(`SELECT count(*) FROM public.chess_moves WHERE game_id = '${game}'`), "1");
  // A different move at a taken ply, a skipped ply, and the wrong player.
  assert.throws(() => commit(white.userId, game, 1, "d2d4", "d4"), /wrong_ply/);
  assert.throws(() => commit(black.userId, game, 3, "e7e5", "e5"), /wrong_ply/);
  assert.throws(() => commit(white.userId, game, 2, "d2d4", "d4"), /not_your_turn/);

  commit(black.userId, game, 2, "e7e5", "e5", "'black', 'checkmate'");
  assert.equal(field(game, "status || ' ' || result || ' ' || result_reason"), "finished black checkmate");
  assert.throws(() => commit(white.userId, game, 3, "d2d4", "d4"), /game_over/);
});

localTest("chess clocks: time used by the database's clock, a late move ends on time, the first moves are free", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  const game = gameRow(party, white, black, "waiting");
  // The first moves use no time, however long they took.
  sql(`UPDATE public.chess_games SET last_move_at = now() - interval '20 seconds' WHERE id = '${game}'`);
  commit(white.userId, game, 1, "e2e4", "e4");
  commit(black.userId, game, 2, "e7e5", "e5");
  assert.equal(field(game, "white_ms || ' ' || black_ms"), "180000 180000");

  // White thinks 5 seconds: 5 seconds off White's clock.
  sql(`UPDATE public.chess_games SET last_move_at = now() - interval '5 seconds' WHERE id = '${game}'`);
  commit(white.userId, game, 3, "g1f3", "Nf3");
  const whiteLeft = Number(field(game, "white_ms"));
  assert.ok(whiteLeft <= 175000 && whiteLeft > 174000, String(whiteLeft));

  // Black's clock runs out before the move arrives: it isn't taken, and Black loses on time.
  sql(`UPDATE public.chess_games SET last_move_at = now() - interval '200 seconds' WHERE id = '${game}'`);
  const late = JSON.parse(commit(black.userId, game, 4, "b8c6", "Nc6", "NULL, NULL", "'white', 'timeout'"));
  assert.equal(late.flagged, true);
  assert.equal(field(game, "result || ' ' || result_reason || ' ' || black_ms"), "white timeout 0");
  assert.equal(sql(`SELECT count(*) FROM public.chess_moves WHERE game_id = '${game}'`), "3");
});

localTest("chess: a timeout claim needs the clock out by the server's time; the sweep and the abort rule", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  const game = gameRow(party, white, black, "waiting");
  commit(white.userId, game, 1, "e2e4", "e4");
  commit(black.userId, game, 2, "e7e5", "e5");
  assert.throws(() => sql(`SELECT public.chess_claim_timeout('${black.userId}', '${game}', 'black', 'timeout')`), /not_timed_out/);
  sql(`UPDATE public.chess_games SET last_move_at = now() - interval '181 seconds' WHERE id = '${game}'`);
  assert.equal(sql(`SELECT count(*) FROM public.chess_due_games() WHERE id = '${game}'`), "1", "the sweep sees it");
  sql(`SELECT public.chess_claim_timeout(NULL, '${game}', 'black', 'timeout')`);
  assert.equal(field(game, "result || ' ' || white_ms"), "black 0");

  // No first move within 30 seconds: aborted, unrated.
  const stalled = gameRow(party, white, black, "waiting");
  sql(`UPDATE public.chess_games SET last_move_at = now() - interval '31 seconds' WHERE id = '${stalled}'`);
  assert.ok(Number(sql("SELECT public.chess_abort_stalled()")) >= 1);
  assert.equal(field(stalled, "status || ' ' || result_reason || ' ' || rating_applied"), "aborted aborted false");
});

localTest("chess reconnect: the game's state alone rebuilds the board and both clocks", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  const game = gameRow(party, white, black, "waiting");
  commit(white.userId, game, 1, "e2e4", "e4");
  commit(black.userId, game, 2, "c7c5", "c5");
  commit(white.userId, game, 3, "g1f3", "Nf3");
  // A phone that dropped out asks again: every move in order, the clocks, and the server's time.
  const state = JSON.parse(sql(`SELECT public.chess_game_state('${black.userId}', '${game}')`));
  assert.deepEqual(state.moves, ["e2e4", "c7c5", "g1f3"]);
  assert.equal(state.game.ply, 3);
  assert.equal(state.game.turn, "b");
  assert.ok(Date.parse(state.now) >= Date.parse(state.game.last_move_at), "the server's now, to correct the phone's clock");
  assert.throws(() => sql(`SELECT public.chess_game_state('${party}', '${game}')`), /not_a_player/);
});
