// Run with: node --test "supabase/functions/**/*.test.ts"   (Node 22.18+ runs the TypeScript directly)
import { test } from "node:test";
import assert from "node:assert/strict";
import {
  type ClaimResult,
  DEVICE_HEADER,
  MAX_PROMPT_CHARS,
  handleProxyRequest,
  rpcClaim,
} from "./handler.ts";

const KEY = "test-gemini-key-123";
const DEVICE = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const GEMINI_OK = JSON.stringify({ candidates: [{ content: { parts: [{ text: '{"reply":"hello"}' }] } }] });

interface Recorded {
  claims: string[];
  geminiCalls: { url: string; init: RequestInit }[];
}

function deps(options: {
  key?: string | undefined;
  claim?: ClaimResult | Error;
  gemini?: Response | Error;
} = {}) {
  const recorded: Recorded = { claims: [], geminiCalls: [] };
  const d = {
    geminiApiKey: "key" in options ? options.key : KEY,
    claimCall: async (deviceId: string) => {
      recorded.claims.push(deviceId);
      if (options.claim instanceof Error) throw options.claim;
      return options.claim ?? "ok";
    },
    fetchGemini: async (url: string, init: RequestInit) => {
      recorded.geminiCalls.push({ url, init });
      if (options.gemini instanceof Error) throw options.gemini;
      return options.gemini ?? new Response(GEMINI_OK, { status: 200 });
    },
  };
  return { d, recorded };
}

function request(body: unknown, headers: Record<string, string> = { [DEVICE_HEADER]: DEVICE }, method = "POST") {
  return new Request("https://example.supabase.co/functions/v1/gemini-proxy", {
    method,
    headers: { "Content-Type": "application/json", ...headers },
    body: method === "POST" ? (typeof body === "string" ? body : JSON.stringify(body)) : undefined,
  });
}

test("a valid request is claimed, forwarded with the key in a header, and Gemini's answer returned", async () => {
  const { d, recorded } = deps();
  const res = await handleProxyRequest(request({ prompt: "How am I doing?", systemInstruction: "Reply in JSON" }), d);

  assert.equal(res.status, 200);
  assert.equal(await res.text(), GEMINI_OK);
  assert.deepEqual(recorded.claims, [DEVICE]);

  const call = recorded.geminiCalls[0];
  assert.ok(call.url.endsWith("/models/gemini-2.5-flash:generateContent"));
  assert.ok(!call.url.includes(KEY), "the key never goes in the URL");
  assert.equal((call.init.headers as Record<string, string>)["x-goog-api-key"], KEY);

  const sent = JSON.parse(call.init.body as string);
  assert.equal(sent.contents[0].parts[0].text, "How am I doing?");
  assert.equal(sent.system_instruction.parts[0].text, "Reply in JSON");
  assert.equal(sent.generationConfig.maxOutputTokens, 800);
  assert.equal(sent.generationConfig.thinkingConfig.thinkingBudget, 0);
  assert.equal(sent.generationConfig.responseMimeType, "application/json");
});

test("the app cannot choose the model or the output limits", async () => {
  const { d, recorded } = deps();
  await handleProxyRequest(
    request({ prompt: "hi", model: "gemini-2.5-pro", generationConfig: { maxOutputTokens: 60000 } }),
    d,
  );
  const call = recorded.geminiCalls[0];
  assert.ok(call.url.includes("gemini-2.5-flash"));
  assert.equal(JSON.parse(call.init.body as string).generationConfig.maxOutputTokens, 800);
});

test("without a configured key nothing is claimed or forwarded", async () => {
  for (const key of [undefined, "", "   "]) {
    const { d, recorded } = deps({ key });
    const res = await handleProxyRequest(request({ prompt: "hi" }), d);
    assert.equal(res.status, 503);
    assert.deepEqual(await res.json(), { error: "not_configured" });
    assert.equal(recorded.claims.length + recorded.geminiCalls.length, 0);
  }
});

test("malformed requests are refused before a call is claimed", async () => {
  const cases: [Request, number, string][] = [
    [request({ prompt: "hi" }, {}, "GET"), 405, "method_not_allowed"],
    [request({ prompt: "hi" }, {}), 400, "invalid_device"],
    [request({ prompt: "hi" }, { [DEVICE_HEADER]: "not-a-uuid" }), 400, "invalid_device"],
    [request({ prompt: "hi" }, { [DEVICE_HEADER]: "*" }), 400, "invalid_device"],
    [request("{not json"), 400, "invalid_json"],
    [request({}), 400, "missing_prompt"],
    [request({ prompt: "   " }), 400, "missing_prompt"],
    [request({ prompt: 42 }), 400, "missing_prompt"],
    [request({ prompt: "hi", systemInstruction: 7 }), 400, "invalid_system_instruction"],
    [request({ prompt: "x".repeat(MAX_PROMPT_CHARS + 1) }), 413, "prompt_too_long"],
    [request({ prompt: "hi", systemInstruction: "x".repeat(3001) }), 413, "system_instruction_too_long"],
  ];
  for (const [req, status, error] of cases) {
    const { d, recorded } = deps();
    const res = await handleProxyRequest(req, d);
    assert.equal(res.status, status, error);
    assert.deepEqual(await res.json(), { error });
    assert.equal(recorded.claims.length, 0, `${error} must not claim a call`);
  }
});

test("device ids are claimed in lower case, so case changes don't reset the limit", async () => {
  const { d, recorded } = deps();
  await handleProxyRequest(request({ prompt: "hi" }, { [DEVICE_HEADER]: DEVICE.toUpperCase() }), d);
  assert.deepEqual(recorded.claims, [DEVICE]);
});

test("daily limits answer 429 without calling Gemini", async () => {
  for (const [claim, error] of [["device_limit", "device_daily_limit"], ["global_limit", "global_daily_limit"]] as const) {
    const { d, recorded } = deps({ claim });
    const res = await handleProxyRequest(request({ prompt: "hi" }), d);
    assert.equal(res.status, 429);
    assert.deepEqual(await res.json(), { error });
    assert.equal(recorded.geminiCalls.length, 0);
  }
});

test("if usage can't be recorded, Gemini is not called", async () => {
  const { d, recorded } = deps({ claim: new Error("database down") });
  const res = await handleProxyRequest(request({ prompt: "hi" }), d);
  assert.equal(res.status, 503);
  assert.equal(recorded.geminiCalls.length, 0);
});

test("Gemini failures are mapped, and Gemini's error bodies are not passed on", async () => {
  const secretBody = JSON.stringify({ error: { message: `API key ${KEY} not valid` } });
  const cases: [Response | Error, number, Record<string, unknown>][] = [
    [new Response(secretBody, { status: 429 }), 429, { error: "upstream_busy" }],
    [new Response(secretBody, { status: 400 }), 502, { error: "upstream_error", status: 400 }],
    [new Response(secretBody, { status: 500 }), 502, { error: "upstream_error", status: 500 }],
    [new Error("connection reset"), 502, { error: "upstream_unreachable" }],
  ];
  for (const [gemini, status, body] of cases) {
    const { d } = deps({ gemini });
    const res = await handleProxyRequest(request({ prompt: "hi" }), d);
    assert.equal(res.status, status);
    const text = await res.text();
    assert.deepEqual(JSON.parse(text), body);
    assert.ok(!text.includes(KEY));
  }
});

test("rpcClaim calls claim_ai_proxy_call with the limits and returns its answer", async () => {
  const calls: { url: string; init: RequestInit }[] = [];
  const fetchFn = async (url: string, init: RequestInit) => {
    calls.push({ url, init });
    return new Response(JSON.stringify("device_limit"), { status: 200 });
  };
  const claim = rpcClaim(fetchFn, "https://abc.supabase.co/", "sb_secret_xyz", 6, 200);

  assert.equal(await claim(DEVICE), "device_limit");
  assert.equal(calls[0].url, "https://abc.supabase.co/rest/v1/rpc/claim_ai_proxy_call");
  assert.deepEqual(JSON.parse(calls[0].init.body as string), { p_device: DEVICE, p_device_limit: 6, p_global_limit: 200 });
  const headers = calls[0].init.headers as Record<string, string>;
  assert.equal(headers.apikey, "sb_secret_xyz");
  assert.equal(headers.Authorization, undefined, "a non-JWT secret key is not sent as a bearer token");
});

test("rpcClaim sends a legacy service-role JWT as a bearer token too", async () => {
  let headers: Record<string, string> = {};
  const fetchFn = async (_url: string, init: RequestInit) => {
    headers = init.headers as Record<string, string>;
    return new Response(JSON.stringify("ok"), { status: 200 });
  };
  await rpcClaim(fetchFn, "https://abc.supabase.co", "aaa.bbb.ccc", 6, 200)(DEVICE);
  assert.equal(headers.Authorization, "Bearer aaa.bbb.ccc");
});

test("rpcClaim fails on an error status or an unexpected answer", async () => {
  const failing = rpcClaim(async () => new Response("{}", { status: 401 }), "https://abc.supabase.co", "k", 6, 200);
  await assert.rejects(failing(DEVICE));
  const odd = rpcClaim(async () => new Response(JSON.stringify("maybe"), { status: 200 }), "https://abc.supabase.co", "k", 6, 200);
  await assert.rejects(odd(DEVICE));
});
