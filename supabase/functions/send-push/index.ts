// Supabase Edge Function entry point (Deno) for send-push. The logic lives in handler.ts.
// Secrets: FCM_SERVICE_ACCOUNT (the Firebase service account's JSON), set with
// `supabase secrets set`, never in the repo. Called with the service role key only; deployed with
// --no-verify-jwt, as the handler checks the key itself.

import { rpcCaller } from "../_shared/study_files.ts";
import { serviceAccountOf } from "../_shared/push.ts";
import { handleSendPush } from "./handler.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";
const rpc = rpcCaller(fetch, supabaseUrl, serviceKey);

Deno.serve((req: Request) =>
  handleSendPush(req, serviceKey, {
    fetch,
    account: serviceAccountOf(Deno.env.get("FCM_SERVICE_ACCOUNT")),
    tokensFor: (users) => rpc("push_tokens_for", { p_users: users }),
    forget: (token) => rpc("push_forget_token", { p_token: token }),
    log: (event) => console.log(JSON.stringify(event)),
  })
);
