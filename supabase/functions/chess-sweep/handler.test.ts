// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleSweep } from "./handler.ts";
import { handleClaimTimeout } from "../chess-claim-timeout/handler.ts";

const KEY = "service-key";
const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const GAME = "5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f";
const post = (token: string, body: unknown = {}) => new Request("http://x", { method: "POST", headers: { Authorization: `Bearer ${token}` }, body: JSON.stringify(body) });

test("the sweep is the server's alone, and ends each game whose clock ran out", async () => {
  const ended: string[][] = [];
  const deps = {
    dueGames: async () => ({ ok: true as const, data: [
      { id: "g1", fen: "rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq - 0 1", turn: "b" as const },
      // White has only a king left: Black's flag against it is a draw.
      { id: "g2", fen: "4k3/8/8/8/8/8/8/q3K3 b - - 0 1", turn: "w" as const },
    ] }),
    endOnTime: async (g: string, r: string, why: string) => { ended.push([g, r, why]); return { ok: true as const, data: {} }; },
  };
  assert.equal((await handleSweep(post("guess"), KEY, deps)).status, 401);
  const res = await handleSweep(post(KEY), KEY, deps);
  assert.deepEqual(await res.json(), { timed_out: 2 });
  assert.deepEqual(ended, [["g1", "white", "timeout"], ["g2", "black", "timeout"]]);
});

test("a timeout claim: the ending comes from the position, the clock from the database", async () => {
  const claims: string[][] = [];
  const deps = {
    getUserId: async (t: string) => (t === "ok" ? USER : null),
    // Black to move, and Black's clock is out; White has a bare king: a draw.
    state: async () => ({ ok: true as const, data: { game: { fen: "4k3/8/8/8/8/8/8/q3K3 b - - 0 1", turn: "b" as const } } }),
    claim: async (u: string, g: string, r: string, why: string) => { claims.push([u, g, r, why]); return { ok: true as const, data: { game: {} } }; },
  };
  assert.equal((await handleClaimTimeout(post("ok", { game_id: GAME }), deps)).status, 200);
  assert.deepEqual(claims, [[USER, GAME, "draw", "timeout_vs_insufficient"]]);
  const early = { ...deps, claim: async () => ({ ok: false as const, error: "not_timed_out" }) };
  assert.equal((await handleClaimTimeout(post("ok", { game_id: GAME }), early)).status, 409);
});
