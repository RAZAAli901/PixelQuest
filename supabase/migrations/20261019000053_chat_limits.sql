-- Migration: 20261019000053_chat_limits.sql
-- Description: The study chat's limits (docs/study-chat.md), held by the tables themselves so no
-- way of writing gets round them, and listed once for the Edge Functions and the app.
--   * study_chat_config: every limit in one place, mirrored by the app's StudyChatConfig (a test
--     holds them equal): 2,000 characters a message, 20 MB a file, 500 MB a party, 20 uploads a
--     member a day, 30 messages a member a minute, 120 characters a file name. A view, as
--     study_points_config is: it can't be written.
--   * study_chat_types: the allowed types, each with its extensions. Only these are ever stored;
--     the content check is finalize-upload's.
--   * chat_message_body: a text message is 1 to 2,000 characters with no spaces at either end; a
--     file message has a file and no text; a system line is a short token. A deleted message keeps
--     no text and no file (chat_message_deleted).
--   * chat_file_size: 1 byte to 20 MB. chat_file_mime: an allowed type. chat_file_name: 1 to 120
--     characters with no path or control characters. chat_file_path: <party_id>/<uuid>, in its party.
-- Re-runnable: CREATE OR REPLACE, DROP CONSTRAINT IF EXISTS first.
-- Rollback: DROP VIEW public.study_chat_config, public.study_chat_types; DROP each constraint below.

CREATE OR REPLACE VIEW public.study_chat_config AS
SELECT
    2000 AS message_max_chars,
    20971520 AS file_max_bytes,
    524288000 AS party_max_bytes,
    20 AS uploads_per_day,
    30 AS messages_per_minute,
    120 AS file_name_max_chars;

CREATE OR REPLACE VIEW public.study_chat_types AS
SELECT * FROM (VALUES
    ('application/pdf', ARRAY['pdf']),
    ('application/msword', ARRAY['doc']),
    ('application/vnd.openxmlformats-officedocument.wordprocessingml.document', ARRAY['docx']),
    ('application/vnd.ms-excel', ARRAY['xls']),
    ('application/vnd.openxmlformats-officedocument.spreadsheetml.sheet', ARRAY['xlsx']),
    ('application/vnd.ms-powerpoint', ARRAY['ppt']),
    ('application/vnd.openxmlformats-officedocument.presentationml.presentation', ARRAY['pptx']),
    ('text/plain', ARRAY['txt']),
    ('text/csv', ARRAY['csv']),
    ('image/png', ARRAY['png']),
    ('image/jpeg', ARRAY['jpg', 'jpeg'])
) AS t (mime, extensions);

GRANT SELECT ON public.study_chat_config, public.study_chat_types TO authenticated;
REVOKE ALL ON public.study_chat_config, public.study_chat_types FROM anon;

ALTER TABLE public.chat_messages DROP CONSTRAINT IF EXISTS chat_message_body;
ALTER TABLE public.chat_messages ADD CONSTRAINT chat_message_body
    CHECK (
        deleted_at IS NOT NULL
        OR (kind = 'text' AND body IS NOT NULL AND char_length(body) BETWEEN 1 AND 2000 AND body = btrim(body) AND file_id IS NULL)
        OR (kind = 'file' AND body IS NULL AND file_id IS NOT NULL)
        OR (kind = 'system' AND body ~ '^[a-z_]{1,40}$')
    );

ALTER TABLE public.chat_messages DROP CONSTRAINT IF EXISTS chat_message_deleted;
ALTER TABLE public.chat_messages ADD CONSTRAINT chat_message_deleted
    CHECK (deleted_at IS NULL OR (body IS NULL AND file_id IS NULL));

ALTER TABLE public.chat_files DROP CONSTRAINT IF EXISTS chat_file_size;
ALTER TABLE public.chat_files ADD CONSTRAINT chat_file_size CHECK (size_bytes BETWEEN 1 AND 20971520);

ALTER TABLE public.chat_files DROP CONSTRAINT IF EXISTS chat_file_mime;
ALTER TABLE public.chat_files ADD CONSTRAINT chat_file_mime
    CHECK (mime IN (
        'application/pdf', 'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'text/plain', 'text/csv', 'image/png', 'image/jpeg'
    ));

ALTER TABLE public.chat_files DROP CONSTRAINT IF EXISTS chat_file_name;
ALTER TABLE public.chat_files ADD CONSTRAINT chat_file_name
    CHECK (
        char_length(display_name) BETWEEN 1 AND 120
        AND display_name = btrim(display_name)
        AND display_name !~ '[/\:*?"<>|[:cntrl:]]'
        AND display_name NOT IN ('.', '..')
    );

ALTER TABLE public.chat_files DROP CONSTRAINT IF EXISTS chat_file_path;
ALTER TABLE public.chat_files ADD CONSTRAINT chat_file_path
    CHECK (storage_path ~ ('^' || party_id::TEXT || '/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$'));

ALTER TABLE public.chat_files DROP CONSTRAINT IF EXISTS chat_file_rejected;
ALTER TABLE public.chat_files ADD CONSTRAINT chat_file_rejected
    CHECK ((status = 'rejected') = (reject_reason IS NOT NULL));
