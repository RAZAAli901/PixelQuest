// Chess's tables and who reads them (migrations 20261019000075 to 082, docs/chess-design.md).
import assert from "node:assert/strict";
import { call, localTest, newPlayer, sql, type Session } from "./stack.ts";

export type Player = Session & { username: string };
const fresh = (prefix: string) => `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
export const rpc = (who: Session | undefined, fn: string, body: unknown) => call(`/rest/v1/rpc/${fn}`, { token: who?.accessToken, body });

async function player(tag: string): Promise<Player> {
  const session = await newPlayer(tag);
  const username = fresh(tag.slice(0, 8));
  assert.equal((await rpc(session, "set_username", { name: username })).body?.status, "ok");
  return { ...session, username };
}

let castPromise: Promise<{ white: Player; black: Player; third: Player; outsider: Player }> | undefined;
/** One cast for every chess test file, to keep under the stack's sign-in limit. */
export function chessCast() {
  castPromise ??= (async () => {
    const [white, black, third, outsider] = await Promise.all(["cw", "cb", "ct", "co"].map((t) => player(`chess_${t}`)));
    return { white, black, third, outsider };
  })();
  return castPromise;
}

/** A study party led by [leader] with [members] in it. */
export async function studyParty(leader: Player, members: Player[]): Promise<string> {
  const made = await rpc(leader, "create_party", { name: fresh("Chess"), max_members: 10, party_type: "study" });
  assert.equal(made.status, 200, JSON.stringify(made.body));
  sql(`DELETE FROM private.invite_log WHERE inviter = '${leader.userId}'`);
  for (const m of members) {
    await rpc(leader, "invite_member", { party_id: made.body, username: m.username });
    await rpc(m, "respond_invite", { party_id: made.body, accept: true });
  }
  return made.body;
}

/** A game straight in the table, as the functions would make it. */
export function gameRow(party: string, white: Session, black: Session, status = "active"): string {
  return sql(
    `INSERT INTO public.chess_games (party_id, white_id, black_id, time_control_sec, status, white_ms, black_ms, last_move_at, started_at)
     VALUES ('${party}', '${white.userId}', '${black.userId}', 180, '${status}', 180000, 180000, now(), now()) RETURNING id`,
  ).split("\n")[0];
}

/** Ends every live game of these players, so each test starts free. */
export function endGames(...who: Session[]) {
  const ids = who.map((w) => `'${w.userId}'`).join(",");
  sql(`UPDATE public.chess_games SET status = 'aborted', result_reason = 'aborted', ended_at = now() WHERE status IN ('waiting','active') AND (white_id IN (${ids}) OR black_id IN (${ids}))`);
}

const read = async (who: Session, path: string) => (await call(`/rest/v1/${path}`, { token: who.accessToken })).body;

localTest("chess: a party's members read its games, moves and challenges; outsiders read nothing", async () => {
  const { white, black, third, outsider } = await chessCast();
  endGames(white, black, third);
  const party = await studyParty(white, [black, third]);
  const game = gameRow(party, white, black);
  sql(`INSERT INTO public.chess_moves (game_id, ply, san, uci, fen_after, white_ms_after, black_ms_after) VALUES ('${game}', 1, 'e4', 'e2e4', 'x', 179000, 180000)`);
  sql(`INSERT INTO public.chess_challenges (party_id, from_user, time_control_sec, expires_at) VALUES ('${party}', '${third.userId}', 300, now() + interval '10 minutes')`);
  sql(`INSERT INTO public.chess_ratings (user_id, time_control_sec, int_points, peak) VALUES ('${white.userId}', 180, 812, 812) ON CONFLICT DO NOTHING`);

  for (const who of [white, third]) {
    assert.equal((await read(who, `chess_games?id=eq.${game}&select=id`)).length, 1);
    assert.equal((await read(who, `chess_moves?game_id=eq.${game}&select=ply`)).length, 1);
    assert.equal((await read(who, `chess_challenges?party_id=eq.${party}&select=id`)).length, 1);
    assert.equal((await read(who, `chess_ratings?user_id=eq.${white.userId}&select=int_points`)).length, 1, "a party mate's rating");
  }
  assert.deepEqual(await read(outsider, `chess_games?id=eq.${game}&select=id`), []);
  assert.deepEqual(await read(outsider, `chess_moves?game_id=eq.${game}&select=ply`), []);
  assert.deepEqual(await read(outsider, `chess_challenges?party_id=eq.${party}&select=id`), []);
  assert.deepEqual(await read(outsider, `chess_ratings?user_id=eq.${white.userId}&select=int_points`), []);
  // The rules are everyone's.
  assert.equal((await read(outsider, "chess_time_controls?select=seconds")).length, 4);
  assert.equal((await read(outsider, "chess_config?select=start_rating"))[0].start_rating, 800);
  // Signed out: nothing.
  assert.equal((await call(`/rest/v1/chess_games?select=id`)).status, 401);

  // Someone removed from the party loses it at once.
  assert.deepEqual((await rpc(white, "remove_member", { party_id: party, user_id: third.userId })).body, { status: "removed" });
  assert.deepEqual(await read(third, `chess_games?id=eq.${game}&select=id`), []);
});

localTest("chess: no player writes a game, a move, a rating or a record directly", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  const game = gameRow(party, white, black);
  const write = (path: string, method: string, body: unknown) => call(`/rest/v1/${path}`, { method, token: white.accessToken, body });

  assert.notEqual((await write("chess_moves", "POST", { game_id: game, ply: 1, san: "e4", uci: "e2e4", fen_after: "x", white_ms_after: 1, black_ms_after: 1 })).status, 201);
  assert.notEqual((await write("chess_games", "POST", { party_id: party, white_id: white.userId, black_id: black.userId, time_control_sec: 60, white_ms: 1, black_ms: 1 })).status, 201);
  const won = await write(`chess_games?id=eq.${game}`, "PATCH", { status: "finished", result: "white", result_reason: "resign", ended_at: new Date().toISOString() });
  assert.ok(won.status >= 400 || (Array.isArray(won.body) && won.body.length === 0));
  assert.equal(sql(`SELECT status FROM public.chess_games WHERE id = '${game}'`), "active");
  assert.notEqual((await write("chess_ratings", "POST", { user_id: white.userId, time_control_sec: 60, int_points: 3000, peak: 3000 })).status, 201);
  const low = [white.userId, black.userId].sort();
  assert.notEqual((await write("chess_head_to_head", "POST", { user_low: low[0], user_high: low[1], time_control_sec: 60, low_wins: 99 })).status, 201);
  assert.notEqual((await write("chess_challenges", "POST", { party_id: party, from_user: white.userId, time_control_sec: 60, expires_at: new Date().toISOString() })).status, 201);
});

localTest("chess: constraints: a time control from the list, two players, one live game each", async () => {
  const { white, black, third } = await chessCast();
  endGames(white, black, third);
  const party = await studyParty(white, [black, third]);
  gameRow(party, white, black);
  assert.throws(() => gameRow(party, white, third), /already_playing/, "white already has a live game");
  assert.throws(() => gameRow(party, third, third), /chess_games_two_players/);
  assert.throws(
    () => sql(`INSERT INTO public.chess_games (party_id, white_id, black_id, time_control_sec, white_ms, black_ms) VALUES ('${party}', '${third.userId}', '${white.userId}', 120, 1, 1)`),
    /chess_games_time_control|already_playing/,
  );
  endGames(white, black, third);
  assert.throws(
    () => sql(`INSERT INTO public.chess_games (party_id, white_id, black_id, time_control_sec, white_ms, black_ms) VALUES ('${party}', '${third.userId}', '${white.userId}', 120, 1, 1)`),
    /chess_games_time_control/,
  );
  assert.throws(
    () => sql(`UPDATE public.chess_games SET status = 'finished' WHERE party_id = '${party}' AND status = 'aborted'`),
    /chess_games_ended/,
    "finished needs a result",
  );
});

localTest("chess spectators: only the party's active members may join a game's channel", async () => {
  const { white, black, third, outsider } = await chessCast();
  endGames(white, black, third);
  const party = await studyParty(white, [black, third]);
  const game = gameRow(party, white, black);
  const mayWatch = (who: Session, topic = `chess:game:${game}`) =>
    sql(`BEGIN; SET LOCAL ROLE authenticated; SELECT set_config('request.jwt.claims', '{"sub":"${who.userId}","role":"authenticated"}', true); SELECT private.chess_may_watch('${topic}'); COMMIT;`).split("\n").filter((l) => l === "t" || l === "f").pop();
  assert.equal(mayWatch(third), "t", "a member who isn't playing");
  assert.equal(mayWatch(white), "t");
  assert.equal(mayWatch(outsider), "f");
  assert.equal(mayWatch(third, `chess:game:${game}x`), "f", "only a real game's topic");
  // Each change to the game goes out on its channel, with its move.
  sql(`SELECT public.chess_commit_move('${white.userId}', '${game}', 1, 'e2e4', 'e4', 'fen', NULL, NULL, 'black', 'timeout')`);
  const sent = JSON.parse(sql(`SELECT payload FROM realtime.messages WHERE topic = 'chess:game:${game}' ORDER BY inserted_at DESC LIMIT 1`));
  assert.equal(sent.move.uci, "e2e4");
  assert.equal(sent.game.ply, 1);
});
