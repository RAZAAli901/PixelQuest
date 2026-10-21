// Supabase Edge Function entry point (Deno) for chess-sweep. The logic lives in handler.ts.
// Called with the service role key (a one-minute schedule); refuses anyone else. Deployed with
// --no-verify-jwt: the handler checks the key itself.

import { rpcCaller } from "../_shared/study_files.ts";
import { handleSweep } from "./handler.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleSweep(req, serviceKey, {
    dueGames: () => rpc("chess_due_games", {}),
    endOnTime: (game, result, reason) => rpc("chess_claim_timeout", { p_user: null, p_game: game, p_result: result, p_reason: reason }),
    abortStalled: () => rpc("chess_abort_stalled", {}),
    log: (event) => console.log(JSON.stringify(event)),
  })
);
