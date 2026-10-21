-- Migration: 20261019000066_chat_reports_blocks.sql
-- Description: Reporting and blocking in study chats (docs/study-chat.md, threat model: Abuse).
--   * chat_reports: report_chat_message(message_id, reason) by an active member of the message's
--     party, about someone else's message (a file message reports its file too). One report per
--     member per message. Nobody but the service role reads reports.
--   * member_blocks: a member's own block list (block_member, unblock_member); they read only
--     their own. The app hides blocked people's messages for the blocker; nothing changes for
--     anyone else.
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE, DROP POLICY IF EXISTS.
-- Rollback: DROP FUNCTION report_chat_message, block_member, unblock_member; DROP TABLE chat_reports, member_blocks.

CREATE TABLE IF NOT EXISTS public.chat_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    message_id UUID NOT NULL REFERENCES public.chat_messages(id) ON DELETE CASCADE,
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    reason TEXT NOT NULL CHECK (reason IN ('spam', 'abuse', 'inappropriate', 'unsafe_file', 'other')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    UNIQUE (reporter_id, message_id)
);
ALTER TABLE public.chat_reports ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.chat_reports FROM anon, authenticated;

CREATE TABLE IF NOT EXISTS public.member_blocks (
    blocker_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    blocked_id UUID NOT NULL REFERENCES auth.users(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (blocker_id, blocked_id),
    CHECK (blocker_id <> blocked_id)
);
ALTER TABLE public.member_blocks ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.member_blocks FROM anon, authenticated;
GRANT SELECT ON public.member_blocks TO authenticated;
DROP POLICY IF EXISTS "Members read their own blocks" ON public.member_blocks;
CREATE POLICY "Members read their own blocks" ON public.member_blocks FOR SELECT TO authenticated
    USING (blocker_id = (SELECT auth.uid()));

CREATE OR REPLACE FUNCTION public.report_chat_message(message_id UUID, reason TEXT)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_column
DECLARE
    me UUID := (SELECT auth.uid());
    m public.chat_messages%ROWTYPE;
BEGIN
    SELECT * INTO m FROM public.chat_messages WHERE id = report_chat_message.message_id;
    IF NOT FOUND OR me IS NULL OR m.sender_id = me OR NOT private.is_active_study_member(m.party_id) THEN
        RAISE EXCEPTION 'not_allowed' USING ERRCODE = 'insufficient_privilege';
    END IF;
    INSERT INTO public.chat_reports (reporter_id, message_id, party_id, reason)
    VALUES (me, m.id, m.party_id, report_chat_message.reason)
    ON CONFLICT (reporter_id, message_id) DO NOTHING;
END;
$$;

CREATE OR REPLACE FUNCTION public.block_member(user_id UUID)
RETURNS VOID LANGUAGE sql SECURITY DEFINER SET search_path = '' AS $$
    INSERT INTO public.member_blocks (blocker_id, blocked_id)
    SELECT (SELECT auth.uid()), block_member.user_id
    WHERE (SELECT auth.uid()) IS NOT NULL AND (SELECT auth.uid()) <> block_member.user_id
    ON CONFLICT DO NOTHING;
$$;

CREATE OR REPLACE FUNCTION public.unblock_member(user_id UUID)
RETURNS VOID LANGUAGE sql SECURITY DEFINER SET search_path = '' AS $$
    DELETE FROM public.member_blocks WHERE blocker_id = (SELECT auth.uid()) AND blocked_id = unblock_member.user_id;
$$;

REVOKE ALL ON FUNCTION public.report_chat_message(UUID, TEXT), public.block_member(UUID), public.unblock_member(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.report_chat_message(UUID, TEXT), public.block_member(UUID), public.unblock_member(UUID) TO authenticated;
