-- Migration: 20261019000004_parties.sql
-- Description: Darood Parties (docs/darood-design.md, Parties): one row per party.
--   * leader_id is whoever leads it now. If that account is deleted the column empties, and
--     leadership passes on (20261019000019_membership_functions.sql).
--   * milestone is the target total the leader sets: none at first, and it may be set later.
--     milestone_set_at is when it was last set; completed_at is when the total first reached it,
--     kept for good.
--   * deleted_at hides a party the leader deleted. The row stays, so nothing else needs to change.
--   * Totals can pass 2^31 over time (30 members x 500,000 a day), so the milestone is a BIGINT.
-- The rules (sizes, limits, who may do what) and the policies come in the next migrations.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE IF EXISTS public.parties CASCADE;

CREATE TABLE IF NOT EXISTS public.parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name TEXT NOT NULL,
    leader_id UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    max_members INTEGER NOT NULL DEFAULT 10,
    milestone BIGINT,
    milestone_set_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS parties_leader_id ON public.parties (leader_id);

-- Closed to clients from the start: with row-level security on and no policy yet, nobody reads or
-- writes it, so deploying only part of this series can't leave it open.
ALTER TABLE public.parties ENABLE ROW LEVEL SECURITY;

COMMENT ON TABLE public.parties IS 'Darood Parties: a name, a leader, a member limit and the milestone the members race to.';
COMMENT ON COLUMN public.parties.completed_at IS 'When the total first reached the milestone. Set once by the server and never changed.';
COMMENT ON COLUMN public.parties.deleted_at IS 'When the leader deleted the party; a deleted party is hidden from everyone.';
