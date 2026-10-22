# Google Play policy checklist

Reviewed 2026-10-22 (S068). This is a release checklist, not a substitute for the current Play
Console policy text. Re-check it before every production submission.

## Sensitive permissions and notifications

| Area | PixelQuest use | Release check |
|---|---|---|
| Exact alarms | A player-created quest, Namaz alert, or study reminder at a chosen time | Confirm that the declared use still fits Play's exact-alarm policy; do not use it for chat, chess or marketing. |
| Full-screen intent | An alarm/reminder that must be actionable at its scheduled time | Confirm it is limited to urgent reminders and that the lock screen hides task text until unlock. |
| Notifications | Reminders, study chat, chess turns and Darood updates | Channels are optional, notification content is private, and public lock-screen versions are generic. |

## User-generated content

Study chat, party names, uploads and chess challenges are user-generated content. The app supplies
report and block controls; reports are visible only to staff, blocks are enforced for chat, invites
and chess challenges, files stay private, and uploaded bytes are checked before use. Verify those
controls against the live service before release.

## Data safety and account controls

- Fill the Play data-safety form from `docs/PRIVACY_POLICY_DRAFT.md` and
  `docs/security/DATA_MINIMISATION.md`; do not guess at collection or sharing.
- Publish the completed privacy policy at the stable URL entered in the Play Console.
- Keep Account > Delete account and Export my cloud data available in every release.
- Check that the deletion flow works against production and that `cleanup-uploads` is scheduled.

## Submission sign-off

Before upload, record the APK version, policy review date, tester, data-safety form review, and the
manual results in `VERIFICATION.md`. A failed or unanswered row blocks the release.
