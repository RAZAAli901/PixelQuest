-- Migration: 20261019000040_study_item_policies.sql
-- Description: Who writes a study party's items (docs/study-party.md, Items).
--   * Any active member adds one; the server fills in who and when (created_by, created_at).
--   * Only its creator or the party's leader edits or deletes it. Its party never changes (no
--     column grant for party_id on update).
--   * Only the leader pins or unpins (not_leader), whoever's item it is, and nobody else adds one
--     pinned.
--   * private.is_party_leader(party): the caller leads that party, and it isn't deleted.
-- Re-runnable: CREATE OR REPLACE, DROP POLICY IF EXISTS, DROP TRIGGER IF EXISTS.
-- Rollback: DROP POLICY for each policy below; DROP TRIGGER study_item_guard;
--   DROP FUNCTION public.study_item_guard(), private.is_party_leader(UUID); REVOKE the grants.

CREATE OR REPLACE FUNCTION private.is_party_leader(party UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1 FROM public.parties p
        WHERE p.id = party AND p.leader_id = (SELECT auth.uid()) AND p.deleted_at IS NULL
    );
$$;

REVOKE ALL ON FUNCTION private.is_party_leader(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION private.is_party_leader(UUID) TO authenticated;

GRANT INSERT (party_id, kind, title, subject, notes, due_at, pinned) ON public.study_items TO authenticated;
GRANT UPDATE (kind, title, subject, notes, due_at, pinned) ON public.study_items TO authenticated;
GRANT DELETE ON public.study_items TO authenticated;

DROP POLICY IF EXISTS "Members add items to their study party" ON public.study_items;
CREATE POLICY "Members add items to their study party"
    ON public.study_items FOR INSERT TO authenticated
    WITH CHECK (created_by = (SELECT auth.uid()) AND private.is_active_study_member(party_id));

DROP POLICY IF EXISTS "The creator or the leader edits an item" ON public.study_items;
CREATE POLICY "The creator or the leader edits an item"
    ON public.study_items FOR UPDATE TO authenticated
    USING (private.is_active_study_member(party_id) AND (created_by = (SELECT auth.uid()) OR private.is_party_leader(party_id)))
    WITH CHECK (private.is_active_study_member(party_id) AND (created_by = (SELECT auth.uid()) OR private.is_party_leader(party_id)));

DROP POLICY IF EXISTS "The creator or the leader deletes an item" ON public.study_items;
CREATE POLICY "The creator or the leader deletes an item"
    ON public.study_items FOR DELETE TO authenticated
    USING (private.is_active_study_member(party_id) AND (created_by = (SELECT auth.uid()) OR private.is_party_leader(party_id)));

CREATE OR REPLACE FUNCTION public.study_item_guard()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
BEGIN
    IF TG_OP = 'INSERT' THEN
        NEW.created_by := (SELECT auth.uid());
        NEW.created_at := now();
    ELSE
        -- Who added it, and when, are for good.
        NEW.created_by := OLD.created_by;
        NEW.created_at := OLD.created_at;
    END IF;
    IF (TG_OP = 'INSERT' AND NEW.pinned) OR (TG_OP = 'UPDATE' AND NEW.pinned IS DISTINCT FROM OLD.pinned) THEN
        -- The server's own key (no caller) may: only a signed-in non-leader may not.
        IF (SELECT auth.uid()) IS NOT NULL AND NOT private.is_party_leader(NEW.party_id) THEN
            RAISE EXCEPTION 'not_leader' USING ERRCODE = 'insufficient_privilege',
                DETAIL = 'Only the party''s leader pins or unpins an item.';
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS study_item_guard ON public.study_items;
CREATE TRIGGER study_item_guard
    BEFORE INSERT OR UPDATE ON public.study_items
    FOR EACH ROW
    EXECUTE FUNCTION public.study_item_guard();

REVOKE ALL ON FUNCTION public.study_item_guard() FROM public, anon, authenticated;
