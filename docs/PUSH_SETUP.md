# Push notifications for chess

Chess challenges reach a phone by Firebase Cloud Messaging (FCM). Without the settings below, push is off: challenges still show in the study party's chat and Chess tab.

## What goes where

| Setting | What it is | Where it's kept |
|---|---|---|
| `FCM_API_KEY`, `FCM_APP_ID`, `FCM_PROJECT_ID`, `FCM_SENDER_ID` | The Firebase Android app's public settings, from its `google-services.json` | `local.properties` (gitignored) for local builds; repository secrets of the same names for CI and release builds |
| `FCM_SERVICE_ACCOUNT` | The Firebase service account's private key (JSON). It lets the server send pushes | Supabase function secrets only. Never in the app, the repo or a chat |

The repo has no `google-services.json`: the app builds its Firebase setup from the four values (`PushSetup` in `push/PushRegistration.kt`).

## Steps

1. **Firebase project.** At [console.firebase.google.com](https://console.firebase.google.com), use an existing project or add one. Google Analytics isn't needed.
2. **Android app.** Project settings → General → Add app → Android, with package name `com.pixelquest.app`. Download its `google-services.json`. From it:
   - `FCM_API_KEY` is `client[0].api_key[0].current_key`;
   - `FCM_APP_ID` is `client[0].client_info.mobilesdk_app_id`;
   - `FCM_PROJECT_ID` is `project_info.project_id`;
   - `FCM_SENDER_ID` is `project_info.project_number`.
3. **Service account key.** Project settings → Service accounts → Generate new private key. It downloads a JSON file. Keep it private, and delete it once step 5 is done.
4. **The app's settings:** add the four `FCM_*` lines to `local.properties`. Add them as repository secrets too (`gh secret set FCM_API_KEY`, and so on), so CI and release builds have push.
5. **The server's key:** `npx supabase@2.120.0 secrets set FCM_SERVICE_ACCOUNT="$(cat path/to/service-account.json)"`. The functions read it on their next call; no redeploy is needed.
6. **Check.** Build and install, sign in on two phones in the same study party, and send a challenge from one. The other gets "<name> challenges you" with the lock screen's content hidden. If nothing arrives, see the `send-push` and `chess-create-challenge` logs in the Supabase dashboard: `push_failed` with a status tells why.

Signing out ends a phone's token; the server forgets a token the first time a push to it fails (FCM answers 404 or 400).
