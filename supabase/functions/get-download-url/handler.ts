// get-download-url: a member opens a file shared in their study party (docs/study-chat.md, How a
// file is shared, step 5). Membership is checked again, now, so a member who left or was removed
// gets nothing; the answer is a signed URL that lasts 60 seconds and serves the file as an
// attachment under its cleaned name, never inline.

import { DOWNLOAD_URL_SECONDS, UUID, bearerToken, json, type RpcResult } from "../_shared/study_files.ts";

export interface DownloadFile {
  storage_path: string;
  display_name: string;
  mime: string;
  size_bytes: number;
}

export interface DownloadDeps {
  getUserId: (accessToken: string) => Promise<string | null>;
  /** chat_file_for_download: a ready file in a party the caller is an active member of, or not_allowed. */
  loadFile: (user: string, file: string) => Promise<RpcResult<DownloadFile>>;
  signDownload: (path: string, seconds: number, name: string) => Promise<string>;
}

export async function handleDownload(req: Request, deps: DownloadDeps): Promise<Response> {
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

  const found = await deps.loadFile(user, file);
  // The same answer whether it doesn't exist or isn't theirs: nothing to learn by guessing ids.
  if (!found.ok) return json(403, { error: "not_allowed" });
  const { storage_path, display_name, mime, size_bytes } = found.data;
  const url = await deps.signDownload(storage_path, DOWNLOAD_URL_SECONDS, display_name);
  return json(200, { url, expires_in: DOWNLOAD_URL_SECONDS, name: display_name, mime, size_bytes }, { "Cache-Control": "no-store" });
}
