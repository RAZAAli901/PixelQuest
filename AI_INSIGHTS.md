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
