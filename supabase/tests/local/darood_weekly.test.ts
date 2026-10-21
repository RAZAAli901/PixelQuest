// Darood Parties' weekly ranking and goals (migrations 20261019000067 on, docs/darood-weekly.md).
import assert from "node:assert/strict";
import { call, localTest, newPlayer, sql, type Session } from "./stack.ts";

type Player = Session & { username: string };
const fresh = (prefix: string) => `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
const rpc = (who: Session | undefined, fn: string, body: unknown) => call(`/rest/v1/rpc/${fn}`, { token: who?.accessToken, body });

async function player(tag: string): Promise<Player> {
  const session = await newPlayer(tag);
  const username = fresh(tag.slice(0, 8));
  assert.equal((await rpc(session, "set_username", { name: username })).body?.status, "ok");
  return { ...session, username };
}

let castPromise: Promise<{ leader: Player; member: Player; other: Player }> | undefined;
export function cast() {
  castPromise ??= (async () => {
    const [leader, member, other] = await Promise.all(["wl", "wm", "wo"].map((t) => player(`weekly_${t}`)));
    return { leader, member, other };
  })();
  return castPromise;
}

export async function daroodParty(leader: Player, members: Player[], milestone?: number): Promise<string> {
  const made = await rpc(leader, "create_party", { name: fresh("Week"), max_members: 10, milestone: milestone ?? null });
  assert.equal(made.status, 200, JSON.stringify(made.body));
  sql(`DELETE FROM private.invite_log WHERE inviter = '${leader.userId}'`);
  for (const m of members) {
    await rpc(leader, "invite_member", { party_id: made.body, username: m.username });
    await rpc(m, "respond_invite", { party_id: made.body, accept: true });
  }
  return made.body;
}

localTest("weekly: a party's week zone is its leader's to set, a real zone only", async () => {
  const { leader, member } = await cast();
  const party = await daroodParty(leader, [member]);
  assert.equal(sql(`SELECT week_tz FROM public.parties WHERE id = '${party}'`), "UTC");
  assert.equal((await rpc(leader, "set_party_week_tz", { party_id: party, tz: "Asia/Karachi" })).status, 204);
  assert.equal(sql(`SELECT week_tz FROM public.parties WHERE id = '${party}'`), "Asia/Karachi");
  assert.equal((await rpc(member, "set_party_week_tz", { party_id: party, tz: "UTC" })).body.message, "not_leader");
  assert.equal((await rpc(leader, "set_party_week_tz", { party_id: party, tz: "Mars/Olympus" })).body.message, "bad_time_zone");
});
