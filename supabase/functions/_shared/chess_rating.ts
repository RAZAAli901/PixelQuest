// Int points (docs/chess-design.md, Ratings and records): Elo, per time control, with the K rules
// and the floor. Pure: no database, no clock. The database applies the same maths in
// private.chess_elo (20261019000091) inside finish_game; the tests hold the two to the same answers.

export interface RatingRules {
  kNew: number;
  kNewGames: number;
  kNormal: number;
  kHigh: number;
  kHighAbove: number;
  floor: number;
}

/** chess_config's values. */
export const RULES: RatingRules = { kNew: 40, kNewGames: 10, kNormal: 32, kHigh: 16, kHighAbove: 1600, floor: 100 };

export interface Side {
  points: number;
  /** Rated games played in this time control before this one. */
  games: number;
}

/** A player's K: 40 for their first 10 games, then 32, and 16 above 1600. */
export function kFactor(side: Side, rules: RatingRules = RULES): number {
  if (side.games < rules.kNewGames) return rules.kNew;
  return side.points > rules.kHighAbove ? rules.kHigh : rules.kNormal;
}

/** A's expected score against B. */
export function expected(a: number, b: number): number {
  return 1 / (1 + Math.pow(10, (b - a) / 400));
}

/** Rounds half away from zero, as Postgres's round() does. */
function roundHalfAway(x: number): number {
  return Math.sign(x) * Math.round(Math.abs(x));
}

/**
 * Each side's change after a game: [scoreA] is 1 for A's win, 0.5 for a draw, 0 for a loss. Each
 * side's new points never go below the floor.
 */
export function ratingChanges(a: Side, b: Side, scoreA: 0 | 0.5 | 1, rules: RatingRules = RULES): [number, number] {
  const change = (me: Side, them: Side, score: number) => {
    const raw = roundHalfAway(kFactor(me, rules) * (score - expected(me.points, them.points)));
    return Math.max(rules.floor, me.points + raw) - me.points;
  };
  return [change(a, b, scoreA), change(b, a, 1 - scoreA)];
}

/** chess_config's anti-farming values. */
export const FARMING = { pairRatedPerDay: 10, minMovesEach: 4 };

/**
 * Whether a rated game changes Int points (20261019000092): the pair has had fewer than 10 count
 * today, and each side made at least 4 moves, unless it ended in checkmate. [ply] is the moves
 * made in all; [pairCountedToday] the pair's games that counted today before this one.
 */
export function ratingCounts(ply: number, reason: string, pairCountedToday: number, rules = FARMING): boolean {
  const eachMoved = Math.floor(ply / 2) >= rules.minMovesEach;
  return (reason === "checkmate" || eachMoved) && pairCountedToday < rules.pairRatedPerDay;
}
