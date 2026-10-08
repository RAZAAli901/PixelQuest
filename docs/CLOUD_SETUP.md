# Cloud setup: sign-in, leaderboard and AI Coach

PixelQuest works offline with no setup. Sign-in, cloud sync, the leaderboard and the AI Coach need a Supabase project. Without one, the app says these features aren't set up in this build. **The AI Coach is only for signed-in players** (Google, or a code sent by email), and the server checks that on every request.

This guide takes you from nothing to all four working. Allow about an hour. Steps 1–3 and 5–7 are required. Step 4 (Google) is optional: without it, players sign in by email only.

| Step | What | Where |
| --- | --- | --- |
| 1 | Create the Supabase project | supabase.com |
| 2 | Create the tables | Supabase CLI |
| 3 | Email-code sign-in | Supabase dashboard (+ an email provider) |
| 4 | Google sign-in (optional) | Google Cloud console + Supabase dashboard |
| 5 | AI Coach server (gemini-proxy) | Supabase CLI |
| 6 | Put the settings in the app and on GitHub | `local.properties`, `gh` |
| 7 | Check it all works | Phone or emulator |

Keep every key and password in a password manager. The Supabase **secret** (`service_role`) key and the server's Gemini key must never go into the app, `local.properties` or git. (`local.properties` keeps only your own `GEMINI_API_KEY` for local debug builds, as now; release builds never contain it.)

You need: Node.js (already installed for the function tests; `npx supabase` runs the Supabase CLI without installing it) and the GitHub CLI `gh` (already signed in).

---

## 1. Create the Supabase project

1. Sign up at [supabase.com](https://supabase.com) and click **New project**.
2. Name it `PixelQuest` and pick the region closest to your players. Set a **database password**, save it in your password manager (the CLI asks for it in step 2), and create the project. The free plan is enough to start.
   - Free projects are paused after a period of inactivity. Open the dashboard and restore the project if that happens; paid plans aren't paused.
3. Open **Project Settings → API Keys** (on older dashboards, **Project Settings → API**) and copy:
   - the **Project URL**, `https://<project-ref>.supabase.co`. The `<project-ref>` part is used below.
   - the **publishable key** (`sb_publishable_…`), or the legacy **anon** key. This one is meant to be public; it ships inside the app.

   Don't copy the **secret** / **service_role** key anywhere. The Edge Function gets it from Supabase automatically.

## 2. Create the tables

In a terminal at the project folder:

```bash
npx supabase login
```

```bash
npx supabase link --project-ref <project-ref>
```

If it complains that `supabase/config.toml` is missing, run `npx supabase init` once (answer No to the editor questions), then `link` again. It only adds a config file; the migrations stay as they are.

```bash
npx supabase db push
```

This applies the seven files in `supabase/migrations`: the `profiles` table and its row-level security, display-name moderation, `reports`, the account-deletion function, the leaderboard indexes and the AI call counter (`ai_proxy_usage`). In the dashboard's **Table Editor** you should now see `profiles`, `reports` and `ai_proxy_usage`.

## 3. Email-code sign-in

Players type their email, get a one-time code, and type it into the app. No passwords. The account is made the first time.

1. **Authentication → Sign In / Providers**:
   - **Allow new users to sign up**: on.
   - **Email** provider: on (it is by default). **Confirm email** can stay on; entering the code confirms the address.
   - In the Email provider settings, **Email OTP Length** should be 6 (the app accepts 6–10) and **Email OTP Expiration** 3600 seconds (or shorter, e.g. 900).
2. **Authentication → Emails → Templates**. Supabase sends a link by default; the app needs the code. Edit **both** **Magic Link** (returning players) and **Confirm signup** (first sign-in) so the body shows `{{ .Token }}`. For example:
   - Subject: `Your PixelQuest sign-in code`
   - Body:
     ```html
     <h2>Your PixelQuest code</h2>
     <p>Enter this code in the app to sign in:</p>
     <p style="font-size:28px;letter-spacing:4px"><strong>{{ .Token }}</strong></p>
     <p>It expires in an hour. If you didn't ask for it, you can ignore this email.</p>
     ```
3. **Set up an email provider before you release.** Supabase's built-in mailer only sends to the members of your Supabase team, and only about 2 emails an hour. That's fine for testing with your own address, but players would see "Sign-in emails can't be sent to this address yet".
   1. Make an account with an email service such as Resend, Brevo, Postmark, Amazon SES or SendGrid. Most have a free tier. They'll ask you to verify a domain you own (adding a few DNS records), so emails come from e.g. `login@yourdomain.com`.
   2. In Supabase, **Authentication → Emails → SMTP Settings**: turn on **Enable custom SMTP** and enter the host, port, username and password the service gives you, plus the sender address and name (`PixelQuest`).
   3. **Authentication → Rate Limits**: raise **emails sent per hour** to fit your players (it starts low once custom SMTP is on).

   Players can ask for a new code once a minute; the app counts that down for them.

## 4. Google sign-in (optional)

Skip this and the Account screen simply shows email sign-in only.

1. Go to [console.cloud.google.com](https://console.cloud.google.com) and create a project (e.g. `PixelQuest`).
2. **APIs & Services → OAuth consent screen** (shown as **Google Auth Platform** on newer consoles): app name `PixelQuest`, your support email, audience **External**. While the app is in *Testing*, only the test users you list can sign in; **Publish** it before you release.
3. **APIs & Services → Credentials → Create credentials → OAuth client ID**, three times:
   1. **Web application**, named `PixelQuest (Supabase)`. Under **Authorized redirect URIs** add `https://<project-ref>.supabase.co/auth/v1/callback`. Copy the **Client ID** and **Client secret**.
   2. **Android**, package name `com.pixelquest.app`, SHA-1 of the **release key**:
      `8C:41:C5:C5:E4:F6:1C:F8:EB:23:49:50:A9:AA:24:23:3C:8E:9E:E6` (also in `docs/RELEASE_SIGNING.md`).
   3. **Android** again, same package, SHA-1 of **this PC's debug key**, so debug builds can sign in too:
      `0F:DC:A3:9D:4C:85:D7:A2:6C:D9:5F:9B:1F:87:22:50:D7:F1:87:08`.
      On another PC, get its debug SHA-1 with:
      ```bash
      keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android
      ```
4. In Supabase, **Authentication → Sign In / Providers → Google**: turn it on.
   - **Client IDs**: the **Web** client ID first, then the two Android client IDs, separated by commas.
   - **Client Secret**: the Web client's secret.
   - Leave **Skip nonce check** off. The app sends the nonce (fixed in Day 31 Step 56).
5. The app's `GOOGLE_WEB_CLIENT_ID` (step 6) is the **Web** client ID, not an Android one.

## 5. AI Coach server (gemini-proxy)

The app never contains the Gemini key. It sends requests, with the player's sign-in token, to the `gemini-proxy` Edge Function, which checks the account with Supabase Auth, counts calls per account and calls Gemini. Details: [GEMINI_PROXY.md](GEMINI_PROXY.md).

1. Get a Gemini API key at [aistudio.google.com](https://aistudio.google.com) → **Get API key**. A separate key just for the server is best, so you can revoke either one alone.
2. Create `supabase/.env.proxy` (gitignored) containing:
   ```
   GEMINI_API_KEY=<your Gemini key>
   AI_PROXY_ACCOUNT_DAILY_LIMIT=6
   AI_PROXY_GLOBAL_DAILY_LIMIT=200
   ```
   The two limits are optional (those are the defaults): calls per signed-in account per UTC day, and for all players together. An honest player uses at most 5 a day.
3. Store the secrets and deploy:
   ```bash
   npx supabase secrets set --env-file supabase/.env.proxy
   ```
   ```bash
   npx supabase functions deploy gemini-proxy --no-verify-jwt
   ```
   `--no-verify-jwt` is right here: the function checks the player's token itself, and refuses the public anon key, expired tokens and anonymous users.
4. You can delete `supabase/.env.proxy` now; Supabase keeps the secrets.

## 6. Put the settings in the app and on GitHub

1. In `local.properties` (gitignored), replace the placeholders:
   ```
   SUPABASE_URL=https://<project-ref>.supabase.co
   SUPABASE_ANON_KEY=<publishable or anon key>
   GOOGLE_WEB_CLIENT_ID=<web client id>.apps.googleusercontent.com
   ```
   Leave out `GOOGLE_WEB_CLIENT_ID` if you skipped step 4. Keep `GEMINI_API_KEY` for local debug builds, which call Gemini directly (still only when signed in). Add `GEMINI_VIA_PROXY=true` to make a debug build use the server instead, which is the better test.
2. Give CI the same values, so the APKs it builds for releases have them. Each command asks for the value, so it doesn't end up in your shell history:
   ```bash
   gh secret set SUPABASE_URL
   ```
   ```bash
   gh secret set SUPABASE_ANON_KEY
   ```
   ```bash
   gh secret set GOOGLE_WEB_CLIENT_ID
   ```
3. The old `GEMINI_API_KEY` GitHub secret isn't used by any workflow. Delete it:
   ```bash
   gh secret delete GEMINI_API_KEY
   ```

## 7. Check it all works

1. The server refuses anyone who isn't signed in:
   ```bash
   curl -s -X POST "https://<project-ref>.supabase.co/functions/v1/gemini-proxy" -H "Content-Type: application/json" -d "{\"prompt\":\"hi\"}"
   ```
   `{"error":"sign_in_required"}` is the right answer. `{"error":"not_configured"}` means the Gemini secret is missing (step 5).
2. Install a debug build (`./gradlew :app:installDebug`) and open **Profile → ⚙️ APP SETTINGS → ☁️ CLOUD & LEADERBOARD** (the Account screen).
3. **SIGN IN WITH EMAIL** → your own address → **SEND CODE**. The email shows a 6-digit code (if it shows a link, redo step 3.2). Type it → **SIGN IN**.
4. **Settings → ✨ OPT IN & ENABLE AI COACH**, then go to Today: the coach card loads an insight once you've logged at least 3 quest results. In the dashboard, **Table Editor → ai_proxy_usage** shows a row `user:<your id>` with today's count.
5. Back on Account, join the leaderboard with a display name. The Leaderboard tab lists you.
6. Sign out. The AI Coach card now asks you to sign in.
7. If you did step 4: **SIGN IN WITH GOOGLE** works on the debug build. The release build needs the release SHA-1 client from step 4.3.2.
8. **Authentication → Users** in the dashboard lists both sign-ins.

Then you're ready for a release: bump `versionName` in `app/build.gradle.kts`, tag `v<that version>`, and the release workflow builds signed APKs with these settings (see `RELEASE_PIPELINE.md`).

## Troubleshooting

| What you see | Why, and what to do |
| --- | --- |
| "Sign-in emails can't be sent to this address yet" | The built-in mailer only reaches your Supabase team. Set up custom SMTP (step 3.3). |
| "Too many codes asked for" | One code per address per minute, plus the hourly email limit (step 3.3.3). |
| The email has a link, not a code | Add `{{ .Token }}` to both templates (step 3.2). |
| "That code is wrong or has expired" | Use the newest email's code; older ones stop working. Codes expire after the OTP expiration time. |
| "Email sign-in is switched off on the server" | Turn on the Email provider and **Allow new users to sign up** (step 3.1). |
| Google: "cloud token exchange failed" | The Web client ID isn't in Supabase's Google provider, or `GOOGLE_WEB_CLIENT_ID` is an Android client ID (step 4.4–4.5). |
| Google's account picker fails or shows no accounts | The package name or SHA-1 for this build isn't registered (step 4.3), or the consent screen is in Testing without you as a test user. |
| No Google button on Account | `GOOGLE_WEB_CLIENT_ID` is empty in this build. |
| "Cloud features are off in this build" | `SUPABASE_URL`/`SUPABASE_ANON_KEY` are still placeholders in `local.properties` (or the GitHub secrets, for CI builds). |
| AI Coach: "used up today's requests" | The per-account or project-wide daily limit (step 5.2). It resets at midnight UTC. |
| AI Coach asks you to sign in although you are | The session expired or was revoked. Sign out and in again. |

To see what the AI server is doing, open **Edge Functions → gemini-proxy → Logs**. It logs limits reached and failures, never prompts, answers, tokens or account IDs.
