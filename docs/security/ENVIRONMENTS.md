# Environments: development and production

S006. The live (production) Supabase project holds real players' data. Nothing experimental should run against it: no test accounts, no trial migrations, no agent with write access.

## The three places a build can talk to

| Environment | What it is | Who uses it |
|---|---|---|
| **Local stack** | Supabase in Docker on this computer ([LOCAL_SUPABASE.md](../LOCAL_SUPABASE.md)). Every migration and function runs here first. | The server tests; agents; trying a migration |
| **Development project** | A second, free Supabase project with the same migrations and functions, and only test accounts | Debug builds on phones; trying features with real phones |
| **Production project** | The live project | Release builds only |

## How builds choose

- **Release builds** use `SUPABASE_URL` and `SUPABASE_ANON_KEY`. They come from `local.properties`, or from GitHub secrets in CI.
- **Debug builds** use `SUPABASE_DEV_URL` and `SUPABASE_DEV_ANON_KEY` when both are set (`app/build.gradle.kts`). Until a development project exists, they fall back to the production settings.
- **A single build** can still be pointed at the local stack with the `PIXELQUEST_SUPABASE_URL` variables ([LOCAL_SUPABASE.md](../LOCAL_SUPABASE.md)).

## Setting up the development project (owner, once)

1. At supabase.com, create a new project, for example `pixelquest-dev`, in the same region.
2. Link and push the migrations to it:
   - `npx supabase@2.120.0 link --project-ref <dev ref>`
   - `npx supabase@2.120.0 db push`
3. Deploy the functions:
   - `npx supabase@2.120.0 functions deploy <name> --no-verify-jwt` for each one.
   - Set its secrets with a test Gemini key and its own Firebase project, or leave push off.
4. Add to `local.properties` (gitignored):
   - `SUPABASE_DEV_URL=https://<dev ref>.supabase.co`
   - `SUPABASE_DEV_ANON_KEY=<its publishable key>`
5. Re-link the CLI to production only when pushing a reviewed migration (`supabase link --project-ref <prod ref>`). Better: keep two clones, one linked to each.

## Rules

- **Migrations:** each one runs on the local stack and passes its tests before it reaches any hosted project, and the development project before production.
- **Agents** (Claude Code and others) get the local stack. They push to production only with the owner's explicit approval, migration by migration.
- **Data:** production data is never copied to development or local; test data is generated.
