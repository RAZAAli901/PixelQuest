# Code audit baseline

Measured 2026-10-22 for Q001. Counts cover Kotlin production sources under
`app/src/main/java/com/pixelquest/app`; generated code and tests are excluded.

| Package | Kotlin files | Lines |
|---|---:|---:|
| ui | 120 | 15,072 |
| study | 79 | 10,390 |
| darood | 63 | 7,102 |
| chess | 37 | 4,603 |
| quran | 42 | 4,267 |
| data | 54 | 4,077 |
| domain | 78 | 3,106 |
| Other feature and platform packages | 74 | 6,962 |

## Largest files

The first splitting candidates are `StudyPartyScreen.kt` (854 lines), `PartyInfoScreen.kt` (718),
`PqMotion.kt` (661), `PartyChatScreen.kt` (622), `PqComponents.kt` (508), `HeroScreen.kt` (494),
`HomeScreen.kt` (486), and `DataExportImport.kt` (477). There are 27 production Kotlin files over
300 lines. They should be split only behind existing tests and without product changes (Q023).

## Duplication and dead-code leads

- Theme and motion code has parallel Pixel and Comic paths; Q024-Q026 should consolidate those
  behind design tokens rather than changing their appearance.
- Data repositories and remote sources use several result/error and dispatcher patterns; Q029-Q033
  will standardise these after characterisation tests.
- R8 and Android lint reports are the authority for unused-code/resource removal (Q011-Q015), not
  a filename search.

## Hygiene findings

- No unresolved `TODO`, `FIXME`, `HACK`, or `XXX` marker exists in production sources; the existing
  `TodoAuditTest` enforces this.
- The release APK baseline is 5,108,142 bytes (`app-release.apk`, 2026-10-22). Q002 records the
  reproducible build and test baseline; Q019 compares the final size and method count.
- The pre-existing `app/src/test/java/com/pixelquest/app/zz/` directory is screenshot scratch work
  and must not be committed.
