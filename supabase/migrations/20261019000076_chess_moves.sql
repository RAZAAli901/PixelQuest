-- Migration: 20261019000076_chess_moves.sql
-- Description: Each move the server took in a chess game (docs/chess-design.md, Moves): its
-- number (ply 1 is White's first), SAN and UCI, the position after it and both clocks after it.
--   * (game_id, ply) is unique, so a move sent twice, or two at once, is stored once.
-- Re-runnable: IF NOT EXISTS.
-- Rollback: DROP TABLE public.chess_moves.

CREATE TABLE IF NOT EXISTS public.chess_moves (
    game_id UUID NOT NULL REFERENCES public.chess_games(id) ON DELETE CASCADE,
    ply INTEGER NOT NULL CHECK (ply >= 1),
    san TEXT NOT NULL CHECK (length(san) BETWEEN 2 AND 10),
    uci TEXT NOT NULL CHECK (uci ~ '^[a-h][1-8][a-h][1-8][qrbn]?$'),
    fen_after TEXT NOT NULL,
    white_ms_after BIGINT NOT NULL CHECK (white_ms_after >= 0),
    black_ms_after BIGINT NOT NULL CHECK (black_ms_after >= 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (game_id, ply)
);

ALTER TABLE public.chess_moves ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chess_moves FROM anon, authenticated;

COMMENT ON TABLE public.chess_moves IS 'The moves of each chess game, in order, as the server took them. Written only by the chess functions.';
