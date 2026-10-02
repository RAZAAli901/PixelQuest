# Gemini Client Architectural Evaluation & Decision

## 1. Context & Objective
PixelQuest Day 24 initiates the foundational integration of Google Gemini to provide AI-powered habit analysis and coaching insights (with full user-facing UI launching on Day 25). The client layer must execute requests to Google's Generative Language API reliably, with minimal binary bloat, robust error handling, full compatibility with Kotlin 2.0, Compose, and Hilt, and zero leaks of user privacy.

This document evaluates the architectural approaches available at time of implementation:
1. **Official Google Generative AI Android SDK** (`com.google.ai.client.generativeai:generativeai`)
2. **Direct REST Client via Ktor** (`io.ktor:ktor-client-android` with `kotlinx.serialization`)
3. **Direct REST Client via Retrofit** (`com.squareup.retrofit2:retrofit`)

---

## 2. Evaluation Matrix

| Criteria | Official Android SDK (`com.google.ai.client...`) | Direct REST via Ktor (`io.ktor:ktor-client-android`) | Direct REST via Retrofit (`com.squareup...`) |
| :--- | :--- | :--- | :--- |
| **Existing Project Footprint** | None (requires new dependency) | **Already bundled** (v2.3.10 via Supabase stack) | None (requires new dependency) |
| **Kotlin 2.0 / Compose BOM Alignment** | Risk of transitive compiler / metadata conflicts | **100% verified** in active project build | Compatible, but extra annotation processing |
| **APK Binary Bloat** | +1.5MB to 3.0MB (transitive OkHttp, protobuf, etc.) | **0 MB additional overhead** | +500KB to 1MB |
| **HTTP Status & Error Granularity** | Abstracted exceptions; opaque HTTP 429 rate limits | **Exact HTTP status codes** (429, 403, 503) & retry headers | Exact HTTP status codes |
| **Testability & Mocking** | Requires mocking Google SDK classes or Robolectric | **Pure Kotlin mock engine** or simple interface injection | Requires MockWebServer |
| **Long-term Maintenance Stability** | Standalone Android SDK undergoing consolidation into Vertex/GenAI | **Direct REST endpoint** (`v1beta`) has guaranteed HTTP contract | Direct REST endpoint contract |
| **Structured Output Support** | Supported via SDK builder | Supported via standard JSON schema payload | Supported via standard JSON schema payload |

---

## 3. Decision: Direct REST Client via Ktor

### Rationale:
1. **Zero Added Dependencies**: PixelQuest already integrates Ktor 2.3.10 and `kotlinx.serialization` 1.6.3 for its Supabase cloud integration (Day 13). Reusing Ktor prevents library duplication and avoids inflating the APK size.
2. **Deterministic Error Handling**: Gemini API rate limiting (HTTP 429) and quota exhaustion require distinct handling from network timeouts or API key invalidation. Ktor's direct HTTP response inspectability allows parsing the exact status and retry-after metadata into a sealed `GeminiResult` hierarchy.
3. **Full Kotlin 2.0 & Coroutines Harmony**: Ktor natively leverages Kotlin Coroutines and structured concurrency without blocking Android main threads or clashing with Gradle kapt/ksp stubs.
4. **Target Model**: PixelQuest will target `gemini-2.5-flash` via `https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent` (key in the `x-goog-api-key` header), optimizing for low latency, high throughput, and cost-effective structured output generation.

---

## 4. Implementation Blueprint
- **Client Interface**: `GeminiClient` in `com.pixelquest.app.data.remote`
- **Engine**: Ktor `HttpClient` configured with `Android` engine and `ContentNegotiation` (Kotlinx JSON)
- **Dependency Injection**: Bound in `AiModule` (`com.pixelquest.app.di`)
- **Key Storage**: `local.properties` -> `BuildConfig.GEMINI_API_KEY` (never committed to version control)
