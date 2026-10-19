-- Migration: 20261019000005_party_limits.sql
-- Description: The party rules a row must keep (docs/darood-design.md, Parties):
--   * a name of 2 to 40 characters, not counting spaces at either end;
--   * a member limit of 2 to 30 (whoever leads counts);
--   * a milestone above 0 once there is one.
-- A write that breaks one fails with 23514 (HTTP 400).
-- Re-runnable: each constraint is dropped first.
-- Rollback: ALTER TABLE public.parties DROP CONSTRAINT IF EXISTS party_name_length,
--   DROP CONSTRAINT IF EXISTS party_member_limit, DROP CONSTRAINT IF EXISTS party_milestone_positive;

ALTER TABLE public.parties DROP CONSTRAINT IF EXISTS party_name_length;
ALTER TABLE public.parties ADD CONSTRAINT party_name_length
    CHECK (char_length(btrim(name)) BETWEEN 2 AND 40 AND name = btrim(name));

ALTER TABLE public.parties DROP CONSTRAINT IF EXISTS party_member_limit;
ALTER TABLE public.parties ADD CONSTRAINT party_member_limit
    CHECK (max_members BETWEEN 2 AND 30);

ALTER TABLE public.parties DROP CONSTRAINT IF EXISTS party_milestone_positive;
ALTER TABLE public.parties ADD CONSTRAINT party_milestone_positive
    CHECK (milestone IS NULL OR milestone > 0);
