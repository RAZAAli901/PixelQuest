// Letting go of study chat files that shouldn't be kept (docs/study-chat.md): pending uploads
// never finished within an hour are rejected, and the bytes of rejected and deleted files, and of
// objects no file row names, are deleted from study-files (chat_expire_uploads, chat_mark_purged).

import type { RpcResult } from "./study_files.ts";

export interface CleanupDeps {
  rpc: <T>(fn: string, args: Record<string, unknown>) => Promise<RpcResult<T>>;
  remove: (paths: string[]) => Promise<void>;
}

/** One round, of up to [limit] files; how many were let go. */
export async function cleanUp(deps: CleanupDeps, limit = 100): Promise<number> {
  const due = await deps.rpc<string[]>("chat_expire_uploads", { p_limit: limit });
  if (!due.ok || due.data.length === 0) return 0;
  await deps.remove(due.data);
  await deps.rpc("chat_mark_purged", { p_paths: due.data });
  return due.data.length;
}
