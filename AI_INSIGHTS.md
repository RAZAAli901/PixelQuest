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
