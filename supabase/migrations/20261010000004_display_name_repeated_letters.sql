-- Migration: 20261010000004_display_name_repeated_letters.sql
-- Description: The server's display-name check also reads stretched letters as one ("fuuuck" ->
-- "fuck", "Titt" -> "tit"), as the app's DisplayNameModerator does. The two disagreed on such names,
-- though both are meant to apply the same rules (supabase/tests/display_names.json).
-- Redefined in full; it keeps the fixed search_path from 20261010000001.

CREATE OR REPLACE FUNCTION public.check_display_name_moderation()
RETURNS TRIGGER
LANGUAGE plpgsql
SET search_path = ''
AS $$
DECLARE
    -- Offensive wherever they appear.
    anywhere_terms TEXT[] := ARRAY[
        'asshole', 'bastard', 'bitch', 'blowjob', 'cunt',
        'dildo', 'douche', 'faggot', 'fuck', 'nazi',
        'nigga', 'nigger', 'pussy', 'retard', 'shit',
        'slut', 'vagina', 'whore'
    ];
    -- Offensive as a word of their own (or its plural), but common inside ordinary names.
    word_terms TEXT[] := ARRAY[
        'crap', 'damn', 'dick', 'fag', 'homo',
        'penis', 'piss', 'prick', 'tit', 'twat'
    ];
    spaced TEXT;
    merged TEXT;
    letters TEXT;
    merged_letters TEXT;
    words TEXT[];
    merged_words TEXT[];
    term TEXT;
BEGIN
    -- Empty is allowed: a linked profile that hasn't joined the board yet.
    IF NEW.display_name IS NOT NULL AND NEW.display_name <> '' THEN
        IF NOT (NEW.display_name ~ '^[a-zA-Z0-9_]{3,20}$') THEN
            RAISE EXCEPTION 'Display name must be 3-20 characters consisting only of letters, numbers, and underscores.'
                USING ERRCODE = 'check_violation';
        END IF;

        -- "DickHead" -> "Dick_Head", then digits read as letters: 4->a 3->e 1->i 0->o 5->s 7->t 8->b.
        spaced := translate(
            lower(regexp_replace(NEW.display_name, '([a-z0-9])([A-Z])', '\1_\2', 'g')),
            '4310578', 'aeiostb'
        );
        -- Stretched letters read as one: "fuuuck" -> "fuck".
        merged := regexp_replace(spaced, '(.)\1+', '\1', 'g');
        letters := replace(spaced, '_', '');
        merged_letters := replace(merged, '_', '');
        words := regexp_split_to_array(spaced, '_+');
        merged_words := regexp_split_to_array(merged, '_+');

        FOREACH term IN ARRAY anywhere_terms LOOP
            IF position(term IN letters) > 0 OR position(term IN merged_letters) > 0 THEN
                RAISE EXCEPTION 'Display name contains disallowed or offensive terminology.'
                    USING ERRCODE = 'check_violation';
            END IF;
        END LOOP;

        FOREACH term IN ARRAY word_terms LOOP
            IF term = ANY (words) OR (term || 's') = ANY (words)
                OR term = ANY (merged_words) OR (term || 's') = ANY (merged_words) THEN
                RAISE EXCEPTION 'Display name contains disallowed or offensive terminology.'
                    USING ERRCODE = 'check_violation';
            END IF;
        END LOOP;
    END IF;
    RETURN NEW;
END;
$$;
