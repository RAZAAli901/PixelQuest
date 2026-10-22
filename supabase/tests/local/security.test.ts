// Phase F's security pass (J170) for chat, files and chess: another party's things are out of
// reach, tampered requests are refused, a move sent again is taken once, a removed member loses
// everything at once, and a signed file link stops working when it expires.
import assert from "node:assert/strict";
import { call, localTest, sql, type Session } from "./stack.ts";
import { chessCast, endGames, gameRow, studyParty, type Player } from "./chess_rls.test.ts";

const fn = (name: string, who: Session, body: unknown) => call(`/functions/v1/${name}`, { token: who.accessToken, body });
const read = async (who: Session, path: string) => (await call(`/rest/v1/${path}`, { token: who.accessToken })).body;

localTest("security: another party's chat, files and chess are out of reach", async () => {
  const { white, black, third, outsider } = await chessCast();
  endGames(white, black, third, outsider);
  const mine = await studyParty(white, [black]);
  const theirs = await studyParty(third, [outsider]);
  const challenge = sql(`SELECT public.chess_create_challenge('${third.userId}', '${theirs}', NULL, 300, 'random', true) ->> 'id'`);
  const game = gameRow(theirs, third, outsider);
  sql(`INSERT INTO public.chat_messages (party_id, sender_id, kind, body) VALUES ('${theirs}', '${third.userId}', 'text', 'private plans')`);

  assert.deepEqual(await read(white, `chat_messages?party_id=eq.${theirs}&select=body`), []);
  assert.deepEqual(await read(white, `chat_files?party_id=eq.${theirs}&select=id`), []);
  assert.deepEqual(await read(white, `chess_games?id=eq.${game}&select=id`), []);
  assert.deepEqual(await read(white, `chess_moves?game_id=eq.${game}&select=ply`), []);
  assert.equal((await fn("chess-live-games", white, { party_id: theirs })).status, 403);
  assert.equal((await fn("chess-move", white, { game_id: game, ply: 1, uci: "e2e4" })).body.error, "not_a_player");
  assert.equal((await fn("chess-resign", white, { game_id: game })).status, 403);
  assert.equal((await fn("chess-respond-challenge", white, { challenge_id: challenge, accept: true })).body.error, "not_found");
  // Challenging someone in a party one isn't in.
  assert.equal((await fn("chess-create-challenge", white, { party_id: theirs, to_user: third.userId, time_control: 60 })).body.error, "not_a_member");
  assert.equal((await fn("chess-create-challenge", white, { party_id: mine, to_user: third.userId, time_control: 60 })).body.error, "not_a_member");
  assert.equal((await fn("request-upload", white, { party_id: theirs, name: "x.pdf", mime: "application/pdf", size: 10 })).status, 403);
});

localTest("security: tampered requests are refused or put right", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  const game = gameRow(party, white, black, "waiting");
  sql(`UPDATE public.chess_games SET last_move_at = now() WHERE id = '${game}'`);

  for (const bad of [{ ply: 1, uci: "e2e9" }, { ply: 0, uci: "e2e4" }, { ply: 1, uci: "e2e4; DROP TABLE x" }, { ply: "1", uci: "e2e4" }]) {
    const res = await fn("chess-move", white, { game_id: game, ...bad });
    assert.ok(res.status >= 400 && res.status < 500, `${JSON.stringify(bad)} -> ${res.status}`);
  }
  assert.equal((await fn("chess-create-challenge", white, { party_id: party, to_user: black.userId, time_control: 999 })).body.error, "bad_time_control");
  assert.equal((await fn("chess-draw", white, { game_id: game, action: "win" })).status, 400);
  assert.equal((await fn("chess-move", white, { game_id: "not-a-uuid", ply: 1, uci: "e2e4" })).status, 400);
  // A forged token, and none at all.
  assert.equal((await call(`/functions/v1/chess-move`, { token: "forged.token.here", body: { game_id: game, ply: 1, uci: "e2e4" } })).status, 401);
  assert.equal((await call(`/functions/v1/chess-sweep`, { token: white.accessToken, body: {} })).status, 401, "the sweep is the server's");
  assert.equal((await call(`/functions/v1/send-push`, { token: white.accessToken, body: { kind: "draw_offered", users: [black.userId] } })).status, 401);

  // A chat message dressed up as the server's line, or as someone else's: put right by the guard.
  const forged = await call(`/rest/v1/chat_messages`, {
    method: "POST", token: white.accessToken, headers: { Prefer: "return=representation" },
    body: { party_id: party, kind: "text", body: "hello", sender_id: black.userId, ref_id: game },
  });
  if (forged.status === 201) {
    assert.equal(forged.body[0].sender_id, white.userId, "sent as the sender, whoever it says");
    assert.equal(forged.body[0].ref_id, null, "only the server points a line at a game");
  } else {
    assert.ok(forged.status >= 400);
  }
  const system = await call(`/rest/v1/chat_messages`, { method: "POST", token: white.accessToken, body: { party_id: party, kind: "system", body: "chess_game_result" } });
  assert.ok(system.status >= 400, "members can't post the server's lines");
});

localTest("security: a move sent again is taken once; an old ply can't be rewritten", async () => {
  const { white, black } = await chessCast();
  endGames(white, black);
  const party = await studyParty(white, [black]);
  const game = gameRow(party, white, black, "waiting");
  sql(`UPDATE public.chess_games SET last_move_at = now() WHERE id = '${game}'`);
  const first = await fn("chess-move", white, { game_id: game, ply: 1, uci: "e2e4" });
  assert.equal(first.status, 200);
  const replays = await Promise.all([1, 2, 3].map(() => fn("chess-move", white, { game_id: game, ply: 1, uci: "e2e4" })));
  assert.deepEqual(replays.map((r) => r.body.repeated), [true, true, true]);
  assert.equal(sql(`SELECT count(*) FROM public.chess_moves WHERE game_id = '${game}'`), "1");
  assert.equal((await fn("chess-move", white, { game_id: game, ply: 1, uci: "d2d4" })).body.error, "wrong_ply");
  // Black, replaying White's move as their own.
  assert.ok((await fn("chess-move", black, { game_id: game, ply: 1, uci: "e2e4" })).status >= 400);
});

localTest("security: a removed member loses chat, files and chess at once", async () => {
  const { white, black, third } = await chessCast();
  endGames(white, black, third);
  const party = await studyParty(white, [black, third]);
  const game = gameRow(party, white, black);
  sql(`INSERT INTO public.chat_messages (party_id, sender_id, kind, body) VALUES ('${party}', '${white.userId}', 'text', 'see you')`);
  assert.equal((await read(third, `chess_games?id=eq.${game}&select=id`)).length, 1);
  assert.equal((await rpc(third, "chess_watch", { game_id: game })).body.status, "watching");

  assert.deepEqual((await rpc(white, "remove_member", { party_id: party, user_id: third.userId })).body, { status: "removed" });
  assert.deepEqual(await read(third, `chess_games?id=eq.${game}&select=id`), []);
  assert.deepEqual(await read(third, `chat_messages?party_id=eq.${party}&select=id`), []);
  assert.equal((await rpc(third, "chess_watch", { game_id: game })).body.message, "not_found");
  assert.equal((await fn("chess-live-games", third, { party_id: party })).status, 403);
  assert.equal((await fn("chess-create-challenge", third, { party_id: party, to_user: white.userId, time_control: 60 })).body.error, "not_a_member");
  // A player removed mid-game can't move; their clock runs on, so the game still ends (20261019000105).
  assert.deepEqual((await rpc(white, "remove_member", { party_id: party, user_id: black.userId })).body, { status: "removed" });
  assert.equal((await fn("chess-move", black, { game_id: game, ply: 1, uci: "e7e5" })).body.error, "not_a_player");
});

// A signed file link stopping once it expires is tested in study_files.test.ts ("a removed member
// loses messages and files at once, and a download link expires"), run with this file in J170.

const rpc = (who: Session | Player, name: string, body: unknown) => call(`/rest/v1/rpc/${name}`, { token: who.accessToken, body });

localTest("deletion: the account's games stay for the opponent, anonymised; its files are cleaned from storage", async () => {
  const { newPlayer } = await import("./stack.ts");
  const { white } = await chessCast();
  endGames(white);
  const leaving = await newPlayer("chess_leaving");
  const name = `lv_${Math.random().toString(36).slice(2, 8)}`;
  assert.equal((await rpc(leaving, "set_username", { name })).body?.status, "ok");
  const party = await studyParty(white, [{ ...leaving, username: name }]);
  const done = gameRow(party, white, leaving);
  sql(`UPDATE public.chess_games SET ply = 20 WHERE id = '${done}'`);
  sql(`SELECT private.chess_finish('${done}', 'white', 'resign')`);
  const live = gameRow(party, white, leaving);

  assert.equal((await rpc(leaving, "delete_user_account", {})).status, 204);
  assert.equal(sql(`SELECT coalesce(black_id::text, 'gone') || ' ' || result FROM public.chess_games WHERE id = '${done}'`), "gone white");
  assert.equal(sql(`SELECT status FROM public.chess_games WHERE id = '${live}'`), "aborted", "a live game ends first");
  const low = [white.userId, leaving.userId].sort();
  assert.equal(sql(`SELECT count(*) FROM public.chess_head_to_head WHERE user_low = '${low[0]}' AND user_high = '${low[1]}'`), "1", "the opponent's record stays");
  assert.equal((await read(white, `chess_head_to_head?user_low=eq.${low[0]}&user_high=eq.${low[1]}&select=time_control_sec`)).length, 1, "and they can read it");
  assert.equal(sql(`SELECT count(*) FROM public.chess_ratings WHERE user_id = '${leaving.userId}'`), "0", "their own ratings go");

  // Their file bytes are orphans now: cleanup-uploads lists them for deleting.
  sql(`INSERT INTO storage.objects (bucket_id, name, owner, created_at) VALUES ('study-files', '${party}/left-behind', NULL, now() - interval '2 hours') ON CONFLICT DO NOTHING`);
  assert.ok(sql(`SELECT array_to_string(public.chat_expire_uploads(1000), ',')`).includes(`${party}/left-behind`));
  // Storage refuses direct deletes: through its API, as cleanup-uploads does.
  const { asService } = await import("./stack.ts");
  await asService(`/storage/v1/object/study-files`, { method: "DELETE", body: { prefixes: [`${party}/left-behind`] } });
});

localTest("security: only active members reach chat and chess; a leader can't retype or finish a party (S013)", async () => {
  const { white, black, third, outsider } = await chessCast();
  endGames(white, black, third);
  const party = await studyParty(white, [black]);
  const game = gameRow(party, white, black);
  sql(`INSERT INTO public.chat_messages (party_id, sender_id, kind, body) VALUES ('${party}', '${white.userId}', 'text', 'members only')`);
  sql(`DELETE FROM private.invite_log WHERE inviter = '${white.userId}'`);

  // Invited, not yet a member: the party's name only, none of its chat or chess.
  await rpc(white, "invite_member", { party_id: party, username: third.username });
  assert.equal((await read(third, `parties?id=eq.${party}&select=id`)).length, 1, "an invitee sees the invitation");
  assert.deepEqual(await read(third, `chat_messages?party_id=eq.${party}&select=id`), []);
  assert.deepEqual(await read(third, `chess_games?id=eq.${game}&select=id`), []);
  assert.deepEqual(await read(third, `party_members?party_id=eq.${party}&user_id=neq.${third.userId}&select=user_id`), [], "nor its members");

  // A member who left: nothing.
  const accepted = await rpc(third, "respond_invite", { party_id: party, accept: true });
  const after = await read(third, `chat_messages?party_id=eq.${party}&select=id`);
  // As a member they read it (with the line saying they joined).
  assert.ok(Array.isArray(after) && after.length >= 1, JSON.stringify({ accepted: accepted.body, after }));
  await rpc(third, "leave_party", { party_id: party });
  assert.deepEqual(await read(third, `chat_messages?party_id=eq.${party}&select=id`), []);
  assert.deepEqual(await read(third, `chess_games?id=eq.${game}&select=id`), []);

  // The leader can rename, but not change what the party is, finish it, or hand it to an outsider.
  const patch = (body: unknown) => call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: white.accessToken, body });
  assert.ok([200, 204].includes((await patch({ name: "Renamed" })).status));
  for (const body of [{ party_type: "darood" }, { completed_at: new Date().toISOString() }, { leader_id: outsider.userId }, { max_members: 999 }]) {
    const res = await patch(body);
    assert.ok([401, 403].includes(res.status), `refused: ${Object.keys(body)[0]}`);
  }
  assert.equal(sql(`SELECT party_type || ' ' || (completed_at IS NULL) || ' ' || (leader_id = '${white.userId}') FROM public.parties WHERE id = '${party}'`), "study true true");
  endGames(white, black);
});

localTest("security: storage is closed to players; files go only through the server (S016)", async () => {
  const { white } = await chessCast();
  const storage = (path: string, init: { method?: string; body?: unknown; raw?: string } = {}) =>
    call(`/storage/v1/${path}`, { method: init.method, token: white.accessToken, body: init.body });
  // No bucket can be made, so nothing public can appear.
  const bucket = await storage("bucket", { method: "POST", body: { id: `zz-${Date.now()}`, name: `zz-${Date.now()}`, public: true } });
  assert.ok(bucket.status >= 400, `creating a bucket is refused (${bucket.status})`);
  // study-files: no upload, no listing, no download, for a player even of their own party's path.
  const party = sql(`SELECT id FROM public.parties LIMIT 1`).split("\n")[0] || "00000000-0000-0000-0000-000000000000";
  const upload = await storage(`object/study-files/${party}/zz-direct.txt`, { method: "POST", body: "hello" });
  assert.ok(upload.status >= 400, `a direct upload is refused (${upload.status})`);
  const list = await storage("object/list/study-files", { method: "POST", body: { prefix: party } });
  assert.ok(list.status >= 400 || (Array.isArray(list.body) && list.body.length === 0), "nothing is listed");
  assert.equal(sql(`SELECT public FROM storage.buckets WHERE id = 'study-files'`), "f", "the bucket is private");
  assert.equal(sql(`SELECT count(*) FROM storage.buckets WHERE public`), "0", "no public bucket");
});

localTest("security: what a phone sends is checked for form and length (S019)", async () => {
  const { white } = await chessCast();
  const made = await rpc(white, "create_party", { name: `Tz ${Date.now() % 100000}`, max_members: 5, milestone: 1000 });
  assert.equal(made.status, 200, JSON.stringify(made.body));
  const party = made.body;
  const entry = (tz: string) => rpc(white, "add_darood_entry", { party_id: party, amount: 1, local_date: new Date().toISOString().slice(0, 10), tz, client_id: crypto.randomUUID() });
  assert.equal((await entry("Asia/Karachi")).status, 200, "a real zone");
  for (const bad of ["<script>alert(1)</script>", "Asia/Karachi; DROP TABLE x", "x".repeat(65)]) {
    assert.ok((await entry(bad)).status >= 400, `refused: ${bad.slice(0, 20)}`);
  }
  assert.ok((await rpc(white, "set_party_week_tz", { party_id: party, tz: "a".repeat(200) })).status >= 400, "a party's week zone too");
});
