// Supabase Edge Function entry point (Deno) for chess-create-challenge. The logic lives in handler.ts.
// Deployed with --no-verify-jwt: the handler checks the caller's access token itself.

import { authUserId, rpcCaller } from "../_shared/study_files.ts";
import { pushFromEnv } from "../_shared/push.ts";
import { handleCreateChallenge } from "./handler.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleCreateChallenge(req, {
    getUserId: authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey),
    create: (user, party, to, timeControl, colour, rated) =>
      rpc("chess_create_challenge", { p_user: user, p_party: party, p_to: to, p_time_control: timeControl, p_colour: colour, p_rated: rated }),
    notify: pushFromEnv((name) => Deno.env.get(name), rpc),
    log: (event) => console.log(JSON.stringify(event)),
  })
);
