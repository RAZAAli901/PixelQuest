# Quran data

The Quran reader shows all 114 surahs, in Arabic with an English translation, from files bundled in the app, so it works offline. This document says where the text comes from, under what terms, how it becomes the app's data, and how to update it.

**The one rule:** the Quran's text and its translation are copied exactly as their source gives them. No ayah is ever retyped, edited, summarised or generated. Nothing in this repository changes a character of either. The build checks counts and checksums instead.

## Source

All three files come from the [Tanzil Project](https://tanzil.net):

| File | What | Version | From |
|---|---|---|---|
| `quran-uthmani.xml` | The Quran in Uthmani script, as in the Madina Mushaf | 1.1 (February 2021) | [tanzil.net/download](https://tanzil.net/download/) |
| `en.sahih.txt` | Saheeh International's English translation (`en.sahih`) | Last update April 24, 2011 | [tanzil.net/trans](https://tanzil.net/trans/) |
| `quran-data.xml` | Metadata: each surah's names, place of revelation, ayah count; the juz | 1.0 | [tanzil.net/docs/Quran_Metadata](https://tanzil.net/docs/Quran_Metadata) |

- **The Arabic is XML.** Tanzil's XML keeps the Bismillah apart: the first ayah of each surah but Al-Fatihah and At-Tawbah carries a `bismillah` attribute, and its `text` doesn't start with it. In the plain-text format, the Bismillah is joined to that ayah's text, and the app would have to cut it off. Each surah keeps its own Bismillah, exactly as Tanzil gives it: they aren't all alike, since surahs 95 and 97 carry one more mark (a shadda), as in their Mushaf.
- **The translation is text,** one ayah a line (`surah|ayah|text`), with Tanzil's notice in `#` lines at the end. Its XML has a header comment that XML doesn't allow (`--` inside it), so a strict reader refuses the file. The text format holds the same 6,236 ayahs, character for character.
- **Options:** Uthmani with Tanzil's defaults: pause marks, sajdah signs, superscript alefs, and tatweel below superscript alefs.

## Terms

- **The Arabic text** (Creative Commons Attribution 3.0, with Tanzil's terms of use):
  - It may be copied and distributed only verbatim: changing it is not allowed.
  - Its source, the Tanzil Project, must be clearly indicated, with a link to tanzil.net so users can follow its updates.
  - Its copyright notice must be in every copy, and reproduced in any file derived from it. So each data file the app bundles carries it.
- **The translation:**
  - Tanzil's translations are for non-commercial use only.
  - PixelQuest is free, with no ads and no purchases, so it fits.
  - Selling the app, or adding ads, would need the publisher's permission, or a translation whose licence allows it.
- **The repository's licence:** PixelQuest's code is MIT, but these files aren't. They keep their own terms, and `licenses/` says so.
- **The About screen** shows the source, the translator's credit, and the links (Phase C).

## Files

- **`data/quran-source/`:** the three files exactly as downloaded. Git treats them as binary (`.gitattributes`), so line endings are never changed and their checksums hold on every machine.
- **`licenses/`:** the terms above, as Tanzil states them, and where and when each file was downloaded.
- **`tools/quran-build/`:** a small Kotlin program, run with Gradle, that reads the three files, checks them, and writes the app's data files.
- **`app/src/main/assets/quran/`:** what the app bundles, written by the tool, never by hand: `quran_ar.json.gz` and `quran_en.json.gz`.
  - The Arabic and the translation are separate files, so another translation can be added as one more file.
  - They're gzipped in the repository. The Android build unzips `.gz` assets as it merges them, so in the APK they're `quran/quran_ar.json` and `quran/quran_en.json`, which the app reads.

## Backups and sync

The Quran is the app's own text, not the player's progress:
- **The game's backup file** and its daily copies on the phone never hold it, and **the synced profile** never mentions it.
- **Resetting all progress, or restoring a backup,** leaves it be: those clear the game's own tables only.
- **Android's own backup** copies the whole database file, so the Quran's tables go with it: a few MB, well under Android's 25 MB per app. A restored copy is checked against the app's bundle, and loaded again if it differs.
- **The player's choice of translation** is a preference on the phone, not backed up.

## Checks

The tool refuses to write anything unless all of these hold:
- 114 surahs and 6,236 ayahs, numbered in order, with no empty text.
- Each surah's ayah count matches the metadata.
- The translation has exactly the same surahs and ayahs as the Arabic.
- The Bismillah is on the first ayah of every surah except 1 and 9, and on no other ayah. It's shown above those 112 surahs; in Al-Fatihah it's the first ayah itself, the only place it counts as an ayah.
- Each source file's SHA-256 is the one recorded for it, so a changed character fails the build.

The app's tests also compare every ayah in its data files with the source files, character for character.

## Size

Measured on 2026-10-20, Tanzil's files as above:

| What | Bytes |
|---|---|
| The three source files | 2,516,670 |
| The app's two files as plain JSON | 2,274,609 |
| The same, gzipped (best compression), as the repository keeps them | 540,295 |
| In the APK: the build unzips them, then deflates them with every asset | 558,690 |
| Release APK, 1.4.1 | 6,936,271 |
| Release APK with the Quran and the Islamic hub | 7,539,359 |

- **The APK:** gzip makes no difference to it, since the build unzips the files and deflates them itself; 559 KB of its 603 KB growth is the Quran. Gzip keeps the repository 1.7 MB smaller.
- **On the phone:** the Quran's tables, with the translation's search index, take a few MB in the database.

**Budget:**
- The Quran's files in the APK: at most 700 KB.
- The release APK: at most 8 MB.

The tool's tests keep the gzipped files within budget (at most 600 KB together), and Phase F checks the APK against it before the release.

## Updating

1. Download the new files from the pages above, with the same options. Record the date and URL in `licenses/README.md`.
2. Replace the files in `data/quran-source/`, and update their recorded checksums in the tool, after reading Tanzil's change log.
3. Run `./gradlew :tools:quran-build:run`. It checks the files and rewrites the app's data files.
4. Run the tests. The new data version makes the app reload the Quran on players' phones.
