// The display-name trigger on a local Postgres, with the names the app's filter is tested on too.
import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import { call, localTest, newEmail, signInWithEmailCode } from "./stack.ts";

const names = JSON.parse(readFileSync(new URL("../display_names.json", import.meta.url), "utf8")) as {
  allowed: string[];
  blocked: string[];
};

localTest("ordinary names are accepted and offensive ones refused, the same as in the app", async () => {
  const player = await signInWithEmailCode(newEmail("names"));
  const upsert = (displayName: string) =>
    call("/rest/v1/profiles?on_conflict=id", {
      token: player.accessToken,
      body: { id: player.userId, display_name: displayName, leaderboard_opt_in: true },
      headers: { Prefer: "resolution=merge-duplicates,return=minimal" },
    });

  for (const name of names.allowed) {
    const res = await upsert(name);
    assert.ok([200, 201].includes(res.status), `${name} should be allowed: ${JSON.stringify(res.body)}`);
  }
  for (const name of names.blocked) {
    const res = await upsert(name);
    assert.equal(res.status, 400, `${name} should be refused`);
    assert.equal(res.body.code, "23514");
  }
});
