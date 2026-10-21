-- Migration: 20261019000069_darood_week_results.sql
-- Description: A Darood Party's closed weeks (docs/darood-weekly.md, Results): each member's place
-- and total, stored once when the week closes (20261019000070) and never changed.
--   * One row per party, week (its Monday) and member. Read by the party's active members; no
--     client writes them, and nobody updates or deletes them (gone only with the party).
-- Re-runnable: IF NOT EXISTS, DROP POLICY IF EXISTS.
-- Rollback: DROP TABLE public.darood_week_results.

CREATE TABLE IF NOT EXISTS public.darood_week_results (
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    week_start DATE NOT NULL CHECK (EXTRACT(ISODOW FROM week_start) = 1),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    position INTEGER NOT NULL CHECK (position >= 1),
    week_total BIGINT NOT NULL CHECK (week_total >= 0),
    active_days INTEGER NOT NULL DEFAULT 0,
    members INTEGER NOT NULL,
    closed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (party_id, week_start, user_id)
);
CREATE INDEX IF NOT EXISTS darood_week_results_week ON public.darood_week_results (party_id, week_start DESC);

ALTER TABLE public.darood_week_results ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.darood_week_results FROM anon, authenticated;
GRANT SELECT ON public.darood_week_results TO authenticated;
DROP POLICY IF EXISTS "Members read their party's weeks" ON public.darood_week_results;
CREATE POLICY "Members read their party's weeks" ON public.darood_week_results FOR SELECT TO authenticated
    USING (EXISTS (
        SELECT 1 FROM public.party_members m
        WHERE m.party_id = darood_week_results.party_id AND m.user_id = (SELECT auth.uid()) AND m.status = 'active'
    ));
