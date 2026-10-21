# Licences of bundled third-party data

PixelQuest's own code is under the MIT License (`LICENSE`). The files below are not: they keep their own terms, and the MIT License doesn't apply to them. See docs/quran-data.md for how the app uses them. (The app's other fonts are listed in ASSETS.md.)

| Files | Whose | Terms | Notice |
|---|---|---|---|
| `data/quran-source/quran-uthmani.xml`, and the Arabic in `app/src/main/assets/quran/` | Tanzil Project | Creative Commons Attribution 3.0. Verbatim copies only, never changed; credit Tanzil and link to tanzil.net; keep the notice in every copy | [tanzil-quran-text.txt](tanzil-quran-text.txt) |
| `data/quran-source/en.sahih.txt`, and the translation in `app/src/main/assets/quran/` | Saheeh International, via Tanzil | Non-commercial use only; other use needs the publisher's permission | [tanzil-translations.txt](tanzil-translations.txt) |
| `data/quran-source/quran-data.xml`, and the surah data in `app/src/main/assets/quran/` | Tanzil Project | Creative Commons Attribution | [tanzil-metadata.txt](tanzil-metadata.txt) |
| `app/src/main/res/font/amiri_quran.ttf`, the Quran's Arabic typeface | The Amiri Project Authors (Khaled Hosny, Sebastian Kosch) | SIL Open Font License 1.1 | [amiri-quran-OFL.txt](amiri-quran-OFL.txt) |

PixelQuest is free, with no ads or purchases, so the translation's non-commercial terms are met. A fork that sells the app, or shows ads, must get Saheeh International's permission or bundle a translation whose licence allows it.

## Where each file came from

All were downloaded on **2026-10-20**, and are kept exactly as downloaded (the font renamed, as Android requires).

| File | URL | Bytes | SHA-256 |
|---|---|---|---|
| `quran-uthmani.xml` | `https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=xml&agree=true&marks=true&sajdah=true&alef=true&tatweel=true` | 1,541,387 | `8c5aeae20363a98f6963720d29fce040ca8b56a8e75f8b564c257fce7f6d0417` |
| `en.sahih.txt` | `https://tanzil.net/trans/?transID=en.sahih&type=txt-2` | 898,049 | `a1778a1a56695d9b59ae910809ec46d9f4a55f05961de51cd56e6ebcf9040883` |
| `quran-data.xml` | `https://tanzil.net/res/text/metadata/quran-data.xml` | 77,234 | `8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a` |
| `amiri_quran.ttf` (as `AmiriQuran-Regular.ttf`) | `https://raw.githubusercontent.com/google/fonts/main/ofl/amiriquran/AmiriQuran-Regular.ttf` | 136,920 | `e2a47644762d16bdfb6d33e0d8db8c6ff30beae84150ef5a705316bbd829455c` |
| `amiri-quran-OFL.txt` (as `OFL.txt`) | `https://raw.githubusercontent.com/google/fonts/main/ofl/amiriquran/OFL.txt` | 4,392 | `b32e3b24e2a943b8acbf6ab8d5499bfed737d5176091b7341a8c1e7fbcdeb0fa` |

- **The Quran text:** the first URL is what [tanzil.net/download](https://tanzil.net/download/) submits for "Uthmani" and "XML", with its default options (pause marks, sajdah signs, superscript alefs, tatweel) and its terms of use accepted.
- **The translation:** the second is what [tanzil.net/trans](https://tanzil.net/trans/) opens for Saheeh International's download, as "Text (with aya numbers)". Its XML has a header comment XML doesn't allow (it contains `--`), so strict readers refuse it; the text format holds the same 6,236 ayahs, character for character.
- **Updates:** Tanzil publishes them at [tanzil.net/updates](https://tanzil.net/updates/).
- **The font:** Amiri Quran 1.003, Google Fonts' copy of [the Amiri project](https://github.com/aliftype/amiri)'s release (its `Amiri-1.003.zip`). It's made for the Quran's text: its marks, and the end-of-ayah sign with its number.

## Chess

The chess screens (docs/chess-ui-integration.md, design in `chess-ui/`) bring no new third-party files: the pieces are Unicode glyphs drawn with the system font, the icons are the Material Symbols the app already has, and the type is the fonts listed in ASSETS.md. chess.js (BSD-2-Clause) runs only on the server, in the Edge Functions (`supabase/functions/package.json`), and is not bundled in the app.
