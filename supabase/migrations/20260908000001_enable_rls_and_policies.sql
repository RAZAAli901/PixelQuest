-- Migration: 20260908000001_enable_rls_and_policies.sql
-- Description: Enables Row Level Security (RLS) on public.profiles and configures access policies

-- Enable Row Level Security
ALTER TABLE public.profiles ENABLE ROW LEVEL SECURITY;

-- Policy 1: Authenticated users can read profiles that have explicitly opted in to the leaderboard
CREATE POLICY "Public read opted in profiles"
    ON public.profiles
    FOR SELECT
    TO authenticated
    USING (leaderboard_opt_in = TRUE);

-- Policy 2: Users can always read their own profile (regardless of opt-in state)
CREATE POLICY "Users can read own profile"
    ON public.profiles
    FOR SELECT
    TO authenticated
    USING (auth.uid() = id);

-- Policy 3: Users can insert their own profile matching their authenticated UID
CREATE POLICY "Users can insert own profile"
    ON public.profiles
    FOR INSERT
    TO authenticated
    WITH CHECK (auth.uid() = id);

-- Policy 4: Users can update their own profile matching their authenticated UID
CREATE POLICY "Users can update own profile"
    ON public.profiles
    FOR UPDATE
    TO authenticated
    USING (auth.uid() = id)
    WITH CHECK (auth.uid() = id);

-- Policy 5: Users can delete their own profile matching their authenticated UID
CREATE POLICY "Users can delete own profile"
    ON public.profiles
    FOR DELETE
    TO authenticated
    USING (auth.uid() = id);
