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
