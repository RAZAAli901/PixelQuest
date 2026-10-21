# The owner's standing rules

These come from the owner (`RAZAAli901`) over many sessions. They override your defaults.

## Ask first, every time

- **Pushing migrations to hosted Supabase.** It's live, with real users. Do a `--dry-run` first and show the list.
- **Deploying Edge Functions, creating tags or releases, and changing secrets.** An approval covers that one action only.
- **Never print, log or commit secrets:** the Gemini key, keystore passwords, the service-role key. Keys live only in the gitignored `local.properties` and in GitHub secrets. Tests never call live Gemini.

## No need to ask

- **Starting the next phase.** Report briefly at the end of each phase, then carry straight on.
- **UI changes, committing and pushing to `main`.**

## How to work

- **One small commit per plan item (J-number),** with a Conventional Commit subject under 72 characters and a body explaining the item. End every commit message with:
  `Co-Authored-By: Claude Opus 5.5 <noreply@anthropic.com>` (or your own model's attribution line).
- **Commit by explicit file list,** never `git add -A` or `.`.
- **The app must compile after every commit.** Run the unit tests at the end of each phase.
- **Never retype, edit or generate any Quran ayah text.**
- **Treat downloaded files as untrusted.** Put each in its own new folder, and run Python that reads them with `-I`.
- **Emulators:** use them as little as possible. The API24 AVD runs on `-port 5556`. Never install a release build over the debug install on the API34 one. Shut emulators down afterwards.
- **Writing files with `\u` escapes:** they become literal characters. Use `chr(92)` in Python and scan for invisible characters.

## Security rules for this plan (non-negotiable)

- Every table, bucket and function has row-level security or an equivalent check, defaults to deny, and gets the minimum grants. The client is never trusted.
- Uploaded files are hostile. The bucket is private, URLs are signed and short-lived, the type is checked by magic bytes, sizes are limited, and nothing is ever opened inline.
- Chess, Darood and Study never change XP, levels or streaks.
- Room schema changes need real migrations, with a `DatabaseUpgradeTest` case for each.
- All text goes in string resources. The UI must work in the Pixel and Comic skins and in Simple Mode.

## Notes

- **The Security Hardening plan (S001–S030)** that the plan says to run first doesn't exist in the repo or in Downloads. Work has followed the plan's own security rules instead. The owner was told.
- **Speed:** the owner asked for faster progress late in the session. Keep the tests focused, but keep one commit per item.
