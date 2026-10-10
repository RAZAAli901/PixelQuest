-- Migration: 20261010000000_display_name_word_matching.sql
-- Description: The display-name filter rejected ordinary names because it matched every listed
-- word anywhere inside a name: "Titan_Slayer", "Stitch" and "Altitude" contain "tit", "Scrappy"
-- contains "crap", "Dickens" contains "dick". Words that are only offensive on their own now match
-- whole words only (a name's words are split at underscores and at lower-to-upper case changes,
-- so "DickHead" is still caught); unambiguous terms still match anywhere. Digits used as letters
-- (sh1t, n4zi) are read as letters first. The app's DisplayNameModerator applies the same rules.

CREATE OR REPLACE FUNCTION public.check_display_name_moderation()
RETURNS TRIGGER AS $$
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
    letters TEXT;
    words TEXT[];
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
        letters := replace(spaced, '_', '');
        words := regexp_split_to_array(spaced, '_+');

        FOREACH term IN ARRAY anywhere_terms LOOP
            IF position(term IN letters) > 0 THEN
                RAISE EXCEPTION 'Display name contains disallowed or offensive terminology.'
                    USING ERRCODE = 'check_violation';
            END IF;
        END LOOP;

        FOREACH term IN ARRAY word_terms LOOP
            IF term = ANY (words) OR (term || 's') = ANY (words) THEN
                RAISE EXCEPTION 'Display name contains disallowed or offensive terminology.'
                    USING ERRCODE = 'check_violation';
            END IF;
        END LOOP;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

COMMENT ON FUNCTION public.check_display_name_moderation() IS
    'Rejects malformed display names and offensive terms: unambiguous terms anywhere, ambiguous ones (tit, crap, dick, ...) only as whole words, so names like Titan_Slayer pass.';
