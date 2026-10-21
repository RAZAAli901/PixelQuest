// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleResign } from "./handler.ts";

const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const GAME = "5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f";
const post = (body: unknown) => new Request("http://x", { method: "POST", headers: { Authorization: "Bearer ok" }, body: JSON.stringify(body) });
const getUserId = async (t: string) => (t === "ok" ? USER : null);

test("resigning: the caller's game, ended by the database, or its refusal", async () => {
  const asked: string[][] = [];
  const res = await handleResign(post({ game_id: GAME }), { getUserId, resign: async (u, g) => { asked.push([u, g]); return { ok: true, data: { game: { result: "black" } } }; } });
  assert.equal(res.status, 200);
  assert.deepEqual(asked, [[USER, GAME]]);
  assert.equal((await handleResign(post({ game_id: "nope" }), { getUserId, resign: async () => ({ ok: true, data: {} }) })).status, 400);
  assert.equal((await handleResign(post({ game_id: GAME }), { getUserId, resign: async () => ({ ok: false, error: "game_over" }) })).status, 409);
});
