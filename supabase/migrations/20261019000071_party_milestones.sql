-- Migration: 20261019000071_party_milestones.sql
-- Description: A Darood Party's goals as a list (docs/darood-weekly.md, Goals that go on): goal 1,
-- goal 2 and on, each with its target, who set it and when, and when it was reached.
--   * One row per goal; at most one not yet reached per party. parties.milestone stays the current
--     goal's target, so everything that reads it goes on working.
--   * The party's single milestone moves in as goal 1 (reached when the party finished). A party
--     made with a milestone gets its goal 1 as it's made.
--   * Read by the party's active members; no client writes them (the server's functions do).
-- Re-runnable: IF NOT EXISTS, DROP ... IF EXISTS, and the copy skips parties already in.
-- Rollback: DROP TABLE public.party_milestones; DROP FUNCTION private.party_first_goal().

CREATE TABLE IF NOT EXISTS public.party_milestones (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    sequence INTEGER NOT NULL CHECK (sequence >= 1),
    target BIGINT NOT NULL CHECK (target > 0),
    set_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    set_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    reached_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (party_id, sequence)
);
CREATE UNIQUE INDEX IF NOT EXISTS party_milestones_one_open ON public.party_milestones (party_id) WHERE reached_at IS NULL;

ALTER TABLE public.party_milestones ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.party_milestones FROM anon, authenticated;
GRANT SELECT ON public.party_milestones TO authenticated;
DROP POLICY IF EXISTS "Members read their party's goals" ON public.party_milestones;
CREATE POLICY "Members read their party's goals" ON public.party_milestones FOR SELECT TO authenticated
    USING (EXISTS (
        SELECT 1 FROM public.party_members m
        WHERE m.party_id = party_milestones.party_id AND m.user_id = (SELECT auth.uid()) AND m.status = 'active'
    ));

-- The single milestone, as goal 1.
INSERT INTO public.party_milestones (party_id, sequence, target, set_by, set_at, reached_at)
SELECT p.id, 1, p.milestone, p.leader_id, coalesce(p.milestone_set_at, p.created_at), p.completed_at
FROM public.parties p
WHERE p.party_type = 'darood' AND p.milestone IS NOT NULL
  AND NOT EXISTS (SELECT 1 FROM public.party_milestones g WHERE g.party_id = p.id);

-- A party made with a milestone: its goal 1, set by its leader.
CREATE OR REPLACE FUNCTION private.party_first_goal()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF NEW.milestone IS NOT NULL THEN
        INSERT INTO public.party_milestones (party_id, sequence, target, set_by, set_at)
        VALUES (NEW.id, 1, NEW.milestone, NEW.leader_id, coalesce(NEW.milestone_set_at, now()));
    END IF;
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION private.party_first_goal() FROM public, anon, authenticated;

DROP TRIGGER IF EXISTS party_first_goal ON public.parties;
CREATE TRIGGER party_first_goal
    AFTER INSERT ON public.parties
    FOR EACH ROW
    EXECUTE FUNCTION private.party_first_goal();

COMMENT ON TABLE public.party_milestones IS
    'A Darood Party''s goals, in order: each target, who set it and when, and when the total first reached it.';
