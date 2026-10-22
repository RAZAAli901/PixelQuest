-- Migration: 20261019000112_reports_grants.sql
-- Description: S012 of the security plan: reports are insert-only for players, and nobody but the
-- service role (staff, from the dashboard) reads them. The only policy was already "insert in your
-- own name", but the table grants gave anon and authenticated every privilege (RLS returned no
-- rows). Now: anon nothing, authenticated INSERT only. chat_reports and chess_reports were already
-- closed (written only through their SECURITY DEFINER functions).
-- Re-runnable: REVOKE and GRANT are idempotent.
-- Rollback: GRANT ALL ON public.reports TO anon, authenticated;

REVOKE ALL ON public.reports FROM anon;
REVOKE ALL ON public.reports FROM authenticated;
GRANT INSERT ON public.reports TO authenticated;
