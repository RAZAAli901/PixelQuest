// Email-code sign-in against a local Supabase Auth (GoTrue), the way the app does it.
// Run: SUPABASE_LOCAL=1 node --test "supabase/tests/**/*.test.ts"
import assert from "node:assert/strict";
import { call, codeIn, latestEmailTo, localTest, newEmail, signInWithEmailCode } from "./stack.ts";

localTest("a code request emails a 6-digit code (not a link), and the code signs in", async () => {
  const email = newEmail("first");
  const sent = await call("/auth/v1/otp", { body: { email, create_user: true } });
  assert.equal(sent.status, 200);

  const mail = await latestEmailTo(email);
  assert.equal(mail.subject, "Your PixelQuest sign-in code");
  const code = codeIn(mail);
  assert.match(code, /^\d{6}$/);
  assert.ok(!/https?:\/\/\S*verify/.test(mail.html), "no sign-in link: the app can't open one");

  const verified = await call("/auth/v1/verify", { body: { type: "email", email, token: code } });
  assert.equal(verified.status, 200);
  assert.ok(verified.body.access_token);
  assert.equal(verified.body.user.email, email);
  assert.ok(verified.body.user.email_confirmed_at, "entering the code confirms the address");
});

localTest("the error codes the app turns into messages are the ones Supabase sends", async () => {
  // EmailSignIn.sendFailure / verifyFailure map these error_code values to player-facing text.
  const email = newEmail("errors");
  assert.equal((await call("/auth/v1/otp", { body: { email, create_user: true } })).status, 200);

  const again = await call("/auth/v1/otp", { body: { email, create_user: true } });
  assert.equal(again.status, 429, "a second code within a minute is refused");
  assert.equal(again.body.error_code, "over_email_send_rate_limit");

  const wrong = await call("/auth/v1/verify", { body: { type: "email", email, token: "000000" } });
  assert.equal(wrong.status, 403);
  assert.equal(wrong.body.error_code, "otp_expired");

  const bad = await call("/auth/v1/otp", { body: { email: "not-an-address", create_user: true } });
  assert.equal(bad.status, 400);
  assert.equal(bad.body.error_code, "validation_failed");
});

localTest("signing in again later is the same account", async () => {
  const email = newEmail("again");
  const first = await signInWithEmailCode(email);
  // The one-code-a-minute limit is per address; a returning player waits it out in real life.
  await new Promise((r) => setTimeout(r, 61_000));
  const second = await signInWithEmailCode(email);
  assert.equal(second.userId, first.userId);
});
