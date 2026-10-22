// Supabase Edge Function entry point (Deno) for finalize-upload. The logic lives in handler.ts.
// SUPABASE_URL, SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY are provided by Supabase; there
// are no secrets of its own. Deployed with --no-verify-jwt: the handler checks the caller's
// access token itself, with Supabase Auth.

import { authUserId, rpcCaller, storage } from "../_shared/study_files.ts";
import { handleFinalize, type PendingUpload } from "./handler.ts";
import { logger } from "../_shared/log.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);
const files = storage(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleFinalize(req, {
    getUserId: authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey),
    loadUpload: (user, file) => rpc<PendingUpload>("chat_upload_for_finalize", { p_user: user, p_file: file }),
    readObject: files.read,
    removeObject: (path) => files.remove([path]),
    markReady: (user, file, mime, size, sha) =>
      rpc<string>("chat_finalize_upload", { p_user: user, p_file: file, p_mime: mime, p_size: size, p_sha256: sha }),
    markRejected: async (file, reason) => {
      await rpc("chat_reject_upload", { p_file: file, p_reason: reason });
    },
    log: logger("finalize-upload"),
  })
);
