// chess-respond-challenge: accept or decline a chess challenge, or (the challenger) cancel it
// (docs/chess-design.md). Accepting makes the game; the database (chess_respond_challenge) checks
// who may answer, that it's still open and not expired, and that neither player has a live game.

import { chessRequest, isUuid, json, refuse } from "../_shared/chess.ts";
import type { RpcResult } from "../_shared/study_files.ts";

export interface RespondChallengeDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  respond: (user: string, challenge: string, accept: boolean) => Promise<RpcResult<{ challenge?: Record<string, unknown>; game?: Record<string, unknown>; error?: string }>>;
  /** Tells the challenger their challenge was taken, by push (J122). */
  notify?: import("../_shared/push.ts").Notify;
  log?: (event: Record<string, unknown>) => void;
}

export function handleRespondChallenge(req: Request, deps: RespondChallengeDeps): Promise<Response> {
  return chessRequest(req, deps.getUserId, async (user, body) => {
    const { challenge_id: challenge, accept } = body;
    if (!isUuid(challenge) || typeof accept !== "boolean") return refuse("bad_request");
    const answer = await deps.respond(user, challenge.toLowerCase(), accept);
    if (!answer.ok) return refuse(answer.error);
    // Expired: stored as such, and said as a refusal.
    if (answer.data.error) return refuse(answer.data.error);
    const { challenge: taken, game } = answer.data;
    if (game && taken && typeof taken.from_user === "string") {
      deps.notify?.("challenge_accepted", [taken.from_user], { game_id: String(game.id), party_id: String(game.party_id) });
    }
    return json(200, { challenge: taken, game: game ?? null });
  }, deps.log);
}
