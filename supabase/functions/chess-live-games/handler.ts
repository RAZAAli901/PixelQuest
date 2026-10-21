// chess-live-games: a study party's games now being played, for its members to watch
// (docs/chess-design.md). The database (chess_live_games) checks the caller is an active member.

import { chessRequest, isUuid, json, refuse } from "../_shared/chess.ts";
import type { RpcResult } from "../_shared/study_files.ts";

export interface LiveGamesDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  liveGames: (user: string, party: string) => Promise<RpcResult<unknown[]>>;
  log?: (event: Record<string, unknown>) => void;
}

export function handleLiveGames(req: Request, deps: LiveGamesDeps): Promise<Response> {
  return chessRequest(req, deps.getUserId, async (user, body) => {
    if (!isUuid(body.party_id)) return refuse("bad_request");
    const games = await deps.liveGames(user, body.party_id.toLowerCase());
    if (!games.ok) return refuse(games.error);
    return json(200, { games: games.data });
  }, deps.log);
}
