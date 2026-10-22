-- S023: the anonymous role (signed out) holds nothing in public, and no bucket is public.
BEGIN;
SELECT plan(5);
SELECT is_empty($$
    SELECT table_name || ' ' || privilege_type FROM information_schema.role_table_grants
    WHERE table_schema = 'public' AND grantee = 'anon'
$$, 'anon has no privilege on any public table or view');
SELECT is_empty($$
    SELECT table_name || '.' || column_name FROM information_schema.column_privileges
    WHERE table_schema = 'public' AND grantee = 'anon'
$$, 'anon has no column privilege in public');
SELECT is_empty($$
    SELECT p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE n.nspname IN ('public', 'private') AND has_function_privilege('anon', p.oid, 'EXECUTE')
$$, 'anon can run no public or private function');
SELECT is_empty($$
    SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind = 'S'
      AND CASE WHEN c.relkind = 'S' THEN has_sequence_privilege('anon', c.oid, 'USAGE') ELSE false END
$$, 'anon can use no public sequence');
SELECT is_empty($$ SELECT id FROM storage.buckets WHERE public $$, 'no storage bucket is public');
SELECT * FROM finish();
ROLLBACK;
