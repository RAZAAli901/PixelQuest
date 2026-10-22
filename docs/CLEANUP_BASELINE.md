# Cleanup baseline

Measured 2026-10-22 for Q002 on the checked-out `main` branch.

| Measure | Result |
|---|---:|
| Debug unit tests discovered | 2,178 |
| Debug unit-test failures | 2 |
| Release APK | 5,108,142 bytes |
| Release APK timestamp | 2026-10-22 06:25 local time |

## Baseline failures

1. `CloudSyncEdgeCasesTest.syncNow_whenServerErrorOccurs_displaysErrorMessage` fails its error-state
   assertion. This is a characterisation failure to resolve before refactoring cloud-sync UI state.
2. `NoGameDataFromIslamicCodeTest.theirMigrations_neverWriteTheProfilesOnTheServer` rejects the
   intentional profile delete in migration 118. The guard needs a narrow account-deletion exception,
   not a weakened ban on Islamic feature writes.

The APK value is a size baseline only; Q019 repeats the measurement after dead-code and resource
cleanup. The full test run is repeated at the end of each cleanup group.
