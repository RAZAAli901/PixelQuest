// Helpers for tests that run against a local Supabase stack (npx supabase start). They are opt-in:
// set SUPABASE_LOCAL=1, or every test in supabase/tests is skipped. See docs/LOCAL_SUPABASE.md.
//
// Nothing here talks to a hosted project: the URLs and keys come from `supabase status` on this PC.
import { execSync } from "node:child_process";
import { test } from "node:test";

export const LOCAL = process.env.SUPABASE_LOCAL === "1";

/** `test` when the local stack is wanted, `test.skip` otherwise. */
export const localTest: typeof test = LOCAL ? test : (test.skip as typeof test);

export interface Stack {
  apiUrl: string;
  mailUrl: string;
  /** The key the app ships with (publishable, or the legacy anon JWT). */
  publishableKey: string;
  /** Server-side only, like the Edge Function's SUPABASE_SERVICE_ROLE_KEY. */
  serviceKey: string;
}

let cached: Stack | undefined;

export function stack(): Stack {
  if (cached) return cached;
  const fromEnv = process.env.SUPABASE_LOCAL_STATUS_JSON;
  const raw = fromEnv ?? execSync("npx --yes supabase@2.120.0 status -o json", { encoding: "utf8", stdio: ["ignore", "pipe", "ignore"] });
  const s = JSON.parse(raw.slice(raw.indexOf("{")));
  cached = {
    apiUrl: s.API_URL,
    mailUrl: s.MAILPIT_URL ?? s.INBUCKET_URL,
    publishableKey: s.PUBLISHABLE_KEY ?? s.ANON_KEY,
    serviceKey: s.SERVICE_ROLE_KEY,
  };
  return cached;
}

export interface Reply {
  status: number;
  body: any;
  headers: Headers;
}

/** A request through the gateway, as the app makes it: the publishable key, plus a user token if given. */
export async function call(
  path: string,
  options: { method?: string; token?: string; body?: unknown; headers?: Record<string, string>; apiKey?: string } = {},
): Promise<Reply> {
  const s = stack();
  const headers: Record<string, string> = { apikey: options.apiKey ?? s.publishableKey, ...(options.headers ?? {}) };
  if (options.token) headers.Authorization = `Bearer ${options.token}`;
  if (options.body !== undefined) headers["Content-Type"] = "application/json";
  const res = await fetch(`${s.apiUrl}${path}`, {
    method: options.method ?? (options.body === undefined ? "GET" : "POST"),
    headers,
    body: options.body === undefined ? undefined : JSON.stringify(options.body),
  });
  const text = await res.text();
  let body: any = text;
  try {
    body = text ? JSON.parse(text) : null;
  } catch {
    // not JSON
  }
  return { status: res.status, body, headers: res.headers };
}

/** A request with the service role key, which bypasses row-level security (for setup and checks). */
export function asService(path: string, options: { method?: string; body?: unknown; headers?: Record<string, string> } = {}) {
  const s = stack();
  const isJwt = s.serviceKey.split(".").length === 3;
  return call(path, {
    ...options,
    apiKey: s.serviceKey,
    headers: { ...(isJwt ? { Authorization: `Bearer ${s.serviceKey}` } : {}), ...(options.headers ?? {}) },
  });
}

/** A fresh address for each test, so tests don't share accounts or the one-code-a-minute limit. */
export function newEmail(tag: string): string {
  return `${tag}.${Date.now()}.${Math.random().toString(36).slice(2, 8)}@pixelquest.test`;
}

/** The newest email Mailpit caught for [to], waiting up to ~10 s for it to arrive. */
export async function latestEmailTo(to: string): Promise<{ subject: string; text: string; html: string }> {
  const s = stack();
  for (let i = 0; i < 50; i++) {
    const res = await fetch(`${s.mailUrl}/api/v1/search?query=${encodeURIComponent(`to:"${to}"`)}`);
    const list = await res.json();
    const first = list.messages?.[0];
    if (first) {
      const message = await (await fetch(`${s.mailUrl}/api/v1/message/${first.ID}`)).json();
      return { subject: message.Subject, text: message.Text ?? "", html: message.HTML ?? "" };
    }
    await new Promise((r) => setTimeout(r, 200));
  }
  throw new Error(`no email arrived for ${to}`);
}

/** The 6-digit code in an email. */
export function codeIn(email: { text: string; html: string }): string {
  const match = /\b(\d{6})\b/.exec(email.text) ?? /\b(\d{6})\b/.exec(email.html);
  if (!match) throw new Error("no code in the email");
  return match[1];
}

export interface Session {
  accessToken: string;
  userId: string;
  email: string;
}

/** Signs in the way the app does: ask for a code, read it from the email, verify it. */
export async function signInWithEmailCode(email: string): Promise<Session> {
  const sent = await call("/auth/v1/otp", { body: { email, create_user: true } });
  if (sent.status !== 200) throw new Error(`otp ${sent.status} ${JSON.stringify(sent.body)}`);
  const code = codeIn(await latestEmailTo(email));
  const verified = await call("/auth/v1/verify", { body: { type: "email", email, token: code } });
  if (verified.status !== 200) throw new Error(`verify ${verified.status} ${JSON.stringify(verified.body)}`);
  return { accessToken: verified.body.access_token, userId: verified.body.user.id, email };
}
