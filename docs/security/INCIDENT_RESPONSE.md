# Incident response

Reviewed 2026-10-22 (S069). Keep this runbook with the release owner; do not put keys, tokens,
user data, screenshots containing them, or attacker payloads in an issue or commit.

## First hour

1. Preserve evidence: record UTC time, reporter, affected feature, release version and request IDs.
   Do not copy credentials or private player data into the report.
2. Contain: disable the affected Edge Function, revoke the relevant API key or Supabase key, or
   disable the affected provider. Do not run destructive SQL as a first response.
3. Assess: use Supabase logs, GitHub Actions and Firebase delivery errors to determine the affected
   data class, accounts and time window. Treat a suspected RLS or storage failure as high severity.
4. Notify the release owner and decide whether the service must remain disabled while a fix is made.

## Remediation and communication

1. Fix with a test that reproduces the failure; review migration and function changes locally first.
2. Deploy only the narrow fix, validate it with a non-production account, and record the deployed
   version and verification result.
3. Rotate exposed keys using `ROTATE.md`, invalidate sessions where appropriate, and remove leaked
   files or rows through an approved, auditable operation.
4. Notify affected people and any required authority using the applicable local legal requirements.
   State what happened, what data was involved, what was fixed, and what users should do next.

## Afterward

Write a blameless post-incident note: timeline, impact, root cause, detection gap, permanent fix,
owner and due date. Add a regression test or release-gate check before closing it.

## Contacts and access

The owner keeps current production contacts, Play Console access, Supabase project ownership,
Firebase ownership and signing-key recovery details outside the repository. Verify this list before
each release; it is intentionally not stored here.
