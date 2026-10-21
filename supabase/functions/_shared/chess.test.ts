// Run with: node --test "supabase/functions/**/*.test.ts" (after npm ci in supabase/functions).
import { test } from "node:test";
import assert from "node:assert/strict";
import { Chess, START_FEN, UCI, chessRequest, refuse } from "./chess.ts";

const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const getUserId = async (token: string) => (token === "ok" ? USER : null);
const post = (body: unknown, token = "ok") =>
  new Request("http://x/fn", { method: "POST", headers: { Authorization: `Bearer ${token}` }, body: typeof body === "string" ? body : JSON.stringify(body) });

test("chess.js is the rules: the start, a legal move, an illegal one refused", () => {
  const game = new Chess(START_FEN);
  assert.equal(game.move({ from: "e2", to: "e4" }).san, "e4");
  assert.throws(() => game.move({ from: "e7", to: "e3" }));
  assert.match("e7e8q", UCI);
  assert.doesNotMatch("e9e8", UCI);
});

test("every function checks the method, the sign-in and the body first", async () => {
  const ran: string[] = [];
  const work = async (user: string) => { ran.push(user); return new Response("{}"); };
  assert.equal((await chessRequest(new Request("http://x", { method: "GET" }), getUserId, work)).status, 405);
  assert.equal((await chessRequest(post({}, "nope"), getUserId, work)).status, 401);
  assert.equal((await chessRequest(post("[1]"), getUserId, work)).status, 400);
  assert.equal((await chessRequest(post({ a: 1 }), getUserId, work)).status, 200);
  assert.deepEqual(ran, [USER]);
  const failed = await chessRequest(post({}), getUserId, async () => { throw new Error("db down: secret"); });
  assert.equal(failed.status, 500);
  assert.deepEqual(await failed.json(), { error: "server_error" });
});

test("refusals carry their token and status", async () => {
  assert.equal(refuse("not_your_turn").status, 409);
  assert.equal(refuse("illegal_move").status, 422);
  assert.equal(refuse("something_else").status, 400);
  assert.deepEqual(await refuse("rate_limited", { retry_after: 5 }).json(), { error: "rate_limited", retry_after: 5 });
});
