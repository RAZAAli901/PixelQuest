-- Migration: 20261019000098_push_tokens_for.sql
-- Description: send-push's database calls (docs/chess-design.md), for the service role only:
--   * push_tokens_for(users): each of these players' phone tokens;
--   * push_forget_token(token): a token Firebase says is gone.
-- Re-runnable: CREATE OR REPLACE.
-- Rollback: DROP FUNCTION public.push_tokens_for(UUID[]), public.push_forget_token(TEXT).

CREATE OR REPLACE FUNCTION public.push_tokens_for(p_users UUID[])
RETURNS TABLE (user_id UUID, token TEXT)
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT d.user_id, d.token FROM public.device_tokens d WHERE d.user_id = ANY (p_users) ORDER BY d.updated_at DESC;
$$;

CREATE OR REPLACE FUNCTION public.push_forget_token(p_token TEXT)
RETURNS VOID
LANGUAGE sql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
    DELETE FROM public.device_tokens WHERE token = p_token;
$$;

REVOKE ALL ON FUNCTION public.push_tokens_for(UUID[]), public.push_forget_token(TEXT) FROM public, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.push_tokens_for(UUID[]), public.push_forget_token(TEXT) TO service_role;
