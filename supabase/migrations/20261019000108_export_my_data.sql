-- Migration: 20261019000108_export_my_data.sql
-- Description: The privacy review (J173, docs/PRIVACY_REVIEW.md):
--   * export_my_data(): everything the cloud keeps about the signed-in player, as one JSON
--     document, for Settings > Export my cloud data. Their profile and username; their parties
--     and memberships; their Darood entries and weekly places; their study completions, sessions,
--     reminders and items; the chat messages they sent and the files they shared (name, type,
--     size and dates, not the bytes, which they can open in the app); the reports and blocks they
--     made; their chess games with every move, ratings, challenges and records; and how many
--     phones are registered for push (not the tokens). Other people's rows aren't included, except
--     as the other side of the player's own games, records and parties (by id).
--   * chess_watch_log keeps a day: who watched which game is needed only for the hourly limit
--     and the channel's six hours, so older rows are deleted as new ones arrive.
-- Re-runnable: CREATE OR REPLACE, DROP TRIGGER IF EXISTS.
-- Rollback: DROP FUNCTION public.export_my_data(); DROP TRIGGER chess_watch_log_keep_a_day ON
--   public.chess_watch_log; DROP FUNCTION private.chess_watch_log_keep_a_day().

CREATE OR REPLACE FUNCTION public.export_my_data()
RETURNS JSONB
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    me UUID := (SELECT auth.uid());
BEGIN
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
$$;
REVOKE ALL ON FUNCTION public.export_my_data() FROM public, anon;
GRANT EXECUTE ON FUNCTION public.export_my_data() TO authenticated;

CREATE OR REPLACE FUNCTION private.chess_watch_log_keep_a_day()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    DELETE FROM public.chess_watch_log w WHERE w.user_id = NEW.user_id AND w.joined_at < now() - INTERVAL '1 day';
    RETURN NULL;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_watch_log_keep_a_day() FROM public, anon, authenticated;

DROP TRIGGER IF EXISTS chess_watch_log_keep_a_day ON public.chess_watch_log;
CREATE TRIGGER chess_watch_log_keep_a_day
    AFTER INSERT ON public.chess_watch_log
    FOR EACH ROW
    EXECUTE FUNCTION private.chess_watch_log_keep_a_day();

DELETE FROM public.chess_watch_log WHERE joined_at < now() - INTERVAL '1 day';
