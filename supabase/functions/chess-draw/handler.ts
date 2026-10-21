// chess-draw: offer a draw, or accept or decline the other player's (docs/chess-design.md).
// The database (chess_draw) keeps one offer at a time and ends the game when it's agreed.

import { gameAction } from "../_shared/chess.ts";
import type { RpcResult } from "../_shared/study_files.ts";

export const ACTIONS = ["offer", "accept", "decline"] as const;

export interface DrawDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  draw: (user: string, game: string, action: string) => Promise<RpcResult<{ game?: unknown }>>;
  log?: (event: Record<string, unknown>) => void;
}

export function handleDraw(req: Request, deps: DrawDeps): Promise<Response> {
  return gameAction(req, deps.getUserId, async (user, game, body) => {
    const action = body.action;
    if (typeof action !== "string" || !(ACTIONS as readonly string[]).includes(action)) return { ok: false, error: "bad_request" };
    return deps.draw(user, game, action);
  }, deps.log);
}
