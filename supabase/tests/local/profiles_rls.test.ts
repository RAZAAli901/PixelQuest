// Row-level security on public.profiles (the leaderboard), against a local Postgres.
// The requests mirror CloudProfileRepository and LeaderboardRepository.
import assert from "node:assert/strict";
import { asService, call, localTest, newEmail, signInWithEmailCode, stack, type Session } from "./stack.ts";

const upsertHeaders = { Prefer: "resolution=merge-duplicates,return=minimal" };

function row(session: Session, overrides: Record<string, unknown> = {}) {
  return {
    id: session.userId,
    display_name: `Hero_${session.userId.slice(0, 6)}`,
    current_streak: 3,
    longest_streak: 5,
    level: 2,
    total_xp: 300,
    leaderboard_opt_in: true,
    ...overrides,
  };
}

async function upsertOwn(session: Session, overrides: Record<string, unknown> = {}) {
  return call("/rest/v1/profiles?on_conflict=id", {
    token: session.accessToken,
    body: row(session, overrides),
    headers: upsertHeaders,
  });
}

localTest("players can write only their own row, and nobody signed out can read the board", async () => {
  const alice = await signInWithEmailCode(newEmail("alice"));
  const bob = await signInWithEmailCode(newEmail("bob"));

  assert.equal((await upsertOwn(alice)).status, 201);
  assert.ok([200, 201].includes((await upsertOwn(alice, { total_xp: 450 })).status), "upserting again updates");

  // Bob writes a row with Alice's id: refused by the insert policy.
  const forged = await call("/rest/v1/profiles?on_conflict=id", {
    token: bob.accessToken,
    body: row(alice, { display_name: "Forged_Name", total_xp: 0 }),
    headers: upsertHeaders,
  });
  assert.equal(forged.status, 403);
  assert.equal(forged.body.code, "42501");

  // Bob patches Alice's row: matches nothing.
  const patched = await call(`/rest/v1/profiles?id=eq.${alice.userId}`, {
    method: "PATCH",
    token: bob.accessToken,
    body: { total_xp: 0 },
    headers: { Prefer: "return=representation" },
  });
  assert.equal(patched.status, 200);
  assert.deepEqual(patched.body, []);

  const aliceSeenByBob = await call(`/rest/v1/profiles?id=eq.${alice.userId}&select=total_xp,display_name`, { token: bob.accessToken });
  assert.deepEqual(aliceSeenByBob.body, [{ total_xp: 450, display_name: `Hero_${alice.userId.slice(0, 6)}` }]);

  const signedOut = await call(`/rest/v1/profiles?id=eq.${alice.userId}`);
  assert.equal(signedOut.status, 200);
  assert.deepEqual(signedOut.body, [], "the leaderboard is for signed-in players");
});

localTest("leaving the board hides the row from others but not from its owner", async () => {
  const carol = await signInWithEmailCode(newEmail("carol"));
  const dave = await signInWithEmailCode(newEmail("dave"));
  assert.equal((await upsertOwn(carol)).status, 201);

  // optOutFromLeaderboard: only the flag, only on the existing row.
  const left = await call(`/rest/v1/profiles?id=eq.${carol.userId}`, {
    method: "PATCH",
    token: carol.accessToken,
    body: { leaderboard_opt_in: false },
    headers: { Prefer: "return=representation" },
  });
  assert.equal(left.status, 200);
  assert.equal(left.body[0].leaderboard_opt_in, false);
  assert.equal(left.body[0].total_xp, 300, "stats untouched");

  assert.deepEqual((await call(`/rest/v1/profiles?id=eq.${carol.userId}`, { token: dave.accessToken })).body, []);
  assert.equal((await call(`/rest/v1/profiles?id=eq.${carol.userId}`, { token: carol.accessToken })).body.length, 1);

  // A spectator who never joined: opting out patches nothing and creates no row.
  const spectator = await call(`/rest/v1/profiles?id=eq.${dave.userId}`, {
    method: "PATCH",
    token: dave.accessToken,
    body: { leaderboard_opt_in: false },
    headers: { Prefer: "return=representation" },
  });
  assert.deepEqual(spectator.body, []);
});

localTest("rank counting (HEAD with an exact count) sees only rows the player may see", async () => {
  // Streaks no other test uses, cleared from earlier runs, so the count is about these rows only.
  assert.equal((await asService("/rest/v1/profiles?current_streak=gte.900000", { method: "DELETE" })).status, 204);
  const streak = 900_000 + Math.floor(Math.random() * 90_000);
  const players = await Promise.all(["e1", "e2", "e3"].map((t) => signInWithEmailCode(newEmail(t))));
  await upsertOwn(players[0], { current_streak: streak + 2 });
  await upsertOwn(players[1], { current_streak: streak + 1, leaderboard_opt_in: false });
  await upsertOwn(players[2], { current_streak: streak });

  // What LeaderboardRepository.countProfiles sends for "higher streaks than mine".
  const res = await fetch(
    `${stack().apiUrl}/rest/v1/profiles?select=id&leaderboard_opt_in=eq.true&current_streak=gt.${streak}`,
    {
      method: "HEAD",
      headers: {
        apikey: stack().publishableKey,
        Authorization: `Bearer ${players[2].accessToken}`,
        Prefer: "count=exact",
      },
    },
  );
  assert.equal(res.status, 200);
  const total = Number(res.headers.get("content-range")?.split("/")[1]);
  assert.equal(total, 1, "only the opted-in player ahead counts; the hidden one doesn't");
});

localTest("stored stats can't be negative", async () => {
  const erin = await signInWithEmailCode(newEmail("erin"));
  const res = await upsertOwn(erin, { total_xp: -5 });
  assert.equal(res.status, 400);
  assert.equal(res.body.code, "23514");
});
