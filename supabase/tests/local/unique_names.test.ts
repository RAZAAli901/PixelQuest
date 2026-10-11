// Public names are unique, ignoring case (migration 20261011000001_unique_display_names.sql).
import assert from "node:assert/strict";
import { call, localTest, newPlayer, type Session } from "./stack.ts";

function setName(player: Session, displayName: string, optIn = true) {
  return call("/rest/v1/profiles?on_conflict=id", {
    token: player.accessToken,
    body: { id: player.userId, display_name: displayName, leaderboard_opt_in: optIn },
    headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
  });
}

const fresh = () => `Uq_${Math.random().toString(36).slice(2, 10)}`;
const ok = (status: number) => [200, 201, 204].includes(status);

localTest("a name another player has is refused, whatever its case", async () => {
  const [first, second] = await Promise.all([newPlayer("name1"), newPlayer("name2")]);
  const name = fresh();
  assert.ok(ok((await setName(first, name)).status));

  for (const attempt of [name, name.toUpperCase(), name.toLowerCase()]) {
    const taken = await setName(second, attempt);
    assert.equal(taken.status, 409, `${attempt}: ${JSON.stringify(taken.body)}`);
    assert.equal(taken.body.code, "23505");
  }
});

localTest("the name stays its owner's off the board, and is free again after a rename or deleting the account", async () => {
  const [owner, other] = await Promise.all([newPlayer("owner"), newPlayer("other")]);
  const name = fresh();
  assert.ok(ok((await setName(owner, name)).status));

  // Off the board: still the owner's.
  const left = await call(`/rest/v1/profiles?id=eq.${owner.userId}`, {
    method: "PATCH",
    token: owner.accessToken,
    body: { leaderboard_opt_in: false },
  });
  assert.equal(left.status, 204);
  assert.equal((await setName(other, name)).status, 409);

  // Renamed: the old name is free.
  assert.ok(ok((await setName(owner, fresh())).status));
  assert.ok(ok((await setName(other, name)).status));

  // Deleting the account frees the owner's current name too.
  const ownersName = fresh();
  assert.ok(ok((await setName(owner, ownersName)).status));
  assert.equal((await call("/rest/v1/rpc/delete_user_account", { token: owner.accessToken, body: {} })).status, 204);
  assert.ok(ok((await setName(other, ownersName)).status));
});

localTest("players who haven't chosen a name yet don't collide", async () => {
  const [a, b] = await Promise.all([newPlayer("noname1"), newPlayer("noname2")]);
  assert.ok(ok((await setName(a, "", false)).status));
  assert.ok(ok((await setName(b, "", false)).status));
});
