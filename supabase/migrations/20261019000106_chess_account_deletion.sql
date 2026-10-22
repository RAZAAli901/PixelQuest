-- Migration: 20261019000106_chess_account_deletion.sql
-- Description: Deleting an account (J174, delete_user_account) keeps the other side's chess:
--   * their games stay, with the deleted player's side anonymised (white_id or black_id null),
--     so the opponent's history and the moves remain;
--   * the pair's head-to-head row stays for the opponent: its user columns no longer reference
--     auth.users (a deleted account's id is only a number then), and it's read by either of the
--     pair as well as by party mates of both;
--   * the deleted player's own ratings, challenges, watches and push tokens go with them, as do
--     their chat messages and file rows; the files' bytes are then orphans that cleanup-uploads
--     deletes from storage (an object no file row names).
-- Re-runnable: DROP CONSTRAINT IF EXISTS, DROP POLICY IF EXISTS.
-- Rollback: restore the NOT NULL columns and CASCADE references from 20261019000075 and 078.

ALTER TABLE public.chess_games ALTER COLUMN white_id DROP NOT NULL;
ALTER TABLE public.chess_games ALTER COLUMN black_id DROP NOT NULL;
ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_white_id_fkey;
ALTER TABLE public.chess_games DROP CONSTRAINT IF EXISTS chess_games_black_id_fkey;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_white_id_fkey FOREIGN KEY (white_id) REFERENCES auth.users(id) ON DELETE SET NULL;
ALTER TABLE public.chess_games ADD CONSTRAINT chess_games_black_id_fkey FOREIGN KEY (black_id) REFERENCES auth.users(id) ON DELETE SET NULL;
-- A live game whose player is deleted can't go on: it ends (aborted, unrated) first.
CREATE OR REPLACE FUNCTION private.chess_end_games_of_deleted()
RETURNS TRIGGER
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = ''
AS $$
DECLARE
    live UUID;
BEGIN
    FOR live IN SELECT g.id FROM public.chess_games g WHERE g.status IN ('waiting', 'active') AND OLD.id IN (g.white_id, g.black_id) LOOP
        PERFORM private.chess_finish(live, NULL, 'aborted');
    END LOOP;
    RETURN OLD;
END;
$$;
REVOKE ALL ON FUNCTION private.chess_end_games_of_deleted() FROM public, anon, authenticated;
DROP TRIGGER IF EXISTS chess_end_games_of_deleted ON auth.users;
CREATE TRIGGER chess_end_games_of_deleted
    BEFORE DELETE ON auth.users
    FOR EACH ROW
    EXECUTE FUNCTION private.chess_end_games_of_deleted();

ALTER TABLE public.chess_head_to_head DROP CONSTRAINT IF EXISTS chess_head_to_head_user_low_fkey;
ALTER TABLE public.chess_head_to_head DROP CONSTRAINT IF EXISTS chess_head_to_head_user_high_fkey;

DROP POLICY IF EXISTS "Party mates read head-to-head" ON public.chess_head_to_head;
CREATE POLICY "Party mates read head-to-head" ON public.chess_head_to_head FOR SELECT TO authenticated
    USING (
        (SELECT auth.uid()) IN (user_low, user_high)
        OR (private.shares_study_party(user_low) AND private.shares_study_party(user_high))
    );
