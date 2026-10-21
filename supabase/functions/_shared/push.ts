// Push for chess (docs/chess-design.md, Threats): Firebase Cloud Messaging's HTTP v1 API with a
// service account kept only in the functions' secrets (FCM_SERVICE_ACCOUNT, its JSON). A push
// carries only a kind and ids, never anyone's words; the phone builds the text and hides it on
// the lock screen. A token Firebase says is gone is deleted. Without the secret, nothing is sent.

import type { Fetch, RpcResult } from "./study_files.ts";

export type PushKind = "challenge_received" | "challenge_accepted" | "draw_offered";

export interface ServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
}

export interface PushDeps {
  fetch: Fetch;
  account: ServiceAccount | null;
  /** push_tokens_for: each user's tokens. */
  tokensFor: (users: string[]) => Promise<RpcResult<{ user_id: string; token: string }[]>>;
  forget: (token: string) => Promise<unknown>;
  now?: () => number;
  log?: (event: Record<string, unknown>) => void;
}

const b64url = (bytes: Uint8Array) =>
  btoa(String.fromCharCode(...bytes)).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
const b64urlText = (text: string) => b64url(new TextEncoder().encode(text));

/** The service account's key, for signing (PKCS#8 PEM). */
async function signingKey(pem: string): Promise<CryptoKey> {
  const body = pem.replace(/-----[^-]+-----/g, "").replace(/\s+/g, "");
  const der = Uint8Array.from(atob(body), (c) => c.charCodeAt(0));
  return crypto.subtle.importKey("pkcs8", der, { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" }, false, ["sign"]);
}

/** A signed JWT asking Google for a token to send with (one hour). */
export async function assertion(account: ServiceAccount, nowSeconds: number): Promise<string> {
  const header = b64urlText(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claims = b64urlText(JSON.stringify({
    iss: account.client_email,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    iat: nowSeconds,
    exp: nowSeconds + 3600,
  }));
  const signature = await crypto.subtle.sign("RSASSA-PKCS1-v1_5", await signingKey(account.private_key), new TextEncoder().encode(`${header}.${claims}`));
  return `${header}.${claims}.${b64url(new Uint8Array(signature))}`;
}

let cached: { token: string; until: number } | null = null;

async function accessToken(deps: PushDeps): Promise<string> {
  const now = deps.now?.() ?? Date.now();
  if (cached && cached.until > now + 60_000) return cached.token;
  const res = await deps.fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({ grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer", assertion: await assertion(deps.account!, Math.floor(now / 1000)) }).toString(),
  });
  if (!res.ok) throw new Error(`token ${res.status}`);
  const body = (await res.json()) as { access_token: string; expires_in: number };
  cached = { token: body.access_token, until: now + body.expires_in * 1000 };
  return body.access_token;
}

/** Only for tests. */
export function forgetCachedToken() {
  cached = null;
}

/** Sends [kind] with [data] (ids only) to each of [users]' phones: how many it reached. */
export async function sendPush(deps: PushDeps, kind: PushKind, users: string[], data: Record<string, string>): Promise<number> {
  if (!deps.account || users.length === 0) return 0;
  const tokens = await deps.tokensFor(users);
  if (!tokens.ok || tokens.data.length === 0) return 0;
  const bearer = await accessToken(deps);
  let sent = 0;
  for (const { token } of tokens.data) {
    const res = await deps.fetch(`https://fcm.googleapis.com/v1/projects/${deps.account.project_id}/messages:send`, {
      method: "POST",
      headers: { Authorization: `Bearer ${bearer}`, "Content-Type": "application/json" },
      body: JSON.stringify({ message: { token, data: { kind, ...data }, android: { priority: "high", ttl: "600s" } } }),
    });
    if (res.ok) {
      sent++;
    } else if (res.status === 404 || res.status === 400) {
      // UNREGISTERED or INVALID_ARGUMENT: the phone let it go.
      await deps.forget(token);
    } else {
      deps.log?.({ event: "push_failed", status: res.status });
    }
  }
  return sent;
}

export type Notify = (kind: PushKind, users: string[], data: Record<string, string>) => void;

/**
 * The functions' notify: each push goes after the answer, kept running by the runtime
 * (EdgeRuntime.waitUntil), and a failure is only logged: a push never fails a move or a challenge.
 */
export function notifier(deps: PushDeps): Notify {
  return (kind, users, data) => {
    const work = sendPush(deps, kind, users, data).catch((e) => deps.log?.({ event: "push_failed", error: String(e) }));
    (globalThis as { EdgeRuntime?: { waitUntil?: (p: Promise<unknown>) => void } }).EdgeRuntime?.waitUntil?.(work);
  };
}

/** A notify for a function's index.ts, from its environment: the service role and FCM_SERVICE_ACCOUNT. */
export function pushFromEnv(env: (name: string) => string | undefined, rpc: <T>(fn: string, args: Record<string, unknown>) => Promise<RpcResult<T>>): Notify {
  return notifier({
    fetch,
    account: serviceAccountOf(env("FCM_SERVICE_ACCOUNT")),
    tokensFor: (users) => rpc("push_tokens_for", { p_users: users }),
    forget: (token) => rpc("push_forget_token", { p_token: token }),
    log: (event) => console.log(JSON.stringify(event)),
  });
}

/** The service account from FCM_SERVICE_ACCOUNT, or null when it isn't set (or isn't one). */
export function serviceAccountOf(json: string | undefined): ServiceAccount | null {
  if (!json) return null;
  try {
    const parsed = JSON.parse(json) as Partial<ServiceAccount>;
    return parsed.project_id && parsed.client_email && parsed.private_key ? (parsed as ServiceAccount) : null;
  } catch {
    return null;
  }
}
