# Local Supabase test bench

The whole PixelQuest backend runs on this computer in Docker: the database with its migrations and row-level security, email sign-in (emails land in a local inbox), the REST API and the `gemini-proxy` Edge Function. Nothing here touches a hosted project, costs anything or needs an account.

Use it to test cloud changes before they reach the real project, and to try the real app against a real backend.

## What you need

- **Docker Desktop**, running. It's installed here; start it from the Start menu.
- **Node.js.** `npx supabase@2.120.0` runs the Supabase CLI without installing it.
- About 9 GB of disk for the Docker images, downloaded the first time.

## Start and stop

From the project folder:

```bash
npx supabase@2.120.0 start -x studio,postgres-meta,storage-api,imgproxy,realtime,logflare,vector,supavisor
```

The first start downloads the images (several minutes). After that it takes about 30 seconds. It applies every file in `supabase/migrations` and prints the local URL and keys. Those keys are the same well-known defaults on every machine, not secrets. `-x` skips services PixelQuest doesn't use.

```bash
npx supabase@2.120.0 status
```

```bash
npx supabase@2.120.0 stop
```

`stop` keeps the data; `stop --no-backup` throws it away. To start over with a clean database, run `npx supabase@2.120.0 db reset`. After adding a migration, `npx supabase@2.120.0 migration up --local` applies just the new one.

The settings for the local stack are in `supabase/config.toml`. They match what the hosted project should use:
- email sign-in by code;
- one code per address per minute;
- confirmations on;
- the code email template in `supabase/templates/sign_in_code.html`.

## The AI server

To run `gemini-proxy` with its settings, create `supabase/functions/.env.local`. It's gitignored, like every `supabase/functions/.env*` file:

```
GEMINI_API_KEY=local-test-not-a-real-key
AI_PROXY_ACCOUNT_DAILY_LIMIT=2
AI_PROXY_GLOBAL_DAILY_LIMIT=200
```

Then serve it, leaving this running:

```bash
npx supabase@2.120.0 functions serve --env-file supabase/functions/.env.local
```

With the fake key, a call from a signed-in player passes every check, gets counted, and reaches Google. Google refuses the key, which uses no quota, so the app shows "Gemini is having trouble". Put a real key there only if you want real answers.

## Reading the sign-in emails

Every email the local stack sends is caught by Mailpit at <http://127.0.0.1:54324>. Open it in a browser to see the code.

## Running the tests

The bench tests are skipped unless you opt in, so ordinary test runs don't need Docker.

| Tests | What they check | Command |
| --- | --- | --- |
| `supabase/tests/local/*.test.ts` (Node) | Email sign-in and its error codes; leaderboard row-level security and rank counting; reports and account deletion; the AI counter's lock-down; `gemini-proxy`'s sign-in check and per-account limit; the display-name rules | `SUPABASE_LOCAL=1 node --test "supabase/tests/**/*.test.ts"` |
| `LocalSupabaseAppTest` (Kotlin) | The app's own repositories against the stack: email sign-in, joining, leaderboard and rank, sync decisions, leaving, account deletion, the AI proxy with a real session | `PIXELQUEST_LOCAL_SUPABASE=1 ./gradlew :app:testDebugUnitTest --tests "*LocalSupabaseAppTest"` |

Run the proxy tests with the function served as above. In PowerShell, set the variable first, for example `$env:SUPABASE_LOCAL = "1"`.

## The real app against the bench

A debug build can use the local stack. Plain `http` is allowed only in debug builds and only for this computer; the emulator reaches it as `10.0.2.2`. The `PIXELQUEST_` variables override `local.properties` for one build, so you don't have to edit it:

```bash
PIXELQUEST_SUPABASE_URL=http://10.0.2.2:54321 PIXELQUEST_SUPABASE_ANON_KEY=<PUBLISHABLE_KEY from status> PIXELQUEST_GEMINI_VIA_PROXY=true ./gradlew :app:installDebug
```

Then, in the app:
1. Sign in from Account with **SIGN IN WITH EMAIL**, using any address. The code is in Mailpit.
2. Join the leaderboard, complete a quest, and turn on the AI Coach.

Build again without the variables to go back to your usual settings.
