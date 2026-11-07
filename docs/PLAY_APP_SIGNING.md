# Google Play App Signing & Release Distribution

Google Play requires all new apps to be published as an **Android App Bundle (`.aab`)** using **Google Play App Signing**.

---

## 1. Play App Signing Architecture

When publishing with Play App Signing:
1. **Upload Key (Developer managed):**
   - You sign the `.aab` file locally or in CI with your private upload key (`app/pixelquest-release.jks`).
   - Google verifies the signature upon upload to ensure authenticity.
   - If this key is lost or compromised, you can request an upload key reset directly in Play Console without stranding existing users.
2. **App Signing Key (Google Cloud KMS managed):**
   - Google securely manages the master app signing key within Google's key management infrastructure.
   - Google re-signs the optimized, device-targeted split APKs generated from your bundle before serving them to users.

---

## 2. Upload Key & Environment Configuration

The upload key details are documented in [docs/RELEASE_SIGNING.md](RELEASE_SIGNING.md):
- **Keystore:** `app/pixelquest-release.jks` (PKCS12)
- **Key Alias:** `pixelquest`
- **Secrets Configuration:**
  - Local builds (`local.properties`):
    ```properties
    KEYSTORE_FILE=pixelquest-release.jks
    KEYSTORE_PASSWORD=<secret>
    KEY_ALIAS=pixelquest
    KEY_PASSWORD=<secret>
    ```
  - CI builds (GitHub Actions Secrets): `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.

---

## 3. Building for Play Console vs. Local Testing

### A. Production Release Bundle (`.aab`)
To build the official release bundle for Google Play Console upload:
```bash
./gradlew :app:bundleRelease
# or convenience task:
./gradlew :app:buildReleaseBundle
```
**Output location:**
`app/build/outputs/bundle/release/app-release.aab`

### B. Local Testing APK (`.apk`)
For installing directly on local test devices or emulators via `adb install`:
```bash
./gradlew :app:assembleRelease
# or convenience task:
./gradlew :app:buildLocalReleaseApk
```
**Output location:**
`app/build/outputs/apk/release/app-release.apk`

---

## 4. Local Testing of App Bundles via `bundletool`

Before uploading to Play Console, you can verify how Google Play splits and delivers your bundle to devices using `bundletool`:

1. **Download `bundletool`:**
   ```bash
   # Obtain bundletool-all-<version>.jar from Google's github.com/google/bundletool/releases
   ```

2. **Generate APK set from the release bundle:**
   ```bash
   java -jar bundletool.jar build-apks \
     --bundle=app/build/outputs/bundle/release/app-release.aab \
     --output=app/build/outputs/bundle/release/pixelquest.apks \
     --mode=universal \
     --ks=app/pixelquest-release.jks \
     --ks-pass=pass:$KEYSTORE_PASSWORD \
     --ks-key-alias=pixelquest \
     --key-pass=pass:$KEY_PASSWORD
   ```

3. **Install on connected device:**
   ```bash
   java -jar bundletool.jar install-apks --apks=app/build/outputs/bundle/release/pixelquest.apks
   ```

---

## 5. Google Sign-In & API Credentials

Google Sign-In validates the SHA-1 fingerprint of the signing certificate. Because Google re-signs the APKs with the Play App Signing key:
1. Navigate to **Google Play Console** → **Release** → **Setup** → **App integrity** → **App signing**.
2. Copy the **App signing key certificate SHA-1 fingerprint**.
3. Add this SHA-1 into the Google Cloud Console (APIs & Services → Credentials → Android OAuth 2.0 Client ID), alongside the upload key SHA-1.

