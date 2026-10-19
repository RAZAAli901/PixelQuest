-- Migration: 20261019000013_darood_rls.sql
-- Description: Row-level security on all three Darood Party tables, and only the grants they need.
--   * Row-level security is on for parties, party_members and darood_entries (each was created
--     with it on, so nothing was ever open; this makes it one place to see).
--   * Supabase grants every role everything on a new public table, and TRUNCATE, REFERENCES and
--     TRIGGER aren't covered by row-level security at all. So:
--       - signed-out callers (anon) get nothing on these tables;
--       - signed-in players (authenticated) keep only SELECT, which the policies filter. What they
--         may write is granted back, column by column, with the policies that allow it.
--   * The party functions (SECURITY DEFINER) and the server's own key are unaffected.
-- Re-runnable: enabling and revoking twice changes nothing.
-- Rollback: GRANT ALL ON public.parties, public.party_members, public.darood_entries TO anon, authenticated;

ALTER TABLE public.parties ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.party_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.darood_entries ENABLE ROW LEVEL SECURITY;

REVOKE ALL ON public.parties, public.party_members, public.darood_entries FROM anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER
    ON public.parties, public.party_members, public.darood_entries FROM authenticated;
GRANT SELECT ON public.parties, public.party_members, public.darood_entries TO authenticated;
