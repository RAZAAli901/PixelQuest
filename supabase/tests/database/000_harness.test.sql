-- pgTAP harness (S022): `npx supabase@2.120.0 test db` runs every *.test.sql here in a transaction
-- that rolls back. Catalogue-level rules live here; behaviour through the API (two players, RLS on
-- real requests) lives in supabase/tests/local/*.test.ts.
BEGIN;
CREATE EXTENSION IF NOT EXISTS pgtap WITH SCHEMA extensions;
SELECT plan(2);
SELECT has_schema('public', 'the public schema exists');
SELECT has_schema('private', 'the private schema exists');
SELECT * FROM finish();
ROLLBACK;
