# PixelQuest Privacy Policy

**Effective Date:** 7 November 2026  
**Last Updated:** 7 November 2026  
**App Version:** 1.2.0+ (PixelQuest)

PixelQuest ("we", "us", or "our") is committed to respecting and protecting your privacy. This Privacy Policy explains what information we collect, how it is used, how we protect it, and the choices you have regarding your data.

---

## 1. Core Principles

- **Offline-First & Local by Default:** PixelQuest functions completely offline without an account. All personal quests, schedules, reminder preferences, completion logs, prayer records, and Quran bookmarks remain strictly on your local device.
- **Zero Advertising or Telemetry Tracking:** We do not include third-party advertising SDKs, behavioral analytics, or third-party trackers.
- **Data Minimization:** When you choose to use cloud features, we collect only the minimal data required to deliver that functionality.
- **You Own Your Data:** You can export all your cloud data or permanently delete your cloud account at any time in-app or via our web portal.

---

## 2. Information That Remains Locally on Your Device

The following data is stored exclusively in PixelQuest's private app sandbox on your device and is never transmitted to our servers:
- **Quests & Habits:** Quest titles, descriptions, schedules, recurrence rules, completion statuses, and XP calculations.
- **Spiritual Data:** Namaz prayer records, Darood count logs on the phone, and Quran reader bookmarks and display settings.
- **Device Location:** If you grant the approximate location permission for Islamic prayer calculations, coordinates are processed solely on-device to compute solar prayer times. Location coordinates are never transmitted or logged.

---

## 3. Information Collected When You Use Online Features

If you choose to sign in to sync progress, join public leaderboards, or participate in study groups:

| Data Type | Specific Information | Purpose | Shared With Others? |
|---|---|---|---|
| **Account Identity** | Email address and Supabase Auth User ID (or Google Auth ID) | Account authentication, passwordless email login, and security | Never shared publicly |
| **Public Pseudonym** | Chosen display name and avatar ID | Leaderboard rank representation | Visible on public leaderboard (only after explicit opt-in) |
| **Game Progress** | Level, total XP, current streak, longest streak | Public leaderboards and party race boards | Visible on leaderboard / party members |
| **Social / Study** | Party memberships, chat messages, shared study files, chess games | Study parties and multiplayer chess | Visible only to active party / game participants |
| **AI Insights** | Aggregated telemetry numbers (e.g. 5 quests completed, 3-day streak) | Generating habit coaching recommendations via Google Gemini proxy | Prompt sent to Gemini; never contains quest names, emails, or PII |
| **Moderation** | Report submissions and user block lists | Enforcing community safety and filtering abusive content | Service role / moderation admins only |

---

## 4. Third-Party Service Providers

We integrate strictly with reputable infrastructure providers under strict privacy and security agreements:

1. **Supabase (Backend as a Service):**
   - Hosts user authentication, cloud database, and file storage.
   - Enforces PostgreSQL Row-Level Security (RLS) on all user data.
2. **Google Gemini (AI Coaching):**
   - When AI Coach is active, an anonymized numerical payload is processed to generate motivational text.
   - Handled in compliance with Google Cloud / Gemini API data terms. Prompts are not used to train models.
3. **Firebase Cloud Messaging (FCM):**
   - Transmits encrypted push tokens for real-time multiplayer alerts (e.g. chess challenges). Notifications contain IDs only; text is assembled locally.

---

## 5. Data Security

- **Encryption in Transit:** All network communication is strictly enforced over HTTPS (TLS 1.3) with certificate verification.
- **Hardware-Backed Session Security:** On-device authentication tokens are sealed using the Android Keystore hardware-backed security enclave.
- **Access Control:** All database operations are secured via PostgreSQL Row-Level Security policies ensuring users can only read or write their own records.

---

## 6. Retention & Data Deletion

- **Retention Period:** We retain your cloud account data only for as long as your account remains active.
- **In-App Deletion:** You can delete your cloud data at any time from **Settings → Account → Delete my cloud data**. This triggers an atomic database RPC that purges your profile, display name, leaderboard rankings, and chat files.
- **Web Deletion Portal:** In accordance with Google Play data safety requirements, you may also request account and data deletion online without reinstalling the app at:  
  `https://razaali901.github.io/PixelQuest/delete-account.html`

---

## 7. Children's Privacy

PixelQuest is intended for general audiences aged 13 and older. We do not knowingly collect personal information from children under 13. If we discover that a child under 13 has provided personal data, we will delete it immediately.

---

## 8. Updates to This Policy

We may update this Privacy Policy periodically. Significant changes will be announced in-app and reflected in our release notes. The "Last Updated" date indicates the latest revision.

---

## 9. Contact Us

If you have any questions about this Privacy Policy or your personal data, please contact:  
**Email:** privacy@pixelquest.app (or submit an issue on our GitHub repository).
