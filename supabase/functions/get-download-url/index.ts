// Supabase Edge Function entry point (Deno) for get-download-url. The logic lives in handler.ts.
// SUPABASE_URL, SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY are provided by Supabase; there
// are no secrets of its own. Deployed with --no-verify-jwt: the handler checks the caller's
// access token itself, with Supabase Auth.

import { authUserId, rpcCaller, storage } from "../_shared/study_files.ts";
import { handleDownload, type DownloadFile } from "./handler.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);
const files = storage(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleDownload(req, {
    getUserId: authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey),
    loadFile: (user, file) => rpc<DownloadFile>("chat_file_for_download", { p_user: user, p_file: file }),
    signDownload: files.signDownload,
  })
);
