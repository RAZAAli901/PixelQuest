// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { expected, kFactor, ratingChanges, ratingCounts } from "./chess_rating.ts";

const seasoned = (points: number) => ({ points, games: 30 });

test("equal ratings: the winner takes K/2, the loser gives as much", () => {
  assert.deepEqual(ratingChanges(seasoned(1200), seasoned(1200), 1), [16, -16]);
  assert.equal(expected(1200, 1200), 0.5);
});

test("an upset moves more than an expected win", () => {
  const [upset] = ratingChanges(seasoned(1000), seasoned(1400), 1);
  const [expectedWin] = ratingChanges(seasoned(1400), seasoned(1000), 1);
  assert.ok(upset > expectedWin, `${upset} > ${expectedWin}`);
  assert.deepEqual(ratingChanges(seasoned(1000), seasoned(1400), 1), [29, -29]);
});

test("a draw moves the stronger player down and the weaker up", () => {
  const [strong, weak] = ratingChanges(seasoned(1500), seasoned(1100), 0.5);
  assert.ok(strong < 0 && weak > 0);
  assert.equal(strong, -weak);
  assert.deepEqual(ratingChanges(seasoned(1300), seasoned(1300), 0.5), [0, 0]);
});

test("K: 40 for the first 10 games, then 32, 16 above 1600", () => {
  assert.equal(kFactor({ points: 800, games: 0 }), 40);
  assert.equal(kFactor({ points: 800, games: 9 }), 40);
  assert.equal(kFactor({ points: 800, games: 10 }), 32);
  assert.equal(kFactor({ points: 1600, games: 50 }), 32);
  assert.equal(kFactor({ points: 1601, games: 50 }), 16);
  // A new player and a seasoned one at the same rating: the new one moves more.
  assert.deepEqual(ratingChanges({ points: 800, games: 2 }, seasoned(800), 1), [20, -16]);
});

test("nobody falls below 100", () => {
  assert.deepEqual(ratingChanges(seasoned(105), seasoned(105), 0), [-5, 16]);
  assert.deepEqual(ratingChanges(seasoned(100), seasoned(900), 0), [0, 0], "at the floor, and an expected win is worth nothing");
});

test("farming: 10 counted games a pair a day; under 4 moves each only counts on mate", () => {
  assert.equal(ratingCounts(8, "resign", 0), true);
  assert.equal(ratingCounts(7, "resign", 0), false, "Black made only 3");
  assert.equal(ratingCounts(3, "checkmate", 0), true, "a quick mate still counts");
  assert.equal(ratingCounts(40, "resign", 9), true);
  assert.equal(ratingCounts(40, "resign", 10), false, "the 11th of the day");
  assert.equal(ratingCounts(3, "checkmate", 10), false);
});
