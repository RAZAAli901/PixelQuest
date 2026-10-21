// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleSendPush } from "./handler.ts";
import { handleCreateChallenge } from "../chess-create-challenge/handler.ts";
import { handleDraw } from "../chess-draw/handler.ts";

const KEY = "service-key";
const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const OTHER = "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d";
const ID = "5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f";
const post = (token: string, body: unknown) => new Request("http://x", { method: "POST", headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify(body) });
const deps = { fetch: async () => new Response(), account: null, tokensFor: async () => ({ ok: true as const, data: [] }), forget: async () => {} };

test("send-push: the server's alone, a known kind, and ids only", async () => {
  assert.equal((await handleSendPush(post("guess", { kind: "draw_offered", users: [USER] }), KEY, deps)).status, 401);
  assert.equal((await handleSendPush(post(KEY, { kind: "hello", users: [USER] }), KEY, deps)).status, 400);
  assert.equal((await handleSendPush(post(KEY, { kind: "draw_offered", users: [USER], data: { text: "hi there" } }), KEY, deps)).status, 400);
  assert.deepEqual(await (await handleSendPush(post(KEY, { kind: "draw_offered", users: [USER], data: { game_id: ID } }), KEY, deps)).json(), { sent: 0 });
});

test("a challenge to someone tells them; a draw offer tells the other player", async () => {
  const told: unknown[][] = [];
  const getUserId = async (t: string) => (t === "ok" ? USER : null);
  await handleCreateChallenge(post("ok", { party_id: ID, to_user: OTHER, time_control: 60 }), {
    getUserId, create: async () => ({ ok: true, data: { id: "c1", party_id: ID, to_user: OTHER } }), notify: (...a) => { told.push(a); },
  });
  await handleCreateChallenge(post("ok", { party_id: ID, time_control: 60 }), {
    getUserId, create: async () => ({ ok: true, data: { id: "c2", party_id: ID, to_user: null } }), notify: (...a) => { told.push(a); },
  });
  await handleDraw(post("ok", { game_id: ID, action: "offer" }), {
    getUserId, draw: async () => ({ ok: true, data: { game: { white_id: USER, black_id: OTHER, draw_offered_by: USER } } }), notify: (...a) => { told.push(a); },
  });
  assert.deepEqual(told, [
    ["challenge_received", [OTHER], { challenge_id: "c1", party_id: ID }],
    ["draw_offered", [OTHER], { game_id: ID }],
  ]);
});
