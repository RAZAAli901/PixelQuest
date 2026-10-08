// Supabase Edge Function entry point (Deno). The logic lives in handler.ts.
//
// Secrets (supabase secrets set ...):
//   GEMINI_API_KEY                 required
//   GEMINI_MODEL                   optional, default gemini-2.5-flash
//   AI_PROXY_ACCOUNT_DAILY_LIMIT   optional, default 6 calls per signed-in account per UTC day
//   AI_PROXY_GLOBAL_DAILY_LIMIT    optional, default 200 calls per UTC day for the whole project
// SUPABASE_URL, SUPABASE_ANON_KEY and SUPABASE_SERVICE_ROLE_KEY are provided by Supabase.
//
// Deploy with --no-verify-jwt: the handler checks the caller's access token itself, with Supabase
// Auth, and refuses anything that isn't a signed-in account. See docs/CLOUD_SETUP.md.

import { authUserId, handleProxyRequest, rpcClaim } from "./handler.ts";

const limit = (name: string, fallback: number): number => {
  const value = Number(Deno.env.get(name));
  return Number.isInteger(value) && value > 0 ? value : fallback;
};

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

const getUserId = authUserId(fetch, supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") || serviceKey);

const claimCall = rpcClaim(
  fetch,
  supabaseUrl,
  serviceKey,
  limit("AI_PROXY_ACCOUNT_DAILY_LIMIT", 6),
  limit("AI_PROXY_GLOBAL_DAILY_LIMIT", 200),
);

Deno.serve((req: Request) =>
  handleProxyRequest(req, {
    geminiApiKey: Deno.env.get("GEMINI_API_KEY"),
    getUserId,
    claimCall,
    fetchGemini: fetch,
    model: Deno.env.get("GEMINI_MODEL"),
    log: (event) => console.log(JSON.stringify(event)),
  })
);
