// S059: sessions. Signing out revokes the phone's session on the server; "sign out on every
// device" (scope=global) revokes all of them, so another phone's refresh fails.
// Run with: SUPABASE_LOCAL=1 node --test supabase/tests/local/sessions.test.ts
import assert from "node:assert/strict";
import { asService, call, localTest, newEmail } from "./stack.ts";

/** Two phones signed in to one account, each with its own session. */
async function twoPhones() {
  const email = newEmail("phones");
  const password = `pw-${Math.random().toString(36).slice(2)}-${Date.now()}`;
  const made = await asService("/auth/v1/admin/users", { body: { email, password, email_confirm: true } });
  assert.ok([200, 201].includes(made.status), JSON.stringify(made.body));
  const signIn = async () => {
    const res = await call("/auth/v1/token?grant_type=password", { body: { email, password } });
    assert.equal(res.status, 200, JSON.stringify(res.body));
    return { access: res.body.access_token as string, refresh: res.body.refresh_token as string };
  };
  return { a: await signIn(), b: await signIn() };
}

const refresh = (token: string) => call("/auth/v1/token?grant_type=refresh_token", { body: { refresh_token: token } });

localTest("signing out on one phone leaves the other phone signed in", async () => {
  const { a, b } = await twoPhones();
  assert.equal((await call("/auth/v1/logout?scope=local", { token: a.access, body: {} })).status, 204);
  assert.notEqual((await refresh(a.refresh)).status, 200, "phone A's session is revoked");
  assert.equal((await refresh(b.refresh)).status, 200, "phone B carries on");
});

localTest("signing out everywhere revokes every phone's session", async () => {
  const { a, b } = await twoPhones();
  // AuthRepositoryImpl.signOutEverywhere: auth.signOut(SignOutScope.GLOBAL).
  assert.equal((await call("/auth/v1/logout?scope=global", { token: a.access, body: {} })).status, 204);
  assert.notEqual((await refresh(a.refresh)).status, 200);
  const other = await refresh(b.refresh);
  assert.notEqual(other.status, 200, "phone B can't refresh: it is signed out at its next refresh");
  // An access token already issued works until it expires (an hour at most), as JWTs do; the app
  // refreshes well before that, and that refresh is what signs phone B out.
  assert.equal((await call("/rest/v1/rpc/export_my_data", { token: b.access, body: {} })).status, 200);
});
