// What the study chat's file functions share (docs/study-chat.md): the limits and allowed types
// (mirroring study_chat_config and study_chat_types), cleaning a file's shown name, and talking to
// Supabase with the service role. No runtime-specific imports: the same code runs in Supabase's
// Deno runtime and under Node for the tests.

export const MAX_FILE_BYTES = 20 * 1024 * 1024;
export const NAME_MAX_CHARS = 120;
/** How long a signed upload URL is honoured: an upload that arrives later is rejected (finalize-upload). */
export const UPLOAD_WINDOW_SECONDS = 5 * 60;
/** How long a download URL lasts. */
export const DOWNLOAD_URL_SECONDS = 60;
/** A pending upload never finished is deleted after this. */
export const PENDING_MAX_SECONDS = 60 * 60;

/** The allowed types and their extensions (study_chat_types): nothing else is ever stored. */
export const ALLOWED_TYPES: Readonly<Record<string, readonly string[]>> = {
  "application/pdf": ["pdf"],
  "application/msword": ["doc"],
  "application/vnd.openxmlformats-officedocument.wordprocessingml.document": ["docx"],
  "application/vnd.ms-excel": ["xls"],
  "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet": ["xlsx"],
  "application/vnd.ms-powerpoint": ["ppt"],
  "application/vnd.openxmlformats-officedocument.presentationml.presentation": ["pptx"],
  "text/plain": ["txt"],
  "text/csv": ["csv"],
  "image/png": ["png"],
  "image/jpeg": ["jpg", "jpeg"],
};

export const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

/** A file name's extension, lower case, or "" when it has none. */
export function extensionOf(name: string): string {
  const dot = name.lastIndexOf(".");
  return dot > 0 && dot < name.length - 1 ? name.slice(dot + 1).toLowerCase() : "";
}

/**
 * The name shown for an upload: only its last part (no folders), without path, reserved or
 * control characters, spaces tidied, and cut to 120 characters keeping its extension. Null if
 * nothing's left of it.
 */
export function cleanName(raw: string): string | null {
  const last = raw.split(/[\\/]/).pop() ?? "";
  let name = last
    // deno-lint-ignore no-control-regex
    .replace(/[\u0000-\u001f\u007f-\u009f​-‏‪-‮⁦-⁩﻿]/g, "")
    .replace(/[:*?"<>|]/g, "")
    .replace(/\s+/g, " ")
    .trim()
    .replace(/^\.+/, "")
    .trim();
  if (name.length > NAME_MAX_CHARS) {
    const ext = extensionOf(name);
    const keep = ext && ext.length < 10 ? `.${ext}` : "";
    name = name.slice(0, NAME_MAX_CHARS - keep.length).trimEnd() + keep;
  }
  return name === "" || name === "." || name === ".." ? null : name;
}

/** Whether [name]'s extension is one of [mime]'s. */
export function nameFitsType(name: string, mime: string): boolean {
  return (ALLOWED_TYPES[mime] ?? []).includes(extensionOf(name));
}

export function json(status: number, body: unknown, headers: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(body), { status, headers: { "Content-Type": "application/json", ...headers } });
}

/** The bearer token from the Authorization header, or null. */
export function bearerToken(req: Request): string | null {
  const match = /^Bearer\s+(\S+)$/i.exec((req.headers.get("Authorization") ?? "").trim());
  return match ? match[1] : null;
}

export type Fetch = (url: string, init?: RequestInit) => Promise<Response>;

/**
 * The signed-in account an access token belongs to (its id), or null for anything that isn't a
 * signed-in, non-anonymous account. Asked of Supabase Auth, as gemini-proxy does.
 */
export function authUserId(fetchFn: Fetch, supabaseUrl: string, apiKey: string): (accessToken: string) => Promise<string | null> {
  return async (accessToken: string) => {
    const res = await fetchFn(`${supabaseUrl.replace(/\/$/, "")}/auth/v1/user`, {
      method: "GET",
      headers: { apikey: apiKey, Authorization: `Bearer ${accessToken}` },
    });
    if (res.status === 401 || res.status === 403) return null;
    if (!res.ok) throw new Error(`auth user lookup failed with ${res.status}`);
    const user = (await res.json()) as { id?: unknown; is_anonymous?: unknown } | null;
    if (!user || user.is_anonymous === true) return null;
    return typeof user.id === "string" && UUID.test(user.id) ? user.id : null;
  };
}

/** The service role's headers: new-style secret keys aren't JWTs, so they go only as the apikey. */
export function serviceHeaders(serviceKey: string, extra: Record<string, string> = {}): Record<string, string> {
  const headers: Record<string, string> = { apikey: serviceKey, ...extra };
  if (serviceKey.split(".").length === 3) headers.Authorization = `Bearer ${serviceKey}`;
  return headers;
}

/** What a database function said: its answer, or the token it refused with. */
export type RpcResult<T> = { ok: true; data: T } | { ok: false; error: string };

/** Calls a database function with the service role; a refusal comes back as its token. */
export function rpcCaller(fetchFn: Fetch, supabaseUrl: string, serviceKey: string) {
  return async <T>(fn: string, args: Record<string, unknown>): Promise<RpcResult<T>> => {
    const res = await fetchFn(`${supabaseUrl.replace(/\/$/, "")}/rest/v1/rpc/${fn}`, {
      method: "POST",
      headers: serviceHeaders(serviceKey, { "Content-Type": "application/json" }),
      body: JSON.stringify(args),
    });
    const text = await res.text();
    let body: unknown = null;
    try {
      body = text ? JSON.parse(text) : null;
    } catch {
      body = text;
    }
    if (res.ok) return { ok: true, data: body as T };
    if (res.status >= 500) throw new Error(`${fn} failed with ${res.status}`);
    return { ok: false, error: refusalOf(body) };
  };
}

/** A database refusal's token: the function's own (its message), or the rule a row broke. */
export function refusalOf(body: unknown): string {
  const error = (body ?? {}) as { message?: unknown };
  const message = typeof error.message === "string" ? error.message : "";
  if (/^[a-z_]{2,40}$/.test(message)) return message;
  const rule = /constraint "([a-z_]+)"/.exec(message)?.[1];
  switch (rule) {
    case "chat_file_size":
      return "too_large";
    case "chat_file_mime":
      return "bad_type";
    case "chat_file_name":
      return "bad_name";
    default:
      return "refused";
  }
}

/** Study-files' storage, with the service role: signing, reading and deleting by path. */
export function storage(fetchFn: Fetch, supabaseUrl: string, serviceKey: string) {
  const base = `${supabaseUrl.replace(/\/$/, "")}/storage/v1`;
  const bucket = "study-files";
  return {
    /** A signed upload URL for one path, which can't overwrite what's there. */
    async signUpload(path: string): Promise<string> {
      const res = await fetchFn(`${base}/object/upload/sign/${bucket}/${path}`, {
        method: "POST",
        headers: serviceHeaders(serviceKey, { "Content-Type": "application/json", "x-upsert": "false" }),
        body: "{}",
      });
      if (!res.ok) throw new Error(`sign upload failed with ${res.status}`);
      const { url } = (await res.json()) as { url: string };
      return `${base}${url.startsWith("/") ? "" : "/"}${url}`;
    },
    /** The stored object's bytes, or null if there's none (never uploaded). */
    async read(path: string, maxBytes: number): Promise<Uint8Array | null> {
      const res = await fetchFn(`${base}/object/${bucket}/${path}`, { method: "GET", headers: serviceHeaders(serviceKey) });
      if (res.status === 400 || res.status === 404) return null;
      if (!res.ok) throw new Error(`read failed with ${res.status}`);
      const declared = Number(res.headers.get("Content-Length"));
      if (Number.isFinite(declared) && declared > maxBytes) return new Uint8Array(maxBytes + 1);
      const bytes = new Uint8Array(await res.arrayBuffer());
      return bytes;
    },
    async remove(paths: string[]): Promise<void> {
      if (paths.length === 0) return;
      const res = await fetchFn(`${base}/object/${bucket}`, {
        method: "DELETE",
        headers: serviceHeaders(serviceKey, { "Content-Type": "application/json" }),
        body: JSON.stringify({ prefixes: paths }),
      });
      if (!res.ok && res.status !== 404) throw new Error(`delete failed with ${res.status}`);
    },
    /** A download URL for [seconds], served as an attachment named [name]. */
    async signDownload(path: string, seconds: number, name: string): Promise<string> {
      const res = await fetchFn(`${base}/object/sign/${bucket}/${path}`, {
        method: "POST",
        headers: serviceHeaders(serviceKey, { "Content-Type": "application/json" }),
        body: JSON.stringify({ expiresIn: seconds }),
      });
      if (!res.ok) throw new Error(`sign download failed with ${res.status}`);
      const { signedURL } = (await res.json()) as { signedURL: string };
      const url = `${base}${signedURL.startsWith("/") ? "" : "/"}${signedURL}`;
      return `${url}${url.includes("?") ? "&" : "?"}download=${encodeURIComponent(name)}`;
    },
  };
}

export type Storage = ReturnType<typeof storage>;
