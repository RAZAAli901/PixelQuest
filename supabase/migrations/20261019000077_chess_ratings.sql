-- Migration: 20261019000077_chess_ratings.sql
-- Description: Int points (docs/chess-design.md, Ratings and records): one row per player per time
-- control, with their games, wins, draws, losses and peak. A player without a row is at the
-- config's start rating. Written only by private.finish_game (20261019000085).
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE public.chess_ratings.

CREATE TABLE IF NOT EXISTS public.chess_ratings (
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    time_control_sec INTEGER NOT NULL REFERENCES public.chess_time_controls(seconds),
    int_points INTEGER NOT NULL CHECK (int_points >= 0),
    games INTEGER NOT NULL DEFAULT 0 CHECK (games >= 0),
    wins INTEGER NOT NULL DEFAULT 0 CHECK (wins >= 0),
    draws INTEGER NOT NULL DEFAULT 0 CHECK (draws >= 0),
    losses INTEGER NOT NULL DEFAULT 0 CHECK (losses >= 0),
    peak INTEGER NOT NULL CHECK (peak >= int_points),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, time_control_sec),
    CHECK (games = wins + draws + losses)
);

ALTER TABLE public.chess_ratings ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chess_ratings FROM anon, authenticated;

COMMENT ON TABLE public.chess_ratings IS 'Each player''s Int points per chess time control, with their record. Written only by the server.';
