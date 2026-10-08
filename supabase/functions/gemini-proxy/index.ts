// Supabase Edge Function entry point (Deno). The logic lives in handler.ts.
//
// Secrets (supabase secrets set ...):
//   GEMINI_API_KEY                 required
//   GEMINI_MODEL                   optional, default gemini-2.5-flash
//   AI_PROXY_DEVICE_DAILY_LIMIT    optional, default 6 calls per install per UTC day
//   AI_PROXY_GLOBAL_DAILY_LIMIT    optional, default 200 calls per UTC day for the whole project
// SUPABASE_URL and SUPABASE_SERVICE_ROLE_KEY are provided by Supabase.
//
// Deploy with --no-verify-jwt: the anon key ships in every APK, so checking it proves nothing; the
// daily limits are what protect the Gemini key's quota. See docs/GEMINI_PROXY.md.

import { handleProxyRequest, rpcClaim } from "./handler.ts";

const limit = (name: string, fallback: number): number => {
  const value = Number(Deno.env.get(name));
  return Number.isInteger(value) && value > 0 ? value : fallback;
};

const claimCall = rpcClaim(
  fetch,
  Deno.env.get("SUPABASE_URL") ?? "",
  Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "",
  limit("AI_PROXY_DEVICE_DAILY_LIMIT", 6),
  limit("AI_PROXY_GLOBAL_DAILY_LIMIT", 200),
);

Deno.serve((req: Request) =>
  handleProxyRequest(req, {
    geminiApiKey: Deno.env.get("GEMINI_API_KEY"),
    claimCall,
    fetchGemini: fetch,
    model: Deno.env.get("GEMINI_MODEL"),
    log: (event) => console.log(JSON.stringify(event)),
  })
);
