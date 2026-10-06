-- Migration: 20261006000000_ai_proxy_usage.sql
-- Description: Daily call counts for the gemini-proxy Edge Function, which keeps the Gemini API key
-- on the server. Each install sends a random device id; the proxy claims one call per request and
-- refuses once the device or the whole project reaches its daily limit, which bounds what a leaked
-- anon key or a scripted client can spend.
--
-- Only the service role (the Edge Function) can read or change these rows: RLS is on with no
-- policies, and the claim function is not executable by anon or authenticated users.

CREATE TABLE IF NOT EXISTS public.ai_proxy_usage (
    device_id TEXT NOT NULL,          -- random per-install id, or '*' for the project-wide total
    day DATE NOT NULL,                -- UTC day
    calls INTEGER NOT NULL DEFAULT 0 CHECK (calls >= 0),
    PRIMARY KEY (device_id, day)
);

ALTER TABLE public.ai_proxy_usage ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE public.ai_proxy_usage FROM anon, authenticated;

COMMENT ON TABLE public.ai_proxy_usage IS 'Per-device and project-wide daily Gemini calls made through the gemini-proxy Edge Function.';

-- Claims one call for p_device today. Returns 'ok', 'device_limit' or 'global_limit'.
-- Both counters move in the same transaction, and a refused global claim gives the device's call back.
CREATE OR REPLACE FUNCTION public.claim_ai_proxy_call(
    p_device TEXT,
    p_device_limit INTEGER,
    p_global_limit INTEGER
)
RETURNS TEXT
LANGUAGE plpgsql
SET search_path = public
AS $$
DECLARE
    v_day DATE := (now() AT TIME ZONE 'utc')::date;
    v_calls INTEGER;
BEGIN
    IF p_device IS NULL OR p_device = '*' OR char_length(p_device) > 64 THEN
        RAISE EXCEPTION 'invalid device id';
    END IF;

    INSERT INTO public.ai_proxy_usage AS u (device_id, day, calls)
    VALUES (p_device, v_day, 1)
    ON CONFLICT (device_id, day) DO UPDATE SET calls = u.calls + 1 WHERE u.calls < p_device_limit
    RETURNING u.calls INTO v_calls;
    IF v_calls IS NULL THEN
        RETURN 'device_limit';
    END IF;

    INSERT INTO public.ai_proxy_usage AS u (device_id, day, calls)
    VALUES ('*', v_day, 1)
    ON CONFLICT (device_id, day) DO UPDATE SET calls = u.calls + 1 WHERE u.calls < p_global_limit
    RETURNING u.calls INTO v_calls;
    IF v_calls IS NULL THEN
        UPDATE public.ai_proxy_usage SET calls = calls - 1 WHERE device_id = p_device AND day = v_day;
        RETURN 'global_limit';
    END IF;

    -- The first call of a new day clears counts older than 30 days.
    IF v_calls = 1 THEN
        DELETE FROM public.ai_proxy_usage WHERE day < v_day - 30;
    END IF;

    RETURN 'ok';
END;
$$;

REVOKE ALL ON FUNCTION public.claim_ai_proxy_call(TEXT, INTEGER, INTEGER) FROM PUBLIC, anon, authenticated;
GRANT EXECUTE ON FUNCTION public.claim_ai_proxy_call(TEXT, INTEGER, INTEGER) TO service_role;
