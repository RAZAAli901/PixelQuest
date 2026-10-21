// chess-move: a player's move (docs/chess-design.md, Moves). The game is read as stored, the move
// checked with chess.js (chess_referee.ts), then taken by the database (chess_commit_move), which
// keeps the order and the clocks by its own time. Sent again after a lost answer, it's taken once.

import { UCI, chessRequest, isUuid, json, refuse } from "../_shared/chess.ts";
import { checkMove, flagEnding } from "../_shared/chess_referee.ts";
import type { RpcResult } from "../_shared/study_files.ts";

export interface GameRow {
  id: string;
  white_id: string;
  black_id: string;
  status: string;
  fen: string;
  turn: "w" | "b";
  ply: number;
  [key: string]: unknown;
}

export interface MoveDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  state: (user: string, game: string) => Promise<RpcResult<{ game: GameRow; moves: string[] }>>;
  commit: (args: {
    user: string; game: string; ply: number; uci: string; san: string; fenAfter: string;
    endResult: string | null; endReason: string | null; flagResult: string; flagReason: string;
  }) => Promise<RpcResult<{ game: GameRow; move?: unknown; repeated?: boolean; flagged?: boolean }>>;
  /** The abort rule (chess_abort_if_stalled): a first move too late aborts the game instead. */
  abortIfStalled: (game: string) => Promise<RpcResult<{ aborted: boolean; game?: GameRow }>>;
  log?: (event: Record<string, unknown>) => void;
}

export function handleMove(req: Request, deps: MoveDeps): Promise<Response> {
  return chessRequest(req, deps.getUserId, async (user, body) => {
    const { game_id: gameId, ply, uci } = body;
    if (!isUuid(gameId) || typeof ply !== "number" || typeof uci !== "string" || !UCI.test(uci)) return refuse("bad_request");
    const id = gameId.toLowerCase();
    const state = await deps.state(user, id);
    if (!state.ok) return refuse(state.error);
    const { game, moves } = state.data;

    // A retry of a stored move goes to the database as it was; a new one must be the next.
    if (ply > game.ply + 1 || ply < 1) return refuse("wrong_ply");
    if (ply === game.ply + 1) {
      if (game.status !== "waiting" && game.status !== "active") return refuse("game_over");
      const mine = game.turn === "w" ? game.white_id : game.black_id;
      if (mine !== user) return refuse("not_your_turn");
    }
    if (ply === game.ply + 1 && game.ply < 2) {
      const stalled = await deps.abortIfStalled(id);
      if (stalled.ok && stalled.data.aborted) return json(409, { error: "aborted", game: stalled.data.game });
    }
    const check = checkMove(moves, ply, uci);
    if (!check.ok) return refuse(check.error);

    // Should the mover's time have run out, the game ends on time as of the position before it.
    const flag = flagEnding(game.fen, game.turn);
    const taken = await deps.commit({
      user, game: id, ply, uci, san: check.san, fenAfter: check.fenAfter,
      endResult: check.end?.result ?? null, endReason: check.end?.reason ?? null,
      flagResult: flag.result, flagReason: flag.reason,
    });
    if (!taken.ok) return refuse(taken.error);
    if (taken.data.flagged) return json(409, { error: "time_out", game: taken.data.game });
    deps.log?.({ event: "chess_move", game: id, ply, ended: check.end?.reason ?? null });
    return json(200, { game: taken.data.game, move: taken.data.move, repeated: taken.data.repeated ?? false });
  }, deps.log);
}
