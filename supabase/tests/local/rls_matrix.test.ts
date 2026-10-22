// The RLS matrix through the real API (S023 to S025): every table, view and RPC is found from the
// catalogue, so a new one is covered without editing this file.
// Run with the local stack: SUPABASE_LOCAL=1 node --test supabase/tests/local/rls_matrix.test.ts
import assert from "node:assert/strict";
import { call, localTest, sql } from "./stack.ts";
import { chessCast, endGames, gameRow, rpc, studyParty } from "./chess_rls.test.ts";

const list = (q: string) => sql(q).split("\n").map((s) => s.trim()).filter(Boolean);

localTest("anon: every public table and view refuses a signed-out read and write (S023)", async () => {
  const relations = list(`SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace WHERE n.nspname = 'public' AND c.relkind IN ('r','p','v','m') ORDER BY 1`);
  assert.ok(relations.length > 30, `found ${relations.length}`);
  const leaks: string[] = [];
  for (const rel of relations) {
    const read = await call(`/rest/v1/${rel}?select=*&limit=1`);
    if (read.status !== 401 && !(Array.isArray(read.body) && read.body.length === 0)) leaks.push(`${rel} read ${read.status}`);
    if (Array.isArray(read.body) && read.body.length > 0) leaks.push(`${rel} returned rows`);
    const write = await call(`/rest/v1/${rel}`, { body: {} });
    if (write.status < 400) leaks.push(`${rel} write ${write.status}`);
  }
  assert.deepEqual(leaks, []);
});

localTest("anon: every RPC refuses a signed-out call (S023)", async () => {
  const fns = list(`SELECT DISTINCT p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace WHERE n.nspname = 'public' AND p.prorettype <> 'trigger'::regtype ORDER BY 1`);
  assert.ok(fns.length > 20, `found ${fns.length}`);
  const open: string[] = [];
  for (const fn of fns) {
    const res = await call(`/rest/v1/rpc/${fn}`, { body: {} });
    // 401: no permission. 404 is PostgREST not exposing it to anon at all. Either way, refused.
    if (![401, 403, 404].includes(res.status)) open.push(`${fn} ${res.status}`);
  }
  assert.deepEqual(open, []);
});

localTest("cross-user: a stranger reads, changes and deletes none of another player's rows, in any table (S024)", async () => {
  const { white: a, black: c, outsider: b } = await chessCast();
  endGames(a, c);
  sql(`DELETE FROM private.invite_log WHERE inviter = '${a.userId}'`);
  sql(`DELETE FROM private.rate_events WHERE subject IN ('${a.userId}', '${c.userId}')`);
  // A's world: a study party with C, a Darood party, and a row of A's (or about A) in every table.
  const study = await studyParty(a, [c]);
  const darood = (await rpc(a, "create_party", { name: `Matrix ${Date.now() % 100000}`, max_members: 5, milestone: 1000 })).body;
  const day = new Date().toISOString().slice(0, 10);
  await rpc(a, "add_darood_entry", { party_id: darood, amount: 7, local_date: day, tz: "UTC", client_id: crypto.randomUUID() });
  // As A through the API, as the app does (triggers stamp the owner from the session).
  const asA = (table: string, body: unknown) => call(`/rest/v1/${table}`, { token: a.accessToken, body, headers: { Prefer: "return=representation" } });
  const itemRes = await asA("study_items", { party_id: study, kind: "assignment", title: "Matrix item" });
  assert.equal(itemRes.status, 201, JSON.stringify(itemRes.body));
  const item = itemRes.body[0].id;
  const seeds: Array<[string, unknown]> = [
    ["study_completions", { item_id: item, local_date: day, tz: "UTC" }],
    ["study_sessions", { party_id: study, minutes: 25, local_date: day, tz: "UTC" }],
    ["study_reminders", { party_id: study, title: "Matrix reminder", kind: "exam", scope: "time", tz: "UTC", audience: "me", due_at: new Date(Date.now() + 86400000).toISOString() }],
  ];
  for (const [table, body] of seeds) {
    const res = await asA(table, body);
    assert.equal(res.status, 201, `${table}: ${JSON.stringify(res.body)}`);
  }
  const message = sql(`INSERT INTO public.chat_messages (party_id, sender_id, kind, body) VALUES ('${study}', '${a.userId}', 'text', 'matrix') RETURNING id`).split("\n")[0];
  const fileId = crypto.randomUUID();
  const file = sql(`INSERT INTO public.chat_files (id, party_id, uploader_id, storage_path, display_name, mime, size_bytes, status) VALUES ('${fileId}', '${study}', '${a.userId}', '${study}/${fileId}', 'matrix.pdf', 'application/pdf', 10, 'ready') RETURNING id`).split("\n")[0];
  sql(`INSERT INTO public.study_item_files (item_id, file_id, party_id) VALUES ('${item}', '${file}', '${study}')`);
  sql(`INSERT INTO public.chat_reads (party_id, user_id) VALUES ('${study}', '${a.userId}') ON CONFLICT DO NOTHING`);
  sql(`INSERT INTO public.darood_week_results (party_id, week_start, user_id, position, week_total, members) VALUES ('${darood}', '2026-01-05', '${a.userId}', 1, 7, 1) ON CONFLICT DO NOTHING`);
  sql(`INSERT INTO public.study_week_winners (party_id, week_start, user_id, points) VALUES ('${study}', '2026-01-05', '${a.userId}', 10) ON CONFLICT DO NOTHING`);
  sql(`INSERT INTO public.party_milestones (party_id, sequence, target, set_by) VALUES ('${darood}', 99, 5000, '${a.userId}') ON CONFLICT DO NOTHING`);
  const game = gameRow(study, a, c);
  sql(`UPDATE public.chess_games SET ply = 20 WHERE id = '${game}'`);
  sql(`SELECT private.chess_finish('${game}', 'white', 'resign')`);
  sql(`INSERT INTO public.chess_moves (game_id, ply, san, uci, fen_after, white_ms_after, black_ms_after) VALUES ('${game}', 21, 'e4', 'e2e4', 'x', 1, 1) ON CONFLICT DO NOTHING`);
  const challenge = sql(`SELECT public.chess_create_challenge('${a.userId}', '${study}', '${c.userId}', 300, 'white', true) ->> 'id'`);
  await rpc(c, "report_chess_challenge", { challenge_id: challenge, reason: "spam" });
  await rpc(c, "report_chat_message", { message_id: message, reason: "spam" });
  await rpc(a, "register_device_token", { token: `matrix-${a.userId}`, platform: "android" });
  await rpc(a, "block_member", { user_id: c.userId });
  sql(`INSERT INTO public.chess_watch_log (user_id, game_id) VALUES ('${a.userId}', '${game}')`);
  sql(`INSERT INTO public.profiles (id, display_name, leaderboard_opt_in) VALUES ('${c.userId}', 'Matrix_${c.userId.slice(0, 6)}', true) ON CONFLICT (id) DO NOTHING`);
  sql(`INSERT INTO public.reports (reporter_id, reported_profile_id, reason) VALUES ('${a.userId}', '${c.userId}', 'matrix') ON CONFLICT DO NOTHING`);

  // Every column that names a user, and every party column, from the catalogue.
  const userColumns = sql(`
    SELECT c.conrelid::regclass::text || '.' || a.attname FROM pg_constraint c
    JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
    WHERE c.contype = 'f' AND c.confrelid = 'auth.users'::regclass AND c.connamespace = 'public'::regnamespace`).split("\n").filter(Boolean);
  const partyTables = sql(`
    SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    JOIN pg_attribute a ON a.attrelid = c.oid AND a.attname = 'party_id' AND NOT a.attisdropped
    WHERE n.nspname = 'public' AND c.relkind = 'r'`).split("\n").filter(Boolean);
  const checks: Array<{ table: string; filter: string }> = [
    ...userColumns.map((tc) => { const [t, col] = tc.split("."); return { table: t, filter: `${col}=eq.${a.userId}` }; }),
    ...partyTables.flatMap((t) => [study, darood].map((p) => ({ table: t, filter: `party_id=eq.${p}` }))),
    { table: "parties", filter: `id=eq.${study}` },
    { table: "chess_moves", filter: `game_id=eq.${game}` },
  ];
  const leaks: string[] = [];
  const seeded = new Set<string>();
  for (const { table, filter } of checks) {
    const [col, , value] = filter.split(/=|\./);
    const before = sql(`SELECT count(*) FROM public.${table} WHERE ${col} = '${value}'`);
    if (before !== "0") seeded.add(table);
    const read = await call(`/rest/v1/${table}?${filter}&select=*`, { token: b.accessToken });
    if (Array.isArray(read.body) && read.body.length > 0) leaks.push(`read ${table}?${filter}`);
    const patch = await call(`/rest/v1/${table}?${filter}`, { method: "PATCH", token: b.accessToken, body: { [col]: value }, headers: { Prefer: "return=representation" } });
    if (patch.status < 300 && Array.isArray(patch.body) && patch.body.length > 0) leaks.push(`changed ${table}?${filter}`);
    const del = await call(`/rest/v1/${table}?${filter}`, { method: "DELETE", token: b.accessToken, headers: { Prefer: "return=representation" } });
    if (del.status < 300 && Array.isArray(del.body) && del.body.length > 0) leaks.push(`deleted ${table}?${filter}`);
    assert.equal(sql(`SELECT count(*) FROM public.${table} WHERE ${col} = '${value}'`), before, `${table} unchanged`);
  }
  assert.deepEqual(leaks, []);
  // Coverage: every table that can hold a player's rows was seeded and tried.
  const unseeded = [...new Set([...userColumns.map((tc) => tc.split(".")[0]), ...partyTables])].filter((t) => !seeded.has(t));
  assert.deepEqual(unseeded.sort(), ["profile_progress_marks", "profiles"], "only A's profile rows are missing (A isn't on the board)");
  endGames(a, c);
  await rpc(a, "unblock_member", { user_id: c.userId });
});
