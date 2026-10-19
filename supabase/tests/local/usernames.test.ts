// Darood Party usernames (migrations 20261019000000_usernames.sql onwards): only the owner reads
// theirs, nobody writes the table directly, and its form, reserved words and uniqueness hold.
import assert from "node:assert/strict";
import { asService, call, localTest, newPlayer } from "./stack.ts";

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
