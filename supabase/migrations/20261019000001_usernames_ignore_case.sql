-- Migration: 20261019000001_usernames_ignore_case.sql
-- Description: Usernames are unique ignoring case, and stored in lowercase, as the app shows them.
--   * Every write is lowercased first (a trigger), so Aria_K is stored as aria_k.
--   * The unique index is on lower(username), so ARIA_K can't sit beside aria_k even if a later
--     change let capitals through. It replaces the plain index from 20261019000000_usernames.sql.
--   * The form check now allows lowercase only: what the trigger leaves.
-- Re-runnable: OR REPLACE, IF EXISTS / IF NOT EXISTS throughout.
-- Rollback: DROP TRIGGER IF EXISTS usernames_lowercase ON public.usernames;
--   DROP FUNCTION IF EXISTS public.usernames_lowercase(); DROP INDEX IF EXISTS public.usernames_username_lower_unique;
--   CREATE UNIQUE INDEX usernames_username_unique ON public.usernames (username); and the old form check.

CREATE OR REPLACE FUNCTION public.usernames_lowercase()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
BEGIN
    NEW.username := lower(NEW.username);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS usernames_lowercase ON public.usernames;
CREATE TRIGGER usernames_lowercase
    BEFORE INSERT OR UPDATE OF username ON public.usernames
    FOR EACH ROW
    EXECUTE FUNCTION public.usernames_lowercase();

UPDATE public.usernames SET username = lower(username) WHERE username <> lower(username);

CREATE UNIQUE INDEX IF NOT EXISTS usernames_username_lower_unique ON public.usernames (lower(username));
DROP INDEX IF EXISTS public.usernames_username_unique;

ALTER TABLE public.usernames DROP CONSTRAINT IF EXISTS username_form;
ALTER TABLE public.usernames ADD CONSTRAINT username_form CHECK (username ~ '^[a-z0-9_]{3,20}$');
