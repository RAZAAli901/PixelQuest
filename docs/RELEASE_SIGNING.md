# Release signing

Created on Day 31 (8 Oct 2026). Every PixelQuest release from now on must be signed with this key. Android installs an update only when its signature matches the installed app's, so a release signed with any other key can't update this one.

## The key

| | |
| --- | --- |
| File | `app/pixelquest-release.jks` (PKCS12, gitignored by `*.jks`) |
| Alias | `pixelquest` |
| Algorithm | RSA 4096, SHA384withRSA |
| Subject | `CN=PixelQuest, O=PixelQuest` |
| Valid | 8 Oct 2026 to 23 Feb 2054 |
| SHA-256 | `9A:84:B2:CF:F2:F6:12:5C:6B:10:D5:D3:8F:D5:65:E4:D9:21:35:18:F6:D2:C1:B9:65:3C:B4:DA:D9:9A:CB:B8` |
| SHA-1 | `8C:41:C5:C5:E4:F6:1C:F8:EB:23:49:50:A9:AA:24:23:3C:8E:9E:E6` |

The fingerprints are public: anyone can read them from an APK. The keystore file and its password are not.

## Where the secrets are

- **This computer.** The keystore is at `app/pixelquest-release.jks`. The password is `KEYSTORE_PASSWORD` / `KEY_PASSWORD` in `local.properties`, with `KEYSTORE_FILE` and `KEY_ALIAS` beside it. Both files are gitignored. `./gradlew assembleRelease` signs with them.
- **GitHub.** `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS` and `KEY_PASSWORD` are repository secrets, set on 8 Oct 2026. Tagged releases (`.github/workflows/release.yml`) decode and sign with them.

## Back it up

If the keystore or its password is lost, no future release can update existing installs: every player would have to uninstall (losing progress they haven't backed up) and install again.

- Keep a copy of `app/pixelquest-release.jks` somewhere other than this computer.
- Keep the password in a password manager. It is the `KEYSTORE_PASSWORD` line in `local.properties`.
- The project folder is under OneDrive, which also keeps a copy of both files. That copy is a backup, but anyone with access to that OneDrive account can read it.

## How releases are checked

`app/release-signing-cert.sha256` holds the SHA-256 above. When the keystore secret is present, the release workflow checks the signed APK with `apksigner`. If the signer isn't this certificate, the release fails, so a wrong or replaced keystore secret can't publish an APK that won't update existing installs.

Locally:

```bash
"$LOCALAPPDATA/Android/Sdk/build-tools/34.0.0/apksigner.bat" verify --print-certs app/build/outputs/apk/release/app-release.apk
```

## Google Sign-In

Google Sign-In checks the signing certificate of the app asking for it. Add the SHA-1 above to the Android OAuth client in Google Cloud Console (APIs & Services → Credentials), next to any debug SHA-1 already there. Until then, Sign in with Google may fail in release builds.

## Players on v1.0.3 or v1.1.0

Those APKs were signed with keys made for one CI run each, so the next release can't install over them. Their players need to back up their progress (Settings → EXPORT QUEST DATA), uninstall, install the new release and restore. Say this in the next release's notes.
