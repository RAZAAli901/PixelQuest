-- Migration: 20260908000000_create_profiles_table.sql
-- Description: Creates the public.profiles table for PixelQuest cloud synchronization

CREATE TABLE IF NOT EXISTS public.profiles (
    id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    display_name TEXT NOT NULL DEFAULT '',
    current_streak INTEGER NOT NULL DEFAULT 0 CHECK (current_streak >= 0),
    longest_streak INTEGER NOT NULL DEFAULT 0 CHECK (longest_streak >= 0),
    level INTEGER NOT NULL DEFAULT 1 CHECK (level >= 1),
    total_xp INTEGER NOT NULL DEFAULT 0 CHECK (total_xp >= 0),
    leaderboard_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT timezone('utc'::text, now()),
    CONSTRAINT display_name_length CHECK (char_length(display_name) <= 20)
);

-- Auto-update updated_at timestamp on row update
CREATE OR REPLACE FUNCTION public.handle_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = timezone('utc'::text, now());
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS set_profiles_updated_at ON public.profiles;
CREATE TRIGGER set_profiles_updated_at
    BEFORE UPDATE ON public.profiles
    FOR EACH ROW
    EXECUTE FUNCTION public.handle_updated_at();

-- Documentation comments
COMMENT ON TABLE public.profiles IS 'PixelQuest player profiles for leaderboard and cloud synchronization.';
COMMENT ON COLUMN public.profiles.leaderboard_opt_in IS 'Controls visibility on the public leaderboard. Defaults strictly to false for privacy.';
COMMENT ON COLUMN public.profiles.display_name IS 'Public pseudonymous display name, completely decoupled from user email or local name.';
