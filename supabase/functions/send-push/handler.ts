// send-push: a push to players' phones (docs/chess-design.md): challenge received, challenge
// accepted, draw offered. Only the project itself may call it (the service role key); the chess
// functions send theirs through the same code (_shared/push.ts) as they answer.

import { fromTheServer, isUuid, json } from "../_shared/chess.ts";
import { sendPush, type PushDeps, type PushKind } from "../_shared/push.ts";

const KINDS: readonly PushKind[] = ["challenge_received", "challenge_accepted", "draw_offered"];

export async function handleSendPush(req: Request, serviceKey: string, deps: PushDeps): Promise<Response> {
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });
  if (!fromTheServer(req, serviceKey)) return json(401, { error: "not_allowed" });
  let body: { kind?: unknown; users?: unknown; data?: unknown };
  try {
    body = await req.json();
  } catch {
    return json(400, { error: "bad_request" });
  }
  const { kind, users, data } = body;
  if (typeof kind !== "string" || !(KINDS as readonly string[]).includes(kind)) return json(400, { error: "bad_kind" });
  if (!Array.isArray(users) || users.length === 0 || users.length > 50 || !users.every(isUuid)) return json(400, { error: "bad_request" });
  const ids: Record<string, string> = {};
  for (const [key, value] of Object.entries((data ?? {}) as Record<string, unknown>)) {
    // Ids only: nothing anyone wrote travels in a push.
    if (!/^[a-z_]{2,20}$/.test(key) || !isUuid(value)) return json(400, { error: "bad_request" });
    ids[key] = value;
  }
  const sent = await sendPush(deps, kind as PushKind, users as string[], ids);
  return json(200, { sent });
}
