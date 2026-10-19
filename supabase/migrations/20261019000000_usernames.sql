-- Migration: 20261019000000_usernames.sql
-- Description: Usernames for Darood Parties (docs/darood-design.md, Usernames).
--   * One per account, 3 to 20 characters of a-z, 0-9 and _, unique, never a reserved word.
--   * Their own table, not a profiles column: every signed-in player can read an opted-in profile
--     row in full (the leaderboard reads them with select *), and usernames must not be listable.
--   * Only the owner reads their row. Nobody writes it directly: set_username (a later migration)
--     is the only way in, so its rules (such as one change every 30 days) can't be skipped.
--   * Deleting the account deletes the row (ON DELETE CASCADE), which frees the name.
-- Re-runnable: everything is IF NOT EXISTS, OR REPLACE, or dropped first.
-- Rollback: DROP TABLE IF EXISTS public.usernames; DROP FUNCTION IF EXISTS public.username_is_reserved(text);

-- Names that could pass for the app or its staff, as in the app (Username.RESERVED). Underscores
-- and trailing digits don't get around it: admin, Admin_1 and pixel_quest are all reserved.
CREATE OR REPLACE FUNCTION public.username_is_reserved(name TEXT)
RETURNS BOOLEAN
LANGUAGE sql
IMMUTABLE
SET search_path = ''
AS $$
    SELECT rtrim(replace(lower(name), '_', ''), '0123456789') = ANY (ARRAY[
        'admin', 'administrator', 'support', 'help', 'pixelquest', 'moderator', 'mod',
        'staff', 'official', 'system', 'root', 'null', 'undefined'
    ]);
$$;

CREATE TABLE IF NOT EXISTS public.usernames (
    user_id UUID PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
    username TEXT NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT username_form CHECK (username ~ '^[A-Za-z0-9_]{3,20}$'),
    CONSTRAINT username_not_reserved CHECK (NOT public.username_is_reserved(username))
);

CREATE UNIQUE INDEX IF NOT EXISTS usernames_username_unique ON public.usernames (username);

ALTER TABLE public.usernames ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS "Read your own username" ON public.usernames;
CREATE POLICY "Read your own username"
    ON public.usernames
    FOR SELECT
    TO authenticated
    USING ((SELECT auth.uid()) = user_id);

-- No insert, update or delete policy, and no grants for them either: clients can't write it.
REVOKE INSERT, UPDATE, DELETE, TRUNCATE ON public.usernames FROM anon, authenticated;
REVOKE ALL ON public.usernames FROM anon;

COMMENT ON TABLE public.usernames IS 'Darood Party usernames: one per account, readable only by its owner, written only by set_username.';
COMMENT ON COLUMN public.usernames.changed_at IS 'When the username was last set; it can change again 30 days later.';
