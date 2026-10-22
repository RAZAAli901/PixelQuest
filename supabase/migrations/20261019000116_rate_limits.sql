-- Migration: 20261019000116_rate_limits.sql
-- Description: S020 of the security plan: one rate-limit table and helper for actions that had no
-- limit of their own. private.rate_limit(bucket, max, per) counts the caller's events in a bucket
-- over the window and raises 'rate_limited' (check_violation) past the limit, then logs the event;
-- events older than a day are pruned as new ones come.
-- Actions with their own limits keep them (docs/security/VERIFY.md, S020): invitations 20 an hour,
-- messages 30 a minute, uploads 20 a day, challenges 10 an hour, moves 5 a second, watching 20 an
-- hour, profile reports one a day, the AI per account and day.
-- New here: message reports and challenge reports 30 an hour each, the cloud export 10 an hour (it's
-- a heavy query), push-token registration 30 an hour. The username check gets one in S021.
-- The four functions are re-created as they were, with the limit added after BEGIN.
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE.
-- Rollback: re-run each function's earlier migration (066, 107, 108, 097); DROP FUNCTION private.rate_limit; DROP TABLE private.rate_events.

CREATE TABLE IF NOT EXISTS private.rate_events (
    id BIGSERIAL PRIMARY KEY,
    bucket TEXT NOT NULL CHECK (char_length(bucket) <= 40),
    subject UUID NOT NULL,
    at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS rate_events_lookup ON private.rate_events (bucket, subject, at);
REVOKE ALL ON private.rate_events FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION private.rate_limit(bucket TEXT, max_events INTEGER, per INTERVAL)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    me UUID := (SELECT auth.uid());
BEGIN
    IF me IS NULL THEN
        RETURN; -- signed out: the function itself refuses
    END IF;
    PERFORM pg_advisory_xact_lock(hashtext('rate:' || rate_limit.bucket || ':' || me::TEXT));
    IF (SELECT count(*) FROM private.rate_events e
        WHERE e.bucket = rate_limit.bucket AND e.subject = me AND e.at > now() - rate_limit.per) >= rate_limit.max_events THEN
        RAISE EXCEPTION 'rate_limited' USING ERRCODE = 'check_violation';
    END IF;
    DELETE FROM private.rate_events e WHERE e.bucket = rate_limit.bucket AND e.subject = me AND e.at < now() - INTERVAL '1 day';
    INSERT INTO private.rate_events (bucket, subject) VALUES (rate_limit.bucket, me);
END;
$$;
REVOKE ALL ON FUNCTION private.rate_limit(TEXT, INTEGER, INTERVAL) FROM PUBLIC, anon, authenticated;

CREATE OR REPLACE FUNCTION public.report_chat_message(message_id uuid, reason text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
#variable_conflict use_column
DECLARE
    me UUID := (SELECT auth.uid());
    m public.chat_messages%ROWTYPE;
BEGIN
    PERFORM private.rate_limit('report_chat', 30, interval '1 hour');
    SELECT * INTO m FROM public.chat_messages WHERE id = report_chat_message.message_id;
    IF NOT FOUND OR me IS NULL OR m.sender_id = me OR NOT private.is_active_study_member(m.party_id) THEN
        RAISE EXCEPTION 'not_allowed' USING ERRCODE = 'insufficient_privilege';
    END IF;
    INSERT INTO public.chat_reports (reporter_id, message_id, party_id, reason)
    VALUES (me, m.id, m.party_id, report_chat_message.reason)
    ON CONFLICT (reporter_id, message_id) DO NOTHING;
END;
$function$;

CREATE OR REPLACE FUNCTION public.report_chess_challenge(challenge_id uuid, reason text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
#variable_conflict use_column
DECLARE
    me UUID := (SELECT auth.uid());
    c public.chess_challenges%ROWTYPE;
BEGIN
    PERFORM private.rate_limit('report_chess', 30, interval '1 hour');
    SELECT * INTO c FROM public.chess_challenges WHERE id = report_chess_challenge.challenge_id;
    IF NOT FOUND OR me IS NULL OR c.from_user = me OR NOT private.chess_is_study_member(c.party_id, me) THEN
        RAISE EXCEPTION 'not_allowed' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF report_chess_challenge.reason IS NULL OR report_chess_challenge.reason NOT IN ('spam', 'abuse', 'other') THEN
        RAISE EXCEPTION 'bad_reason' USING ERRCODE = 'invalid_parameter_value';
    END IF;
    INSERT INTO public.chess_reports (reporter_id, reported_id, challenge_id, party_id, reason)
    VALUES (me, c.from_user, c.id, c.party_id, report_chess_challenge.reason)
    ON CONFLICT (reporter_id, challenge_id) DO NOTHING;
END;
$function$;

CREATE OR REPLACE FUNCTION public.export_my_data()
 RETURNS jsonb
 LANGUAGE plpgsql
 VOLATILE SECURITY DEFINER
 SET search_path TO ''
AS $function$
DECLARE
    me UUID := (SELECT auth.uid());
BEGIN
    PERFORM private.rate_limit('export_my_data', 10, interval '1 hour');
    IF me IS NULL THEN
        RAISE EXCEPTION 'not_signed_in' USING ERRCODE = 'insufficient_privilege';
    END IF;
    RETURN jsonb_build_object(
        'format', 1,
        'exported_at', now(),
        'user_id', me,
        'profile', (SELECT to_jsonb(p) FROM public.profiles p WHERE p.id = me),
        'username', (SELECT to_jsonb(u) - 'user_id' FROM public.usernames u WHERE u.user_id = me),
        'parties', coalesce((
            SELECT jsonb_agg(jsonb_build_object(
                'party_id', p.id, 'name', p.name, 'type', p.party_type, 'leader', p.leader_id = me,
                'role', m.role, 'status', m.status, 'invited_at', m.invited_at, 'joined_at', m.joined_at
            ) ORDER BY m.invited_at)
            FROM public.party_members m JOIN public.parties p ON p.id = m.party_id
            WHERE m.user_id = me), '[]'::JSONB),
        'darood_entries', coalesce((
            SELECT jsonb_agg(to_jsonb(e) - 'user_id' ORDER BY e.created_at) FROM public.darood_entries e WHERE e.user_id = me), '[]'::JSONB),
        'darood_weeks', coalesce((
            SELECT jsonb_agg(to_jsonb(w) - 'user_id' ORDER BY w.week_start) FROM public.darood_week_results w WHERE w.user_id = me), '[]'::JSONB),
        'study_completions', coalesce((
            SELECT jsonb_agg(to_jsonb(c) - 'user_id' ORDER BY c.completed_at) FROM public.study_completions c WHERE c.user_id = me), '[]'::JSONB),
        'study_sessions', coalesce((
            SELECT jsonb_agg(to_jsonb(s) - 'user_id' ORDER BY s.created_at) FROM public.study_sessions s WHERE s.user_id = me), '[]'::JSONB),
        'study_week_wins', coalesce((
            SELECT jsonb_agg(to_jsonb(w) - 'user_id' ORDER BY w.week_start) FROM public.study_week_winners w WHERE w.user_id = me), '[]'::JSONB),
        'study_items_created', coalesce((
            SELECT jsonb_agg(to_jsonb(i) - 'created_by' ORDER BY i.created_at) FROM public.study_items i WHERE i.created_by = me), '[]'::JSONB),
        'study_reminders_created', coalesce((
            SELECT jsonb_agg(to_jsonb(r) - 'created_by' ORDER BY r.created_at) FROM public.study_reminders r WHERE r.created_by = me), '[]'::JSONB),
        'chat_messages_sent', coalesce((
            SELECT jsonb_agg(jsonb_build_object(
                'id', m.id, 'party_id', m.party_id, 'kind', m.kind, 'body', m.body, 'file_id', m.file_id,
                'reply_to', m.reply_to, 'created_at', m.created_at
            ) ORDER BY m.created_at)
            FROM public.chat_messages m WHERE m.sender_id = me AND m.deleted_at IS NULL), '[]'::JSONB),
        'chat_files_shared', coalesce((
            SELECT jsonb_agg(jsonb_build_object(
                'id', f.id, 'party_id', f.party_id, 'name', f.display_name, 'mime', f.mime, 'size_bytes', f.size_bytes,
                'status', f.status, 'created_at', f.created_at, 'deleted_at', f.deleted_at
            ) ORDER BY f.created_at)
            FROM public.chat_files f WHERE f.uploader_id = me), '[]'::JSONB),
        'reports_made', jsonb_build_object(
            'chat', coalesce((SELECT jsonb_agg(to_jsonb(r) - 'reporter_id' ORDER BY r.created_at) FROM public.chat_reports r WHERE r.reporter_id = me), '[]'::JSONB),
            'chess', coalesce((SELECT jsonb_agg(to_jsonb(r) - 'reporter_id' ORDER BY r.created_at) FROM public.chess_reports r WHERE r.reporter_id = me), '[]'::JSONB),
            'profiles', coalesce((SELECT jsonb_agg(to_jsonb(r) - 'reporter_id' ORDER BY r.created_at) FROM public.reports r WHERE r.reporter_id = me), '[]'::JSONB)
        ),
        'blocked', coalesce((
            SELECT jsonb_agg(jsonb_build_object('user_id', b.blocked_id, 'since', b.created_at) ORDER BY b.created_at)
            FROM public.member_blocks b WHERE b.blocker_id = me), '[]'::JSONB),
        'chess_games', coalesce((
            SELECT jsonb_agg(to_jsonb(g) || jsonb_build_object('moves', coalesce((
                SELECT jsonb_agg(jsonb_build_object('ply', mv.ply, 'san', mv.san, 'uci', mv.uci, 'at', mv.created_at) ORDER BY mv.ply)
                FROM public.chess_moves mv WHERE mv.game_id = g.id), '[]'::JSONB)) ORDER BY g.created_at)
            FROM public.chess_games g WHERE me IN (g.white_id, g.black_id)), '[]'::JSONB),
        'chess_ratings', coalesce((
            SELECT jsonb_agg(to_jsonb(r) - 'user_id' ORDER BY r.time_control_sec) FROM public.chess_ratings r WHERE r.user_id = me), '[]'::JSONB),
        'chess_challenges', coalesce((
            SELECT jsonb_agg(to_jsonb(c) ORDER BY c.created_at) FROM public.chess_challenges c WHERE me IN (c.from_user, c.to_user)), '[]'::JSONB),
        'chess_records', coalesce((
            SELECT jsonb_agg(to_jsonb(h) ORDER BY h.updated_at) FROM public.chess_head_to_head h WHERE me IN (h.user_low, h.user_high)), '[]'::JSONB),
        'push_phones', (SELECT count(*) FROM public.device_tokens t WHERE t.user_id = me)
    );
END;
$function$;

CREATE OR REPLACE FUNCTION public.register_device_token(token text, platform text DEFAULT 'android'::text)
 RETURNS void
 LANGUAGE plpgsql
 SECURITY DEFINER
 SET search_path TO ''
AS $function$
#variable_conflict use_variable
DECLARE
    caller UUID := (SELECT auth.uid());
BEGIN
    PERFORM private.rate_limit('device_token', 30, interval '1 hour');
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
$function$;
