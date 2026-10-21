-- Migration: 20261019000067_party_week_tz.sql
-- Description: Each party's week runs in its own time zone (docs/darood-weekly.md, The week).
--   * parties.week_tz: a time zone name; for a party already made, the leader's zone from their
--     latest Darood entry, or UTC. A new party's leader sets it right after making it (the app
--     sends the phone's zone), and can change it later: set_party_week_tz(party_id, tz), leader
--     only (not_leader), a real zone (bad_time_zone).
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.set_party_week_tz(UUID, TEXT); ALTER TABLE public.parties DROP COLUMN week_tz.

ALTER TABLE public.parties ADD COLUMN IF NOT EXISTS week_tz TEXT NOT NULL DEFAULT 'UTC';

UPDATE public.parties p SET week_tz = coalesce((
    SELECT e.tz FROM public.darood_entries e WHERE e.party_id = p.id AND e.user_id = p.leader_id ORDER BY e.created_at DESC LIMIT 1
), 'UTC')
WHERE p.week_tz = 'UTC';

CREATE OR REPLACE FUNCTION public.set_party_week_tz(party_id UUID, tz TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_column
DECLARE
    probe TIMESTAMP;
BEGIN
    IF NOT EXISTS (SELECT 1 FROM public.parties p WHERE p.id = set_party_week_tz.party_id AND p.leader_id = (SELECT auth.uid()) AND p.deleted_at IS NULL) THEN
        RAISE EXCEPTION 'not_leader' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF char_length(set_party_week_tz.tz) > 64 THEN
        RAISE EXCEPTION 'bad_time_zone' USING ERRCODE = 'invalid_parameter_value';
    END IF;
    BEGIN
        probe := now() AT TIME ZONE set_party_week_tz.tz;
    EXCEPTION WHEN invalid_parameter_value THEN
        RAISE EXCEPTION 'bad_time_zone' USING ERRCODE = 'invalid_parameter_value';
    END;
    UPDATE public.parties SET week_tz = set_party_week_tz.tz WHERE id = set_party_week_tz.party_id;
END;
$$;
REVOKE ALL ON FUNCTION public.set_party_week_tz(UUID, TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.set_party_week_tz(UUID, TEXT) TO authenticated;
