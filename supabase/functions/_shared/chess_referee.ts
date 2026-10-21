// The referee's rules (docs/chess-design.md, Moves): every move checked by chess.js against the
// game as stored, and what a move or a flag ends the game as. Pure: no database, no clock.

import { Chess, START_FEN, UCI } from "./chess.ts";

export type Result = "white" | "black" | "draw";
export interface Ending {
  result: Result;
  reason: string;
}

export type MoveCheck =
  | { ok: true; san: string; fenAfter: string; end: Ending | null }
  | { ok: false; error: "illegal_move" | "wrong_ply" };

/** The game rebuilt from its moves (UCI, oldest first), so repetition counts its whole history. */
export function replay(moves: readonly string[]): InstanceType<typeof Chess> {
  const board = new Chess(START_FEN);
  for (const uci of moves) {
    const [, from, to, promotion] = UCI.exec(uci) ?? [];
    board.move({ from, to, promotion });
  }
  return board;
}

/**
 * Whether [uci] is legal as move number [ply], after [moves]: its SAN and the position after it,
 * and how it ends the game, if it does. [ply] may be an earlier one (a retry): it's checked against
 * the moves before it.
 */
export function checkMove(moves: readonly string[], ply: number, uci: string): MoveCheck {
  if (!Number.isInteger(ply) || ply < 1 || ply > moves.length + 1) return { ok: false, error: "wrong_ply" };
  const parts = UCI.exec(uci);
  if (!parts) return { ok: false, error: "illegal_move" };
  const board = replay(moves.slice(0, ply - 1));
  const [, from, to, promotion] = parts;
  let san: string;
  try {
    san = board.move({ from, to, promotion }).san;
  } catch {
    return { ok: false, error: "illegal_move" };
  }
  return { ok: true, san, fenAfter: board.fen(), end: endingOf(board) };
}

/** How the position ends the game, if it does: the side that just moved mates, or a draw. */
export function endingOf(board: InstanceType<typeof Chess>): Ending | null {
  if (board.isCheckmate()) return { result: board.turn() === "w" ? "black" : "white", reason: "checkmate" };
  if (board.isStalemate()) return { result: "draw", reason: "stalemate" };
  if (board.isInsufficientMaterial()) return { result: "draw", reason: "insufficient_material" };
  if (board.isThreefoldRepetition()) return { result: "draw", reason: "threefold_repetition" };
  if (board.isDrawByFiftyMoves()) return { result: "draw", reason: "fifty_moves" };
  return null;
}

/** Whether [side] can't possibly mate: a bare king, or a king and one knight or bishop. */
export function cannotMate(fen: string, side: "w" | "b"): boolean {
  const pieces = new Chess(fen).board().flat().filter((sq) => sq && sq.color === side && sq.type !== "k");
  return pieces.length === 0 || (pieces.length === 1 && (pieces[0]!.type === "n" || pieces[0]!.type === "b"));
}

/** [flagged]'s clock ran out: the other side wins, or it's a draw if they couldn't mate anyway. */
export function flagEnding(fen: string, flagged: "w" | "b"): Ending {
  const other = flagged === "w" ? "b" : "w";
  if (cannotMate(fen, other)) return { result: "draw", reason: "timeout_vs_insufficient" };
  return { result: other === "w" ? "white" : "black", reason: "timeout" };
}
