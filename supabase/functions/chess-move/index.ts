// Supabase Edge Function entry point (Deno) for chess-move. The logic lives in handler.ts.
// Deployed with --no-verify-jwt: the handler checks the caller's access token itself.

import { authUserId, rpcCaller } from "../_shared/study_files.ts";
import { handleMove } from "./handler.ts";
import { logger } from "../_shared/log.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleMove(req, {
    getUserId: authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey),
    state: (user, game) => rpc("chess_game_state", { p_user: user, p_game: game }),
    commit: (a) =>
      rpc("chess_commit_move", {
        p_user: a.user, p_game: a.game, p_ply: a.ply, p_uci: a.uci, p_san: a.san, p_fen_after: a.fenAfter,
        p_end_result: a.endResult, p_end_reason: a.endReason, p_flag_result: a.flagResult, p_flag_reason: a.flagReason,
      }),
    abortIfStalled: (game) => rpc("chess_abort_if_stalled", { p_game: game }),
    log: logger("chess-move"),
  })
);
