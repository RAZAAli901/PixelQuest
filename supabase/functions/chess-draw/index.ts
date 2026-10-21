// Supabase Edge Function entry point (Deno) for chess-draw. The logic lives in handler.ts.
// Deployed with --no-verify-jwt: the handler checks the caller's access token itself.

import { authUserId, rpcCaller } from "../_shared/study_files.ts";
import { pushFromEnv } from "../_shared/push.ts";
import { handleDraw } from "./handler.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleDraw(req, {
    getUserId: authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey),
    draw: (user, game, action) => rpc("chess_draw", { p_user: user, p_game: game, p_action: action }),
    notify: pushFromEnv((name) => Deno.env.get(name), rpc),
    log: (event) => console.log(JSON.stringify(event)),
  })
);
