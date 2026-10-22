// Supabase Edge Function entry point (Deno) for chess-respond-challenge. The logic lives in handler.ts.
// Deployed with --no-verify-jwt: the handler checks the caller's access token itself.

import { authUserId, rpcCaller } from "../_shared/study_files.ts";
import { pushFromEnv } from "../_shared/push.ts";
import { handleRespondChallenge } from "./handler.ts";
import { logger } from "../_shared/log.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleRespondChallenge(req, {
    getUserId: authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey),
    respond: (user, challenge, accept) => rpc("chess_respond_challenge", { p_user: user, p_challenge: challenge, p_accept: accept }),
    notify: pushFromEnv((name) => Deno.env.get(name), rpc),
    log: logger("chess-respond-challenge"),
  })
);
