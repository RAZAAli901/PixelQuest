// chess-create-challenge: a study party member challenges another, or the whole room (docs/chess-design.md).
// The phone's request is only a request: the database (chess_create_challenge) checks both are
// active members of the party, the challenger has no live game, the time control and colour, and
// the hourly limit.

import { TIME_CONTROLS, chessRequest, isUuid, json, refuse } from "../_shared/chess.ts";
import type { RpcResult } from "../_shared/study_files.ts";

export interface CreateChallengeDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  create: (user: string, party: string, to: string | null, timeControl: number, colour: string, rated: boolean) => Promise<RpcResult<Record<string, unknown>>>;
  /** Tells the one challenged, by push (J122). */
  notify?: import("../_shared/push.ts").Notify;
  log?: (event: Record<string, unknown>) => void;
}

export function handleCreateChallenge(req: Request, deps: CreateChallengeDeps): Promise<Response> {
  return chessRequest(req, deps.getUserId, async (user, body) => {
    const { party_id: party, to_user: to, time_control: timeControl, colour, rated } = body;
    if (!isUuid(party) || (to !== undefined && to !== null && !isUuid(to))) return refuse("bad_request");
    if (typeof timeControl !== "number" || !TIME_CONTROLS.includes(timeControl)) return refuse("bad_time_control");
    if (colour !== undefined && !["white", "black", "random"].includes(colour as string)) return refuse("bad_colour");
    if (rated !== undefined && typeof rated !== "boolean") return refuse("bad_request");
    const made = await deps.create(user, party.toLowerCase(), typeof to === "string" ? to.toLowerCase() : null, timeControl, (colour as string) ?? "random", (rated as boolean) ?? true);
    if (!made.ok) {
      deps.log?.({ event: "challenge_refused", reason: made.error });
      return refuse(made.error);
    }
    if (typeof made.data.to_user === "string") {
      deps.notify?.("challenge_received", [made.data.to_user], { challenge_id: String(made.data.id), party_id: String(made.data.party_id) });
    }
    return json(200, { challenge: made.data });
  }, deps.log);
}
