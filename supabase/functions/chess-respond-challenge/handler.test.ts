// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleRespondChallenge, type RespondChallengeDeps } from "./handler.ts";

const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const CHALLENGE = "5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f";
const post = (body: unknown) => new Request("http://x", { method: "POST", headers: { Authorization: "Bearer ok" }, body: JSON.stringify(body) });

function deps(answer: Awaited<ReturnType<RespondChallengeDeps["respond"]>>) {
  const asked: unknown[][] = [];
  const d: RespondChallengeDeps = {
    getUserId: async (t) => (t === "ok" ? USER : null),
    respond: async (...args) => { asked.push(args); return answer; },
  };
  return { d, asked };
}

test("accepting returns the game the database made", async () => {
  const { d, asked } = deps({ ok: true, data: { challenge: { id: CHALLENGE }, game: { id: "g1" } } });
  const res = await handleRespondChallenge(post({ challenge_id: CHALLENGE, accept: true }), d);
  assert.equal(res.status, 200);
  assert.deepEqual((await res.json()).game, { id: "g1" });
  assert.deepEqual(asked, [[USER, CHALLENGE, true]]);
});

test("a bad request, an expired challenge and the database's refusals", async () => {
  assert.equal((await handleRespondChallenge(post({ challenge_id: CHALLENGE, accept: "yes" }), deps({ ok: true, data: {} }).d)).status, 400);
  assert.equal((await handleRespondChallenge(post({ challenge_id: CHALLENGE, accept: true }), deps({ ok: true, data: { error: "expired" } }).d)).status, 410);
  assert.equal((await handleRespondChallenge(post({ challenge_id: CHALLENGE, accept: true }), deps({ ok: false, error: "already_answered" }).d)).status, 409);
  assert.equal((await handleRespondChallenge(post({ challenge_id: CHALLENGE, accept: false }), deps({ ok: false, error: "not_found" }).d)).status, 404);
});
