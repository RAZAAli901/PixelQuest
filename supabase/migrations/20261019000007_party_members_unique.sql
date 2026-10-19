-- Migration: 20261019000007_party_members_unique.sql
-- Description: One row per person per party, found fast from either side.
--   * UNIQUE (party_id, user_id): inviting someone again updates their row instead of adding one.
--   * An index on user_id, for "my parties" and "my invites".
--   * At most one leader per party (a partial unique index), so handing the leadership over can
--     never leave two.
-- Re-runnable: IF NOT EXISTS, and the constraint is dropped first.
-- Rollback: ALTER TABLE public.party_members DROP CONSTRAINT IF EXISTS party_members_one_row;
--   DROP INDEX IF EXISTS public.party_members_user_id; DROP INDEX IF EXISTS public.party_members_one_leader;

ALTER TABLE public.party_members DROP CONSTRAINT IF EXISTS party_members_one_row;
ALTER TABLE public.party_members ADD CONSTRAINT party_members_one_row UNIQUE (party_id, user_id);

CREATE INDEX IF NOT EXISTS party_members_user_id ON public.party_members (user_id);

CREATE UNIQUE INDEX IF NOT EXISTS party_members_one_leader ON public.party_members (party_id) WHERE role = 'leader';
