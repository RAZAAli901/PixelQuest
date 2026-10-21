// Supabase Edge Function entry point (Deno) for cleanup-uploads. The logic lives in handler.ts.
// Called with the service role key (a schedule, docs/study-chat.md); refuses anyone else. Deployed
// with --no-verify-jwt: the handler checks the key itself, so the new-style secret key works too.

import { rpcCaller, storage } from "../_shared/study_files.ts";
import { handleCleanup } from "./handler.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const files = storage(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleCleanup(req, serviceKey, { rpc: rpcCaller(fetch, supabaseUrl, serviceKey), remove: files.remove })
);
