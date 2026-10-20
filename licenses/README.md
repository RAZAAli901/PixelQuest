# Licences of bundled third-party data

PixelQuest's own code is under the MIT License (`LICENSE`). The files below are not: they keep their own terms, and the MIT License doesn't apply to them. See docs/quran-data.md for how the app uses them.

| Files | Whose | Terms | Notice |
|---|---|---|---|
| `data/quran-source/quran-uthmani.xml`, and the Arabic in `app/src/main/assets/quran/` | Tanzil Project | Creative Commons Attribution 3.0. Verbatim copies only, never changed; credit Tanzil and link to tanzil.net; keep the notice in every copy | [tanzil-quran-text.txt](tanzil-quran-text.txt) |
| `data/quran-source/en.sahih.txt`, and the translation in `app/src/main/assets/quran/` | Saheeh International, via Tanzil | Non-commercial use only; other use needs the publisher's permission | [tanzil-translations.txt](tanzil-translations.txt) |
| `data/quran-source/quran-data.xml`, and the surah data in `app/src/main/assets/quran/` | Tanzil Project | Creative Commons Attribution | [tanzil-metadata.txt](tanzil-metadata.txt) |

PixelQuest is free, with no ads or purchases, so the translation's non-commercial terms are met. A fork that sells the app, or shows ads, must get Saheeh International's permission or bundle a translation whose licence allows it.

## Where each file came from

All three were downloaded on **2026-10-20**, and are kept exactly as downloaded.

| File | URL | Bytes | SHA-256 |
|---|---|---|---|
| `quran-uthmani.xml` | `https://tanzil.net/pub/download/index.php?quranType=uthmani&outType=xml&agree=true&marks=true&sajdah=true&alef=true&tatweel=true` | 1,541,387 | `8c5aeae20363a98f6963720d29fce040ca8b56a8e75f8b564c257fce7f6d0417` |
| `en.sahih.txt` | `https://tanzil.net/trans/?transID=en.sahih&type=txt-2` | 898,049 | `a1778a1a56695d9b59ae910809ec46d9f4a55f05961de51cd56e6ebcf9040883` |
| `quran-data.xml` | `https://tanzil.net/res/text/metadata/quran-data.xml` | 77,234 | `8867c1d88191472adec9db694b3cd9f135b1a2ef580574d32cf888dcb22c5c7a` |

- **The Quran text:** the first URL is what [tanzil.net/download](https://tanzil.net/download/) submits for "Uthmani" and "XML", with its default options (pause marks, sajdah signs, superscript alefs, tatweel) and its terms of use accepted.
- **The translation:** the second is what [tanzil.net/trans](https://tanzil.net/trans/) opens for Saheeh International's download, as "Text (with aya numbers)". Its XML has a header comment XML doesn't allow (it contains `--`), so strict readers refuse it; the text format holds the same 6,236 ayahs, character for character.
- **Updates:** Tanzil publishes them at [tanzil.net/updates](https://tanzil.net/updates/).
