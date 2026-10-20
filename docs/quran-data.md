# Quran data

The Quran reader shows all 114 surahs, in Arabic with an English translation, from files bundled in the app, so it works offline. This document says where the text comes from, under what terms, how it becomes the app's data, and how to update it.

**The one rule:** the Quran's text and its translation are copied exactly as their source gives them. No ayah is ever retyped, edited, summarised or generated. Nothing in this repository changes a character of either. The build checks counts and checksums instead.

## Source

All three files come from the [Tanzil Project](https://tanzil.net):

| File | What | Version | From |
|---|---|---|---|
| `quran-uthmani.xml` | The Quran in Uthmani script, as in the Madina Mushaf | 1.1 (February 2021) | [tanzil.net/download](https://tanzil.net/download/) |
| `en.sahih.xml` | Saheeh International's English translation (`en.sahih`) | Last update April 24, 2011 | [tanzil.net/trans](https://tanzil.net/trans/) |
| `quran-data.xml` | Metadata: each surah's names, place of revelation, ayah count; the juz | 1.0 | [tanzil.net/docs/Quran_Metadata](https://tanzil.net/docs/Quran_Metadata) |

- **Format:** XML, for both texts. Tanzil's XML keeps the Bismillah apart: the first ayah of each surah but Al-Fatihah and At-Tawbah carries a `bismillah` attribute, and its `text` doesn't start with it. In the plain-text format, the Bismillah is joined to that ayah's text, and the app would have to cut it off.
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
- **`app/src/main/assets/quran/`:** what the app bundles, written by the tool, never by hand. The Arabic and the translation are separate files, so another translation can be added as one more file.

## Checks

The tool refuses to write anything unless all of these hold:
- 114 surahs and 6,236 ayahs, numbered in order, with no empty text.
- Each surah's ayah count matches the metadata.
- The translation has exactly the same surahs and ayahs as the Arabic.
- The Bismillah is on the first ayah of every surah except 1 and 9.
- Each source file's SHA-256 is the one recorded for it, so a changed character fails the build.

The app's tests also compare every ayah in its data files with the source files, character for character.

## Updating

1. Download the new files from the pages above, with the same options. Record the date and URL in `licenses/README.md`.
2. Replace the files in `data/quran-source/`, and update their recorded checksums in the tool, after reading Tanzil's change log.
3. Run `./gradlew :tools:quran-build:run`. It checks the files and rewrites the app's data files.
4. Run the tests. The new data version makes the app reload the Quran on players' phones.
