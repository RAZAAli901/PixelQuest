# GitHub Actions Secret-Injection Readiness Verification: Gemini API Key

> **Superseded on Day 30.** This plan was replaced: release builds no longer receive `GEMINI_API_KEY`. The repository is public and so are its release APKs, and a key compiled into `BuildConfig` can be read straight out of an APK. R8 renames classes and members but leaves string constants as they are, so the "obfuscated via ProGuard/R8" point below was wrong. The key now lives only in the `gemini-proxy` Supabase Edge Function's secrets; see `docs/GEMINI_PROXY.md`.


## 1. Context & Objective
In Day 13 (Step 8), PixelQuest established a secure CI/CD build secret injection pattern in `.github/workflows/build.yml` for Supabase credentials and Google OAuth client IDs. 

Day 24 Step 7 requires verifying that this established pattern is structurally prepared to accommodate the `GEMINI_API_KEY` for release workflows (scheduled for Day 30) without modifying the release workflow prematurely.

---

## 2. Established CI Secret Injection Pattern (Day 13 Baseline)
In `.github/workflows/build.yml`:
```yaml
      - name: Inject Supabase & OAuth Build Secrets
        env:
          SUPABASE_URL: ${{ secrets.SUPABASE_URL }}
          SUPABASE_ANON_KEY: ${{ secrets.SUPABASE_ANON_KEY }}
          GOOGLE_WEB_CLIENT_ID: ${{ secrets.GOOGLE_WEB_CLIENT_ID }}
        run: |
          echo "SUPABASE_URL=${SUPABASE_URL:-https://placeholder-project.supabase.co}" >> local.properties
          echo "SUPABASE_ANON_KEY=${SUPABASE_ANON_KEY:-placeholder-anon-key}" >> local.properties
          echo "GOOGLE_WEB_CLIENT_ID=${GOOGLE_WEB_CLIENT_ID:-}" >> local.properties
```

---

## 3. Structural Readiness Audit for `GEMINI_API_KEY`

1. **Local Properties Consumer (`app/build.gradle.kts`)**:
   As wired in Day 24 Step 3, Gradle reads:
   ```kotlin
   val geminiApiKeyProp = localProps.getProperty("GEMINI_API_KEY") ?: "placeholder-gemini-key"
   ...
   buildConfigField("String", "GEMINI_API_KEY", "\"$geminiApiKeyProp\"")
   ```
   If `local.properties` contains `GEMINI_API_KEY=<secret>`, Gradle extracts it. If missing, it provides a safe non-empty placeholder, preventing CI build breaks on public PRs where repository secrets are not exposed.

2. **Day 30 Integration Contract**:
   When Day 30 configures the full release pipeline, the step will simply receive:
   ```yaml
   env:
     ...
     GEMINI_API_KEY: ${{ secrets.GEMINI_API_KEY }}
   run: |
     ...
     echo "GEMINI_API_KEY=${GEMINI_API_KEY:-placeholder-gemini-key}" >> local.properties
   ```
   No custom build plugins, external secret managers, or Gradle changes are required.

3. **Security Assessment**:
   - Secrets are never committed to git (both `local.properties` and `/local.properties` are in `.gitignore`).
   - `BuildConfig.GEMINI_API_KEY` is compiled into bytecode and obfuscated via ProGuard/R8 in release builds (`isMinifyEnabled = true`).
   - Untrusted pull requests without repository secret access continue to build cleanly using the default placeholder fallback.

---

## 4. Verification Conclusion
The CI workflow architecture is **100% structurally ready** to accept `GEMINI_API_KEY` when Day 30 activates release-pipeline secret injection. No modifications to `.github/workflows/build.yml` are needed on Day 24.
