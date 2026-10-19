// Darood Party usernames (migrations 20261019000000_usernames.sql onwards): only the owner reads
// theirs, nobody writes the table directly, and its form, reserved words and uniqueness hold.
import assert from "node:assert/strict";
import { asService, call, localTest, newPlayer, sql } from "./stack.ts";

const fresh = () => `u_${Math.random().toString(36).slice(2, 12)}`;

/** Stores a username the way only the server can (the service key skips row-level security). */
function storeAsServer(userId: string, username: string) {
  return asService("/rest/v1/usernames", {
    body: { user_id: userId, username },
    headers: { Prefer: "return=minimal" },
  });
}

localTest("a player can't write a username directly", async () => {
  const player = await newPlayer("uname_write");
  const insert = await call("/rest/v1/usernames", {
    token: player.accessToken,
    body: { user_id: player.userId, username: fresh() },
  });
  assert.equal(insert.status, 403, JSON.stringify(insert.body));
  assert.equal(insert.body.code, "42501");

  assert.equal((await storeAsServer(player.userId, fresh())).status, 201);
  const update = await call(`/rest/v1/usernames?user_id=eq.${player.userId}`, {
    method: "PATCH",
    token: player.accessToken,
    body: { username: fresh() },
  });
  assert.equal(update.status, 403, JSON.stringify(update.body));
  const remove = await call(`/rest/v1/usernames?user_id=eq.${player.userId}`, { method: "DELETE", token: player.accessToken });
  assert.equal(remove.status, 403, JSON.stringify(remove.body));
});

localTest("only the owner reads their username", async () => {
  const [owner, other] = await Promise.all([newPlayer("uname_owner"), newPlayer("uname_other")]);
  const name = fresh();
  assert.equal((await storeAsServer(owner.userId, name)).status, 201);

  const own = await call(`/rest/v1/usernames?select=username`, { token: owner.accessToken });
  assert.deepEqual(own.body, [{ username: name }]);

  // Another player sees nothing, by id, by name, or by listing the table.
  for (const path of [`/rest/v1/usernames?user_id=eq.${owner.userId}`, `/rest/v1/usernames?username=eq.${name}`, "/rest/v1/usernames"]) {
    const seen = await call(path, { token: other.accessToken });
    assert.equal(seen.status, 200);
    assert.ok(seen.body.every((row: any) => row.user_id === other.userId), `${path}: ${JSON.stringify(seen.body)}`);
  }
  // Signed out, the table isn't readable at all.
  assert.equal((await call("/rest/v1/usernames")).status, 401);
});

localTest("the form and reserved words hold, even for the server", async () => {
  const player = await newPlayer("uname_form");
  for (const bad of ["ab", "a".repeat(21), "aria k", "aria-k", "ária", "admin", "Admin_1", "pixel_quest", "staff2026"]) {
    const refused = await storeAsServer(player.userId, bad);
    assert.equal(refused.status, 400, `${bad}: ${JSON.stringify(refused.body)}`);
    assert.equal(refused.body.code, "23514", bad);
  }
  // Containing a reserved word is fine.
  assert.equal((await storeAsServer(player.userId, `adminah_${Math.random().toString(36).slice(2, 8)}`)).status, 201);
});

localTest("a name has one owner, and deleting the account frees it", async () => {
  const [owner, other] = await Promise.all([newPlayer("uname_one"), newPlayer("uname_two")]);
  const name = fresh();
  assert.equal((await storeAsServer(owner.userId, name)).status, 201);
  const taken = await storeAsServer(other.userId, name);
  assert.equal(taken.status, 409, JSON.stringify(taken.body));
  assert.equal(taken.body.code, "23505");

  assert.equal((await call("/rest/v1/rpc/delete_user_account", { token: owner.accessToken, body: {} })).status, 204);
  assert.equal((await storeAsServer(other.userId, name)).status, 201);
});

localTest("names are stored in lowercase and unique whatever their case (20261019000001)", async () => {
  const [first, second] = await Promise.all([newPlayer("uname_case1"), newPlayer("uname_case2")]);
  const name = fresh();
  assert.equal((await storeAsServer(first.userId, name.toUpperCase())).status, 201);
  const stored = await call("/rest/v1/usernames?select=username", { token: first.accessToken });
  assert.deepEqual(stored.body, [{ username: name }]);

  for (const attempt of [name, name.toUpperCase(), name[0].toUpperCase() + name.slice(1)]) {
    const taken = await storeAsServer(second.userId, attempt);
    assert.equal(taken.status, 409, `${attempt}: ${JSON.stringify(taken.body)}`);
    assert.equal(taken.body.code, "23505");
  }
});

function isAvailable(token: string | undefined, name: string) {
  return call("/rest/v1/rpc/is_username_available", { token, body: { name } });
}

localTest("is_username_available: free, taken, yours, malformed or reserved (20261019000002)", async () => {
  const [owner, other] = await Promise.all([newPlayer("uname_avail1"), newPlayer("uname_avail2")]);
  const name = fresh();
  assert.deepEqual((await isAvailable(other.accessToken, name)).body, true);

  assert.equal((await storeAsServer(owner.userId, name)).status, 201);
  // Taken, whatever its case; but the owner's own name is theirs to keep.
  assert.deepEqual((await isAvailable(other.accessToken, name)).body, false);
  assert.deepEqual((await isAvailable(other.accessToken, name.toUpperCase())).body, false);
  assert.deepEqual((await isAvailable(owner.accessToken, name)).body, true);

  for (const bad of ["ab", "aria k", "admin", "Pixel_Quest1", ""]) {
    assert.deepEqual((await isAvailable(other.accessToken, bad)).body, false, bad);
  }
  // Signed out: no answer at all.
  const signedOut = await isAvailable(undefined, name);
  assert.equal(signedOut.status, 401, JSON.stringify(signedOut.body));
});

function setUsername(token: string | undefined, name: string) {
  return call("/rest/v1/rpc/set_username", { token, body: { name } });
}

localTest("set_username claims a name, then allows a change once every 30 days (20261019000003)", async () => {
  const player = await newPlayer("uname_set");
  const first = fresh();
  const claimed = await setUsername(player.accessToken, first.toUpperCase());
  assert.equal(claimed.status, 200, JSON.stringify(claimed.body));
  assert.equal(claimed.body.status, "ok");
  assert.equal(claimed.body.username, first);
  const changeableAt = new Date(claimed.body.changeable_at).getTime();
  assert.ok(Math.abs(changeableAt - (Date.now() + 30 * 86_400_000)) < 120_000, claimed.body.changeable_at);

  // The same name again is fine; a new one waits 30 days.
  assert.equal((await setUsername(player.accessToken, first)).body.status, "ok");
  const tooSoon = await setUsername(player.accessToken, fresh());
  assert.equal(tooSoon.body.status, "too_soon");
  assert.equal(tooSoon.body.username, first);

  // 30 days on, it changes.
  sql(`UPDATE public.usernames SET changed_at = now() - interval '30 days 1 minute' WHERE user_id = '${player.userId}'`);
  const second = fresh();
  const changed = await setUsername(player.accessToken, second);
  assert.equal(changed.body.status, "ok", JSON.stringify(changed.body));
  const own = await call("/rest/v1/usernames?select=username", { token: player.accessToken });
  assert.deepEqual(own.body, [{ username: second }]);
});

localTest("set_username says taken or invalid, and needs a sign-in", async () => {
  const [owner, other] = await Promise.all([newPlayer("uname_set1"), newPlayer("uname_set2")]);
  const name = fresh();
  assert.equal((await setUsername(owner.accessToken, name)).body.status, "ok");

  assert.deepEqual((await setUsername(other.accessToken, name.toUpperCase())).body, { status: "taken" });
  for (const bad of ["ab", "aria k", "admin_1", ""]) {
    assert.deepEqual((await setUsername(other.accessToken, bad)).body, { status: "invalid" }, bad);
  }
  // A refused name doesn't start the 30 days: the player can still claim another one.
  assert.equal((await setUsername(other.accessToken, fresh())).body.status, "ok");

  assert.equal((await setUsername(undefined, fresh())).status, 401);
});

localTest("two changes sent at once can't both get through", async () => {
  const player = await newPlayer("uname_race");
  assert.equal((await setUsername(player.accessToken, fresh())).body.status, "ok");
  sql(`UPDATE public.usernames SET changed_at = now() - interval '31 days' WHERE user_id = '${player.userId}'`);

  const results = await Promise.all([fresh(), fresh(), fresh()].map((n) => setUsername(player.accessToken, n)));
  assert.equal(results.filter((r) => r.body.status === "ok").length, 1, JSON.stringify(results.map((r) => r.body)));
});
