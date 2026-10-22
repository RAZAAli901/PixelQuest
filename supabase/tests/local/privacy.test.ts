// Privacy (J173, docs/PRIVACY_REVIEW.md): export_my_data hands a player everything the cloud keeps
// about them and nobody else's; the watch log keeps a day; deleting an account leaves no row that
// still names it, apart from the anonymised kinds the review lists.
// Run with the local stack: SUPABASE_LOCAL=1 node --test supabase/tests/local/privacy.test.ts
import assert from "node:assert/strict";
import { call, localTest, newPlayer, sql } from "./stack.ts";
import { chessCast, endGames, gameRow, rpc, studyParty } from "./chess_rls.test.ts";

const send = (who: { accessToken: string }, party: string, body: string) =>
  call(`/rest/v1/chat_messages`, { token: who.accessToken, body: { party_id: party, kind: "text", body }, headers: { Prefer: "return=representation" } });

localTest("privacy: export_my_data has the player's chat, files, chess and parties, and nobody else's", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  const mine = `mine ${Math.random()}`;
  const theirs = `theirs ${Math.random()}`;
  assert.equal((await send(white, party, mine)).status, 201);
  assert.equal((await send(black, party, theirs)).status, 201);
  const game = gameRow(party, white, black);
  sql(`INSERT INTO public.chess_moves (game_id, ply, san, uci, fen_after, white_ms_after, black_ms_after) VALUES ('${game}', 1, 'e4', 'e2e4', 'x', 179000, 180000)`);
  sql(`INSERT INTO public.device_tokens (token, user_id, platform) VALUES ('tok-${white.userId}', '${white.userId}', 'android') ON CONFLICT DO NOTHING`);

  const exported = await rpc(white, "export_my_data", {});
  assert.equal(exported.status, 200);
  const data = exported.body;
  assert.equal(data.user_id, white.userId);
  assert.equal(data.username.username, white.username);
  assert.ok(data.parties.some((p: { party_id: string }) => p.party_id === party));
  const bodies = data.chat_messages_sent.map((m: { body: string }) => m.body);
  assert.ok(bodies.includes(mine), "their own message");
  assert.ok(!bodies.includes(theirs), "not a party mate's");
  const exportedGame = data.chess_games.find((g: { id: string }) => g.id === game);
  assert.deepEqual(exportedGame.moves.map((m: { san: string }) => m.san), ["e4"]);
  assert.ok(Array.isArray(data.chat_files_shared) && Array.isArray(data.chess_ratings) && Array.isArray(data.reports_made.chess));
  assert.ok(data.push_phones >= 1);
  assert.ok(!JSON.stringify(data).includes(`tok-${white.userId}`), "push tokens aren't handed out");
  assert.equal((await rpc(undefined, "export_my_data", {})).status, 401);
  endGames(white, black);
  sql(`DELETE FROM public.device_tokens WHERE token = 'tok-${white.userId}'`);
});

localTest("privacy: the chess watch log keeps a day", async () => {
  const { white, black, third } = await chessCast();
  endGames(white, black, third);
  const party = await studyParty(white, [black, third]);
  const game = gameRow(party, white, black);
  sql(`DELETE FROM public.chess_watch_log WHERE user_id = '${third.userId}'`);
  sql(`INSERT INTO public.chess_watch_log (user_id, game_id, joined_at) VALUES ('${third.userId}', '${game}', now() - interval '2 days')`);
  assert.deepEqual((await rpc(third, "chess_watch", { game_id: game })).status, 200);
  assert.equal(sql(`SELECT count(*) FROM public.chess_watch_log WHERE user_id = '${third.userId}'`), "1", "only today's");
  endGames(white, black);
});

localTest("privacy: a deleted account leaves no row naming it but the anonymised kinds", async () => {
  const { white } = await chessCast();
  endGames(white);
  const leaving = await newPlayer("privacy_leaving");
  const name = `pv_${Math.random().toString(36).slice(2, 8)}`;
  assert.equal((await rpc(leaving, "set_username", { name })).body?.status, "ok");
  const party = await studyParty(white, [{ ...leaving, username: name }]);
  assert.equal((await send(leaving, party, "bye")).status, 201);
  const game = gameRow(party, white, leaving);
  sql(`SELECT private.chess_finish('${game}', 'white', 'resign')`);
  assert.equal((await rpc(leaving, "block_member", { user_id: white.userId })).status, 204);
  sql(`INSERT INTO public.chess_watch_log (user_id, game_id) VALUES ('${leaving.userId}', '${game}')`);

  assert.equal((await rpc(leaving, "delete_user_account", {})).status, 204);
  // Every column in public that references a user, found from the catalogue so new ones are covered.
  const columns = sql(`
    SELECT c.conrelid::regclass || '.' || a.attname FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
    WHERE c.contype = 'f' AND c.confrelid = 'auth.users'::regclass AND c.connamespace = 'public'::regnamespace`).split("\n").filter(Boolean);
  assert.ok(columns.length > 20, columns.join(","));
  for (const column of columns) {
    const [table, col] = column.split(".");
    assert.equal(sql(`SELECT count(*) FROM ${table} WHERE ${col} = '${leaving.userId}'`), "0", column);
  }
  // The anonymised kinds: the game stays with their side empty; the pair's record stays by id.
  assert.equal(sql(`SELECT count(*) FROM public.chess_games WHERE id = '${game}' AND black_id IS NULL`), "1");
  assert.equal(sql(`SELECT count(*) FROM private.invite_log WHERE inviter = '${leaving.userId}'`), "0");
});
