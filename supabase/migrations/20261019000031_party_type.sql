-- Migration: 20261019000031_party_type.sql
-- Description: One party system, two kinds of party (docs/party-core.md).
--   * parties.party_type: what kind of party a row is, darood or study. Not null, darood by default,
--     so every party there is today, and every party the 1.4 apps start, is a Darood Party, as it was.
--   * Adding a not-null column with a default fills it in on every existing row at once.
--   * Nobody may change it: there's no column grant for it, so a party's kind is fixed when it starts.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: ALTER TABLE public.parties DROP COLUMN IF EXISTS party_type;

ALTER TABLE public.parties ADD COLUMN IF NOT EXISTS party_type TEXT NOT NULL DEFAULT 'darood';

COMMENT ON COLUMN public.parties.party_type IS
    'The party''s kind: darood (Darood Party) or study (Study party). Set when it starts; never changes.';
