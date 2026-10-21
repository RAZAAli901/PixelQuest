// chess-resign: a player resigns their live game; the other side wins (docs/chess-design.md).
// The database (chess_resign) checks they play in it and that it's still live.

import { gameAction } from "../_shared/chess.ts";
import type { RpcResult } from "../_shared/study_files.ts";

export interface ResignDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  resign: (user: string, game: string) => Promise<RpcResult<{ game?: unknown }>>;
  log?: (event: Record<string, unknown>) => void;
}

export function handleResign(req: Request, deps: ResignDeps): Promise<Response> {
  return gameAction(req, deps.getUserId, (user, game) => deps.resign(user, game), deps.log);
}
