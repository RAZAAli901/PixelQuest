# Feature Packaging Architecture

Recorded 2026-10-22 for Q022.

PixelQuest packages code by feature, keeping feature-owned UI, ViewModels, domain rules, and data sources colocated, with platform primitives and reusable visual tokens residing in a shared core.

## Feature Taxonomy

| Feature | Primary Package(s) | Responsibility |
|---|---|---|
| **Home** | `ui.screens.home`, `ui.screens.HomeScreen.kt` | Daily quest progression overview, Coach card, Namaz preview, Study card |
| **Tasks / Quests** | `ui.screens.tasks`, `ui.screens.QuestsScreen.kt`, `domain.usecase.*Task*` | Quest CRUD, recurrence scheduling, history, completion states |
| **Hero / Stats** | `ui.screens.profile`, `ui.screens.stats`, `ui.screens.HeroScreen.kt`, `domain.model.Level` | Player level, XP progression, streaks, activity heatmaps, avatars |
| **AI Coach** | `domain.ai`, `ui.screens.insight` | Daily briefing and personalized habit tips via secure Gemini proxy |
| **Islamic (Deen)** | `islamic`, `namaz`, `quran` | Tanzil offline Quran reader, local Adhan prayer schedules, and prayer logging |
| **Party** | `party`, `darood`, `study` | Social accountability hubs: collective Darood counting, study task boards, and chat |
| **Chess** | `chess` | Server-refereed minigame, live spectator boards, party ladder |

## Shared Core Layer

- `ui.components`: Shared design-token driven components (`PqCard`, `PqButton`, `PqTopBar`, `PqDialog`).
- `ui.theme`: Color palettes, shapes, typography tokens for **Comic** and **Pixel** skins.
- `ui.motion`: Choreographed animations with reduced-motion support.
- `data.local`: Room SQLite database, base entities, Room transaction runner.
- `data.repository`: Cross-cutting offline-first repositories (`UserProfileRepository`, `SettingsRepository`).
- `worker`: WorkManager background synchronization engines (`ProfileSyncWorker`, `CleanupWorker`).
- `scheduling`: AlarmManager exact scheduling abstractions.
- `util`: Security-hardened release logger (`AppLog`) and uncaught crash handler.
