-- Migration: 20261019000075_chess_games.sql
-- Description: Chess in study parties (docs/chess-design.md): its one config, the time controls,
-- and chess_games, a game as the server referees it.
--   * chess_config: one row of every rule's value (the app's ChessConfig matches it).
--   * chess_time_controls: the allowed ones (60, 180, 300, 600 seconds, no increment yet).
--   * chess_games: the players, the time control, rated or not, its state and result, the
--     position (FEN), whose turn, both clocks as of last_move_at, and when it was made, started
--     and ended. Written by the server's functions only (RLS in 20261019000081).
-- Re-runnable: IF NOT EXISTS, ON CONFLICT DO NOTHING.
-- Rollback: DROP TABLE public.chess_games, public.chess_time_controls, public.chess_config.

CREATE TABLE IF NOT EXISTS public.chess_config (
    id BOOLEAN PRIMARY KEY DEFAULT TRUE CHECK (id),
    start_rating INTEGER NOT NULL DEFAULT 800,
    rating_floor INTEGER NOT NULL DEFAULT 100,
    k_new INTEGER NOT NULL DEFAULT 40,
    k_new_games INTEGER NOT NULL DEFAULT 10,
    k_normal INTEGER NOT NULL DEFAULT 32,
    k_high INTEGER NOT NULL DEFAULT 16,
    k_high_above INTEGER NOT NULL DEFAULT 1600,
    challenge_minutes INTEGER NOT NULL DEFAULT 10,
    abort_seconds INTEGER NOT NULL DEFAULT 30,
    pair_rated_per_day INTEGER NOT NULL DEFAULT 10,
    min_moves_each INTEGER NOT NULL DEFAULT 4,
    challenges_per_hour INTEGER NOT NULL DEFAULT 10,
    moves_per_second INTEGER NOT NULL DEFAULT 5,
    spectator_joins_per_hour INTEGER NOT NULL DEFAULT 20,
    -- Tier names and the Int points each starts at, lowest first.
    tiers JSONB NOT NULL DEFAULT '[{"name":"Pawn","from":0},{"name":"Knight","from":900},{"name":"Bishop","from":1100},{"name":"Rook","from":1300},{"name":"Queen","from":1500},{"name":"King","from":1800}]'
);
INSERT INTO public.chess_config (id) VALUES (TRUE) ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS public.chess_time_controls (
    seconds INTEGER PRIMARY KEY CHECK (seconds > 0),
    increment_seconds INTEGER NOT NULL DEFAULT 0 CHECK (increment_seconds >= 0)
);
INSERT INTO public.chess_time_controls (seconds) VALUES (60), (180), (300), (600) ON CONFLICT DO NOTHING;

CREATE TABLE IF NOT EXISTS public.chess_games (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    white_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    black_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    time_control_sec INTEGER NOT NULL,
    rated BOOLEAN NOT NULL DEFAULT TRUE,
    status TEXT NOT NULL DEFAULT 'waiting',
    result TEXT,
    result_reason TEXT,
    fen TEXT NOT NULL DEFAULT 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1',
    turn TEXT NOT NULL DEFAULT 'w',
    ply INTEGER NOT NULL DEFAULT 0 CHECK (ply >= 0),
    white_ms BIGINT NOT NULL CHECK (white_ms >= 0),
    black_ms BIGINT NOT NULL CHECK (black_ms >= 0),
    last_move_at TIMESTAMP WITH TIME ZONE,
    draw_offered_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE
);
CREATE INDEX IF NOT EXISTS chess_games_party ON public.chess_games (party_id, created_at DESC);
CREATE INDEX IF NOT EXISTS chess_games_white ON public.chess_games (white_id) WHERE status IN ('waiting', 'active');
CREATE INDEX IF NOT EXISTS chess_games_black ON public.chess_games (black_id) WHERE status IN ('waiting', 'active');

-- Closed until their policies (20261019000081): the config and time controls are read by everyone signed in.
ALTER TABLE public.chess_config ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chess_time_controls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.chess_games ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chess_config, public.chess_time_controls, public.chess_games FROM anon, authenticated;

COMMENT ON TABLE public.chess_games IS
    'Chess games in study parties, refereed by the server: position, turn, clocks and result. Written only by the chess functions.';
