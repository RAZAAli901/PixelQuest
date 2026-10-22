# Dependency audit

Q014 reviewed each direct Android dependency against production imports and the release shrinker
report. No direct production dependency is unused at this baseline.

Notable pairs that must both remain:

- `ktor-client-android` backs the Gemini and Gemini-proxy `HttpClient(Android)` clients.
- `ktor-client-okhttp` is selected explicitly for Supabase because Realtime needs WebSocket support.
- Raw OkHttp is used by chat-file and chess transports.
- Room, WorkManager, Hilt, DataStore, Credentials, Firebase Messaging, Coil, Adhan, Compose, and
  the Supabase modules all have direct production call sites.

Test-only dependencies are retained only where test sources import them. Review this file whenever a
direct dependency is added or a feature is removed; do not rely on R8 alone for dependency removal,
because generated code and runtime registration can be legitimate users.
