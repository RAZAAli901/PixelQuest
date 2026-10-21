-- Migration: 20261019000065_study_item_files.sql
-- Description: A study item's materials (docs/study-chat.md): files shared in the party's chat,
-- linked to a quiz or assignment so it shows them.
--   * study_item_files(item_id, file_id): both in the same study party, a ready file.
--   * Reading: the party's active members. Linking and unlinking: link_item_file(item, file,
--     linked), by the item's creator or the party's leader (not_allowed otherwise).
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE, DROP POLICY IF EXISTS.
-- Rollback: DROP FUNCTION public.link_item_file(UUID, UUID, BOOLEAN); DROP TABLE public.study_item_files.

CREATE TABLE IF NOT EXISTS public.study_item_files (
    item_id UUID NOT NULL REFERENCES public.study_items(id) ON DELETE CASCADE,
    file_id UUID NOT NULL REFERENCES public.chat_files(id) ON DELETE CASCADE,
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    linked_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    PRIMARY KEY (item_id, file_id)
);
CREATE INDEX IF NOT EXISTS study_item_files_party ON public.study_item_files (party_id);

ALTER TABLE public.study_item_files ENABLE ROW LEVEL SECURITY;
REVOKE ALL ON public.study_item_files FROM anon, authenticated;
GRANT SELECT ON public.study_item_files TO authenticated;

DROP POLICY IF EXISTS "Members read their items' materials" ON public.study_item_files;
CREATE POLICY "Members read their items' materials"
    ON public.study_item_files FOR SELECT TO authenticated
    USING (private.is_active_study_member(party_id));

CREATE OR REPLACE FUNCTION public.link_item_file(item_id UUID, file_id UUID, linked BOOLEAN)
RETURNS VOID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
#variable_conflict use_column
DECLARE
    i public.study_items%ROWTYPE;
    the_file UUID := link_item_file.file_id;
BEGIN
    SELECT * INTO i FROM public.study_items WHERE id = link_item_file.item_id;
    IF NOT FOUND OR NOT private.is_active_study_member(i.party_id)
        OR NOT (i.created_by = (SELECT auth.uid()) OR private.is_party_leader(i.party_id))
        OR NOT EXISTS (SELECT 1 FROM public.chat_files f WHERE f.id = the_file AND f.party_id = i.party_id AND f.status = 'ready' AND f.deleted_at IS NULL) THEN
        RAISE EXCEPTION 'not_allowed' USING ERRCODE = 'insufficient_privilege';
    END IF;
    IF link_item_file.linked THEN
        INSERT INTO public.study_item_files (item_id, file_id, party_id) VALUES (i.id, the_file, i.party_id) ON CONFLICT DO NOTHING;
    ELSE
        DELETE FROM public.study_item_files WHERE item_id = i.id AND file_id = the_file;
    END IF;
END;
$$;
REVOKE ALL ON FUNCTION public.link_item_file(UUID, UUID, BOOLEAN) FROM public, anon;
GRANT EXECUTE ON FUNCTION public.link_item_file(UUID, UUID, BOOLEAN) TO authenticated;
