# ADR 0002: Offline-First Persistence with Room and Supabase RLS

- **Status**: Accepted
- **Date**: 2026-10-22
- **Deciders**: PixelQuest Core Engineering

## Context
Habit formation, prayer tracking, and quest logging must operate with zero network connectivity. Meanwhile, leaderboard rankings, parties, and multiplayer features require cloud synchronization with strict privacy controls.

## Decision
1. **Local Single Source of Truth**: Room SQLite is the authoritative source for user habits, completed logs, leveling progress, and preferences.
2. **Background Synchronization**: WorkManager (`ProfileSyncWorker`, `PeriodicSyncWorker`) pushes changes to Supabase when network is available with exponential backoff.
3. **Server-Authoritative Row Level Security (RLS)**:
   - Supabase PostgreSQL tables strictly enforce `auth.uid() = user_id` for reading and writing private rows.
   - Leaderboard is opt-in and exposes only public display names and high scores.
   - Storage buckets deny public read access by default and restrict uploads by MIME type and size limit.

## Consequences
- The application remains 100% functional without cellular or Wi-Fi connectivity.
- Leaked public anon keys cannot grant unauthorized access to other players' private data.
