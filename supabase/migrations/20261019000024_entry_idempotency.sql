-- Migration: 20261019000024_entry_idempotency.sql
-- Description: Sending an entry twice never counts it twice (docs/darood-design.md, Entries).
--   * The phone gives each entry its own key, client_id, and keeps sending it until it hears back.
--     If the first send arrived but its answer was lost, the next one finds it and gets it back.
--   * client_id is unique per account (a partial index: entries without one stay as they were).
--   * add_darood_entry(party_id, amount, local_date, tz, client_id) is how the app posts: it looks
--     for the caller's entry with that key first, and returns it if it's there. Only a new one is
--     inserted, under the caller's own rights, so the insert policy and every trigger still apply.
--     The lookup comes first so a lost answer from the entry that finished the party isn't retried
--     into "party_closed". Two copies sent at once: one inserts, the other gets that one back.
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE.
-- Rollback: DROP FUNCTION IF EXISTS public.add_darood_entry(uuid, integer, date, text, uuid);
--   DROP INDEX IF EXISTS public.darood_entries_client_id; ALTER TABLE public.darood_entries DROP COLUMN IF EXISTS client_id;

ALTER TABLE public.darood_entries ADD COLUMN IF NOT EXISTS client_id UUID;
CREATE UNIQUE INDEX IF NOT EXISTS darood_entries_client_id ON public.darood_entries (user_id, client_id) WHERE client_id IS NOT NULL;
GRANT INSERT (client_id) ON public.darood_entries TO authenticated;

CREATE OR REPLACE FUNCTION public.add_darood_entry(party_id UUID, amount INTEGER, local_date DATE, tz TEXT, client_id UUID)
RETURNS public.darood_entries
LANGUAGE plpgsql
VOLATILE
SECURITY INVOKER
SET search_path = ''
AS $$
#variable_conflict use_variable
DECLARE
    caller UUID := (SELECT auth.uid());
    stored public.darood_entries%ROWTYPE;
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF client_id IS NOT NULL THEN
        SELECT * INTO stored FROM public.darood_entries e WHERE e.user_id = caller AND e.client_id = client_id;
        IF FOUND THEN
            RETURN stored;
        END IF;
    END IF;
    BEGIN
        INSERT INTO public.darood_entries (party_id, amount, local_date, tz, client_id)
        VALUES (party_id, amount, local_date, tz, client_id)
        RETURNING * INTO stored;
    EXCEPTION WHEN unique_violation THEN
        -- The same entry, sent twice at once: the other copy got in first.
        SELECT * INTO stored FROM public.darood_entries e WHERE e.user_id = caller AND e.client_id = client_id;
    END;
    RETURN stored;
END;
$$;

REVOKE ALL ON FUNCTION public.add_darood_entry(UUID, INTEGER, DATE, TEXT, UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.add_darood_entry(UUID, INTEGER, DATE, TEXT, UUID) TO authenticated;

COMMENT ON FUNCTION public.add_darood_entry(UUID, INTEGER, DATE, TEXT, UUID) IS 'Posts a Darood entry once: the same client_id again returns the entry already stored.';
