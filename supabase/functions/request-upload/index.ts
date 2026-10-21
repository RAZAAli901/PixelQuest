// Supabase Edge Function entry point (Deno) for request-upload. The logic lives in handler.ts.
// SUPABASE_URL, SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY are provided by Supabase; there
// are no secrets of its own. Deployed with --no-verify-jwt: the handler checks the caller's
// access token itself, with Supabase Auth, as gemini-proxy does.

import { authUserId, rpcCaller, storage } from "../_shared/study_files.ts";
import { handleRequestUpload } from "./handler.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);
const files = storage(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleRequestUpload(req, {
    getUserId: authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey),
    requestRow: (user, party, name, mime, size) =>
      rpc("chat_request_upload", { p_user: user, p_party: party, p_name: name, p_mime: mime, p_size: size }),
    signUpload: files.signUpload,
    log: (event) => console.log(JSON.stringify(event)),
  })
);
