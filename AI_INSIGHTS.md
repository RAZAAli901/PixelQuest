# PixelQuest — AI Habit Insights Architecture (`AI_INSIGHTS.md`)

## 1. Overview & Scope
PixelQuest integrates Google Gemini (targeting `gemini-2.5-flash` via direct Ktor REST communication) to generate intelligent, personalized habit coaching and quest debriefs based on players' historical task performance and streak momentum. 

Day 24 establishes the complete, production-grade foundation:
- **Remote Client**: Direct Ktor REST client (`GeminiClient`) configured with `GEMINI_API_KEY` from `local.properties` via `BuildConfig`.
- **Privacy-Conscious Prompt Engine**: Strict data-minimization pipeline with active client-side regex and blacklist PII sanitization (`HabitInsightPromptBuilder`).
- **Tone Adaptation**: Simple Mode forward-compatibility via `HabitInsightToneHook` (RPG questmaster vs. supportive minimalist habit coach).
- **Error Handling**: Sealed `GeminiResult` hierarchy with graceful offline, rate-limit, and malformed JSON recovery.
- **Opt-In Groundwork**: Strict opt-in preference (`aiInsightsEnabled = false`) with Settings toggle and zero network calls when disabled.
- **Throttling & Debug Trigger**: Minimum 6-hour call interval enforcement and debug-only trigger (`DebugAiInsightTrigger`).

The user-facing insights screen is strictly **Day 25 scope**.

---

## 2. Gemini API Client Choice: Direct REST via Ktor
In Step 1, PixelQuest evaluated the official Google GenAI Android SDK vs. a direct REST client via Ktor:
- **Selected Approach**: **Direct REST Client via Ktor (`io.ktor:ktor-client-android`)**.
- **Rationale**:
  1. **Zero Added APK Bloat**: Ktor and `kotlinx.serialization` were already bundled in Day 13 for Supabase, adding 0 MB to APK footprint.
  2. **Predictable Dependency Graph**: Bypasses heavy transitive OkHttp/gRPC and AndroidX lifecycle dependencies introduced by Google AI SDKs.
  3. **Targeting Efficiency**: Direct `POST /v1beta/models/gemini-2.5-flash:generateContent`, with the key in the `x-goog-api-key` header, gives complete control over connection timeouts (25s), request headers, and JSON serialization.
- **Transport (Day 30)**: Release builds call Gemini through the `gemini-proxy` Supabase Edge Function (`GeminiProxyClient`). The Gemini key lives only in the function's secrets, because release APKs are public and a key compiled into them can be read. Local debug builds call Gemini directly (`GeminiClientImpl`) with the developer's key from `local.properties`, unless `GEMINI_VIA_PROXY=true`. The proxy applies the same generation config, plus per-install and project-wide daily limits. See `docs/GEMINI_PROXY.md`.
- **Generation config**: temperature 0.7, `maxOutputTokens` 800, `responseMimeType` `application/json`, and `thinkingConfig.thinkingBudget` 0. Gemini 2.5 Flash thinks by default, and its thinking tokens come out of `maxOutputTokens`. Until Day 29, a long think left the AI Coach's JSON cut off or empty, which the app showed as "came back garbled". These replies are short and structured, so thinking is off. The client joins the answer's text parts and skips thought summaries. An answer with no text is reported as an API error that names its `finishReason`.

---

## 3. Core Prompt Template & HabitInsightPromptBuilder

### 3.1 Prompt Data Inputs
The prompt builder gathers local Room data from `StreakDao`, `UserProfileDao`, `TaskDao`, and `TaskCompletionLogDao`, aggregating it into `HabitTelemetrySummary`:
- **Current Streak**, **Longest Streak**, and **Perfect Days Count**.
- **Player Progression Tier** (Level).
- **7-Day Consistency**: Completed over scheduled quests for today and the 6 days before it, and the missed count. Every quest that was due counts, so a day nobody logged is a miss (it used to be left out, and 1 completion out of 14 due quests read as 100%). Today only counts quests that already have a result, so an unfinished morning isn't held against the player. Older history is left out.
- **Category Ratios**: Completion ratios by enum category (e.g. `FITNESS: 80% (4/5 completed)`).

### 3.2 Tone Resolution via HabitInsightToneHook
- **Classic Gamified Heroic Mode (Default)**:
  - **Persona**: 8-bit Questmaster of PixelQuest.
  - **Vocabulary**: Battles, quests, dungeon mastery, hero momentum, guild inspiration.
- **Simple Minimalist Mode (`simpleModeEnabled = true`)**:
  - **Persona**: Empathetic, pragmatic habit coach.
  - **Vocabulary**: Steady routines, sustainable cadence, calm encouragement, zero fantasy/gaming metaphors.

---

## 4. Structured Output Contract (`HabitInsightResponse`)

### 4.1 JSON Schema
Gemini is instructed to output strictly valid JSON matching:
```json
{
  "summary": "High-level observation of recent habit patterns and momentum.",
  "suggestion": "Actionable recommendation for building or sustaining consistency.",
  "encouragement": "Motivating closing sentiment celebrating progress.",
  "highlightCategory": "FITNESS",
  "specificTaskCallout": "Morning workouts maintain peak momentum"
}
```

### 4.2 Robust Parsing & Sanitization (`HabitInsightResponse.parseFromJson`)
- Safely extracts JSON from markdown code blocks (````json ... ````) even when accompanied by conversational model preambles or postambles.
- Substring locator bounds JSON between outermost `{` and `}` curly braces.
- Normalizes literal `"null"` strings to native Kotlin `null` values.
- Throws typed `IllegalArgumentException` on malformed payloads, intercepted cleanly by `HabitInsightRepositoryImpl`.

---

## 5. API Call Throttling & Minimum Call Interval

### 5.1 Minimum Interval Definition
- **Minimum Enforced Interval**: **6 Hours (360 minutes / 21,600,000 milliseconds)** between live Gemini API calls per user.
- **Enforcement Scope**: Enforced in `DebugAiInsightTrigger` on Day 24; enforced on `HabitInsightRepository` and user-facing UI on Day 25.

### 5.2 Rationale
1. **Habit Formation Cycles**: Habits naturally evolve over morning, afternoon, and evening routines. Triggering AI generation multiple times within minutes or hours produces repetitive, stagnant insights because the underlying completion logs have not changed meaningfully.
2. **Quota & Rate Limit Protection**: Gemini API tiers feature explicit Requests-Per-Minute (RPM) and Requests-Per-Day (RPD) quotas (e.g. 15 RPM, 1,500 RPD on free tiers). A 6-hour minimum interval guarantees that a single active user can consume at most 4 requests per 24-hour cycle, preventing rate limit exhaustion (`HTTP 429`).
3. **Battery & Mobile Data Conservation**: AI network payloads and JSON deserialization consume radio power and data. Limiting calls to natural daily checkpoints (e.g. morning plan, evening debrief) respects device resources.
4. **Cognitive Bandwidth**: Habit coaching is most impactful when thoughtful and episodic, rather than constant background noise.

---

## 6. Cache-Key Strategy & Staleness Window

### 6.1 Cache-Key Structure
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

### 6.2 Staleness Window (Time-To-Live)
- **Standard TTL**: **12 Hours** from generation timestamp (`generatedAt`).
- Cached responses within their 12-hour validity window are returned instantly with zero network roundtrips.

### 6.3 Dynamic Cache Invalidation Events
A cached insight is marked stale and purged ahead of TTL upon any of the following state transitions:
1. **Level-Up Milestone**: Player earns enough XP to advance their level.
2. **Streak Broken Event**: Consecutive day broken, requiring urgent recovery encouragement.
3. **Perfect Day Milestone**: All daily quests completed, triggering heroic celebration.
4. **Manual Debug Refresh**: Developer explicitly invokes debug pipeline regeneration.

---

## 7. Privacy & Data Minimization Policy

### 7.1 Strict Data Boundary
PixelQuest enforces an absolute data-minimization architecture for AI requests. User habit data can be deeply personal (e.g. medical habits, mental health routines, therapy appointments, financial tasks). Therefore, individual raw activities are strictly isolated to on-device SQLite storage and **never** leave the device.

### 7.2 What Data IS Transmitted to Google Gemini
Only anonymized, abstract aggregate statistics are transmitted in the prompt payload:
- **Streak Counts**: Integer metrics (`currentStreak`, `longestStreak`, `perfectDaysCount`).
- **Category Completion Ratios**: Predefined enum category names and numeric ratios (e.g. `FITNESS: 80% (4/5 completed)`).
- **Recent Momentum**: 7-day completion percentage (completed over scheduled) and missed count.
- **Progression Level**: Anonymous numeric tier (e.g. `Level 5`).

### 7.3 What Data is EXPLICITLY FORBIDDEN from Transmission
The following data elements are strictly blocked and excluded:
1. **Verbatim Task Names**: No task title (e.g. "Take antidepressant medication", "Call AA sponsor", "Pay overdue rent") is ever sent.
2. **Task Descriptions & Notes**: Freeform user text is completely excluded.
3. **User Identifiers**: No usernames, real names, or display names.
4. **Account Information**: No emails, OAuth tokens, or Supabase user IDs.
5. **Exact Timestamps & Geolocation**: No time-of-day execution markers or coordinates.
6. **Device Identifiers**: No Android ID, advertising ID, MAC address, or hardware telemetry.

### 7.4 Active Client-Side Sanitization
Rather than simply trusting the prompt builder not to include PII, `HabitInsightPromptBuilder.sanitizePromptText` performs an active defense-in-depth sanitization pass:
- Strips any regex email patterns (`[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}`).
- Strips phone number patterns.
- Redacts any strings matching profile usernames, leaderboard display names, or task titles collected in an active blacklist.

---

## 8. Google Gemini Data-Usage & Retention Policy Implications

### 8.1 Google AI Studio Tier Comparison

| Dimension | Free / Unpaid Developer Tier | Paid Pay-As-You-Go Tier | Vertex AI (Google Cloud) |
| :--- | :--- | :--- | :--- |
| **Model Training** | **Data MAY be used for model training**: Google's terms permit using prompt inputs and completions to improve Google products, services, and machine learning models. | **Data is NOT used for training**: Prompts and completions are never utilized to train Google models. | **Data is NOT used for training**: Customer isolation governed by Google Cloud BAA / SOC 2. |
| **Human Review** | Human reviewers may inspect and annotate conversation logs for quality and safety. | No human review of customer prompt data. | Zero human review; enterprise data governance. |
| **Data Retention** | Stored up to 18–36 months in Google logging infrastructure. | Transient logging for billing audits and operational abuse prevention. | Configurable log retention policies. |
| **Rate Limits** | 15 Requests Per Minute (RPM), 1,500 Requests Per Day (RPD). | Higher dynamic quotas (up to 360–1,000+ RPM). | Enterprise quotas. |

### 8.2 Architectural Implications for PixelQuest
1. **Defense-in-Depth Justification**: Because PixelQuest develops under the Developer Free Tier during early phases, Google could theoretically log transmitted prompts. This provides absolute justification for PixelQuest's aggressive client-side data minimization:
   - Since no personal task names, descriptions, emails, or usernames ever reach Google, any prompt retained by Google consists solely of abstract game numbers (e.g. `Current Streak: 5, FITNESS: 80%`).
   - Even in the event of human review on Google's end, the reviewer sees only non-identifiable habit statistics.
2. **Production Recommendation for Developer**:
   - Prior to publishing the production release of PixelQuest on Google Play, the developer should attach a billing account to Google AI Studio to graduate to the **Paid Pay-As-You-Go Tier**.
   - This eliminates model training and human review at negligible cost given PixelQuest's 6-hour minimum throttle and 12-hour client caching.

---

## 9. Architectural Decision: Opt-In Gating Architecture

### 9.1 Formal Decision: Strictly OPT-IN (`aiInsightsEnabled = false`)
PixelQuest formally designates AI Habit Insights as an **opt-in feature**. On all fresh installs, the preference `SettingsRepository.aiInsightsEnabled` defaults strictly to `false`.

### 9.2 Precedent & Rationale
1. **Consistency with Leaderboard Precedent (Day 14-15)**: PixelQuest's core identity is local-first. The Supabase global leaderboard defaults to `leaderboard_opt_in = false`. Departing from this precedent by enabling third-party AI calls by default would contradict the app's established privacy contract.
2. **Affirmative User Consent**: Transmitting user habit completion statistics to an external cloud API (Google) requires explicit, informed consent under modern data protection frameworks (GDPR, CCPA) and Android privacy guidelines.
3. **Controlled User Experience**: When players enable AI Habit Insights in Settings, they will be presented with a clear consent confirmation explaining the data-minimization guarantees before their first prompt is dispatched.

---

## 10. Architectural Decision: Day 25 Three-Theme Dispatch Integration

### 10.1 Decision: Implement Full Theme Dispatch (Pixel / Light / Comic) from Day One
Day 25's user-facing AI Insights screen will natively support all three established themes (**Pixel**, **Light**, and **Comic**) from initial release, rather than shipping a Pixel/Light baseline and retrofitting Comic later.

### 10.2 Thematic Presentation Tokens
- **👾 Pixel Mode**: 8-bit CRT quest debrief card with gold borders, arcade typography (`PressStart2P`), and scanlines.
- **☀️ Light Mode**: High-contrast ivory productivity panel (`#FFFFFF`) with crisp stone pixel contours (`#E1E4E8`) and deep slate text.
- **💥 Comic Mode**: Bold comic-strip mission intel with pop-art explosive bursts ("💥 QUESTMASTER MISSION BRIEFING!"), `Bangers` header, 2.5dp solid black ink borders, and 4dp flat drop shadow.

---


## 11. Room Caching Architecture & Database Migration (Day 25 Steps 1–6)

### 11.1 Persistence Layer Details
- **Entity**: `InsightCacheEntity` storing `id`, `generatedAt`, `summary`, `suggestion`, `encouragement`, and `dataHash`.
- **DAO**: `InsightCacheDao` with conflict-replacement insertion, `getLatestInsight()`, reactive `observeLatestInsight()`, and `deleteOldInsights()`.
- **Repository**: `InsightCacheRepository` and `InsightCacheRepositoryImpl` with `isCacheValid(dataHash, ttlMillis, nowMillis)`.
- **Database Migration (`MIGRATION_3_4`)**: Bumps `AppDatabase` from version 3 to 4, safely creating `insight_cache` table:
```sql
CREATE TABLE IF NOT EXISTS insight_cache (
    id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
    generatedAt INTEGER NOT NULL,
    summary TEXT NOT NULL,
    suggestion TEXT NOT NULL,
    encouragement TEXT NOT NULL,
    dataHash TEXT NOT NULL
);
CREATE INDEX IF NOT EXISTS index_insight_cache_generatedAt ON insight_cache (generatedAt);
CREATE INDEX IF NOT EXISTS index_insight_cache_dataHash ON insight_cache (dataHash);
```

### 11.2 Compound Invalidation Key
- **`dataHash`**: SHA-256 hash computed over sanitized telemetry (`currentStreak`, `weeklyCompletionRate`, `totalScheduledQuestsPastWeek`, `categoryBreakdown`) plus the resolved `HabitInsightTone`.
- Invalidation triggers:
  1. Habit history changes (completions, missed tasks, streak adjustments).
  2. Tone change (Simple Mode toggle).
  3. Staleness: 12-hour TTL expiration.
  4. Manual user-triggered refresh after the 6-hour rate-limit cooldown window.

---

## 12. Architectural Decision: Screen Placement & Discoverability (Day 25 Step 23)

### 12.1 Decision: Surface on Today Dashboard as Contextual Card Over Dedicated Bottom Nav Tab
PixelQuest resolves the discoverability placement by surfacing AI Habit Insights as a first-class, theme-dispatching card on **`TodayScreen`** (the player's primary daily quest dashboard), supported by a focused destination route (`Screen.AiInsight.route`).

### 12.2 Tradeoff Analysis & Rationale
1. **Bottom Navigation Bar Ergonomics**:
   - The app's bottom navigation bar already contains 5 primary tabs (Today, Quests, Stats, Leaderboard, Profile).
   - Adding a 6th bottom navigation tab overcrowds mobile viewports, especially within the 8-bit retro arcade and comic design systems where touch targets require distinct padding and outlines.
2. **Contextual Proximity to Action**:
   - `TodayScreen` is where players plan, execute, and log daily habits. Presenting AI coaching alongside today's active quest list connects strategic advice directly to real-time player actions.
3. **Respecting the Opt-In Default**:
   - AI Insights defaults to disabled for data privacy. A persistent bottom tab dedicated to an optional disabled feature confuses players and wastes prime UI real estate.
   - On `TodayScreen`, an opt-in card smoothly communicates value when disabled, and blossoms into a live coaching HUD once enabled.
4. **Focused Drilldown Accessibility**:
   - In addition to the dashboard card, `Screen.AiInsight.route` enables full-screen debriefs, deep-links from notifications, or tap-to-expand workflows without nav bar bloat.

---

## 13. Architectural Decision: Simple Mode Visual Framing Adaptation (Day 25 Step 28)

### 13.1 Decision: Adjust Visual Framing Dynamically Under Simple Mode
PixelQuest formally decides that the AI Insights UI itself must dynamically adjust its visual framing when Simple Mode is active, extending beyond prompt-level copy generation to align visual aesthetics with the user's desire for un-gamified habit tracking.

### 13.2 Visual Treatment Comparison

| Visual Element | Gamified Mode (Default) | Simple Mode (`isSimpleMode = true`) |
| :--- | :--- | :--- |
| **Top App Bar Title** | `"AI COACH"` / `"AI QUESTMASTER"` | `"HABIT COACH"` |
| **Avatar Icon** | `🧙` (8-Bit Arcane Sage) | `🌱` (Clean Habit Sprout) |
| **Avatar Frame Color** | `#2E1065` (Arcane Violet) / Gold border | `#064E3B` (Calm Forest) / Emerald border |
| **Header Persona Title** | `"QUESTMASTER COACH"` | `"HABIT COACH"` |
| **System Status Tag** | `SYS.GEMINI.AI // ONLINE` | `SYS.HABIT.AI // ONLINE` |
| **Telemetry Badge** | `[QUEST TELEMETRY]` (Gold) | `[HABIT TELEMETRY]` (Emerald) |
| **Summary Tag** | `► OBSERVATION SCAN` | `► HABIT PATTERN SCAN` |
| **Action Tag** | `► STRATEGIC PROTOCOL` | `► SUGGESTED ACTION` |
| **Encouragement Tag** | `► HEROIC BLESSING` | `► DAILY PERSPECTIVE` |

### 13.3 Design Rationale
1. **Elimination of Cognitive Dissonance**: Generating serene, pragmatic habit copy (e.g. "Focus on consistent morning hydration routines") inside a card adorned with an RPG wizard avatar, "HEROIC BLESSING", and "QUESTMASTER COACH" headers produces jarring visual conflict.
2. **Respecting Simple Mode Philosophy**: Simple Mode users explicitly opted out of gamification, levels, and fantasy framing. The AI coach UI honors this contract by presenting a clean, objective coaching dashboard while preserving the chosen theme's surface identity (Pixel, Light, or Comic).
3. **Deterministic Cache Invalidation**: Because `HabitInsightTone` is embedded directly into the SHA-256 `dataHash` (Step 27), toggling Simple Mode automatically invalidates cached copy and refreshes both the content and visual framing without stale overlap.

---

## 14. Cost & Usage Safeguards: Hard Daily & Monthly Caps (Day 25 Step 30)

### 14.1 Limits Overview
To safeguard developer API budgets against infinite UI recomposition loops, background worker re-triggers, or client-side clock tampering, PixelQuest enforces two hard caps on total Gemini API calls independent of the per-request 6-hour cooldown window:

| Cap Dimension | Limit | Enforcement Mechanism | Failure Response |
| :--- | :--- | :--- | :--- |
| **Hard Daily Cap** | **4 Calls / Day** | Local date bucket counter (`yyyy-MM-dd`) | "You've reached today's insight limit (4/day). Check back tomorrow for a fresh debrief." |
| **Hard Monthly Cap** | **60 Calls / Month** | Local month bucket counter (`yyyy-MM`) | "You've reached this month's insight limit (60/month). Check back next month for fresh coaching." |

### 14.2 Financial & Operational Rationale
1. **Mathematical Alignment with 6-Hour Throttle**:
   - A 24-hour day divided by a 6-hour cooldown window allows an absolute maximum of 4 legitimate calls per day under perfect conditions (e.g. 00:00, 06:00, 12:00, 18:00).
   - Any attempt to exceed 4 calls in a single calendar day indicates either a clock anomaly, an automated script, or a logic defect. Capping at 4 requests/day enforces a zero-tolerance ceiling.
2. **Cost Containment Under Pay-As-You-Go**:
   - On Gemini 1.5 Flash:
     - Input token cost: ~$0.075 per 1,000,000 tokens (~$0.0000375 per 500-token prompt).
     - Output token cost: ~$0.30 per 1,000,000 tokens (~$0.000045 per 150-token JSON completion).
     - Combined cost per call: **~$0.0000825 USD**.
   - With a hard ceiling of 60 calls/month per user, the absolute maximum theoretical cost per user is **~$0.00495 USD / month** (< half a cent).
   - Even with 10,000 active monthly players, the total API bill cannot exceed **$50.00 USD / month**.
3. **Defense-in-Depth Against Runaway Costs**:
   - While the 6-hour rate limit checks `currentTime - lastTimestamp >= 6 hours`, a software bug in timestamp serialization or device clock manipulation could theoretically bypass the time delta check.
   - The daily/monthly usage counter tracks an independent integer count, creating a fail-safe circuit breaker that cannot be bypassed by clock adjustments.
4. **What counts toward the caps (Day 29)**:
   - Every call Gemini answers counts. That includes an answer the app can't use: unparseable JSON, an empty reply, or a 4xx/5xx error. Before Day 29 only a usable insight counted. A reply that kept failing to parse then cost one live call on every visit to Today, with no limit.
   - Calls that never got an answer don't count: offline, timed out, or refused by Gemini's own 429 quota.
   - Only a usable insight starts the 6-hour cooldown, so the player can retry after a failure (up to the daily cap).
   - The reminder-message pack (`EncouragementPackWorker`) counts every call it dispatches.

---

## 15. UI State Machine Architecture

### 15.1 Sealed UI State Contract (`AiInsightUiState`)
The UI layer is driven strictly by reactive StateFlow emissions from `AiInsightViewModel`:

```kotlin
sealed interface AiInsightUiState {
    data object Loading : AiInsightUiState
    data class Success(
        val insight: HabitInsightResponse,
        val isCached: Boolean = false,
        val remainingCooldownSeconds: Long = 0L,
        val canRefresh: Boolean = true
    ) : AiInsightUiState
    data class RateLimited(
        val retryAfterSeconds: Long,
        val message: String,
        val lastInsight: HabitInsightResponse? = null
    ) : AiInsightUiState
    data class NotEnoughData(
        val daysLogged: Int,
        val minimumRequiredDays: Int = 3,
        val message: String,
        val encouragingTip: String
    ) : AiInsightUiState
    data class Disabled(
        val message: String = "AI Habit Insights are disabled. Enable them in Settings."
    ) : AiInsightUiState
    data class CapReached(
        val message: String,
        val isMonthly: Boolean = false,
        val lastInsight: HabitInsightResponse? = null
    ) : AiInsightUiState
    data class Error(
        val message: String,
        val canRetry: Boolean = true,
        val fallbackInsight: HabitInsightResponse? = null
    ) : AiInsightUiState
}
```

### 15.2 State Transition Lifecycle
1. **Initial Load (`loadInsight(forceRefresh = false)`)**:
   - Check `SettingsRepository.aiInsightsEnabled`. If false -> `Disabled`.
   - Check completion history. If < 3 distinct days -> `NotEnoughData`.
   - Query `InsightCacheRepository.getLatestInsight()`. If warm -> immediately emit `Success(isCached = true)` for instant rendering.
   - Call `HabitInsightRepository.generateHabitInsight()`. If cache is valid (same `dataHash`), emit `Success(isCached = true)`. If stale, call Gemini and emit fresh `Success(isCached = false)`.
2. **Rate Limit / Cooldown**:
   - If user taps refresh before 6 hours elapse, emit `RateLimited` displaying countdown and preserving `lastInsight`.
3. **Usage Limit Hit**:
   - If user hits 4 calls/day or 60 calls/month, emit `CapReached` disabling refresh and showing friendly reset notice.

---

## 16. Feature Status: 100% Complete
- **Backend & Model**: Google Gemini 1.5 Flash via direct Ktor REST.
- **Privacy & Sanitization**: Zero PII, regex email/phone stripping, active username/task title redaction.
- **Caching**: Room database v4 (`insight_cache`), 12h TTL, SHA-256 `dataHash`.
- **Throttling & Caps**: 6-hour minimum interval, 4 calls/day, 60 calls/month.
- **User-Facing UI**: Multi-theme dispatch (Pixel, Light, Comic) on `TodayScreen` and dedicated `AiInsightScreen`.
- **Simple Mode**: Tone hook (`CALM_SIMPLE`) and visual framing adaptation.
- **Settings**: Dedicated AI Habit Coach section with privacy confirmation dialog.
- **QA Verification**: 100% passing unit, integration, theme rendering, and regression test suites.



