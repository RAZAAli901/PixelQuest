// gemini-proxy: forwards PixelQuest's AI requests to Google Gemini so the Gemini API key stays on the
// server and never ships inside the app.
//
// This file holds the request handling and has no runtime-specific imports, so the same code runs in
// Supabase's Deno runtime (index.ts) and under Node for the tests (handler.test.ts).
//
// Protection, since the app (and anything in it) is public:
// - the server fixes the model, output limits and JSON output, so the proxy is a poor general chatbot;
// - prompts and system instructions have size limits;
// - every call is claimed against a per-device and a project-wide daily limit before Gemini is called.

export type ClaimResult = "ok" | "device_limit" | "global_limit";

export interface ProxyDeps {
  /** Gemini API key from the function's secrets; undefined or blank when not configured. */
  geminiApiKey: string | undefined;
  /** Claims one call for this device today (see claim_ai_proxy_call in the migrations). */
  claimCall: (deviceId: string) => Promise<ClaimResult>;
  fetchGemini: (url: string, init: RequestInit) => Promise<Response>;
  model?: string;
}

export const DEFAULT_MODEL = "gemini-2.5-flash";
export const MAX_PROMPT_CHARS = 6000;
export const MAX_SYSTEM_INSTRUCTION_CHARS = 3000;
export const DEVICE_HEADER = "x-pixelquest-device";

const DEVICE_ID = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function json(status: number, body: Record<string, unknown>): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "Content-Type": "application/json" },
  });
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

  const deviceId = req.headers.get(DEVICE_HEADER) ?? "";
  if (!DEVICE_ID.test(deviceId)) return json(400, { error: "invalid_device" });

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

  // Claimed only for well-formed requests, so malformed ones don't use up a device's calls.
  let claim: ClaimResult;
  try {
    claim = await deps.claimCall(deviceId.toLowerCase());
  } catch {
    return json(503, { error: "usage_unavailable" });
  }
  if (claim === "device_limit") return json(429, { error: "device_daily_limit" });
  if (claim === "global_limit") return json(429, { error: "global_daily_limit" });

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
    return json(502, { error: "upstream_unreachable" });
  }

  if (upstream.status === 200) {
    // Gemini's own response, so the app parses it the same way as a direct call.
    return new Response(await upstream.text(), { status: 200, headers: { "Content-Type": "application/json" } });
  }
  // Gemini's error bodies are not passed on.
  if (upstream.status === 429) return json(429, { error: "upstream_busy" });
  return json(502, { error: "upstream_error", status: upstream.status });
}

/**
 * claimCall backed by the claim_ai_proxy_call RPC, called with the service role key.
 * New-style secret keys (sb_secret_...) are not JWTs, so they go only in the apikey header.
 */
export function rpcClaim(
  fetchFn: (url: string, init: RequestInit) => Promise<Response>,
  supabaseUrl: string,
  serviceKey: string,
  deviceLimit: number,
  globalLimit: number,
): (deviceId: string) => Promise<ClaimResult> {
  return async (deviceId: string) => {
    const headers: Record<string, string> = { "Content-Type": "application/json", apikey: serviceKey };
    if (serviceKey.split(".").length === 3) headers.Authorization = `Bearer ${serviceKey}`;
    const res = await fetchFn(`${supabaseUrl.replace(/\/$/, "")}/rest/v1/rpc/claim_ai_proxy_call`, {
      method: "POST",
      headers,
      body: JSON.stringify({ p_device: deviceId, p_device_limit: deviceLimit, p_global_limit: globalLimit }),
    });
    if (!res.ok) throw new Error(`claim_ai_proxy_call failed with ${res.status}`);
    const result = await res.json();
    if (result !== "ok" && result !== "device_limit" && result !== "global_limit") {
      throw new Error("unexpected claim result");
    }
    return result;
  };
}
