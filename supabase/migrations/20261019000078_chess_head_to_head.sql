-- Migration: 20261019000078_chess_head_to_head.sql
-- Description: Two players' record against each other (docs/chess-design.md, Ratings and
-- records): one row per pair per time control, the pair ordered (user_low < user_high) so each
-- has one row. Written only by private.finish_game (20261019000085).
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE public.chess_head_to_head.

CREATE TABLE IF NOT EXISTS public.chess_head_to_head (
    user_low UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    user_high UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    time_control_sec INTEGER NOT NULL REFERENCES public.chess_time_controls(seconds),
    low_wins INTEGER NOT NULL DEFAULT 0 CHECK (low_wins >= 0),
    high_wins INTEGER NOT NULL DEFAULT 0 CHECK (high_wins >= 0),
    draws INTEGER NOT NULL DEFAULT 0 CHECK (draws >= 0),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (user_low, user_high, time_control_sec),
    CHECK (user_low < user_high)
);

ALTER TABLE public.chess_head_to_head ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chess_head_to_head FROM anon, authenticated;

COMMENT ON TABLE public.chess_head_to_head IS 'Each pair of chess players'' wins and draws against each other, per time control. Written only by the server.';
