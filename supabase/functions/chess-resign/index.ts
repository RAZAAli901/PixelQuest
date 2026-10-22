// Supabase Edge Function entry point (Deno) for chess-resign. The logic lives in handler.ts.
// Deployed with --no-verify-jwt: the handler checks the caller's access token itself.

import { authUserId, rpcCaller } from "../_shared/study_files.ts";
import { handleResign } from "./handler.ts";
import { logger } from "../_shared/log.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleResign(req, {
    getUserId: authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey),
    resign: (user, game) => rpc("chess_resign", { p_user: user, p_game: game }),
    log: logger("chess-resign"),
  })
);
