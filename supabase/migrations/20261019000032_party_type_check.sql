-- Migration: 20261019000032_party_type_check.sql
-- Description: Every party is a darood or a study party, and nothing else (docs/party-core.md).
--   * Backfill: any row whose kind isn't one of the two is a Darood Party, as every party before
--     party_type was. (The column's default already filled the rows there were; this makes sure.)
--   * party_type_known: the kind is darood or study. A new kind needs a migration of its own.
-- Re-runnable: the update changes nothing a second time; DROP CONSTRAINT IF EXISTS first.
-- Rollback: ALTER TABLE public.parties DROP CONSTRAINT IF EXISTS party_type_known;

UPDATE public.parties SET party_type = 'darood' WHERE party_type IS NULL OR party_type NOT IN ('darood', 'study');

ALTER TABLE public.parties DROP CONSTRAINT IF EXISTS party_type_known;
ALTER TABLE public.parties ADD CONSTRAINT party_type_known CHECK (party_type IN ('darood', 'study'));
