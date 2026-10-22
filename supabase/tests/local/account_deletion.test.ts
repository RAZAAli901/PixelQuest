// S057: deleting an account leaves nothing on the server that names the player.
// Run with: SUPABASE_LOCAL=1 node --test supabase/tests/local/account_deletion.test.ts
import assert from "node:assert/strict";
import { asService, call, localTest, newPlayer, sql } from "./stack.ts";

/** Every base table column in public and private that holds a uuid. */
function uuidColumns(): { table: string; column: string }[] {
  return sql(`
    SELECT c.table_schema || '.' || c.table_name || '|' || c.column_name
    FROM information_schema.columns c
    JOIN information_schema.tables t ON t.table_schema = c.table_schema AND t.table_name = c.table_name
    WHERE c.table_schema IN ('public', 'private') AND t.table_type = 'BASE TABLE' AND c.data_type = 'uuid'
    ORDER BY 1`).split("\n").filter(Boolean).map((line) => {
    const [table, column] = line.split("|");
    return { table, column };
  });
}

localTest("every key to a player cascades or clears when the account goes", () => {
  const rules = sql(`
    SELECT conrelid::regclass::text || ':' || confdeltype::text FROM pg_constraint
    WHERE contype = 'f' AND confrelid IN ('auth.users'::regclass, 'public.profiles'::regclass)
      AND connamespace = 'public'::regnamespace`).split("\n").filter(Boolean);
  assert.ok(rules.length >= 25, "the keys were found");
  const keeping = rules.filter((r) => !r.endsWith(":c") && !r.endsWith(":n"));
  assert.deepEqual(keeping, [], "no key blocks or keeps a deleted player's rows");
});

localTest("deleting an account removes every row that names it, keys or not", async () => {
  const leaving = await newPlayer("deleteall");
  const other = await newPlayer("deleteother");
  const id = leaving.userId;

  // What names the player without a key: AI counters, rate-limit events, and the opponent's
  // head-to-head record, which stays theirs (security.test.ts, "deletion: the account's games stay").
  const [low, high] = [id, other.userId].sort();
  sql(`INSERT INTO public.chess_head_to_head (user_low, user_high, time_control_sec, low_wins) VALUES ('${low}', '${high}', 300, 1)`);
  sql(`INSERT INTO public.ai_proxy_usage (device_id, day, calls) VALUES ('user:${id}', current_date, 2)`);
  const check = await call("/rest/v1/rpc/is_username_available", { token: leaving.accessToken, body: { name: "somename" } });
  assert.equal(check.status, 200, JSON.stringify(check.body));
  assert.ok(Number(sql(`SELECT count(*) FROM private.rate_events WHERE subject = '${id}'`)) >= 1, "a rate event was logged");

  const deleted = await call("/rest/v1/rpc/delete_user_account", { token: leaving.accessToken, body: {} });
  assert.equal(deleted.status, 204, JSON.stringify(deleted.body));

  const kept = new Set(["public.chess_head_to_head.user_low", "public.chess_head_to_head.user_high"]);
  const left = uuidColumns().filter(({ table, column }) => !kept.has(`${table}.${column}`)).flatMap(({ table, column }) => {
    const n = Number(sql(`SELECT count(*) FROM ${table} WHERE "${column}" = '${id}'`));
    return n > 0 ? [`${table}.${column}: ${n}`] : [];
  });
  assert.deepEqual(left, [], "no row anywhere names the deleted player");
  assert.equal(sql(`SELECT count(*) FROM public.chess_head_to_head WHERE user_low = '${low}' AND user_high = '${high}'`), "1", "the opponent keeps their record");
  assert.equal(sql(`SELECT count(*) FROM public.ai_proxy_usage WHERE device_id = 'user:${id}'`), "0");
  assert.equal((await asService(`/auth/v1/admin/users/${id}`)).status, 404);

  // The other player keeps their account.
  assert.equal((await asService(`/auth/v1/admin/users/${other.userId}`)).status, 200);
});
