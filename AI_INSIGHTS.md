# PixelQuest — AI Habit Insights Architecture (`AI_INSIGHTS.md`)

## 1. Overview & Scope
PixelQuest integrates Google Gemini (targeting `gemini-1.5-flash`) to generate intelligent, personalized habit coaching and quest debriefs based on players' historical task performance and streak momentum. Day 24 provides the foundational pipeline (remote client, privacy-safe prompts, error handling, opt-in gating, and debug trigger), preparing for the full user-facing screen in Day 25.

---

## 2. API Call Throttling & Minimum Call Interval

### 2.1 Minimum Interval Definition
- **Minimum Enforced Interval**: **6 Hours (360 minutes / 21,600,000 milliseconds)** between live Gemini API calls per user.
- **Enforcement Scope**: Documented and architected on Day 24; enforced on debug trigger and finalized for full production UI on Day 25.

### 2.2 Rationale
1. **Habit Formation Cycles**: Habits naturally evolve over morning, afternoon, and evening routines. Triggering AI generation multiple times within minutes or hours produces repetitive, stagnant insights because the underlying completion logs have not changed meaningfully.
2. **Quota & Rate Limit Protection**: Gemini API tiers feature explicit Requests-Per-Minute (RPM) and Requests-Per-Day (RPD) quotas (e.g. 15 RPM, 1,500 RPD on free tiers). A 6-hour minimum interval guarantees that a single active user can consume at most 4 requests per 24-hour cycle, preventing rate limit exhaustion (`HTTP 429`).
3. **Battery & Mobile Data Conservation**: AI network payloads and JSON deserialization consume radio power and data. Limiting calls to natural daily checkpoints (e.g. morning plan, evening debrief) respects device resources.
4. **Cognitive Bandwidth**: Habit coaching is most impactful when thoughtful and episodic, rather than constant background noise.

---

## 3. Cache-Key Strategy & Staleness Window

### 3.1 Cache-Key Structure
To prevent redundant API dispatches, insight responses are indexed using a compound deterministic key:
```text
ai_insight_{user_scope}_{date}_{period_block}
```
- **`user_scope`**: `"local"` (for offline/anonymous local players) or authenticated Supabase UUID hash (never raw email).
- **`date`**: ISO-8601 calendar date (`YYYY-MM-DD`).
- **`period_block`**: Temporal day partition:
  - `AM`: 00:00 to 11:59 (Morning Quest Briefing).
  - `PM`: 12:00 to 23:59 (Evening Quest Debrief).

Example Key: `ai_insight_local_2026-09-30_PM`

### 3.2 Staleness Window (Time-To-Live)
- **Standard TTL**: **12 Hours** from generation timestamp (`generatedAt`).
- Cached responses within their 12-hour validity window are returned instantly with zero network roundtrips.

### 3.3 Dynamic Cache Invalidation Events
A cached insight is marked stale and purged ahead of TTL upon any of the following state transitions:
1. **Level-Up Milestone**: Player earns enough XP to advance their level.
2. **Streak Broken Event**: Consecutive day broken, requiring urgent recovery encouragement.
3. **Perfect Day Milestone**: All daily quests completed, triggering heroic celebration.
4. **Manual Debug Refresh**: Developer explicitly invokes debug pipeline regeneration.

---

## 4. Privacy & Data Minimization Policy

### 4.1 Strict Data Boundary
PixelQuest enforces an absolute data-minimization architecture for AI requests. User habit data can be deeply personal (e.g. medical habits, mental health routines, therapy appointments, financial tasks). Therefore, individual raw activities are strictly isolated to on-device SQLite storage and **never** leave the device.

### 4.2 What Data IS Transmitted to Google Gemini
Only anonymized, abstract aggregate statistics are transmitted in the prompt payload:
- **Streak Counts**: Integer metrics (`currentStreak`, `longestStreak`, `perfectDaysCount`).
- **Category Completion Ratios**: Predefined enum category names and numeric ratios (e.g. `FITNESS: 80% (4/5 completed)`).
- **Recent Momentum**: 7-day completion percentage and missed count.
- **Progression Level**: Anonymous numeric tier (e.g. `Level 5`).

### 4.3 What Data is EXPLICITLY FORBIDDEN from Transmission
The following data elements are strictly blocked and excluded:
1. **Verbatim Task Names**: No task title (e.g. "Take antidepressant medication", "Call AA sponsor", "Pay overdue rent") is ever sent.
2. **Task Descriptions & Notes**: Freeform user text is completely excluded.
3. **User Identifiers**: No usernames, real names, or display names.
4. **Account Information**: No emails, OAuth tokens, or Supabase user IDs.
5. **Exact Timestamps & Geolocation**: No time-of-day execution markers or coordinates.
6. **Device Identifiers**: No Android ID, advertising ID, MAC address, or hardware telemetry.

---

## 5. Google Gemini Data-Usage & Retention Policy Implications

### 5.1 Google AI Studio Tier Comparison

| Dimension | Free / Unpaid Developer Tier | Paid Pay-As-You-Go Tier | Vertex AI (Google Cloud) |
| :--- | :--- | :--- | :--- |
| **Model Training** | **Data MAY be used for model training**: Google's terms permit using prompt inputs and completions to improve Google products, services, and machine learning models. | **Data is NOT used for training**: Prompts and completions are never utilized to train Google models. | **Data is NOT used for training**: Customer isolation governed by Google Cloud BAA / SOC 2. |
| **Human Review** | Human reviewers may inspect and annotate conversation logs for quality and safety. | No human review of customer prompt data. | Zero human review; enterprise data governance. |
| **Data Retention** | Stored up to 18–36 months in Google logging infrastructure. | Transient logging for billing audits and operational abuse prevention. | Configurable log retention policies. |
| **Rate Limits** | 15 Requests Per Minute (RPM), 1,500 Requests Per Day (RPD). | Higher dynamic quotas (up to 360–1,000+ RPM). | Enterprise quotas. |

### 5.2 Architectural Implications for PixelQuest
1. **Defense-in-Depth Justification**: Because PixelQuest develops under the Developer Free Tier during early phases, Google could theoretically log transmitted prompts. This provides absolute justification for PixelQuest's aggressive client-side data minimization:
   - Since no personal task names, descriptions, emails, or usernames ever reach Google, any prompt retained by Google consists solely of abstract game numbers (e.g. `Current Streak: 5, FITNESS: 80%`).
   - Even in the event of human review on Google's end, the reviewer sees only non-identifiable habit statistics.
2. **Production Recommendation for Developer**:
   - Prior to publishing the production release of PixelQuest on Google Play, the developer should attach a billing account to Google AI Studio to graduate to the **Paid Pay-As-You-Go Tier**.
   - This eliminates model training and human review at negligible cost given PixelQuest's 6-hour minimum throttle and 12-hour client caching.

---

## 6. Architectural Decision: Opt-In Gating Architecture

### 6.1 Formal Decision: Strictly OPT-IN (`aiInsightsEnabled = false`)
PixelQuest formally designates AI Habit Insights as an **opt-in feature**. On all fresh installs, the preference `SettingsRepository.aiInsightsEnabled` defaults strictly to `false`.

### 6.2 Precedent & Rationale
1. **Consistency with Leaderboard Precedent (Day 14-15)**: PixelQuest's core identity is local-first. The Supabase global leaderboard defaults to `leaderboard_opt_in = false`. Departing from this precedent by enabling third-party AI calls by default would contradict the app's established privacy contract.
2. **Affirmative User Consent**: Transmitting user habit completion statistics to an external cloud API (Google) requires explicit, informed consent under modern data protection frameworks (GDPR, CCPA) and Android privacy guidelines.
4. **Controlled User Experience**: When players enable AI Habit Insights in Settings, they will be presented with a clear consent confirmation explaining the data-minimization guarantees before their first prompt is dispatched.

---

## 7. Architectural Decision: Day 25 Three-Theme Dispatch Integration

### 7.1 Decision: Implement Full Theme Dispatch (Pixel / Light / Comic) from Day One
Day 25's user-facing AI Insights screen will natively support all three established themes (**Pixel**, **Light**, and **Comic**) from initial release, rather than shipping a Pixel/Light baseline and retrofitting Comic later.

### 7.2 Rationale
1. **Established Dispatch Pattern**: Days 20–23 fully stabilized the `ComponentThemeFamily.from(PixelTheme.mode)` architecture. Discarding Comic mode for new screens would introduce technical debt and degrade the user experience for players who have unlocked and selected Comic mode.
2. **Reusability of Comic Tokens**: All required design tokens (`ComicPanel`, `ComicButton`, `ComicTopAppBar`, `Bangers` font, 2.5dp black ink contours, 4dp flat drop-shadows) are already implemented and tested in the design system.
3. **Thematic Presentation**:
   - **Pixel Mode**: 8-bit CRT quest debrief card with gold borders, arcade typography, and scanlines.
   - **Light Mode**: High-contrast ivory productivity panel with crisp stone pixel contours and deep slate text.
   - **Comic Mode**: Bold comic-strip mission intel with pop-art explosive bursts ("💥 MISSION DEBRIEF!"), Bangers header, and speech-bubble styling.
