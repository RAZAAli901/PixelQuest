# Gemini proxy (`supabase/functions/gemini-proxy`)

PixelQuest's release APKs are public on GitHub. A key compiled into an APK can be read out of it in seconds, because R8 renames code but leaves string constants as they are. So since Day 30, release builds don't contain the Gemini API key. They send AI requests to a Supabase Edge Function that holds the key.

```
App (release)                      Supabase                                   Google
GeminiProxyClient  --POST-->  functions/v1/gemini-proxy  --x-goog-api-key-->  Gemini generateContent
  prompt, systemInstruction      checks size, claims a call                    (response passed back
  x-pixelquest-device: <uuid>    (claim_ai_proxy_call)                          unchanged)
```

## Which builds use it

| Build | Gemini access |
| --- | --- |
| Release (local or CI) | Proxy. `GEMINI_API_KEY` is blank in the release build type, even when `local.properties` has a key. |
| Debug built by CI (attached to releases) | Proxy: the workflows write `GEMINI_VIA_PROXY=true`. |
| Debug built locally | Direct, with `GEMINI_API_KEY` from `local.properties`. Set `GEMINI_VIA_PROXY=true` there to try the proxy instead. |

The proxy needs a real Supabase project in the build (`SUPABASE_URL`, `SUPABASE_ANON_KEY`). With the placeholders, `GeminiProxyClient` sends nothing, and the AI Coach reports that AI isn't configured.

## What protects the key's quota

The anon key ships in every APK, so anyone can call the function. These are the limits:

- **Fixed request shape.** The server picks the model (`gemini-2.5-flash`), 800 output tokens, JSON output and no thinking. The app only supplies the prompt (up to 6,000 characters) and system instruction (up to 3,000).
- **Per-install daily limit**, default 6 calls per UTC day. An honest install makes at most 5: 4 AI Coach insights and 1 reminder-message pack.
- **Project-wide daily limit**, default 200 calls per UTC day. This is the hard ceiling: a client that keeps inventing new install ids still stops here.
- Malformed requests are refused before a call is claimed. Gemini's error bodies are never passed back.

If the global limit is reached, every player sees the AI as busy until the next UTC day. Raise `AI_PROXY_GLOBAL_DAILY_LIMIT` as the player count grows, keeping it within your Gemini quota and budget.

## Deploying (one time, then after changes to the function)

You need a Supabase project and the Supabase CLI (`npx supabase` runs it without installing).

1. Put the project's real values in `local.properties` and in the GitHub repository secrets, so CI builds get them:
   ```
   SUPABASE_URL=https://<project-ref>.supabase.co
   SUPABASE_ANON_KEY=<anon or publishable key>
   ```
   ```bash
   gh secret set SUPABASE_URL
   ```
   ```bash
   gh secret set SUPABASE_ANON_KEY
   ```
   Sign-in also needs `GOOGLE_WEB_CLIENT_ID` (already in `local.properties`; add it as a secret too, or release builds say cloud features are off):
   ```bash
   gh secret set GOOGLE_WEB_CLIENT_ID
   ```
2. Sign in and link the project:
   ```bash
   npx supabase login
   ```
   ```bash
   npx supabase link --project-ref <project-ref>
   ```
3. Apply the migrations, including `20261006000000_ai_proxy_usage.sql`:
   ```bash
   npx supabase db push
   ```
4. Store the Gemini key as a function secret. Put it in `supabase/.env.proxy` (gitignored) as `GEMINI_API_KEY=<your key>`, rather than typing it on the command line where it lands in your shell history. Then:
   ```bash
   npx supabase secrets set --env-file supabase/.env.proxy
   ```
   The same file can also set `GEMINI_MODEL`, `AI_PROXY_DEVICE_DAILY_LIMIT` and `AI_PROXY_GLOBAL_DAILY_LIMIT`. Delete it afterwards if you like; Supabase keeps the secret.
5. Deploy the function. Use `--no-verify-jwt`: the anon key is public, so checking it adds nothing, and new-style publishable keys aren't JWTs.
   ```bash
   npx supabase functions deploy gemini-proxy --no-verify-jwt
   ```
6. Check it end to end (any UUID works as the install id):
   ```bash
   curl -s -X POST "https://<project-ref>.supabase.co/functions/v1/gemini-proxy" -H "Content-Type: application/json" -H "x-pixelquest-device: 3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b" -d "{\"prompt\":\"Reply with {\\\"ok\\\":true}\"}"
   ```
   A working proxy returns Gemini's JSON (`{"candidates":[...]}`). `{"error":"not_configured"}` means step 4 is missing.
7. The `GEMINI_API_KEY` GitHub secret is no longer used by any workflow and can be deleted.

## Tests

- `supabase/functions/gemini-proxy/handler.test.ts` runs under Node: `node --test "supabase/functions/**/*.test.ts"`. CI runs it on every push.
- App side: `GeminiProxyClientTest`, `GeminiClientSelectionTest` (including a check that the release build type blanks the key) and `AiDeviceIdTest`.
