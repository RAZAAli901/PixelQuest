# Google Play Data Safety Form: Field-by-Field Worksheet

**App Name:** PixelQuest  
**Package Name:** `com.pixelquest.app`  
**Target SDK:** API 36 (Android 16)  
**Document Purpose:** Exact, field-by-field responses required to complete the Google Play Console **App Content → Data safety** questionnaire.

---

## 1. Overview & Data Collection Questions

### Does your app collect or share any of the required user data types?
- **Answer:** **Yes** (when using optional cloud features).

### Is all of the user data collected by your app encrypted in transit?
- **Answer:** **Yes** (All network communication enforces HTTPS / TLS 1.3).

### Do you provide a way for users to request that their data is deleted?
- **Answer:** **Yes**
  - **In-App:** Direct deletion in app via **Settings → Account → Delete my cloud data**.
  - **Web Deletion URL:** `https://razaali901.github.io/PixelQuest/delete-account.html`

---

## 2. Field-by-Field Data Types Matrix

| Category | Data Type | Collected? | Shared? | Handled Ephemerally? | Optional / Required? | Data Usage Purpose |
|---|---|---|---|---|---|---|
| **Location** | Approximate location | **No** (Processed locally on-device for solar prayer time calculations only; never sent to server) | No | Yes | Optional | Not collected |
| **Personal Info** | Email address | **Yes** | **No** | No | Optional (Only for cloud sign-in) | Account management, App functionality |
| **Personal Info** | User IDs (Account UUID) | **Yes** | **No** | No | Optional (Only for cloud sign-in) | Account management, App functionality |
| **Personal Info** | Name / Display name | **Yes** | **Yes** (Shown to other users on public leaderboard after explicit opt-in) | No | Optional | App functionality, Personalization |
| **App Activity** | App interactions | **Yes** (Aggregated numbers: streak count, XP, levels, completion counts) | **Yes** (Leaderboard / Party race board) | No | Optional | App functionality, Personalization |
| **Messages** | Emails | **No** | No | - | - | - |
| **Messages** | Other in-app messages | **Yes** (Study party chat messages) | **Yes** (Shared only with other active members of the private study party) | No | Optional | App functionality |
| **Files & Docs** | Files and docs | **Yes** (Study item attachment files uploaded by party members) | **Yes** (Shared only with members of that study party via short-lived signed URLs) | No | Optional | App functionality |
| **AI Content** | AI input / output | **Yes** (Aggregated numerical metrics sent to Gemini proxy; no PII) | **No** | Yes | Optional | App functionality, Personalization |
| **App Info & Performance** | Crash logs / Diagnostics | **No** (No third-party crash reporting SDKs installed; local Logcat only) | No | - | - | - |
| **Device or other IDs** | Device or other IDs | **Yes** (Firebase Cloud Messaging push notification token) | **No** | No | Optional | App functionality (Delivering chess game turn alerts) |

---

## 3. Data Safety Details Per Collected Type

### A. Personal Info: Email Address
- **Collected:** Yes (when user signs in with Google or email verification code).
- **Shared:** No (never disclosed to third parties or other players).
- **Encrypted in transit:** Yes.
- **Can be deleted:** Yes (user can delete account via in-app button or web deletion form).
- **Purposes:**
  - **Account management:** Used to authenticate user and restore cloud data.

### B. Personal Info: User IDs (Account UUID)
- **Collected:** Yes.
- **Shared:** No.
- **Encrypted in transit:** Yes.
- **Can be deleted:** Yes.
- **Purposes:**
  - **Account management & App functionality:** Database foreign keys for cloud synchronization.

### C. Personal Info: Name / Display Name
- **Collected:** Yes (when user joins the leaderboard or enters a public pseudonym).
- **Shared:** Yes (displayed publicly on the leaderboard to other players).
- **Encrypted in transit:** Yes.
- **Can be deleted:** Yes (leaving the leaderboard removes name; deleting account erases it).
- **Purposes:**
  - **App functionality & Community:** Displaying rankings.

### D. App Activity: App Interactions (Streaks, XP, Levels)
- **Collected:** Yes.
- **Shared:** Yes (visible on public leaderboard and party race boards).
- **Encrypted in transit:** Yes.
- **Can be deleted:** Yes.
- **Purposes:**
  - **App functionality:** Synchronizing progress across devices.

### E. Messages: Other In-App Messages (Study Party Chat)
- **Collected:** Yes.
- **Shared:** Yes (restricted to authenticated members of the same study party).
- **Encrypted in transit:** Yes.
- **Can be deleted:** Yes (sender or party leader can delete individual messages; account deletion purges user messages).
- **Purposes:**
  - **App functionality:** Group study collaboration.

### F. Files & Documents: Study Files
- **Collected:** Yes (PDFs, notes, study material uploaded to a study party).
- **Shared:** Yes (restricted to authenticated members of the same study party).
- **Encrypted in transit:** Yes.
- **Can be deleted:** Yes (uploader can delete file; account deletion purges uploaded files).
- **Purposes:**
  - **App functionality:** Group study sharing.

### G. Device or Other IDs: Push Notification Token
- **Collected:** Yes (FCM push token).
- **Shared:** No.
- **Encrypted in transit:** Yes.
- **Can be deleted:** Yes (revoked on sign-out or account deletion).
- **Purposes:**
  - **App functionality:** Real-time turn notifications for multiplayer chess.

---

## 4. Privacy Practices Declarations

1. **Does the app collect data from children under 13?**
   - **No.** PixelQuest is targeted at general audiences aged 13 and older.
2. **Does the app run third-party advertising SDKs?**
   - **No.** PixelQuest is 100% ad-free.
3. **Does the app sell user personal data?**
   - **No.** Never sold or shared with data brokers.
4. **Is user data transferred internationally?**
   - **Yes.** Data is stored in secure cloud infrastructure (Supabase EU/US region, Google Cloud Gemini API).
