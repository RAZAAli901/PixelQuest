-- PixelQuest database security check (S029). Every query must return no rows.
-- Run it anywhere: the local stack (docker exec supabase_db_pixelquest psql -U postgres -f ...), or
-- the hosted project's SQL editor. CI enforces the same rules with pgTAP
-- (supabase/tests/database/020_rules.test.sql), so a breach fails the build.

-- 1. Tables in public without row-level security.
SELECT 'no RLS' AS problem, c.relname AS object FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'public' AND c.relkind IN ('r', 'p') AND NOT c.relrowsecurity
UNION ALL
-- 2. Public storage buckets.
SELECT 'public bucket', id FROM storage.buckets WHERE public
UNION ALL
-- 3. Anything the signed-out role can touch.
SELECT 'anon grant', table_name || ' ' || privilege_type FROM information_schema.role_table_grants
 WHERE table_schema = 'public' AND grantee = 'anon'
UNION ALL
SELECT 'anon execute', p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE n.nspname IN ('public', 'private') AND has_function_privilege('anon', p.oid, 'EXECUTE')
UNION ALL
-- 4. Grants that ignore RLS.
SELECT 'truncate grant', table_name || ' to ' || grantee FROM information_schema.role_table_grants
 WHERE table_schema = 'public' AND grantee IN ('anon', 'authenticated') AND privilege_type IN ('TRUNCATE', 'TRIGGER', 'REFERENCES')
UNION ALL
-- 5. SECURITY DEFINER functions without an empty search_path.
SELECT 'definer search_path', n.nspname || '.' || p.proname FROM pg_proc p JOIN pg_namespace n ON n.oid = p.pronamespace
 WHERE p.prosecdef AND n.nspname IN ('public', 'private') AND NOT ('search_path=""' = ANY (coalesce(p.proconfig, '{}')))
UNION ALL
-- 6. Views that run as their owner, other than the ones that are meant to (docs/security/INVENTORY.md).
SELECT 'definer view', c.relname FROM pg_class c JOIN pg_namespace n ON n.oid = c.relnamespace
 WHERE n.nspname = 'public' AND c.relkind = 'v'
   AND NOT coalesce('security_invoker=true' = ANY (c.reloptions), false)
   AND c.relname NOT IN ('race_board', 'study_chat_config', 'study_chat_types', 'study_points_config')
UNION ALL
-- 7. Writes through views.
SELECT 'view write grant', table_name || ' ' || privilege_type FROM information_schema.role_table_grants g
 JOIN pg_class c ON c.relname = g.table_name AND c.relnamespace = 'public'::regnamespace AND c.relkind = 'v'
 WHERE g.table_schema = 'public' AND g.grantee IN ('anon', 'authenticated') AND g.privilege_type IN ('INSERT', 'UPDATE', 'DELETE');
