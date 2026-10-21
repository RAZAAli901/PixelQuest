// Supabase Edge Function entry point (Deno) for chess-claim-timeout. The logic lives in handler.ts.
// Deployed with --no-verify-jwt: the handler checks the caller's access token itself.

import { authUserId, rpcCaller } from "../_shared/study_files.ts";
import { handleClaimTimeout } from "./handler.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleClaimTimeout(req, {
    getUserId: authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey),
    state: (user, game) => rpc("chess_game_state", { p_user: user, p_game: game }),
    claim: (user, game, result, reason) => rpc("chess_claim_timeout", { p_user: user, p_game: game, p_result: result, p_reason: reason }),
    log: (event) => console.log(JSON.stringify(event)),
  })
);
