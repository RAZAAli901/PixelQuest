# Writing a migration (default deny)

Since `20261019000110_default_deny`, a new table, sequence or function in `public` starts with **no access** for `anon` and `authenticated`. A new function has none for `PUBLIC` either, in any schema. Grant exactly what the feature needs, in the same migration.

## A new table

```sql
CREATE TABLE public.thing (...);
ALTER TABLE public.thing ENABLE ROW LEVEL SECURITY;           -- always (S008 refuses otherwise)
GRANT SELECT ON public.thing TO authenticated;                 -- only what the app does directly
CREATE POLICY "Members read their party's things" ON public.thing FOR SELECT TO authenticated
    USING (private.is_active_member(party_id));                -- rows, not just the table
```

- Writes go through a SECURITY DEFINER function where the rules are more than "your own row" (counts, limits, other tables).
- Grant `INSERT` or `UPDATE` only with a policy whose `WITH CHECK` pins the owner to `auth.uid()`.
- `anon` gets nothing, unless the feature is for signed-out players (only the leaderboard is).

## A new function

```sql
CREATE OR REPLACE FUNCTION public.do_thing(p_x TEXT)
RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER SET search_path = ''   -- fixed search_path (S018)
AS $$
DECLARE me UUID := (SELECT auth.uid());
BEGIN
    IF me IS NULL THEN RAISE EXCEPTION 'not_signed_in' USING ERRCODE = 'insufficient_privilege'; END IF;
    -- check the caller may do this to these rows; never trust an id the client sends as "me"
END $$;
GRANT EXECUTE ON FUNCTION public.do_thing(TEXT) TO authenticated;       -- or service_role only
```

- **Helpers that a policy calls** (in `private`) need `GRANT EXECUTE ... TO authenticated`, because policies run with the reader's rights.
- **Functions only Edge Functions call** get `GRANT EXECUTE ... TO service_role` and nothing else.

## Before pushing

1. Apply it to the local stack: `npx supabase@2.120.0 migration up --local`.
2. Run the server tests (`supabase/tests/local`) and `scripts/security/check_db.sql`.
3. Push to the development project, then production with the owner's approval ([ENVIRONMENTS.md](ENVIRONMENTS.md)).
