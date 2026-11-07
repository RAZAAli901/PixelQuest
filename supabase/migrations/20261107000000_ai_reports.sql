-- Migration: 20261107000000_ai_reports.sql
-- Description: AI-generated content reporting table and RPC (P020, Play AI Policy)
-- Allows authenticated users to report offensive, harmful, inaccurate, or inappropriate AI Coach answers.
-- Enforces row-level security: only authenticated users can insert reports with reporter_id = auth.uid(),
-- and reads are strictly restricted to service_role (moderation admins).

CREATE TABLE IF NOT EXISTS public.ai_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    insight_content TEXT NOT NULL CHECK (char_length(insight_content) <= 5000),
    reason TEXT NOT NULL CHECK (char_length(reason) <= 500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc'::text, now())
);

-- Enable Row Level Security
ALTER TABLE public.ai_reports ENABLE ROW LEVEL SECURITY;

-- Revoke default public/anon access
REVOKE ALL ON public.ai_reports FROM anon;
REVOKE ALL ON public.ai_reports FROM authenticated;

-- Allow authenticated users to INSERT their own reports
GRANT INSERT ON public.ai_reports TO authenticated;

DROP POLICY IF EXISTS allow_insert_authenticated_ai_reports ON public.ai_reports;
CREATE POLICY allow_insert_authenticated_ai_reports ON public.ai_reports
    FOR INSERT
    TO authenticated
    WITH CHECK ((SELECT auth.uid()) = reporter_id);

-- Performance index for foreign key reporter_id
CREATE INDEX IF NOT EXISTS ai_reports_reporter_id_idx ON public.ai_reports (reporter_id);

COMMENT ON TABLE public.ai_reports IS 'Reports submitted by authenticated users for offensive or inappropriate AI coach responses.';
COMMENT ON COLUMN public.ai_reports.reporter_id IS 'Auth user ID of the reporter submitting the flag.';
COMMENT ON COLUMN public.ai_reports.insight_content IS 'Full content of the flagged AI coach insight.';
COMMENT ON COLUMN public.ai_reports.reason IS 'Reason or category of the report.';
