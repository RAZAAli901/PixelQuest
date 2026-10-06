# PixelQuest GitHub Actions Release Pipeline Documentation

This document outlines the workflow and secrets configuration for the PixelQuest Automated Release Pipeline (`.github/workflows/release.yml`).

## Trigger Conditions
- Any git tag push matching `v*` (e.g. `v1.0.0`, `v1.0.0-rc1`).

## Required GitHub Repository Secrets
To build and publish signed release APKs, the following secrets must be set in GitHub Repository Settings -> Secrets and variables -> Actions:

| Secret Name | Description | Example / Value |
|-------------|-------------|-----------------|
| `KEYSTORE_BASE64` | Base64-encoded string of `pixelquest-release.jks` | `MIIK...` |
| `KEYSTORE_PASSWORD` | Passphrase for the release keystore | `your_store_password` |
| `KEY_ALIAS` | Key alias inside the keystore | `pixelquest` |
| `KEY_PASSWORD` | Passphrase for the key alias | `your_key_password` |
| `SUPABASE_URL` | The Supabase project URL. Without it the build gets a placeholder, and sign-in, cloud sync, the leaderboard and the AI proxy don't work. | `https://<ref>.supabase.co` |
| `SUPABASE_ANON_KEY` | The project's anon or publishable key (public by design; it ships in the APK) | `eyJ...` or `sb_publishable_...` |
| `GOOGLE_WEB_CLIENT_ID` | Web OAuth client id for Google Sign-In | `...apps.googleusercontent.com` |
| `GITHUB_TOKEN` | Automatic GitHub token provided by Actions | Default `${{ secrets.GITHUB_TOKEN }}` |

**No Gemini key.** Release APKs are public, so they never contain the Gemini key. The release build type blanks `GEMINI_API_KEY`, and the AI goes through the `gemini-proxy` Edge Function, which holds the key (see `docs/GEMINI_PROXY.md`). Don't add the key to any workflow; `CiBuildWorkflowTest` fails if one reads it.

**Missing secrets are not fatal.** Without the keystore secrets, the release APK is signed with a throwaway debug key. Without the Supabase secrets, it gets placeholder Supabase settings. Each case adds a warning to the workflow run and a line to the release notes, so the release page says what's missing. v1.0.3 and v1.1.0 were built this way: the repository had none of these secrets.

## Local Test Tag Verification Script
```bash
# Push test tag to trigger release workflow run
git tag v1.0.0-rc1
git push origin v1.0.0-rc1
```
