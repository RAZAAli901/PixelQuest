-- Migration: 20261019000014_darood_read_policies.sql
-- Description: Who reads what in Darood Parties (docs/darood-design.md, The rules). Nobody else
-- reads anything: not signed-out callers, not players outside the party.
--   * A party: its active members, and anyone invited to it (to decide). Never once deleted.
--   * Its member list and its entries: its active members only. Everyone also sees their own
--     membership rows, which is how invitations reach them.
--   * Usernames: still your own, and now also those of people in a party with you while you're an
--     active member of it, and, while you're invited, the leader's and whoever invited you.
-- The checks are SECURITY DEFINER functions in a schema the API doesn't expose (private): a
-- policy on party_members can't query party_members itself without recursing, and the functions
-- answer only about the caller.
-- Re-runnable: IF NOT EXISTS, CREATE OR REPLACE, and policies dropped first.
-- Rollback: DROP POLICY IF EXISTS "Members read their party" ON public.parties; (and the four below)
--   DROP SCHEMA IF EXISTS private CASCADE;

CREATE SCHEMA IF NOT EXISTS private;
REVOKE ALL ON SCHEMA private FROM public, anon;
GRANT USAGE ON SCHEMA private TO authenticated;

-- The caller is an active member of a party that hasn't been deleted.
CREATE OR REPLACE FUNCTION private.is_active_member(party UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.party_members m
        JOIN public.parties p ON p.id = m.party_id
        WHERE m.party_id = party
          AND m.user_id = (SELECT auth.uid())
          AND m.status = 'active'
          AND p.deleted_at IS NULL
    );
$$;

-- The caller has an unanswered invitation to a party that hasn't been deleted.
CREATE OR REPLACE FUNCTION private.is_invited(party UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.party_members m
        JOIN public.parties p ON p.id = m.party_id
        WHERE m.party_id = party
          AND m.user_id = (SELECT auth.uid())
          AND m.status = 'invited'
          AND p.deleted_at IS NULL
    );
$$;

-- Whether the caller may see [other]'s username because of a party they share.
CREATE OR REPLACE FUNCTION private.can_see_username(other UUID)
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = ''
AS $$
    SELECT EXISTS (
        SELECT 1
        FROM public.party_members me
        JOIN public.party_members them ON them.party_id = me.party_id
        JOIN public.parties p ON p.id = me.party_id
        WHERE me.user_id = (SELECT auth.uid())
          AND them.user_id = other
          AND p.deleted_at IS NULL
          AND (
              me.status = 'active'
              OR (me.status = 'invited' AND (them.role = 'leader' OR them.user_id = me.invited_by))
          )
    );
$$;

REVOKE ALL ON FUNCTION private.is_active_member(UUID), private.is_invited(UUID), private.can_see_username(UUID) FROM public, anon;
GRANT EXECUTE ON FUNCTION private.is_active_member(UUID), private.is_invited(UUID), private.can_see_username(UUID) TO authenticated;

DROP POLICY IF EXISTS "Members read their party" ON public.parties;
CREATE POLICY "Members read their party"
    ON public.parties
    FOR SELECT
    TO authenticated
    USING (deleted_at IS NULL AND (private.is_active_member(id) OR private.is_invited(id)));

DROP POLICY IF EXISTS "Members read their party's members" ON public.party_members;
CREATE POLICY "Members read their party's members"
    ON public.party_members
    FOR SELECT
    TO authenticated
    USING (user_id = (SELECT auth.uid()) OR private.is_active_member(party_id));

DROP POLICY IF EXISTS "Members read their party's entries" ON public.darood_entries;
CREATE POLICY "Members read their party's entries"
    ON public.darood_entries
    FOR SELECT
    TO authenticated
    USING (private.is_active_member(party_id));

DROP POLICY IF EXISTS "Read the usernames of people in a party with you" ON public.usernames;
CREATE POLICY "Read the usernames of people in a party with you"
    ON public.usernames
    FOR SELECT
    TO authenticated
    USING (private.can_see_username(user_id));
