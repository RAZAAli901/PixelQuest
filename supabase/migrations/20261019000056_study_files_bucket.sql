-- Migration: 20261019000056_study_files_bucket.sql
-- Description: The private bucket for files shared in study chats, study-files (docs/study-chat.md,
-- How a file is shared).
--   * Private: no public URLs, ever. Up to 20 MB an object, and only the allowed types as
--     declared (the content check is finalize-upload's; this is one more wall).
--   * Paths are the server's own: <party_id>/<uuid> (chat_files.storage_path).
--   * No client reads, writes or lists it directly, member or not: a restrictive policy shuts it
--     to every signed-in and signed-out caller, so no permissive policy, now or later, can open
--     it. Members get to its files only through the Edge Functions, which check active membership
--     in the file's party each time: request-upload gives a 5-minute signed upload URL for one
--     path, get-download-url a 60-second signed download URL. (The simplest safe reading of
--     "policies by party membership": the membership policy is the functions' check.)
-- Re-runnable: ON CONFLICT, DROP POLICY IF EXISTS.
-- Rollback: DROP POLICY "study-files is the server's alone" ON storage.objects;
--   DELETE FROM storage.buckets WHERE id = 'study-files' (once empty).

INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
    'study-files', 'study-files', false, 20971520,
    ARRAY[
        'application/pdf', 'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        'application/vnd.ms-excel', 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
        'application/vnd.ms-powerpoint', 'application/vnd.openxmlformats-officedocument.presentationml.presentation',
        'text/plain', 'text/csv', 'image/png', 'image/jpeg'
    ]
)
ON CONFLICT (id) DO UPDATE SET
    public = false,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

DROP POLICY IF EXISTS "study-files is the server's alone" ON storage.objects;
CREATE POLICY "study-files is the server's alone"
    ON storage.objects AS RESTRICTIVE FOR ALL TO anon, authenticated
    USING (bucket_id <> 'study-files')
    WITH CHECK (bucket_id <> 'study-files');
