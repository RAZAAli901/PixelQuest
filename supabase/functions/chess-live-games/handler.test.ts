// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleLiveGames } from "./handler.ts";

const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const PARTY = "5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f";
const post = (body: unknown) => new Request("http://x", { method: "POST", headers: { Authorization: "Bearer ok" }, body: JSON.stringify(body) });
const getUserId = async (t: string) => (t === "ok" ? USER : null);

test("a member gets the party's live games; anyone else is refused", async () => {
  const ok = await handleLiveGames(post({ party_id: PARTY }), { getUserId, liveGames: async () => ({ ok: true, data: [{ id: "g1" }] }) });
  assert.deepEqual(await ok.json(), { games: [{ id: "g1" }] });
  const out = await handleLiveGames(post({ party_id: PARTY }), { getUserId, liveGames: async () => ({ ok: false, error: "not_a_member" }) });
  assert.equal(out.status, 403);
  assert.equal((await handleLiveGames(post({}), { getUserId, liveGames: async () => ({ ok: true, data: [] }) })).status, 400);
});
