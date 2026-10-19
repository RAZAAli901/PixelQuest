-- Migration: 20261019000030_race_board.sql
-- Description: The milestone race, opt-in per party (docs/darood-design.md, The milestone and the race).
--   * parties.is_public_race, false by default: a party is on the race board only once its leader
--     turns it on. The leader turns it on and off with an update of that column alone, under the
--     same row-level security as renaming (the column grant below; nobody else's update changes a row).
--   * race_board: the opted-in parties that have a milestone and aren't deleted, and only
--     name, member_count, total, milestone, progress (0 to 1) and completed_at. Never members,
--     usernames, entries or ids. mine says whether the reader is an active member, so their own can
--     be picked out without exposing ids.
--   * It's an ordinary view (its owner's rights), so any signed-in player reads other parties'
--     lines, which the parties' own row-level security wouldn't let them; it filters for itself, and
--     its columns are all there is. Signed out reads nothing.
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE.
-- Rollback: DROP VIEW IF EXISTS public.race_board; ALTER TABLE public.parties DROP COLUMN IF EXISTS is_public_race;
--   REVOKE UPDATE (is_public_race) ON public.parties FROM authenticated;

ALTER TABLE public.parties ADD COLUMN IF NOT EXISTS is_public_race BOOLEAN NOT NULL DEFAULT false;
COMMENT ON COLUMN public.parties.is_public_race IS 'The leader has put the party on the race board: its name and progress only.';

GRANT UPDATE (is_public_race) ON public.parties TO authenticated;

CREATE OR REPLACE VIEW public.race_board
WITH (security_invoker = false)
AS
SELECT
    p.name,
    coalesce(m.member_count, 0) AS member_count,
    coalesce(e.total, 0) AS total,
    p.milestone,
    least(coalesce(e.total, 0)::NUMERIC / p.milestone, 1)::REAL AS progress,
    p.completed_at,
    EXISTS (
        SELECT 1 FROM public.party_members me
        WHERE me.party_id = p.id AND me.user_id = (SELECT auth.uid()) AND me.status = 'active'
    ) AS mine
FROM public.parties p
LEFT JOIN (
    SELECT party_id, sum(amount)::BIGINT AS total
    FROM public.darood_entries
    GROUP BY party_id
) e ON e.party_id = p.id
LEFT JOIN (
    SELECT party_id, count(*)::INTEGER AS member_count
    FROM public.party_members
    WHERE status = 'active'
    GROUP BY party_id
) m ON m.party_id = p.id
WHERE p.is_public_race
  AND p.deleted_at IS NULL
  AND p.milestone IS NOT NULL
  AND (SELECT auth.uid()) IS NOT NULL;

REVOKE ALL ON public.race_board FROM public, anon;
REVOKE INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER ON public.race_board FROM authenticated;
GRANT SELECT ON public.race_board TO authenticated;
