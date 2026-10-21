// The database's rating maths and farming rules (migrations 20261019000090 to 092), held to the
// pure functions in supabase/functions/_shared/chess_rating.ts.
import assert from "node:assert/strict";
import { call, localTest, sql, type Session } from "./stack.ts";
import { ratingChanges } from "../../functions/_shared/chess_rating.ts";
import { chessCast, endGames, gameRow, studyParty } from "./chess_rls.test.ts";

localTest("chess: private.chess_elo gives the pure function's answers", () => {
  const cases: [number, number, number, number, 0 | 0.5 | 1][] = [];
  for (const ra of [100, 105, 800, 1200, 1601, 2100]) {
    for (const rb of [100, 800, 1500, 2400]) {
      for (const ga of [0, 9, 10, 40]) cases.push([ra, ga, rb, 30, ([0, 0.5, 1] as const)[(ra + rb + ga) % 3]]);
    }
  }
  const rows = sql(
    "SELECT string_agg(array_to_string(private.chess_elo(ra, ga, rb, gb, s, 40, 10, 32, 16, 1600, 100), ':'), ',' ORDER BY n) FROM (VALUES " +
      cases.map(([ra, ga, rb, gb, s], n) => `(${n}, ${ra}, ${ga}, ${rb}, ${gb}, ${s}::numeric)`).join(",") +
      ") v(n, ra, ga, rb, gb, s)",
  ).split(",");
  cases.forEach(([ra, ga, rb, gb, s], n) => {
    assert.equal(rows[n], ratingChanges({ points: ra, games: ga }, { points: rb, games: gb }, s).join(":"), `case ${n}: ${ra}/${ga} v ${rb}/${gb} scoring ${s}`);
  });
});

localTest("chess: a finished rated game moves both ratings once; farmed games move none", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  sql(`DELETE FROM public.chess_ratings WHERE user_id IN ('${white.userId}', '${black.userId}')`);
  const points = () => sql(`SELECT string_agg(int_points || '/' || games, ' ' ORDER BY user_id = '${black.userId}') FROM public.chess_ratings WHERE user_id IN ('${white.userId}', '${black.userId}') AND time_control_sec = 180`);
  const play = (ply: number, result: string, reason: string) => {
    const game = gameRow(party, white, black);
    sql(`UPDATE public.chess_games SET ply = ${ply} WHERE id = '${game}'`);
    sql(`SELECT private.chess_finish('${game}', '${result}', '${reason}')`);
    sql(`SELECT private.chess_finish('${game}', 'black', 'resign')`); // Asked again: nothing.
    return game;
  };

  const first = play(20, "white", "resign");
  assert.equal(points(), "820/1 780/1", "both new: K 40, equal ratings");
  assert.equal(sql(`SELECT white_change || ' ' || black_change || ' ' || rating_applied FROM public.chess_games WHERE id = '${first}'`), "20 -20 true");

  // Under 4 moves each: no change, unless it was mate.
  play(6, "black", "resign");
  assert.equal(points(), "820/1 780/1");
  play(5, "white", "checkmate");
  assert.equal(points().split(" ")[0].split("/")[1], "2");

  // The pair's 10 a day: the 11th counts in head-to-head only.
  sql(`UPDATE public.chess_games SET rating_applied = TRUE WHERE id IN (SELECT id FROM public.chess_games WHERE party_id = '${party}' LIMIT 1)`);
  for (let i = 0; i < 8; i++) play(20, "draw", "draw_agreed");
  const before = points();
  const h2h = () => Number(sql(`SELECT low_wins + high_wins + draws FROM public.chess_head_to_head WHERE time_control_sec = 180 AND '${white.userId}' IN (user_low, user_high) AND '${black.userId}' IN (user_low, user_high)`));
  const games = h2h();
  play(20, "white", "resign");
  assert.equal(points(), before, "farmed: no rating change");
  assert.equal(h2h(), games + 1, "but it's in the head-to-head");
});

localTest("chess views: head-to-head per time control and total, the party board, profile stats; party mates only", async () => {
  const { white, black, third, outsider } = await chessCast();
  endGames(white, black, third);
  const party = await studyParty(white, [black, third]);
  // The farming test's games were today: moved back, so these count.
  sql(`UPDATE public.chess_games SET ended_at = ended_at - interval '2 days' WHERE ended_at IS NOT NULL AND '${white.userId}' IN (white_id, black_id) AND '${black.userId}' IN (white_id, black_id)`);
  for (const [tc, result] of [[180, "white"], [180, "draw"], [600, "black"]] as const) {
    const game = sql(`INSERT INTO public.chess_games (party_id, white_id, black_id, time_control_sec, status, ply, white_ms, black_ms, last_move_at)
      VALUES ('${party}', '${white.userId}', '${black.userId}', ${tc}, 'active', 20, 1, 1, now()) RETURNING id`).split("\n")[0];
    sql(`SELECT private.chess_finish('${game}', '${result}', '${result === "draw" ? "draw_agreed" : "resign"}')`);
  }
  const low = [white.userId, black.userId].sort()[0];
  const view = async (who: Session, path: string) => (await call(`/rest/v1/${path}`, { token: who.accessToken })).body;
  const total = (await view(third, `chess_head_to_head_totals?user_low=eq.${low}&time_control_sec=is.null`))[0];
  assert.ok(total.games >= 3 && total.draws >= 1, JSON.stringify(total));
  assert.deepEqual(await view(outsider, `chess_head_to_head_totals?user_low=eq.${low}`), [], "outsiders see no record");

  const board = await view(third, `chess_party_leaderboard?party_id=eq.${party}&time_control_sec=eq.600&order=place`);
  assert.equal(board.length, 3, "every active member, games or not");
  assert.equal(board[0].user_id, black.userId, "Black won the 10-minute game");
  assert.equal(board.find((r: { user_id: string }) => r.user_id === third.userId).int_points, 800);
  assert.deepEqual(await view(outsider, `chess_party_leaderboard?party_id=eq.${party}`), []);

  const stats = await view(third, `chess_profile_stats?user_id=eq.${white.userId}&time_control_sec=eq.180`);
  assert.equal(stats[0].tier, "Pawn");
  assert.ok(stats[0].games >= 2);
  assert.deepEqual(await view(outsider, `chess_profile_stats?user_id=eq.${white.userId}`), []);
});
