// chess-sweep: every minute, the server ends games nobody is left to end (docs/chess-design.md,
// Clocks): those whose clock has run out, and those whose first move never came (aborted). Only the project itself may call it: the caller must
// hold the service role key.

import { fromTheServer, json } from "../_shared/chess.ts";
import { flagEnding } from "../_shared/chess_referee.ts";
import type { RpcResult } from "../_shared/study_files.ts";

export interface SweepDeps {
  dueGames: () => Promise<RpcResult<{ id: string; fen: string; turn: "w" | "b" }[]>>;
  endOnTime: (game: string, result: string, reason: string) => Promise<RpcResult<unknown>>;
  /** Games whose first move never came (chess_abort_stalled): how many it aborted. */
  abortStalled: () => Promise<RpcResult<number>>;
  log?: (event: Record<string, unknown>) => void;
}

export async function handleSweep(req: Request, serviceKey: string, deps: SweepDeps): Promise<Response> {
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });
  if (!fromTheServer(req, serviceKey)) return json(401, { error: "not_allowed" });
  const due = await deps.dueGames();
  if (!due.ok) return json(500, { error: "server_error" });
  let ended = 0;
  for (const game of due.data) {
    const ending = flagEnding(game.fen, game.turn);
    // Refused only if a move got in first, or it already ended: nothing to do then.
    if ((await deps.endOnTime(game.id, ending.result, ending.reason)).ok) ended++;
  }
  const aborted = await deps.abortStalled();
  deps.log?.({ event: "chess_sweep", due: due.data.length, ended, aborted: aborted.ok ? aborted.data : null });
  return json(200, { timed_out: ended, aborted: aborted.ok ? aborted.data : 0 });
}
