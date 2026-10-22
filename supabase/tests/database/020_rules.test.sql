-- S029: the database security rules; the same queries as scripts/security/check_db.sql. Any row
-- here fails CI.
BEGIN;
SELECT plan(7);
SELECT is_empty($$ SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p') AND NOT c.relrowsecurity $$, 'every public table has row-level security');
SELECT is_empty($$ SELECT id FROM storage.buckets WHERE public $$, 'no bucket is public');
SELECT is_empty($$ SELECT table_name FROM information_schema.role_table_grants WHERE table_schema = 'public' AND grantee = 'anon' $$, 'anon has no table grant');
SELECT is_empty($$ SELECT table_name || ' to ' || grantee FROM information_schema.role_table_grants
    WHERE table_schema = 'public' AND grantee IN ('anon', 'authenticated') AND privilege_type IN ('TRUNCATE', 'TRIGGER', 'REFERENCES') $$,
    'no app role holds TRUNCATE, TRIGGER or REFERENCES');
SELECT is_empty($$ SELECT n.nspname || '.' || p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
    WHERE p.prosecdef AND n.nspname IN ('public', 'private') AND NOT ('search_path=""' = ANY (coalesce(p.proconfig, '{}'))) $$,
    'every SECURITY DEFINER function has an empty search_path');
SELECT is_empty($$ SELECT c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
    WHERE n.nspname = 'public' AND c.relkind = 'v' AND NOT coalesce('security_invoker=true' = ANY (c.reloptions), false)
      AND c.relname NOT IN ('race_board', 'study_chat_config', 'study_chat_types', 'study_points_config') $$,
    'views run as the reader, except the reviewed ones');
SELECT is_empty($$ SELECT g.table_name FROM information_schema.role_table_grants g
    JOIN pg_class c ON c.relname = g.table_name AND c.relnamespace = 'public'::regnamespace AND c.relkind = 'v'
    WHERE g.table_schema = 'public' AND g.grantee IN ('anon', 'authenticated') AND g.privilege_type IN ('INSERT', 'UPDATE', 'DELETE') $$,
    'nobody writes through a view');
SELECT * FROM finish();
ROLLBACK;
