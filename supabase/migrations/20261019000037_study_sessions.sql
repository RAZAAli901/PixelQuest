-- Migration: 20261019000037_study_sessions.sql
-- Description: Study time a member logged in a study party (docs/study-party.md, Study time).
--   * One row per session: 5 to 240 minutes (study_session_minutes), on the member's own day and
--     time zone (local_date, tz), which decide the week it counts in.
--   * client_id is the phone's own key: a session sent again is the same row, never a second.
--   * Final once logged, as a Darood entry is: there's no edit; the daily cap on its points keeps
--     a mistake small.
--   * Deleted with the party or the account.
--   * Row-level security on, with no policy yet: nobody reads or writes a row until 20261019000039.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE IF EXISTS public.study_sessions;

CREATE TABLE IF NOT EXISTS public.study_sessions (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    user_id UUID NOT NULL DEFAULT auth.uid() REFERENCES auth.users(id) ON DELETE CASCADE,
    minutes INTEGER NOT NULL,
    local_date DATE NOT NULL,
    tz TEXT NOT NULL,
    client_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT study_session_minutes CHECK (minutes BETWEEN 5 AND 240)
);

CREATE UNIQUE INDEX IF NOT EXISTS study_sessions_client_id ON public.study_sessions (user_id, client_id) WHERE client_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS study_sessions_party ON public.study_sessions (party_id, user_id, local_date);

ALTER TABLE public.study_sessions ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.study_sessions FROM anon;

COMMENT ON TABLE public.study_sessions IS 'Study time a member logged in a study party: 5 to 240 minutes a session.';
