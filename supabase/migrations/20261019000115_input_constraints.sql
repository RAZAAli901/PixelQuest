-- Migration: 20261019000115_input_constraints.sql
-- Description: S019 of the security plan: every column a phone or a request can fill has a length,
-- range or format limit. Most already had one (names, bodies, amounts, minutes, offsets); the
-- audit (docs/security/VERIFY.md, S019) found these without:
--   * time zones the phone sends (darood_entries.tz, study_completions.tz, study_sessions.tz,
--     parties.week_tz): an IANA name or a UTC offset, at most 64 characters;
--   * ai_proxy_usage.device_id, the counter's key the proxy takes from the request: at most 128;
--   * chat_files.sha256, set by finalize-upload: 64 hex digits, or empty until it's set;
--   * chess positions (chess_games.fen, chess_moves.fen_after), written by the referee: at most 100.
-- Server-computed columns (weekly places, points, chess config) need none.
-- New rows are checked at once (NOT VALID); existing rows are then validated, and if an old row
-- doesn't fit, the constraint stays NOT VALID (still enforced for new rows) with a notice, so this
-- can't fail on hosted data.
-- Re-runnable: DROP CONSTRAINT IF EXISTS before each ADD.
-- Rollback: ALTER TABLE ... DROP CONSTRAINT for each name below.

CREATE OR REPLACE FUNCTION private.add_check(tbl TEXT, name TEXT, expr TEXT)
RETURNS VOID LANGUAGE plpgsql SET search_path = '' AS $$
BEGIN
    EXECUTE format('ALTER TABLE public.%I DROP CONSTRAINT IF EXISTS %I', tbl, name);
    EXECUTE format('ALTER TABLE public.%I ADD CONSTRAINT %I CHECK (%s) NOT VALID', tbl, name, expr);
    BEGIN
        EXECUTE format('ALTER TABLE public.%I VALIDATE CONSTRAINT %I', tbl, name);
    EXCEPTION WHEN check_violation THEN
        RAISE NOTICE '% on %: an existing row does not fit; enforced for new rows only', name, tbl;
    END;
END;
$$;
REVOKE ALL ON FUNCTION private.add_check(TEXT, TEXT, TEXT) FROM PUBLIC, anon, authenticated;

SELECT private.add_check('darood_entries', 'darood_entries_tz_form', $c$tz IS NULL OR (char_length(tz) <= 64 AND tz ~ '^[A-Za-z0-9_+:/-]+$')$c$);
SELECT private.add_check('study_completions', 'study_completions_tz_form', $c$tz IS NULL OR (char_length(tz) <= 64 AND tz ~ '^[A-Za-z0-9_+:/-]+$')$c$);
SELECT private.add_check('study_sessions', 'study_sessions_tz_form', $c$tz IS NULL OR (char_length(tz) <= 64 AND tz ~ '^[A-Za-z0-9_+:/-]+$')$c$);
SELECT private.add_check('parties', 'parties_week_tz_form', $c$week_tz IS NULL OR (char_length(week_tz) <= 64 AND week_tz ~ '^[A-Za-z0-9_+:/-]+$')$c$);
SELECT private.add_check('ai_proxy_usage', 'ai_proxy_usage_device_id_length', $c$char_length(device_id) <= 128$c$);
SELECT private.add_check('chat_files', 'chat_files_sha256_form', $c$sha256 IS NULL OR sha256 = '' OR sha256 ~ '^[0-9a-f]{64}$'$c$);
SELECT private.add_check('chess_games', 'chess_games_fen_length', $c$char_length(fen) <= 100$c$);
SELECT private.add_check('chess_moves', 'chess_moves_fen_length', $c$char_length(fen_after) <= 100$c$);
