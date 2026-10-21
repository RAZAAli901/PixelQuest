-- Migration: 20261019000082_chess_rating_policies.sql
-- Description: Who reads chess ratings and records (docs/chess-design.md): a player's own, and
-- those of anyone they share an active study party with. Written by the server only.
--   * private.shares_study_party(other): the caller and [other] are both active in some study party.
-- Re-runnable: CREATE OR REPLACE, DROP POLICY IF EXISTS.
-- Rollback: drop the policies, the grants and private.shares_study_party(UUID).

CREATE OR REPLACE FUNCTION private.shares_study_party(other UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT other = (SELECT auth.uid()) OR EXISTS (
        SELECT 1
        FROM public.party_members me
        JOIN public.party_members them ON them.party_id = me.party_id
        JOIN public.parties p ON p.id = me.party_id
        WHERE me.user_id = (SELECT auth.uid()) AND me.status = 'active'
          AND them.user_id = other AND them.status = 'active'
          AND p.party_type = 'study' AND p.deleted_at IS NULL
    );
$$;
REVOKE ALL ON FUNCTION private.shares_study_party(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION private.shares_study_party(UUID) TO authenticated;

GRANT SELECT ON public.chess_ratings, public.chess_head_to_head TO authenticated;

DROP POLICY IF EXISTS "Party mates read ratings" ON public.chess_ratings;
CREATE POLICY "Party mates read ratings" ON public.chess_ratings FOR SELECT TO authenticated
    USING (private.shares_study_party(user_id));

DROP POLICY IF EXISTS "Party mates read head-to-head" ON public.chess_head_to_head;
CREATE POLICY "Party mates read head-to-head" ON public.chess_head_to_head FOR SELECT TO authenticated
    USING (private.shares_study_party(user_low) AND private.shares_study_party(user_high));
