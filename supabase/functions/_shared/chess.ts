// What the chess functions share (docs/chess-design.md): the caller's sign-in, reading a request,
// the refusal tokens and their HTTP statuses, and the rules, which are chess.js's (no home-made move
// generator). The same code runs in Supabase's Deno runtime (chess.js through each function's
// deno.json) and under Node for the tests (supabase/functions/package.json).

import { Chess } from "chess.js";
import { UUID, bearerToken, json } from "./study_files.ts";

export { Chess, UUID, json };

export const START_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1";

/** The time controls the server allows (chess_time_controls), in seconds a side. */
export const TIME_CONTROLS: readonly number[] = [60, 180, 300, 600];

/** Each refusal's HTTP status: who you are, what you asked, or when. Anything else is a 400. */
export const STATUS: Readonly<Record<string, number>> = {
  sign_in_required: 401,
  not_a_member: 403,
  not_a_player: 403,
  blocked: 403,
  not_found: 404,
  not_your_turn: 409,
  wrong_ply: 409,
  game_over: 409,
  already_playing: 409,
  already_answered: 409,
  no_draw_offer: 409,
  not_timed_out: 409,
  expired: 410,
  illegal_move: 422,
  rate_limited: 429,
};

export function refuse(token: string, extra: Record<string, unknown> = {}): Response {
  return json(STATUS[token] ?? 400, { error: token, ...extra });
}

/** The signed-in caller's id, or null. */
export async function callerOf(req: Request, getUserId: (token: string) => Promise<string | null>): Promise<string | null> {
  const token = bearerToken(req);
  return token ? await getUserId(token) : null;
}

/** The request's JSON object, or null when it isn't one. */
export async function bodyOf(req: Request): Promise<Record<string, unknown> | null> {
  try {
    const parsed = await req.json();
    return parsed && typeof parsed === "object" && !Array.isArray(parsed) ? (parsed as Record<string, unknown>) : null;
  } catch {
    return null;
  }
}

export const isUuid = (v: unknown): v is string => typeof v === "string" && UUID.test(v);

/** A move as sent: from, to and an optional promotion piece, e.g. "e2e4" or "e7e8q". */
export const UCI = /^([a-h][1-8])([a-h][1-8])([qrbn])?$/;

/**
 * Every POST function's frame: the method, the caller's sign-in and a JSON body, then [work].
 * A thrown error is logged and answered as a 500 that says nothing about it.
 */
export async function chessRequest(
  req: Request,
  getUserId: (token: string) => Promise<string | null>,
  work: (user: string, body: Record<string, unknown>) => Promise<Response>,
  log?: (event: Record<string, unknown>) => void,
): Promise<Response> {
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });
  const user = await callerOf(req, getUserId);
  if (!user) return refuse("sign_in_required");
  const body = await bodyOf(req);
  if (!body) return refuse("bad_request");
  try {
    return await work(user, body);
  } catch (e) {
    log?.({ event: "chess_failed", error: String(e) });
    return json(500, { error: "server_error" });
  }
}
