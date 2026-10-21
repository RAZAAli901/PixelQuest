// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleDraw } from "./handler.ts";

const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const GAME = "5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f";
const post = (body: unknown) => new Request("http://x", { method: "POST", headers: { Authorization: "Bearer ok" }, body: JSON.stringify(body) });
const getUserId = async (t: string) => (t === "ok" ? USER : null);

test("an offer, an answer, and nothing else", async () => {
  const asked: string[][] = [];
  const d = { getUserId, draw: async (u: string, g: string, a: string) => { asked.push([u, g, a]); return { ok: true as const, data: { game: {} } }; } };
  assert.equal((await handleDraw(post({ game_id: GAME, action: "offer" }), d)).status, 200);
  assert.equal((await handleDraw(post({ game_id: GAME, action: "accept" }), d)).status, 200);
  assert.equal((await handleDraw(post({ game_id: GAME, action: "takeback" }), d)).status, 400);
  assert.deepEqual(asked.map((a) => a[2]), ["offer", "accept"]);
  const none = { getUserId, draw: async () => ({ ok: false as const, error: "no_draw_offer" }) };
  assert.equal((await handleDraw(post({ game_id: GAME, action: "decline" }), none)).status, 409);
});
