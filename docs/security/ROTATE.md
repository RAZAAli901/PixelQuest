# Secrets: scan results and rotation notes

Never write a secret's value here. Name it, say where it was and what to do.

## Scan, 2026-10-22 (S004)

`python3 scripts/security/scan_secrets.py --history` looked at every added line in every commit on every branch for:
- private keys;
- Google, AWS, GitHub, Slack, Stripe and OpenAI keys;
- Supabase secret keys, service-role JWTs and access tokens;
- passwords written as literals.

The working tree was scanned too.

**Result: no secret in the repo or its history.** No Supabase key of any kind was ever committed: the app gets the publishable key from `local.properties` or CI secrets at build time.

Five matches from a first, looser rule were checked by hand and were false positives:
- two `storePassword = storePasswordValue` lines in `app/build.gradle.kts` (a variable);
- two `password` parameters in the test helper `LocalSupabase.kt`;
- a test that generates a throwaway RSA key at run time (`supabase/functions/_shared/push.test.ts`).

The rules now skip each of these, and still catch a real `KEYSTORE_PASSWORD=...` line or a Google API key.

## Nothing to rotate in the repo

No key has to be rotated because of the repo. These are worth doing anyway:

| What | Where | Do |
|---|---|---|
| `GEMINI_API_KEY` GitHub secret | Repository secrets | **Delete it.** No workflow reads it (CI builds call Gemini only through `gemini-proxy`), so it's an extra copy of a paid key with no use. The proxy's copy is the Supabase function secret. |
| The Firebase service account key file | Was downloaded to `Downloads\firebase-push`; now in the Recycle Bin | **Empty the Recycle Bin.** The key is stored where it's needed (Supabase secret `FCM_SERVICE_ACCOUNT`). If the file was ever copied anywhere else, delete that key in Firebase (Project settings → Service accounts) and set a new one. |
| The Gemini key in `local.properties` | The owner's machine | Debug builds made on that machine embed it (`GEMINI_VIA_PROXY` false). Never share such a debug APK. CI's builds never have it. |
| The release keystore | The owner's machine and GitHub secrets | Keep two offline backups (it can't be replaced: a new key can't update installed apps). See [INCIDENT_RESPONSE.md](INCIDENT_RESPONSE.md). |

## How to rotate a key

| Key | Steps |
|---|---|
| Supabase service role / secret key | Dashboard → Project settings → API keys → roll. Functions read it from the platform, so nothing else changes. |
| Supabase publishable key | Roll in the dashboard; update `SUPABASE_ANON_KEY` in GitHub secrets and `local.properties`; release an update. Old app versions stop working until updated. |
| Gemini API key | Make a new key in Google AI Studio; `supabase secrets set GEMINI_API_KEY=...` (from a file, not the command line history); delete the old key. |
| FCM service account | Firebase → Service accounts → Generate new private key; `supabase secrets set --env-file` with it; delete the old key in Google Cloud → IAM → Service accounts → Keys. |
| GitHub secrets | Settings → Secrets and variables → Actions → Update. |
| Supabase access token (CLI) | supabase.com → Account → Access tokens → revoke; `npx supabase login` again. |
