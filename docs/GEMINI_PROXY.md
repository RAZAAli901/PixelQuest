# Gemini proxy (`supabase/functions/gemini-proxy`)

PixelQuest's release APKs are public on GitHub. A key compiled into an APK can be read out of it in seconds, because R8 renames code but leaves string constants as they are. So since Day 30, release builds don't contain the Gemini API key. They send AI requests to a Supabase Edge Function that holds the key. Since Day 31, only **signed-in accounts** can use it: the AI Coach is for signed-in players (Google, or a code sent by email).

```
App (release)                          Supabase                                       Google
GeminiProxyClient  --POST-->  functions/v1/gemini-proxy          --x-goog-api-key-->  Gemini generateContent
  prompt, systemInstruction      1. checks the token with Auth                         (response passed back
  Authorization: Bearer <the        (/auth/v1/user): signed-in account?                 unchanged)
    account's access token>      2. checks sizes, claims a call for
                                    user:<account id> (claim_ai_proxy_call)
```

## Which builds use it

| Build | Gemini access |
| --- | --- |
| Release (local or CI) | Proxy. `GEMINI_API_KEY` is blank in the release build type, even when `local.properties` has a key. |
| Debug built by CI (attached to releases) | Proxy: the workflows write `GEMINI_VIA_PROXY=true`. |
| Debug built locally | Direct, with `GEMINI_API_KEY` from `local.properties`. Set `GEMINI_VIA_PROXY=true` there to try the proxy instead. |

Every build needs a real Supabase project (`SUPABASE_URL`, `SUPABASE_ANON_KEY`) for the AI Coach, because the player has to be signed in. Without one, the AI Coach isn't offered. Signed out, no client sends anything (`AccountRequiredGeminiClient` wraps both the proxy and the direct client), and the AI Coach card asks the player to sign in.

## What protects the key's quota

The function is public: anyone can call it. These are the limits:

- **Signed-in accounts only.** Each request must carry a Supabase user access token in `Authorization: Bearer`. The function asks Supabase Auth (`/auth/v1/user`) whose it is. The public anon key, expired or made-up tokens, and anonymous users (if the project ever enables them) get `401 {"error":"sign_in_required"}` before anything is read or counted. If Auth can't be reached, the answer is `503 auth_unavailable` and nothing is counted.
- **Fixed request shape.** The server picks the model (`gemini-2.5-flash`), 800 output tokens, JSON output and no thinking. The app only supplies the prompt (up to 6,000 characters) and system instruction (up to 3,000).
- **Per-account daily limit**, default 6 calls per UTC day (`AI_PROXY_ACCOUNT_DAILY_LIMIT`). An honest player makes at most 5: 4 AI Coach insights and 1 reminder-message pack. Calls are counted under `user:<account id>` in `ai_proxy_usage`, so reinstalling doesn't reset them.
- **Project-wide daily limit**, default 200 calls per UTC day (`AI_PROXY_GLOBAL_DAILY_LIMIT`). The hard ceiling, however many accounts someone creates.
- Malformed requests are refused before a call is claimed. Gemini's error bodies are never passed back.

Email sign-up makes making accounts cheap, so the global limit is what bounds the cost. If abuse shows up in the logs, lower `AI_PROXY_ACCOUNT_DAILY_LIMIT`, or turn on CAPTCHA for sign-ups in Supabase (**Authentication → Attack Protection**; the app would then need to send a CAPTCHA token).

The function logs one JSON line per refusal or failure: `{"event":"limit","scope":"account"}` or `"global"`, `{"event":"upstream_error","status":503}`, `upstream_unreachable`, `usage_unavailable` and `auth_unavailable`. It never logs the prompt, the answer, the token or the account id. In the Supabase dashboard, open **Edge Functions → gemini-proxy → Logs** to see how often limits are hit.

If the global limit is reached, every player sees "used up today's requests" until the next UTC day. Raise `AI_PROXY_GLOBAL_DAILY_LIMIT` as the player count grows, keeping it within your Gemini quota and budget.

## Deploying

Step 5 of [CLOUD_SETUP.md](CLOUD_SETUP.md) has the full steps (Gemini key → `supabase/.env.proxy` → `npx supabase secrets set --env-file supabase/.env.proxy` → `npx supabase functions deploy gemini-proxy --no-verify-jwt`). Redeploy with the same command after changing the function.

`--no-verify-jwt` is deliberate: the handler checks the token itself, with Supabase Auth, which works with both legacy JWT keys and the newer key types.

Quick check that it's deployed and protected:

```bash
curl -s -X POST "https://<project-ref>.supabase.co/functions/v1/gemini-proxy" -H "Content-Type: application/json" -d "{\"prompt\":\"hi\"}"
```

`{"error":"sign_in_required"}` means it's working. `{"error":"not_configured"}` means the `GEMINI_API_KEY` secret is missing. To try a real call, sign in on a debug build with `GEMINI_VIA_PROXY=true` and open the AI Coach.

## Tests

- `supabase/functions/gemini-proxy/handler.test.ts` runs under Node: `node --test "supabase/functions/**/*.test.ts"` (17 tests). CI runs it on every push.
- App side: `GeminiProxyClientTest`, `GeminiClientSelectionTest` (including a check that the release build type blanks the key) and `AiNeedsAccountTest` (nothing is sent while signed out, with either client).
