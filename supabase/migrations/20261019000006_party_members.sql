-- Migration: 20261019000006_party_members.sql
-- Description: Who is in which party, and how (docs/darood-design.md, Parties).
--   * role: 'leader' (one per party, as parties.leader_id) or 'member'.
--   * status: 'invited' (waiting for an answer), 'active', 'declined', 'left' or 'removed'.
--     A row is kept after someone declines, leaves or is removed, so the party's history (and
--     their past entries' place in the total) stays clear.
--   * invited_by, invited_at: who asked them, and when. joined_at: when they became active.
--   * Deleting an account deletes its rows here; its entries stay in the totals (see darood_entries).
-- Clients never write this table directly: the party functions (later migrations) do.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE IF EXISTS public.party_members CASCADE;

CREATE TABLE IF NOT EXISTS public.party_members (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    role TEXT NOT NULL DEFAULT 'member',
    status TEXT NOT NULL DEFAULT 'invited',
    invited_by UUID REFERENCES auth.users(id) ON DELETE SET NULL,
    invited_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    joined_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT party_member_role CHECK (role IN ('leader', 'member')),
    CONSTRAINT party_member_status CHECK (status IN ('invited', 'active', 'declined', 'left', 'removed')),
    CONSTRAINT party_leader_is_active CHECK (role <> 'leader' OR status = 'active')
);

-- Closed to clients until its policies come, as parties is.
ALTER TABLE public.party_members ENABLE ROW LEVEL SECURITY;

COMMENT ON TABLE public.party_members IS 'Darood Party memberships: invited, active, declined, left or removed. Written only by the party functions.';
