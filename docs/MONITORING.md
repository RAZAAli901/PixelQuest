# Monitoring the cloud

What the Edge Functions log, how a spike in errors is noticed, and where to look (J175).

## The functions' logs

Each function writes one JSON line per event through `supabase/functions/_shared/log.ts`, for example:

```json
{"fn":"chess-move","event":"chess_move","ply":31,"ended":"checkmate"}
{"fn":"send-push","event":"push_failed","status":500}
```

What a line may hold:
- the function's name and the event;
- a reason token (`too_large`, `rate_limited`), an HTTP status, a count or a ply.

What it never holds:
- user, game or party ids;
- names, emails, message text or file names;
- tokens or keys.

Any text that does reach a log goes through `scrub` first, and an error's message above all, since it can echo a database row. `scrub` replaces ids, emails, long tokens and long numbers, and cuts the text to 200 characters. `log.test.ts` checks this.

Failures are written at error level. That covers every event ending in `_failed`, `_error`, `_unavailable` or `_unreachable`, so the dashboard's filter for errors finds them.

| Function | Events |
|---|---|
| `gemini-proxy` | `limit` (account or global), `auth_unavailable`, `usage_unavailable`, `upstream_unreachable`, `upstream_error` |
| `request-upload`, `finalize-upload` | `upload_refused`, `upload_rejected` (a reason), `cleanup_failed` |
| `chess-*` | `chess_move` (ply, how it ended), `challenge_refused` (a reason), `chess_failed`, `chess_sweep` (counts) |
| `send-push` | `push_failed` (a status) |

Read them in the Supabase dashboard: **Edge Functions → (function) → Logs**, or **Logs → Edge Functions** for all of them.

## Alerts

`.github/workflows/monitor.yml` runs every hour at :17. It runs `supabase/functions/_monitor/function_errors.ts`, which asks the Management API for the last hour's requests by function and status. Only counts are asked for and printed.

It fails when a function had a spike:
- 20 or more server errors (5xx); or
- 5 or more server errors that were at least 5% of its requests.

A failed run makes GitHub email the repository's owner. Its log names the function, and the function's own logs in the dashboard say why. Refusals (4xx: limits, bad input) are not errors.

### Turning it on

Until two repository secrets exist, the check passes without reading anything. Add them under **GitHub → Settings → Secrets and variables → Actions**:

| Secret | Value |
|---|---|
| `SUPABASE_ACCESS_TOKEN` | A personal access token from **supabase.com → Account → Access Tokens**. Name it "monitor"; it can be revoked at any time. |
| `SUPABASE_PROJECT_REF` | The project's reference, the part before `.supabase.co` in its URL. |

Then run it once by hand (**Actions → Edge Function error check → Run workflow**) to see it read the logs. If the token or reference is wrong, the run fails and says it couldn't read the logs.

### Other alerts worth turning on

These are in the Supabase dashboard and need no code:
- **Settings → Billing → Usage**: email when the project nears its plan's limits (database size, storage, function calls, Realtime connections).
- **Reports → Database**: CPU and connections. Watch these after a release.
- **Authentication → Rate limits**: these are what keep sign-in email from being abused; leave them on.

## Not covered

- **Database errors outside the functions.** A PostgREST call from the app that fails shows up in **Logs → API**. Nothing alerts on these yet.
- **The phone's own crashes.** The app has no crash-reporting SDK (it sends no analytics), so a crash is only known when a player says so.
