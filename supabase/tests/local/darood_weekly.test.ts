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

/** An entry straight in the table, on a day, past the daily rules (for building weeks gone by). */
function entry(party: string, who: Session, amount: number, day: string) {
  sql(`SET session_replication_role = replica; INSERT INTO public.darood_entries (party_id, user_id, amount, local_date, tz, created_at) VALUES ('${party}', '${who.userId}', ${amount}, '${day}', 'UTC', '${day}T12:00:00Z')`);
}
const ranking = async (who: Session, party: string, monday: string) =>
  (await rpc(who, "weekly_ranking", { party_id: party, week_start: monday })).body;

localTest("weekly: ties share a place, nothing ranks last, the removed and invited aren't ranked", async () => {
  const { leader, member, other } = await cast();
  const party = await daroodParty(leader, [member, other]);
  const monday = "2026-09-07";
  entry(party, leader, 300, "2026-09-07");
  entry(party, member, 200, "2026-09-08");
  entry(party, member, 100, "2026-09-13"); // Sunday: the same week.
  entry(party, other, 999, "2026-09-14"); // Next Monday: not this week.
  const rows = await ranking(leader, party, monday);
  const by = (id: string) => rows.find((r: { user_id: string }) => r.user_id === id);
  assert.deepEqual([by(leader.userId).position, by(member.userId).position, by(other.userId).position], [1, 1, 3]);
  assert.equal(by(member.userId).active_days, 2);
  assert.equal(by(other.userId).week_total, 0, "nothing that week ranks last with 0");
  assert.equal((await rpc(leader, "weekly_ranking", { party_id: party, week_start: "2026-09-08" })).body.message, "bad_week");

  assert.deepEqual((await rpc(leader, "remove_member", { party_id: party, user_id: other.userId })).body, { status: "removed" });
  assert.equal((await ranking(leader, party, monday)).length, 2, "a removed member isn't ranked");
  assert.equal((await rpc(other, "weekly_ranking", { party_id: party, week_start: monday })).body.message, "not_a_member");
});

localTest("weekly: a week closes once it's over in the party's zone, and its results never change", async () => {
  const { leader, member } = await cast();
  const party = await daroodParty(leader, [member]);
  const thisMonday = sql("SELECT date_trunc('week', now() AT TIME ZONE 'UTC')::date");
  assert.equal((await rpc(member, "close_darood_week", { party_id: party, week_start: thisMonday })).body.message, "week_not_over");

  const monday = "2026-09-07";
  entry(party, member, 500, "2026-09-09");
  assert.equal((await rpc(member, "close_darood_week", { party_id: party, week_start: monday })).body, 2);
  entry(party, leader, 9000, "2026-09-10"); // Late, after it closed.
  assert.equal((await rpc(leader, "close_darood_week", { party_id: party, week_start: monday })).body, 2, "again changes nothing");
  const stored = (await call(`/rest/v1/darood_week_results?party_id=eq.${party}&select=user_id,position,week_total&order=position`, { token: member.accessToken })).body;
  assert.deepEqual(stored.map((r: { position: number; week_total: number }) => [r.position, r.week_total]), [[1, 500], [2, 0]]);
  assert.equal((await call(`/rest/v1/darood_week_results?party_id=eq.${party}`, { method: "PATCH", token: leader.accessToken, body: { position: 1 } })).status, 403);

  // Time zone edge: in a zone far ahead, this week ends sooner, but not before its own Sunday ends.
  assert.equal((await rpc(leader, "set_party_week_tz", { party_id: party, tz: "Pacific/Kiritimati" })).status, 204);
  assert.equal((await rpc(member, "close_darood_week", { party_id: party, week_start: thisMonday })).body.message, "week_not_over");
});
