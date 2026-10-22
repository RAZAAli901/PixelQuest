# Database and server tests

Two harnesses, both run against the local stack ([docs/LOCAL_SUPABASE.md](../../docs/LOCAL_SUPABASE.md)). CI runs both on every push and pull request (`.github/workflows/cloud-tests.yml`).

## pgTAP: rules about the database itself (`supabase/tests/database/*.test.sql`)

```bash
npx supabase@2.120.0 test db
```

Each file runs in a transaction that's rolled back. Use it for facts the catalogue can prove:
- RLS is on everywhere;
- `anon` holds nothing;
- every SECURITY DEFINER function fixes its search path;
- no bucket is public.

Write `SELECT plan(n);`, then pgTAP assertions (`is`, `ok`, `is_empty`, `has_table`...), then `SELECT * FROM finish();`.

## Node: behaviour through the real API (`supabase/tests/local/*.test.ts`)

```bash
SUPABASE_LOCAL=1 node --test "supabase/tests/local/*.test.ts"
```

Each test signs real players in and calls the REST API, RPCs, Storage and the Edge Functions as they would. That proves RLS on actual requests. For example:
- player B can't read player A's rows;
- a forged id is refused;
- a removed member loses access.

Helpers:
- `stack.ts`: `newPlayer`, `call`, `asService`, `sql` (psql in the database container) and `localTest` (skipped without `SUPABASE_LOCAL=1`).
- `chess_rls.test.ts`: `chessCast` (four shared players, to stay under the sign-in rate limit), `studyParty` and `rpc`.

The tests that call Edge Functions need them served: `npx supabase@2.120.0 functions serve --env-file supabase/functions/.env.local`. The load test runs only with `LOAD=1`.

## What each security test covers

See [docs/security/VERIFY.md](../../docs/security/VERIFY.md): every security fix names its test there.
