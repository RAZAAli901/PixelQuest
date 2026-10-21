// Run with: node --test "supabase/functions/**/*.test.ts"
import { test } from "node:test";
import assert from "node:assert/strict";
import { assertion, forgetCachedToken, sendPush, serviceAccountOf, type PushDeps } from "./push.ts";

async function account() {
  const pair = await crypto.subtle.generateKey({ name: "RSASSA-PKCS1-v1_5", modulusLength: 2048, publicExponent: new Uint8Array([1, 0, 1]), hash: "SHA-256" }, true, ["sign", "verify"]);
  const der = new Uint8Array(await crypto.subtle.exportKey("pkcs8", pair.privateKey));
  const pem = `-----BEGIN PRIVATE KEY-----\n${btoa(String.fromCharCode(...der))}\n-----END PRIVATE KEY-----\n`;
  return { pair, account: { project_id: "pq-test", client_email: "push@pq-test.iam.gserviceaccount.com", private_key: pem } };
}

test("the assertion is a JWT signed with the service account's key", async () => {
  const { pair, account: sa } = await account();
  const jwt = await assertion(sa, 1_800_000_000);
  const [h, c, s] = jwt.split(".");
  const claims = JSON.parse(Buffer.from(c, "base64url").toString());
  assert.equal(claims.iss, sa.client_email);
  assert.equal(claims.exp - claims.iat, 3600);
  const ok = await crypto.subtle.verify("RSASSA-PKCS1-v1_5", pair.publicKey, Buffer.from(s, "base64url"), new TextEncoder().encode(`${h}.${c}`));
  assert.ok(ok);
});

test("a push carries only its kind and ids, to each token; a dead token is forgotten", async () => {
  forgetCachedToken();
  const { account: sa } = await account();
  const sent: { url: string; body: unknown }[] = [];
  const forgotten: string[] = [];
  const deps: PushDeps = {
    account: sa,
    fetch: async (url, init) => {
      if (url.startsWith("https://oauth2")) return new Response(JSON.stringify({ access_token: "at", expires_in: 3600 }));
      const body = JSON.parse(String(init?.body));
      sent.push({ url, body });
      return new Response("{}", { status: body.message.token === "dead" ? 404 : 200 });
    },
    tokensFor: async () => ({ ok: true, data: [{ user_id: "u", token: "live" }, { user_id: "u", token: "dead" }] }),
    forget: async (t) => { forgotten.push(t); },
  };
  assert.equal(await sendPush(deps, "challenge_received", ["u"], { challenge_id: "c1", party_id: "p1" }), 1);
  assert.equal(sent[0].url, "https://fcm.googleapis.com/v1/projects/pq-test/messages:send");
  assert.deepEqual((sent[0].body as { message: { data: unknown } }).message.data, { kind: "challenge_received", challenge_id: "c1", party_id: "p1" });
  assert.deepEqual(forgotten, ["dead"]);
});

test("without the secret, nothing is sent", async () => {
  let called = false;
  const deps: PushDeps = { account: serviceAccountOf(undefined), fetch: async () => { called = true; return new Response(); }, tokensFor: async () => ({ ok: true, data: [] }), forget: async () => {} };
  assert.equal(await sendPush(deps, "draw_offered", ["u"], {}), 0);
  assert.equal(called, false);
  assert.equal(serviceAccountOf("{not json"), null);
  assert.equal(serviceAccountOf(JSON.stringify({ project_id: "x" })), null);
});
