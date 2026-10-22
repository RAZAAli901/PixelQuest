-- Migration: 20261019000107_chess_reports.sql
-- Description: Reporting a chess challenge (J172, docs/ABUSE_REVIEW.md). Chat messages and files
-- are reported with report_chat_message (20261019000066); a challenge isn't a member's message
-- (its chat line is the server's), so it has its own report:
--   * chess_reports: report_chess_challenge(challenge_id, reason) by an active member of the
--     challenge's study party who didn't send it; it names the challenger. One report per member
--     per challenge. Nobody but the service role reads reports.
--   * Blocking the challenger is block_member (20261019000066); blocked pairs can't challenge
--     each other or take each other's open challenges (20261019000096).
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.report_chess_challenge(UUID, TEXT); DROP TABLE public.chess_reports.

CREATE TABLE IF NOT EXISTS public.chess_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    reported_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    challenge_id UUID NOT NULL REFERENCES public.chess_challenges(id) ON DELETE CASCADE,
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    reason TEXT NOT NULL CHECK (reason IN ('spam', 'abuse', 'other')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (reporter_id, challenge_id)
);
ALTER TABLE public.chess_reports ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chess_reports FROM anon, authenticated;

CREATE OR REPLACE FUNCTION public.report_chess_challenge(challenge_id UUID, reason TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_column
DECLARE
    me UUID := (SELECT auth.uid());
    c public.chess_challenges%ROWTYPE;
BEGIN
    SELECT * INTO c FROM public.chess_challenges WHERE id = report_chess_challenge.challenge_id;
    IF NOT FOUND OR me IS NULL OR c.from_user = me OR NOT private.chess_is_study_member(c.party_id, me) THEN
        RAISE EXCEPTION 'not_allowed' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF report_chess_challenge.reason IS NULL OR report_chess_challenge.reason NOT IN ('spam', 'abuse', 'other') THEN
        RAISE EXCEPTION 'bad_reason' USING ERRCODE = 'invalid_parameter_value';
    END IF;
    INSERT INTO public.chess_reports (reporter_id, reported_id, challenge_id, party_id, reason)
    VALUES (me, c.from_user, c.id, c.party_id, report_chess_challenge.reason)
    ON CONFLICT (reporter_id, challenge_id) DO NOTHING;
END;
$$;
REVOKE ALL ON FUNCTION public.report_chess_challenge(UUID, TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.report_chess_challenge(UUID, TEXT) TO authenticated;
