// Run with: node --test "supabase/functions/**/*.test.ts"   (Node 22.18+ runs the TypeScript directly)
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleCleanup, sameSecret } from "./handler.ts";
import { cleanUp } from "../_shared/cleanup.ts";

const KEY = "service-role-key-for-tests";

function deps(backlog: string[]) {
  const removed: string[][] = [];
  const marked: string[][] = [];
  return {
    removed,
    marked,
    d: {
      rpc: async <T>(fn: string, args: Record<string, unknown>) => {
        if (fn === "chat_expire_uploads") return { ok: true as const, data: backlog.splice(0, args.p_limit as number) as unknown as T };
        marked.push(args.p_paths as string[]);
        return { ok: true as const, data: null as unknown as T };
      },
      remove: async (paths: string[]) => {
        removed.push(paths);
      },
    },
  };
}

const call = (key: string | null) =>
  new Request("https://example.test/functions/v1/cleanup-uploads", { method: "POST", headers: key ? { Authorization: `Bearer ${key}` } : {} });

test("only the project's own key runs it", async () => {
  for (const key of [null, "", "service-role-key-for-test", "a user's token"]) {
    const { d, removed } = deps(["p/a"]);
    assert.equal((await handleCleanup(call(key), KEY, d)).status, 401);
    assert.equal(removed.length, 0);
  }
  assert.ok(sameSecret(KEY, KEY));
  assert.ok(!sameSecret(KEY, KEY.slice(0, -1) + "X"));
});

test("what's due is deleted from storage, then marked purged, round by round", async () => {
  const backlog = Array.from({ length: 450 }, (_, i) => `p/${i}`);
  const { d, removed, marked } = deps(backlog);
  const res = await handleCleanup(call(KEY), KEY, d);
  assert.deepEqual(await res.json(), { purged: 450 });
  assert.deepEqual(removed.map((r) => r.length), [200, 200, 50]);
  assert.deepEqual(marked, removed, "only what was deleted is marked");
});

test("nothing due, nothing done", async () => {
  const { d, removed, marked } = deps([]);
  assert.equal(await cleanUp(d), 0);
  assert.equal(removed.length + marked.length, 0);
});
