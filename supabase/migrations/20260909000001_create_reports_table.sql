-- Migration: 20260909000001_create_reports_table.sql
-- Description: Creates the public.reports table with insert-only RLS for reporting offensive profiles

CREATE TABLE IF NOT EXISTS public.reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    reported_profile_id UUID NOT NULL REFERENCES public.profiles(id) ON DELETE CASCADE,
    reason TEXT NOT NULL CHECK (char_length(reason) <= 500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Enable Row Level Security
ALTER TABLE public.reports ENABLE ROW LEVEL SECURITY;

-- Policy: Authenticated users can insert reports where reporter_id matches their own auth UID
DROP POLICY IF EXISTS allow_insert_authenticated_reports ON public.reports;
CREATE POLICY allow_insert_authenticated_reports ON public.reports
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = reporter_id);

-- Explicitly ensure no SELECT, UPDATE, or DELETE policies exist for public/authenticated users.
-- Reads and resolution are strictly confined to internal service role / admin dashboard.
COMMENT ON TABLE public.reports IS 'Moderation reports submitted by authenticated users for abusive or offensive player display names.';
COMMENT ON COLUMN public.reports.reporter_id IS 'Auth user ID of the reporter submitting the flag.';
COMMENT ON COLUMN public.reports.reported_profile_id IS 'Target profile ID being reported.';
