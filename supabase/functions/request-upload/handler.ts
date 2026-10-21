// request-upload: a member asks to share a file in their study party's chat (docs/study-chat.md,
// How a file is shared, step 1). Nothing the phone says is trusted beyond being a request: the
// account is checked with Supabase Auth, the party membership, type, size, the party's storage and
// the member's uploads today by the database (chat_request_upload), and the name is cleaned here.
// The answer is a pending file and a signed upload URL for one server-chosen path, honoured for
// 5 minutes (finalize-upload refuses what arrives later).

import {
  ALLOWED_TYPES,
  MAX_FILE_BYTES,
  UPLOAD_WINDOW_SECONDS,
  UUID,
  bearerToken,
  cleanName,
  json,
  nameFitsType,
  type RpcResult,
} from "../_shared/study_files.ts";

export interface RequestUploadDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  /** chat_request_upload: the pending row, or the token it was refused with. */
  requestRow: (user: string, party: string, name: string, mime: string, size: number) => Promise<RpcResult<{ file_id: string; storage_path: string }>>;
  signUpload: (path: string) => Promise<string>;
  log?: (event: Record<string, unknown>) => void;
}

/** A refusal's HTTP status: who you are, what you sent, or how much. */
const STATUS: Record<string, number> = {
  not_a_member: 403,
  too_large: 413,
  party_storage_full: 413,
  too_many_uploads: 429,
};

export async function handleRequestUpload(req: Request, deps: RequestUploadDeps): Promise<Response> {
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });
  const token = bearerToken(req);
  const user = token ? await deps.getUserId(token) : null;
  if (!user) return json(401, { error: "sign_in_required" });

  let body: Record<string, unknown>;
  try {
    const parsed = await req.json();
    if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) throw new Error("not an object");
    body = parsed as Record<string, unknown>;
  } catch {
    return json(400, { error: "bad_request" });
  }
  const { party_id: party, name: rawName, mime, size } = body;
  if (typeof party !== "string" || !UUID.test(party)) return json(400, { error: "bad_request" });
  if (typeof rawName !== "string" || rawName.length > 1000) return json(400, { error: "bad_name" });
  if (typeof mime !== "string" || !(mime in ALLOWED_TYPES)) return json(400, { error: "bad_type" });
  if (typeof size !== "number" || !Number.isInteger(size) || size < 1) return json(400, { error: "bad_request" });
  if (size > MAX_FILE_BYTES) return json(413, { error: "too_large" });
  const name = cleanName(rawName);
  if (!name) return json(400, { error: "bad_name" });
  // The name's extension must be the type's: a .exe declared as a PDF goes no further.
  if (!nameFitsType(name, mime)) return json(400, { error: "bad_type" });

  const row = await deps.requestRow(user, party.toLowerCase(), name, mime, size);
  if (!row.ok) {
    deps.log?.({ event: "upload_refused", reason: row.error });
    return json(STATUS[row.error] ?? 400, { error: row.error });
  }
  const url = await deps.signUpload(row.data.storage_path);
  return json(200, { file_id: row.data.file_id, upload_url: url, display_name: name, expires_in: UPLOAD_WINDOW_SECONDS });
}
