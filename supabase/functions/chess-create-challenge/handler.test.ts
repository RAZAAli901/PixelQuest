// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleCreateChallenge, type CreateChallengeDeps } from "./handler.ts";

const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const PARTY = "5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f";
const OTHER = "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d";

function deps(refusal?: string) {
  const asked: unknown[][] = [];
  const d: CreateChallengeDeps = {
    getUserId: async (t) => (t === "ok" ? USER : null),
    create: async (...args) => {
      asked.push(args);
      return refusal ? { ok: false, error: refusal } : { ok: true, data: { id: "c1" } };
    },
  };
  return { d, asked };
}
const post = (body: unknown) => new Request("http://x", { method: "POST", headers: { Authorization: "Bearer ok" }, body: JSON.stringify(body) });

test("a challenge goes to the database with the caller as its sender", async () => {
  const { d, asked } = deps();
  const res = await handleCreateChallenge(post({ party_id: PARTY.toUpperCase(), to_user: OTHER, time_control: 300, colour: "white", rated: false }), d);
  assert.equal(res.status, 200);
  assert.deepEqual(asked, [[USER, PARTY, OTHER, 300, "white", false]]);
  const open = await handleCreateChallenge(post({ party_id: PARTY, time_control: 60 }), d);
  assert.equal(open.status, 200);
  assert.deepEqual(asked[1], [USER, PARTY, null, 60, "random", true]);
});

test("what it sends is checked first, and the database's refusals pass on", async () => {
  const { d, asked } = deps();
  assert.equal((await handleCreateChallenge(post({ party_id: "x", time_control: 60 }), d)).status, 400);
  assert.deepEqual(await (await handleCreateChallenge(post({ party_id: PARTY, time_control: 120 }), d)).json(), { error: "bad_time_control" });
  assert.equal((await handleCreateChallenge(post({ party_id: PARTY, time_control: 60, colour: "green" }), d)).status, 400);
  assert.equal(asked.length, 0);
  assert.equal((await handleCreateChallenge(post({ party_id: PARTY, time_control: 60 }), deps("rate_limited").d)).status, 429);
  assert.equal((await handleCreateChallenge(post({ party_id: PARTY, time_control: 60 }), deps("already_playing").d)).status, 409);
  assert.equal((await handleCreateChallenge(post({ party_id: PARTY, time_control: 60 }), deps("not_a_member").d)).status, 403);
});
