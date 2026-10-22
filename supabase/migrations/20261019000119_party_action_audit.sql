-- Migration: 20261019000119_party_action_audit.sql
-- Description: S064 staff-only record of party leader actions. The client never writes or reads it.
-- Re-runnable: table/function/trigger definitions are guarded or replaced.
-- Rollback: DROP TRIGGER party_action_audit_parties ON public.parties; DROP TRIGGER
-- party_action_audit_members ON public.party_members; DROP TABLE private.party_action_audit;

CREATE TABLE IF NOT EXISTS private.party_action_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    happened_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    actor_id UUID NULL REFERENCES auth.users(id) ON DELETE SET NULL,
    party_id UUID NOT NULL REFERENCES public.parties(id) ON DELETE CASCADE,
    action TEXT NOT NULL CHECK (action IN ('member_removed', 'leadership_transferred', 'party_deleted')),
    subject_id UUID NULL REFERENCES auth.users(id) ON DELETE SET NULL
);

ALTER TABLE private.party_action_audit ENABLE ROW LEVEL SECURITY;
ALTER TABLE private.party_action_audit FORCE ROW LEVEL SECURITY;
REVOKE ALL ON TABLE private.party_action_audit FROM public, anon, authenticated;
REVOKE ALL ON SEQUENCE private.party_action_audit_id_seq FROM public, anon, authenticated;

CREATE OR REPLACE FUNCTION private.audit_party_action()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    caller UUID := (SELECT auth.uid());
    recorded_actor UUID;
BEGIN
    -- Account deletion can trigger a succession after auth.users has removed the caller.
    -- Keep the system event without trying to reference a user that no longer exists.
    SELECT u.id INTO recorded_actor FROM auth.users u WHERE u.id = caller;
    IF TG_TABLE_NAME = 'party_members' THEN
        IF NEW.status = 'removed' AND OLD.status IS DISTINCT FROM 'removed' THEN
            INSERT INTO private.party_action_audit(actor_id, party_id, action, subject_id)
            VALUES (recorded_actor, NEW.party_id, 'member_removed', NEW.user_id);
        END IF;
    ELSIF TG_TABLE_NAME = 'parties' THEN
        IF NEW.leader_id IS DISTINCT FROM OLD.leader_id THEN
            INSERT INTO private.party_action_audit(actor_id, party_id, action, subject_id)
            VALUES (recorded_actor, NEW.id, 'leadership_transferred', NEW.leader_id);
        ELSIF NEW.deleted_at IS NOT NULL AND OLD.deleted_at IS NULL THEN
            INSERT INTO private.party_action_audit(actor_id, party_id, action)
            VALUES (recorded_actor, NEW.id, 'party_deleted');
        END IF;
    END IF;
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS party_action_audit_members ON public.party_members;
CREATE TRIGGER party_action_audit_members
    AFTER UPDATE OF status ON public.party_members
    FOR EACH ROW EXECUTE FUNCTION private.audit_party_action();

DROP TRIGGER IF EXISTS party_action_audit_parties ON public.parties;
CREATE TRIGGER party_action_audit_parties
    AFTER UPDATE OF leader_id, deleted_at ON public.parties
    FOR EACH ROW EXECUTE FUNCTION private.audit_party_action();

REVOKE ALL ON FUNCTION private.audit_party_action() FROM public, anon, authenticated;
COMMENT ON TABLE private.party_action_audit IS
    'Staff-only immutable audit of party leader actions; clients have no grants.';
