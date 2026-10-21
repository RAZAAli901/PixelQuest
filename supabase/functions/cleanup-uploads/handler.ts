// cleanup-uploads: the study chat's files let go on a schedule (docs/study-chat.md). Only the
// project itself may call it: its caller must hold the service role key. request-upload also runs
// a small round each time it's asked, so nothing waits on the schedule being set up.

import { bearerToken, json } from "../_shared/study_files.ts";
import { cleanUp, type CleanupDeps } from "../_shared/cleanup.ts";

/** Compares without leaking, by timing, how much of a guess was right. */
export function sameSecret(a: string, b: string): boolean {
  if (a.length !== b.length || a.length === 0) return false;
  let diff = 0;
  for (let i = 0; i < a.length; i++) diff |= a.charCodeAt(i) ^ b.charCodeAt(i);
  return diff === 0;
}

export async function handleCleanup(req: Request, serviceKey: string, deps: CleanupDeps): Promise<Response> {
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });
  const given = bearerToken(req) ?? req.headers.get("apikey") ?? "";
  if (!sameSecret(given, serviceKey)) return json(401, { error: "not_allowed" });
  let total = 0;
  // A few rounds at most per call: a long backlog is worked through over several.
  for (let round = 0; round < 5; round++) {
    const done = await cleanUp(deps, 200);
    total += done;
    if (done < 200) break;
  }
  return json(200, { purged: total });
}
