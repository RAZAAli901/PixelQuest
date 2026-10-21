// Goals that go on (migrations 20261019000071 to 074, docs/darood-weekly.md).
import assert from "node:assert/strict";
import { call, localTest, sql, type Session } from "./stack.ts";
import { cast, daroodParty } from "./darood_weekly.test.ts";

const rpc = (who: Session | undefined, fn: string, body: unknown) => call(`/rest/v1/rpc/${fn}`, { token: who?.accessToken, body });
const day = () => new Date().toISOString().slice(0, 10);
const add = (who: Session, party: string, amount: number) =>
  rpc(who, "add_darood_entry", { party_id: party, amount, local_date: day(), tz: "UTC", client_id: crypto.randomUUID() });
const goals = (party: string) =>
  sql(`SELECT string_agg(sequence || ':' || target || ':' || (reached_at IS NOT NULL), ',' ORDER BY sequence) FROM public.party_milestones WHERE party_id = '${party}'`);

localTest("goals: the first goal, reached, extended, reached again; a lower target is refused", async () => {
  const { leader, member, other } = await cast();
  const party = await daroodParty(leader, [member], 100);
  assert.equal(goals(party), "1:100:false", "made with a milestone: goal 1");

  // Before it's reached, the goal itself can still change (above the total).
  assert.equal((await add(member, party, 40)).status, 200);
  assert.equal((await rpc(leader, "set_milestone", { party_id: party, target: 120 })).body.goal, 1);
  assert.equal(goals(party), "1:120:false");

  // Reached: marked once, and the party takes no more until it's extended.
  assert.equal((await add(member, party, 90)).status, 200);
  assert.equal(goals(party), "1:120:true");
  const reached = sql(`SELECT reached_at FROM public.party_milestones WHERE party_id = '${party}' AND sequence = 1`);
  assert.equal((await add(member, party, 1)).body.message, "party_closed");

  // Extending: the leader's, above the total now (130), at most a trillion.
  assert.equal((await rpc(member, "set_milestone", { party_id: party, target: 500 })).body.message, "not_leader");
  assert.equal((await rpc(leader, "set_milestone", { party_id: party, target: 130 })).body.message, "milestone_too_low");
  assert.equal((await rpc(leader, "set_milestone", { party_id: party, target: 1_000_000_000_001 })).body.message, "milestone_too_high");
  const extended = await rpc(leader, "set_milestone", { party_id: party, target: 200 });
  assert.equal(extended.status, 200, JSON.stringify(extended.body));
  assert.equal(extended.body.goal, 2);
  assert.equal(goals(party), "1:120:true,2:200:false");
  assert.equal(sql(`SELECT completed_at IS NULL FROM public.parties WHERE id = '${party}'`), "t", "open again");
  assert.equal(sql(`SELECT reached_at FROM public.party_milestones WHERE party_id = '${party}' AND sequence = 1`), reached, "goal 1 keeps its finish time");

  // The second goal, reached the same way.
  assert.equal((await add(member, party, 70)).status, 200);
  assert.equal(goals(party), "1:120:true,2:200:true");

  // A reached goal never changes, even for the database's own users.
  assert.throws(() => sql(`UPDATE public.party_milestones SET target = 5 WHERE party_id = '${party}' AND sequence = 1`), /goal_is_final/);

  // Members read the list; someone outside the party sees none of it; nobody writes it.
  const read = await call(`/rest/v1/party_milestones?party_id=eq.${party}&select=sequence`, { token: member.accessToken });
  assert.equal(read.body.length, 2);
  assert.equal((await call(`/rest/v1/party_milestones?party_id=eq.${party}&select=sequence`, { token: other.accessToken })).body.length, 0);
  const write = await call(`/rest/v1/party_milestones`, { method: "POST", token: leader.accessToken, body: { party_id: party, sequence: 9, target: 999 } });
  assert.notEqual(write.status, 201);
});

localTest("goals: the race board shows the goal a party is on, and when each earlier one was reached", async () => {
  const { leader, member, other } = await cast();
  const party = await daroodParty(leader, [member], 50);
  await call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: leader.accessToken, body: { is_public_race: true } });
  assert.equal((await add(member, party, 50)).status, 200);
  assert.equal((await rpc(leader, "set_milestone", { party_id: party, target: 80 })).status, 200);
  const name = sql(`SELECT name FROM public.parties WHERE id = '${party}'`);
  const line = (await call(`/rest/v1/race_board?name=eq.${name}&select=goal_number,goals_reached,milestone,progress`, { token: other.accessToken })).body[0];
  assert.equal(line.goal_number, 2);
  assert.equal(line.milestone, 80);
  assert.deepEqual(line.goals_reached.map((g: { goal: number; target: number }) => [g.goal, g.target]), [[1, 50]]);
  assert.ok(line.goals_reached[0].reached_at);
});
