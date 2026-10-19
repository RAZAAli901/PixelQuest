-- Migration: 20261019000009_entry_amount.sql
-- Description: An entry is a whole number from 1 to 100,000 (docs/darood-design.md, Entries).
--   * The column is an INTEGER, so 300.5, 1e3 or "300 today" never get in: Postgres refuses to
--     read them as one (22P02, HTTP 400).
--   * The check keeps it between 1 and 100,000: no zero, no negatives to take from the total, and
--     no slip of the thumb adding a million (23514, HTTP 400).
-- Re-runnable: the constraint is dropped first.
-- Rollback: ALTER TABLE public.darood_entries DROP CONSTRAINT IF EXISTS darood_entry_amount;

ALTER TABLE public.darood_entries DROP CONSTRAINT IF EXISTS darood_entry_amount;
ALTER TABLE public.darood_entries ADD CONSTRAINT darood_entry_amount CHECK (amount BETWEEN 1 AND 100000);
