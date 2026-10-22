# Static-analysis baseline

Q004 verified 2026-10-22.

Detekt is applied to the Android module with the shared configuration in
`config/detekt/detekt.yml` and the established baseline in `config/detekt/baseline.xml`.
`./gradlew :app:detekt` produced its reports successfully.

The baseline records pre-existing complexity, long-line, magic-number and similar findings so that
the cleanup can address them deliberately instead of hiding them by weakening rules. Any finding
not represented in that baseline fails the Gradle task. Reports are generated under
`app/build/reports/detekt/` in HTML, Markdown, SARIF, text and XML formats.

The largest baseline clusters align with Q001's audit: screen composables, party/study flows, chess
rules and backup serialization. Q023-Q034 retire these entries incrementally as code is split and
rules are moved into named abstractions.
