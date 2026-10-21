-- Migration: 20261019000097_device_tokens.sql
-- Description: Where a player's push notifications go (docs/chess-design.md, Threats): each of
-- their phones' Firebase Cloud Messaging token, so a challenge reaches a closed app.
--   * Only the owner writes, reads or deletes their own rows; send-push reads them with the
--     service role. A token belongs to one account at a time: registering it moves it.
--   * register_device_token(token, platform) and forget_device_token(token) are the app's calls.
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE, DROP POLICY IF EXISTS.
-- Rollback: DROP TABLE public.device_tokens; DROP FUNCTION public.register_device_token(TEXT, TEXT), public.forget_device_token(TEXT).

CREATE TABLE IF NOT EXISTS public.device_tokens (
    token TEXT PRIMARY KEY CHECK (length(token) BETWEEN 20 AND 4096 AND token ~ '^[A-Za-z0-9_:.-]+$'),
    user_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    platform TEXT NOT NULL DEFAULT 'android' CHECK (platform IN ('android')),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS device_tokens_user ON public.device_tokens (user_id);

ALTER TABLE public.device_tokens ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.device_tokens FROM anon, authenticated;
GRANT SELECT, DELETE ON public.device_tokens TO authenticated;
DROP POLICY IF EXISTS "Owners read their tokens" ON public.device_tokens;
CREATE POLICY "Owners read their tokens" ON public.device_tokens FOR SELECT TO authenticated
    USING (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS "Owners delete their tokens" ON public.device_tokens;
CREATE POLICY "Owners delete their tokens" ON public.device_tokens FOR DELETE TO authenticated
    USING (user_id = (SELECT auth.uid()));

CREATE OR REPLACE FUNCTION public.register_device_token(token TEXT, platform TEXT DEFAULT 'android')
RETURNS VOID
LANGUAGE plpgsql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_variable
DECLARE
    caller UUID := (SELECT auth.uid());
BEGIN
    IF caller IS NULL THEN
        RAISE EXCEPTION 'Not authenticated' USING ERRCODE = 'insufficient_privilege';
    END IF;
    -- At most 10 phones an account: the oldest goes.
    DELETE FROM public.device_tokens d
    WHERE d.user_id = caller AND d.token <> token AND d.token IN (
        SELECT o.token FROM public.device_tokens o WHERE o.user_id = caller ORDER BY o.updated_at DESC OFFSET 9
    );
    INSERT INTO public.device_tokens AS d (token, user_id, platform) VALUES (token, caller, coalesce(platform, 'android'))
    ON CONFLICT ON CONSTRAINT device_tokens_pkey DO UPDATE SET user_id = caller, platform = excluded.platform, updated_at = now();
END;
$$;

CREATE OR REPLACE FUNCTION public.forget_device_token(token TEXT)
RETURNS VOID
LANGUAGE sql
VOLATILE
SECURITY DEFINER
SET search_path = ''
AS $$
    DELETE FROM public.device_tokens d WHERE d.token = forget_device_token.token AND d.user_id = (SELECT auth.uid());
$$;

REVOKE ALL ON FUNCTION public.register_device_token(TEXT, TEXT), public.forget_device_token(TEXT) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.register_device_token(TEXT, TEXT), public.forget_device_token(TEXT) TO authenticated;

COMMENT ON TABLE public.device_tokens IS 'Each phone''s push token, for its owner''s chess notifications. Written only by its owner.';
