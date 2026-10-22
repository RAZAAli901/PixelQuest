// The hourly error-spike check (J175, docs/MONITORING.md), run by .github/workflows/monitor.yml.
// It reads the last hour of the Edge Functions' request logs from the Supabase Management API
// (counts by function and status only: nothing personal is asked for or printed) and fails, so
// GitHub emails the owner, when a function's server errors spike.
//
//   SUPABASE_ACCESS_TOKEN=... SUPABASE_PROJECT_REF=... node supabase/functions/_monitor/function_errors.ts
//
// Without those two settings it says so and passes: nothing is checked until they're added.

export type Row = { path: string; status: number; n: number };
export type Verdict = { fn: string; requests: number; errors: number; alert: boolean };

/** A spike: at least this many 5xx answers in the hour... */
export const MIN_ERRORS = 5;
/** ...that are at least this share of the function's requests, or this many whatever the share. */
export const MIN_SHARE = 0.05;
export const ALWAYS = 20;

export const QUERY = `select req.pathname as path, r.status_code as status, count(*) as n
from function_edge_logs
cross join unnest(metadata) as m
cross join unnest(m.response) as r
cross join unnest(m.request) as req
group by path, status`;

/** The function a request path is for ("/functions/v1/chess-move" → "chess-move"). */
export function functionOf(path: string): string {
  const parts = path.split("/").filter(Boolean);
  const at = parts.indexOf("v1");
  return (at >= 0 ? parts[at + 1] : parts[parts.length - 1]) ?? "?";
}

/** Each function's hour: requests, server errors, and whether that's a spike. */
export function judge(rows: Row[]): Verdict[] {
  const byFn = new Map<string, { requests: number; errors: number }>();
  for (const row of rows) {
    const fn = functionOf(row.path);
    const seen = byFn.get(fn) ?? { requests: 0, errors: 0 };
    seen.requests += Number(row.n);
    if (Number(row.status) >= 500) seen.errors += Number(row.n);
    byFn.set(fn, seen);
  }
  return [...byFn.entries()]
    .map(([fn, { requests, errors }]) => ({
      fn, requests, errors,
      alert: errors >= ALWAYS || (errors >= MIN_ERRORS && errors / requests >= MIN_SHARE),
    }))
    .sort((a, b) => b.errors - a.errors || a.fn.localeCompare(b.fn));
}

async function main(): Promise<number> {
  const token = process.env.SUPABASE_ACCESS_TOKEN ?? "";
  const ref = process.env.SUPABASE_PROJECT_REF ?? "";
  if (!token || !ref) {
    console.log("Not configured: add SUPABASE_ACCESS_TOKEN and SUPABASE_PROJECT_REF as repository secrets (docs/MONITORING.md).");
    return 0;
  }
  const end = new Date();
  const start = new Date(end.getTime() - 60 * 60 * 1000);
  const url = new URL(`https://api.supabase.com/v1/projects/${ref}/analytics/endpoints/logs.all`);
  url.searchParams.set("sql", QUERY);
  url.searchParams.set("iso_timestamp_start", start.toISOString());
  url.searchParams.set("iso_timestamp_end", end.toISOString());
  const res = await fetch(url, { headers: { Authorization: `Bearer ${token}` } });
  const body = await res.json().catch(() => ({}));
  if (!res.ok || body.error) {
    console.error(`Couldn't read the function logs (HTTP ${res.status}). Check the token and project ref.`);
    return 1;
  }
  const verdicts = judge((body.result ?? []) as Row[]);
  for (const v of verdicts) {
    console.log(`${v.alert ? "SPIKE" : "ok   "} ${v.fn}: ${v.errors} server errors in ${v.requests} requests`);
  }
  const spikes = verdicts.filter((v) => v.alert);
  if (spikes.length > 0) {
    console.error(`Error spike in ${spikes.map((v) => v.fn).join(", ")}. See the function's logs in the Supabase dashboard.`);
    return 1;
  }
  console.log(verdicts.length ? "No spikes in the last hour." : "No function requests in the last hour.");
  return 0;
}

if (import.meta.url === `file://${process.argv[1]?.replace(/\\/g, "/").replace(/^(?=[A-Za-z]:)/, "/")}`) {
  process.exitCode = await main();
}
