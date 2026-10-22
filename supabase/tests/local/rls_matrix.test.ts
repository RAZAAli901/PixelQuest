// The RLS matrix through the real API (S023 to S025): every table, view and RPC is found from the
// catalogue, so a new one is covered without editing this file.
// Run with the local stack: SUPABASE_LOCAL=1 node --test supabase/tests/local/rls_matrix.test.ts
import assert from "node:assert/strict";
import { call, localTest, sql } from "./stack.ts";

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
