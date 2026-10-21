-- Migration: 20261019000079_chess_challenges.sql
-- Description: Chess challenges in a study party (docs/chess-design.md, Game states): from one
-- member to another, or open to the room (to_user null), with the time control, the colour asked
-- for, rated or not, its state and when it expires (10 minutes). The game it became, once accepted.
-- Written only by the chess functions.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE public.chess_challenges.

CREATE TABLE IF NOT EXISTS public.chess_challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    from_user UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    to_user UUID REFERENCES auth.users(id) ON DELETE CASCADE,
    time_control_sec INTEGER NOT NULL,
    colour TEXT NOT NULL DEFAULT 'random',
    rated BOOLEAN NOT NULL DEFAULT TRUE,
    status TEXT NOT NULL DEFAULT 'open',
    game_id UUID REFERENCES public.chess_games(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    answered_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS chess_challenges_party ON public.chess_challenges (party_id, created_at DESC);
CREATE INDEX IF NOT EXISTS chess_challenges_from ON public.chess_challenges (from_user, created_at DESC);

ALTER TABLE public.chess_challenges ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chess_challenges FROM anon, authenticated;

COMMENT ON TABLE public.chess_challenges IS 'Chess challenges in study parties, to a member or open to the room. Written only by the chess functions.';
