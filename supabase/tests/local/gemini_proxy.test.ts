// gemini-proxy on the local Edge Functions runtime, with real Supabase Auth tokens.
// Needs `npx supabase functions serve --env-file supabase/functions/.env.local`, where the env file
// holds a fake GEMINI_API_KEY and AI_PROXY_ACCOUNT_DAILY_LIMIT=2 (docs/LOCAL_SUPABASE.md): calls that
// get past the checks reach Google, which refuses the fake key, so no quota is used.
import assert from "node:assert/strict";
import { asService, call, localTest, newEmail, signInWithEmailCode, stack } from "./stack.ts";

const proxy = (token?: string, body: unknown = { prompt: "How am I doing?" }) =>
  call("/functions/v1/gemini-proxy", { token, body });

localTest("callers who aren't signed in are turned away before anything is counted", async () => {
  const before = await asService("/rest/v1/ai_proxy_usage?device_id=eq.*&select=calls");
  const globalBefore = before.body[0]?.calls ?? 0;

  for (const token of [undefined, stack().publishableKey, "not.a.jwt", "eyJhbGciOiJIUzI1NiJ9.e30.x"]) {
    const res = await proxy(token);
    assert.equal(res.status, 401, `token ${token}`);
    assert.deepEqual(res.body, { error: "sign_in_required" });
  }

  const after = await asService("/rest/v1/ai_proxy_usage?device_id=eq.*&select=calls");
  assert.equal(after.body[0]?.calls ?? 0, globalBefore, "nothing claimed for them");
});

localTest("a signed-in player's calls are counted on their account and stop at its daily limit", async () => {
  const player = await signInWithEmailCode(newEmail("coach"));

  // Past the checks: Google refuses the fake key, which the proxy reports without its body.
  for (let i = 0; i < 2; i++) {
    const res = await proxy(player.accessToken);
    assert.equal(res.status, 502, JSON.stringify(res.body));
    assert.equal(res.body.error, "upstream_error");
  }
  const third = await proxy(player.accessToken);
  assert.equal(third.status, 429);
  assert.deepEqual(third.body, { error: "account_daily_limit" });

  const counted = await asService(`/rest/v1/ai_proxy_usage?device_id=eq.user:${player.userId}&select=calls`);
  assert.deepEqual(counted.body, [{ calls: 2 }]);

  // Another account has its own allowance.
  const other = await signInWithEmailCode(newEmail("coach2"));
  assert.equal((await proxy(other.accessToken)).status, 502);
});

localTest("after signing out, the old token no longer works", async () => {
  const player = await signInWithEmailCode(newEmail("signout"));
  assert.equal((await call("/auth/v1/logout", { token: player.accessToken, body: {} })).status, 204);

  const res = await proxy(player.accessToken);
  assert.equal(res.status, 401);
  assert.deepEqual(res.body, { error: "sign_in_required" });
});

localTest("malformed requests from a signed-in player don't use up a call", async () => {
  const player = await signInWithEmailCode(newEmail("malformed"));
  assert.equal((await proxy(player.accessToken, { prompt: "" })).status, 400);
  assert.equal((await proxy(player.accessToken, { prompt: "x".repeat(6001) })).status, 413);
  const counted = await asService(`/rest/v1/ai_proxy_usage?device_id=eq.user:${player.userId}&select=calls`);
  assert.deepEqual(counted.body, []);
});
