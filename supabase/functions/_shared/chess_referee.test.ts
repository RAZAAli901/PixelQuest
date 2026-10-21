// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { cannotMate, checkMove, flagEnding } from "./chess_referee.ts";

const ok = (moves: string[], uci: string) => {
  const check = checkMove(moves, moves.length + 1, uci);
  assert.ok(check.ok, `${uci} after ${moves.join(" ")}`);
  return check;
};

test("an illegal move, a malformed one and a ply out of range are refused", () => {
  assert.deepEqual(checkMove([], 1, "e2e5"), { ok: false, error: "illegal_move" });
  assert.deepEqual(checkMove([], 1, "e7e5"), { ok: false, error: "illegal_move" }, "Black's pawn on White's turn");
  assert.deepEqual(checkMove([], 1, "zz"), { ok: false, error: "illegal_move" });
  assert.deepEqual(checkMove(["e2e4"], 3, "d2d4"), { ok: false, error: "wrong_ply" });
  assert.deepEqual(checkMove([], 0, "e2e4"), { ok: false, error: "wrong_ply" });
});

test("castling, both ways, and not through check", () => {
  const ready = ["e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6"];
  assert.equal(ok(ready, "e1g1").san, "O-O");
  const long = ["d2d4", "d7d5", "b1c3", "b8c6", "c1f4", "c8f5", "d1d2", "d8d7"];
  assert.equal(ok(long, "e1c1").san, "O-O-O");
  // The f1 square attacked by a bishop on c4: no castling short.
  const through = ["e2e4", "e7e5", "g1f3", "f8c5", "f1e2", "c5f2"];
  assert.equal(checkMove(through, through.length + 1, "e1g1").ok, false);
});

test("en passant, only straight after the double step", () => {
  const moves = ["e2e4", "a7a6", "e4e5", "d7d5"];
  const ep = ok(moves, "e5d6");
  assert.equal(ep.san, "exd6");
  assert.equal(checkMove([...moves, "h2h3", "h7h6"], 7, "e5d6").ok, false, "too late");
});

test("promotion: to the piece asked for", () => {
  const moves = ["h2h4", "g7g5", "h4g5", "h7h6", "g5h6", "g8f6", "h6h7", "f6g8"];
  assert.equal(ok(moves, "h7g8q").san, "hxg8=Q");
  assert.equal(ok(moves, "h7g8n").san, "hxg8=N");
  assert.equal(checkMove(moves, moves.length + 1, "h7g8k").ok, false);
});

test("checkmate, stalemate and threefold repetition end the game", () => {
  const mate = ok(["f2f3", "e7e5", "g2g4"], "d8h4");
  assert.deepEqual(mate.end, { result: "black", reason: "checkmate" });
  // A known 10-move stalemate (Sam Loyd's).
  const stalemate = ["e2e3", "a7a5", "d1h5", "a8a6", "h5a5", "h7h5", "h2h4", "a6h6", "a5c7", "f7f6", "c7d7", "e8f7", "d7b7", "d8d3", "b7b8", "d3h7", "b8c8", "f7g6"];
  assert.deepEqual(ok(stalemate, "c8e6").end, { result: "draw", reason: "stalemate" });
  const shuffle = ["g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8"];
  assert.equal(checkMove(shuffle.slice(0, 7), 8, "f6g8").ok, true);
  assert.deepEqual(checkMove(shuffle.slice(0, 7), 8, "f6g8").ok && (checkMove(shuffle.slice(0, 7), 8, "f6g8") as { end: unknown }).end, { result: "draw", reason: "threefold_repetition" });
});

test("on time: a win, unless the other side can't mate", () => {
  assert.equal(cannotMate("4k3/8/8/8/8/8/8/4K3 w - - 0 1", "w"), true);
  assert.equal(cannotMate("4k3/8/8/8/8/8/8/3NK3 w - - 0 1", "w"), true);
  assert.equal(cannotMate("4k3/8/8/8/8/8/8/2NNK3 w - - 0 1", "w"), false);
  assert.deepEqual(flagEnding("4k3/8/8/8/8/8/8/3QK3 b - - 0 1", "b"), { result: "white", reason: "timeout" });
  assert.deepEqual(flagEnding("4k3/8/8/8/8/8/8/3BK3 b - - 0 1", "b"), { result: "draw", reason: "timeout_vs_insufficient" });
});
