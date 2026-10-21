# How to build, test and ship

The shell is Git Bash on Windows. Python is not on PATH. Previous sessions used a scratchpad `bin/` folder that won't exist for you, so install Python or use `py` if you need it.

## Build and unit tests

```bash
./gradlew :app:compileDebugKotlin :app:compileDebugUnitTestKotlin :app:compileDebugAndroidTestKotlin
./gradlew :app:testDebugUnitTest --tests "com.pixelquest.app.study.chat.*"
./gradlew :app:testDebugUnitTest
./gradlew :app:assembleDebug
```

- **Some test assertions read source files** (migrations, the manifest), so run tests from the repo root.
- **The whole unit suite** is about 2,000 tests and takes 15 minutes or more.
- **Robolectric can't reach a ModalBottomSheet's window:** test sheet logic through state, as `StudyPartyFlowTest` does.

## Local Supabase (Docker)

```bash
npx --yes supabase@2.120.0 start -x studio,postgres-meta,imgproxy,logflare,vector,supavisor
npx --yes supabase@2.120.0 migration up --local
npx --yes supabase@2.120.0 functions serve --env-file supabase/functions/.env.local
SUPABASE_LOCAL=1 node --test supabase/tests/local/study_chat.test.ts supabase/tests/local/study_files.test.ts supabase/tests/local/darood_parties.test.ts
node --test "supabase/functions/**/*.test.ts"
```

- **Storage-api is needed** for the chat, so don't exclude it from `start`.
- **`functions serve` keeps running,** so run it in the background. `supabase/functions/.env.local` is gitignored and holds a fake Gemini key.
- **Sign-ins are rate-limited** (30 per 5 minutes). Running every test file at once can hit it: wait a few minutes and run again.
- **`sql(...)`** in `supabase/tests/local/stack.ts` runs psql in the `supabase_db_pixelquest` container.

## Hosted Supabase (ask the owner first, every time)

```bash
npx --yes supabase@2.120.0 db push --dry-run
echo Y | npx --yes supabase@2.120.0 db push
npx --yes supabase@2.120.0 functions deploy <name> --no-verify-jwt
```

The project is already linked (`supabase/.temp/project-ref`).

## CI (`.github/workflows/`)

- **`build.yml` (Android CI Build):** unit tests, and the functions' node tests.
- **`cloud-tests.yml` (Cloud Tests):** a local stack (storage included), `db lint`, serving the functions, then every `supabase/tests/**` file.
- **`release.yml`:** runs on `v*` tags.

## Commits

One per plan item, explicit file list:

```bash
git add -- <files>
git commit -F msg.txt
git push origin main
```

The subject is `feat(study): ... (J0xx)`. End the body with the attribution line.

## Conventions worth knowing

- **Migrations:** `supabase/migrations/20261019000NNN_name.sql`, each with a header comment (description, re-runnable, rollback). Errors are short tokens (`not_allowed`, `too_many_uploads`), which the app maps to strings.
- **Repositories:** an interface plus `...Impl` with a primary constructor for tests and an `@Inject` secondary constructor. Fakes live in `app/src/test/java/com/pixelquest/app/testing/`.
- **Room:** bump `version` in `AppDatabase.kt`, build to export `app/schemas/.../N.json`, copy the exported `createSql` into a `MIGRATION_x_y`, and add a case to `DatabaseUpgradeTest`.
- **Strings:** `res/values/strings_study.xml`, `strings_study_reminders.xml`, `strings_chat.xml`.
