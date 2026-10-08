// gemini-proxy: forwards PixelQuest's AI requests to Google Gemini so the Gemini API key stays on the
// server and never ships inside the app.
//
// This file holds the request handling and has no runtime-specific imports, so the same code runs in
// Supabase's Deno runtime (index.ts) and under Node for the tests (handler.test.ts).
//
// Protection, since the app (and anything in it) is public:
// - only signed-in PixelQuest accounts (Google or an emailed code) can use it: every request carries
//   the account's access token, which is checked with Supabase Auth; anonymous users are refused;
// - the server fixes the model, output limits and JSON output, so the proxy is a poor general chatbot;
// - prompts and system instructions have size limits;
// - every call is claimed against a per-account and a project-wide daily limit before Gemini is called.

export type ClaimResult = "ok" | "account_limit" | "global_limit";

export interface ProxyDeps {
  /** Gemini API key from the function's secrets; undefined or blank when not configured. */
  geminiApiKey: string | undefined;
  /**
   * The signed-in account an access token belongs to: its user id, or null when the token isn't a
   * signed-in user's (expired, made up, the anon key, or an anonymous user). Throws when Supabase
   * Auth can't be asked.
   */
  getUserId: (accessToken: string) => Promise<string | null>;
  /** Claims one call for this account today (see claim_ai_proxy_call in the migrations). */
  claimCall: (accountKey: string) => Promise<ClaimResult>;
  fetchGemini: (url: string, init: RequestInit) => Promise<Response>;
  model?: string;
  /**
   * Operational events for the function's logs: limits reached and failures. They never carry the
   * prompt, the answer, the access token or the account id.
   */
  log?: (event: Record<string, unknown>) => void;
}

export const DEFAULT_MODEL = "gemini-2.5-flash";
export const MAX_PROMPT_CHARS = 6000;
export const MAX_SYSTEM_INSTRUCTION_CHARS = 3000;

const UUID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function json(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
}

/** The bearer token from the Authorization header, or null. */
export function bearerToken(req: Request): string | null {
  const header = req.headers.get("Authorization") ?? "";
  const match = /^Bearer\s+(\S+)$/i.exec(header.trim());
  return match ? match[1] : null;
}

/** The key an account's calls are counted under in ai_proxy_usage. */
export function accountKey(userId: string): string {
  return `user:${userId.toLowerCase()}`;
}

/** The Gemini request body. Only the prompt and system instruction come from the app. */
export function buildGeminiPayload(prompt: string, systemInstruction: string | undefined): string {
  const payload: Record<string, unknown> = {
    contents: [{ role: "user", parts: [{ text: prompt }] }],
    generationConfig: {
      temperature: 0.7,
      maxOutputTokens: 800,
      responseMimeType: "application/json",
      // 2.5 Flash thinking would come out of maxOutputTokens (see AI_INSIGHTS.md).
      thinkingConfig: { thinkingBudget: 0 },
    },
  };
  if (systemInstruction && systemInstruction.trim() !== "") {
    payload.system_instruction = { parts: [{ text: systemInstruction }] };
  }
  return JSON.stringify(payload);
}

export async function handleProxyRequest(req: Request, deps: ProxyDeps): Promise<Response> {
  if (req.method !== "POST") return json(405, { error: "method_not_allowed" });

  const apiKey = deps.geminiApiKey?.trim();
  if (!apiKey) return json(503, { error: "not_configured" });

  // The AI Coach is for signed-in players only. Checked before anything else is read.
  const token = bearerToken(req);
  if (!token) return json(401, { error: "sign_in_required" });
  let userId: string | null;
  try {
    userId = await deps.getUserId(token);
  } catch {
    deps.log?.({ event: "auth_unavailable" });
    return json(503, { error: "auth_unavailable" });
  }
  if (!userId) return json(401, { error: "sign_in_required" });

  let body: unknown;
  try {
    body = await req.json();
  } catch {
    return json(400, { error: "invalid_json" });
  }
  const { prompt, systemInstruction } = (body ?? {}) as { prompt?: unknown; systemInstruction?: unknown };
  if (typeof prompt !== "string" || prompt.trim() === "") return json(400, { error: "missing_prompt" });
  if (systemInstruction !== undefined && systemInstruction !== null && typeof systemInstruction !== "string") {
    return json(400, { error: "invalid_system_instruction" });
  }
  if (prompt.length > MAX_PROMPT_CHARS) return json(413, { error: "prompt_too_long" });
  if (typeof systemInstruction === "string" && systemInstruction.length > MAX_SYSTEM_INSTRUCTION_CHARS) {
    return json(413, { error: "system_instruction_too_long" });
  }

  // Claimed only for well-formed requests, so malformed ones don't use up an account's calls.
  let claim: ClaimResult;
  try {
    claim = await deps.claimCall(accountKey(userId));
  } catch {
    deps.log?.({ event: "usage_unavailable" });
    return json(503, { error: "usage_unavailable" });
  }
  if (claim === "account_limit") {
    deps.log?.({ event: "limit", scope: "account" });
    return json(429, { error: "account_daily_limit" });
  }
  if (claim === "global_limit") {
    deps.log?.({ event: "limit", scope: "global" });
    return json(429, { error: "global_daily_limit" });
  }

  const model = deps.model?.trim() || DEFAULT_MODEL;
  let upstream: Response;
  try {
    upstream = await deps.fetchGemini(
      `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json", "x-goog-api-key": apiKey },
        body: buildGeminiPayload(prompt, typeof systemInstruction === "string" ? systemInstruction : undefined),
      },
    );
  } catch {
    deps.log?.({ event: "upstream_unreachable" });
    return json(502, { error: "upstream_unreachable" });
  }

  if (upstream.status === 200) {
    // Gemini's own response, so the app parses it the same way as a direct call.
    return new Response(await upstream.text(), { status: 200, headers: { "Content-Type": "application/json" } });
  }
  // Gemini's error bodies are not passed on (or logged).
  deps.log?.({ event: "upstream_error", status: upstream.status });
  if (upstream.status === 429) return json(429, { error: "upstream_busy" });
  return json(502, { error: "upstream_error", status: upstream.status });
}

/**
 * getUserId backed by Supabase Auth's /auth/v1/user, which accepts only a valid, unexpired user
 * access token. The anon key and made-up tokens get 401/403 (null). Anonymous users, if the project
 * ever turns them on, are refused too: anyone could make as many as they like.
 */
export function authUserId(
  fetchFn: (url: string, init: RequestInit) => Promise<Response>,
  supabaseUrl: string,
  apiKey: string,
): (accessToken: string) => Promise<string | null> {
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

/**
 * claimCall backed by the claim_ai_proxy_call RPC, called with the service role key. The RPC's
 * first argument is the counter's key (its column is still called device_id); its "device_limit"
 * answer is this account's limit.
 * New-style secret keys (sb_secret_...) are not JWTs, so they go only in the apikey header.
 */
export function rpcClaim(
  fetchFn: (url: string, init: RequestInit) => Promise<Response>,
  supabaseUrl: string,
  serviceKey: string,
  accountLimit: number,
  globalLimit: number,
): (accountKey: string) => Promise<ClaimResult> {
  return async (key: string) => {
    const headers: Record<string, string> = { "Content-Type": "application/json", apikey: serviceKey };
    if (serviceKey.split(".").length === 3) headers.Authorization = `Bearer ${serviceKey}`;
    const res = await fetchFn(`${supabaseUrl.replace(/\/$/, "")}/rest/v1/rpc/claim_ai_proxy_call`, {
      method: "POST",
      headers,
      body: JSON.stringify({ p_device: key, p_device_limit: accountLimit, p_global_limit: globalLimit }),
    });
    if (!res.ok) throw new Error(`claim_ai_proxy_call failed with ${res.status}`);
    const result = await res.json();
    if (result === "ok" || result === "global_limit") return result;
    if (result === "device_limit") return "account_limit";
    throw new Error("unexpected claim result");
  };
}
