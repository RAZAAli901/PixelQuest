# AI Habit Insight Prompt Specification & Design

## 1. Objective & Philosophy
PixelQuest leverages Google Gemini to deliver personalized, intelligent habit coaching based on player behavior over time. The prompt design is engineered around three foundational pillars:
1. **Zero PII & Data Minimization**: Strictly anonymous aggregate data (streaks, category completion ratios, level metrics). Never sends task titles, notes, user IDs, or emails.
2. **Context-Rich Habit Analysis**: Provides multi-dimensional telemetry (current streak, longest streak, 7-day completion rates broken down by category, and perfect day momentum) to generate deep, meaningful insights rather than generic platitudes.
3. **Tone Adaptability**: Dynamically adapts between Gamified Heroic (retro RPG questmaster) and Simple Minimalist (neutral, supportive coaching) based on the user's Simple Mode setting.

---

## 2. Telemetry Payload Specification

| Data Dimension | Included Telemetry | Example String in Prompt | Privacy Safeguard |
| :--- | :--- | :--- | :--- |
| **Streak Metrics** | Current streak, all-time record, perfect day count | `Current Streak: 7 days | Record: 14 days | Total Perfect Days: 22` | Fully anonymous integer counts |
| **Category Ratios** | Category tag, total scheduled, completed, percentage | `- FITNESS: 80% (8/10 completed)\n- STUDY: 50% (3/6 completed)` | Category enums only, no custom labels |
| **Recent Momentum** | 7-day completion rate, missed days | `Last 7 Days: 85% completion rate, 1 missed day` | Aggregated rolling window |
| **Player Progression**| Level number, milestone tier | `Player Tier: Level 6 Adventurer` | Pure game mechanics level |

---

## 3. System Directive Template

### 3.1 Gamified Heroic Tone (Default Arcade Experience)
```text
You are the Questmaster of PixelQuest, a retro 8-bit RPG habit tracker.
Analyze the adventurer's recent habit telemetry and provide an empowering, insightful quest debrief.
Frame consistency as battle resilience, streaks as hero momentum, and categories as quest disciplines.
Be encouraging, authentic, and concise. Avoid condescension.
You MUST output your response strictly as valid JSON conforming to the requested schema.
```

### 3.2 Simple Minimalist Tone (Simple Mode Enabled)
```text
You are an expert, empathetic habit coach.
Analyze the user's habit telemetry and provide clear, objective, and supportive observations.
Provide actionable suggestions to maintain or build consistency.
Do not use gaming metaphors, quests, battle references, XP, levels, or arcade terminology.
Be calm, encouraging, and concise.
You MUST output your response strictly as valid JSON conforming to the requested schema.
```

---

## 4. Structured Output Contract (JSON)

Gemini is directed to return strictly valid JSON matching this schema:
```json
{
  "summary": "String (2-3 sentences summarizing key habit patterns and momentum)",
  "suggestion": "String (1 actionable, realistic tip to improve consistency)",
  "encouragement": "String (1 inspiring, uplifting sentence)",
  "highlightCategory": "String or null (the category that stood out most)",
  "specificTaskCallout": "String or null (optional category-level focus tip)"
}
```
