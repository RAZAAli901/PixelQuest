// finalize-upload: the uploader says their file has gone up (docs/study-chat.md, How a file is
// shared, steps 3 and 4). The server reads what's really stored, not the phone's word for it: it
// must have arrived within 5 minutes of being asked for, be the size declared, and be, by its own
// bytes, the allowed type it claimed to be, with no macros or embedded programs. Then its SHA-256
// is worked out, it's marked ready and its message posted. Anything else is rejected: its bytes
// deleted and its row saying why.

import { MAX_FILE_BYTES, UPLOAD_WINDOW_SECONDS, UUID, bearerToken, json, type RpcResult } from "../_shared/study_files.ts";
import { inspect, sha256, type Rejection } from "../_shared/file_check.ts";

export interface PendingUpload {
  party_id: string;
  storage_path: string;
  mime: string;
  size_bytes: number;
  status: "pending" | "ready" | "rejected";
  created_at: string;
  /** When the stored object arrived; null if nothing's been uploaded yet. */
  uploaded_at: string | null;
}

export interface FinalizeDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  /** chat_upload_for_finalize: the uploader's own file, or not_yours / no_such_file. */
  loadUpload: (user: string, file: string) => Promise<RpcResult<PendingUpload>>;
  readObject: (path: string, maxBytes: number) => Promise<Uint8Array | null>;
  removeObject: (path: string) => Promise<void>;
  /** chat_finalize_upload: ready, and its message posted; the message's id. */
  markReady: (user: string, file: string, mime: string, size: number, sha256: string) => Promise<RpcResult<string>>;
  /** chat_reject_upload: why, for its uploader (a check's reason, or the database's refusal). */
  markRejected: (file: string, reason: string) => Promise<void>;
  log?: (event: Record<string, unknown>) => void;
}

export async function handleFinalize(req: Request, deps: FinalizeDeps): Promise<Response> {
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });
  const token = bearerToken(req);
  const user = token ? await deps.getUserId(token) : null;
  if (!user) return json(401, { error: "sign_in_required" });

  let file: unknown;
  try {
    file = ((await req.json()) as Record<string, unknown> | null)?.file_id;
  } catch {
    return json(400, { error: "bad_request" });
  }
  if (typeof file !== "string" || !UUID.test(file)) return json(400, { error: "bad_request" });

  const loaded = await deps.loadUpload(user, file);
  if (!loaded.ok) return json(loaded.error === "not_yours" ? 403 : 404, { error: loaded.error });
  const upload = loaded.data;
  // Already decided: say so again (a retry after a lost answer), never check twice.
  if (upload.status !== "pending") return json(200, { status: upload.status });
  // Nothing there yet: still on its way, or never sent. The phone may try again.
  if (!upload.uploaded_at) return json(409, { error: "not_uploaded" });

  const reject = async (reason: Rejection | "upload_expired" | "size_mismatch") => {
    await deps.removeObject(upload.storage_path);
    await deps.markRejected(file as string, reason);
    deps.log?.({ event: "upload_rejected", reason });
    return json(200, { status: "rejected", reason });
  };

  const late = Date.parse(upload.uploaded_at) - Date.parse(upload.created_at) > UPLOAD_WINDOW_SECONDS * 1000;
  if (late) return reject("upload_expired");

  const bytes = await deps.readObject(upload.storage_path, MAX_FILE_BYTES);
  if (!bytes) return json(409, { error: "not_uploaded" });
  if (bytes.length > MAX_FILE_BYTES) return reject("too_large");
  if (bytes.length !== upload.size_bytes) return reject(bytes.length === 0 ? "empty" : "size_mismatch");

  const verdict = inspect(bytes, upload.mime, MAX_FILE_BYTES);
  if (!verdict.ok) return reject(verdict.reason);

  const ready = await deps.markReady(user, file, verdict.mime, bytes.length, await sha256(bytes));
  if (!ready.ok) {
    // Its uploader left the party meanwhile, or the party filled up: it isn't kept.
    await deps.removeObject(upload.storage_path);
    await deps.markRejected(file, ready.error);
    return json(200, { status: "rejected", reason: ready.error });
  }
  return json(200, { status: "ready", message_id: ready.data });
}
