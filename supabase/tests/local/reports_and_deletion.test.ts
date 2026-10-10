// Reports, account deletion and the AI call counter, against a local Supabase.
import assert from "node:assert/strict";
import { asService, call, localTest, newEmail, signInWithEmailCode } from "./stack.ts";

async function joinBoard(session: { accessToken: string; userId: string }) {
  const res = await call("/rest/v1/profiles?on_conflict=id", {
    token: session.accessToken,
    body: { id: session.userId, display_name: `P_${session.userId.slice(0, 8)}`, leaderboard_opt_in: true },
    headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
  });
  assert.ok([200, 201].includes(res.status), JSON.stringify(res.body));
}

localTest("a report is filed in the reporter's own name, and nobody can read reports back", async () => {
  const reporter = await signInWithEmailCode(newEmail("reporter"));
  const reported = await signInWithEmailCode(newEmail("reported"));
  await joinBoard(reported);

  // LeaderboardRepository.reportProfile
  const filed = await call("/rest/v1/reports", {
    token: reporter.accessToken,
    body: { reporter_id: reporter.userId, reported_profile_id: reported.userId, reason: "Flagged offensive display name: x" },
  });
  assert.equal(filed.status, 201);

  const forged = await call("/rest/v1/reports", {
    token: reporter.accessToken,
    body: { reporter_id: reported.userId, reported_profile_id: reported.userId, reason: "framed" },
  });
  assert.equal(forged.status, 403);

  const readBack = await call(`/rest/v1/reports?reporter_id=eq.${reporter.userId}`, { token: reporter.accessToken });
  assert.deepEqual(readBack.body, [], "only the project owner reads reports (dashboard / service role)");

  const stored = await asService(`/rest/v1/reports?reporter_id=eq.${reporter.userId}&select=reason`);
  assert.deepEqual(stored.body, [{ reason: "Flagged offensive display name: x" }]);
});

localTest("delete_user_account removes the player's row and their sign-in, and only for a signed-in player", async () => {
  const leaving = await signInWithEmailCode(newEmail("leaving"));
  await joinBoard(leaving);

  const signedOut = await call("/rest/v1/rpc/delete_user_account", { body: {} });
  assert.ok([401, 403].includes(signedOut.status), `anon got ${signedOut.status}`);

  // AuthRepositoryImpl.deleteAccount
  const deleted = await call("/rest/v1/rpc/delete_user_account", { token: leaving.accessToken, body: {} });
  assert.equal(deleted.status, 204, JSON.stringify(deleted.body));

  assert.deepEqual((await asService(`/rest/v1/profiles?id=eq.${leaving.userId}`)).body, []);
  const authUser = await asService(`/auth/v1/admin/users/${leaving.userId}`);
  assert.equal(authUser.status, 404, "the sign-in is gone too");
});

localTest("the AI call counter is closed to players: only the Edge Function's service role can claim", async () => {
  const player = await signInWithEmailCode(newEmail("counter"));
  const args = { p_device: `user:${player.userId}`, p_device_limit: 6, p_global_limit: 200 };

  for (const token of [undefined, player.accessToken]) {
    const res = await call("/rest/v1/rpc/claim_ai_proxy_call", { token, body: args });
    assert.ok([401, 403, 404].includes(res.status), `got ${res.status} ${JSON.stringify(res.body)}`);
    const table = await call("/rest/v1/ai_proxy_usage?select=*", { token });
    assert.ok(table.status >= 400 || (Array.isArray(table.body) && table.body.length === 0), "the counts aren't readable");
  }

  const claim = () => asService("/rest/v1/rpc/claim_ai_proxy_call", { body: { ...args, p_device_limit: 2 } });
  assert.equal((await claim()).body, "ok");
  assert.equal((await claim()).body, "ok");
  assert.equal((await claim()).body, "device_limit", "the per-key limit, which the proxy reports as the account's");

  const counted = await asService(`/rest/v1/ai_proxy_usage?device_id=eq.user:${player.userId}&select=calls`);
  assert.deepEqual(counted.body, [{ calls: 2 }], "a refused claim isn't counted");
});
