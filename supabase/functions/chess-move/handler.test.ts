// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleMove, type GameRow, type MoveDeps } from "./handler.ts";
import { START_FEN } from "../_shared/chess.ts";

const WHITE = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const BLACK = "7a1b2c3d-4e5f-4a6b-8c7d-9e0f1a2b3c4d";
const GAME = "5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f";

function setup(moves: string[] = [], fen = START_FEN, status = "active") {
  const game: GameRow = { id: GAME, white_id: WHITE, black_id: BLACK, status, fen, turn: moves.length % 2 === 0 ? "w" : "b", ply: moves.length };
  const commits: Parameters<MoveDeps["commit"]>[0][] = [];
  const d: MoveDeps = {
    getUserId: async (t) => (t === "white" ? WHITE : t === "black" ? BLACK : null),
    state: async () => ({ ok: true, data: { game, moves } }),
    commit: async (a) => { commits.push(a); return { ok: true, data: { game, move: { ply: a.ply } } }; },
    abortIfStalled: async () => ({ ok: true, data: { aborted: false } }),
  };
  return { d, commits };
}
const move = (who: string, body: unknown) => new Request("http://x", { method: "POST", headers: { Authorization: `Bearer ${who}` }, body: JSON.stringify(body) });

test("a legal move goes to the database with its SAN, the position after it and the flag ending", async () => {
  const { d, commits } = setup();
  const res = await handleMove(move("white", { game_id: GAME, ply: 1, uci: "e2e4" }), d);
  assert.equal(res.status, 200);
  assert.equal(commits[0].san, "e4");
  assert.equal(commits[0].endResult, null);
  assert.deepEqual([commits[0].flagResult, commits[0].flagReason], ["black", "timeout"]);
});

test("an illegal move, the wrong turn, the wrong ply and a finished game are refused before the database", async () => {
  const { d, commits } = setup(["e2e4"]);
  assert.equal((await handleMove(move("black", { game_id: GAME, ply: 2, uci: "e7e4" }), d)).status, 422);
  assert.deepEqual(await (await handleMove(move("white", { game_id: GAME, ply: 2, uci: "d2d4" }), d)).json(), { error: "not_your_turn" });
  assert.equal((await handleMove(move("black", { game_id: GAME, ply: 3, uci: "e7e5" }), d)).status, 409);
  assert.equal((await handleMove(move("black", { game_id: GAME, ply: 2, uci: "e7e5x" }), d)).status, 400);
  assert.equal((await handleMove(move("black", { game_id: GAME, ply: 2, uci: "e7e5" }), setup(["e2e4"], START_FEN, "finished").d)).status, 409);
  assert.equal(commits.length, 0);
});

test("a retry of a stored move is checked against the moves before it, and passed on", async () => {
  const { d, commits } = setup(["e2e4", "e7e5"]);
  assert.equal((await handleMove(move("white", { game_id: GAME, ply: 1, uci: "e2e4" }), d)).status, 200);
  assert.equal(commits[0].ply, 1);
});

test("checkmate ends the game in the same step", async () => {
  // Scholar's mate: 1.e4 e5 2.Bc4 Nc6 3.Qh5 Nf6 4.Qxf7#
  const { d, commits } = setup(["e2e4", "e7e5", "f1c4", "b8c6", "d1h5", "g8f6"]);
  assert.equal((await handleMove(move("white", { game_id: GAME, ply: 7, uci: "h5f7" }), d)).status, 200);
  assert.deepEqual([commits[0].san, commits[0].endResult, commits[0].endReason], ["Qxf7#", "white", "checkmate"]);
});

test("a move after the mover's time ran out ends it on time instead", async () => {
  const { d } = setup(["e2e4", "e7e5"]);
  d.commit = async () => ({ ok: true, data: { game: { id: GAME } as GameRow, flagged: true } });
  const res = await handleMove(move("white", { game_id: GAME, ply: 3, uci: "g1f3" }), d);
  assert.equal(res.status, 409);
  assert.equal((await res.json()).error, "time_out");
});

test("a first move after 30 seconds aborts the game instead", async () => {
  const { d, commits } = setup(["e2e4"]);
  d.abortIfStalled = async () => ({ ok: true, data: { aborted: true, game: { id: GAME } as GameRow } });
  const res = await handleMove(move("black", { game_id: GAME, ply: 2, uci: "e7e5" }), d);
  assert.equal(res.status, 409);
  assert.equal((await res.json()).error, "aborted");
  assert.equal(commits.length, 0);
});
