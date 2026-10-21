// chess-claim-timeout: a player claims the other side's clock has run out (docs/chess-design.md,
// Clocks). The position says how it ends (chess.js: a win, or a draw if the claimant couldn't
// mate); the database (chess_claim_timeout) checks the clock by its own time.

import { gameAction } from "../_shared/chess.ts";
import { flagEnding } from "../_shared/chess_referee.ts";
import type { RpcResult } from "../_shared/study_files.ts";

export interface ClaimTimeoutDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  state: (user: string, game: string) => Promise<RpcResult<{ game: { fen: string; turn: "w" | "b" } }>>;
  claim: (user: string, game: string, result: string, reason: string) => Promise<RpcResult<{ game?: unknown }>>;
  log?: (event: Record<string, unknown>) => void;
}

export function handleClaimTimeout(req: Request, deps: ClaimTimeoutDeps): Promise<Response> {
  return gameAction(req, deps.getUserId, async (user, game) => {
    const state = await deps.state(user, game);
    if (!state.ok) return state;
    const ending = flagEnding(state.data.game.fen, state.data.game.turn);
    return deps.claim(user, game, ending.result, ending.reason);
  }, deps.log);
}
