// Darood Parties on the server (migrations 20261019000004 to 20261019000022): who reads what, the
// limits, the leader's rights, invitations, leaving, and finishing. Every rule the app relies on is
// checked here against the real database, row-level security and functions.
import assert from "node:assert/strict";
import { asService, call, localTest, newEmail, newPlayer, type Session } from "./stack.ts";

type Player = Session & { username: string };

const fresh = (prefix: string) => `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
const day = (back = 0) => new Date(Date.now() - back * 86_400_000).toISOString().slice(0, 10);
const ok = (status: number) => [200, 201, 204].includes(status);

function rpc(who: Session | undefined, fn: string, body: unknown) {
  return call(`/rest/v1/rpc/${fn}`, { token: who?.accessToken, body });
}

async function player(tag: string): Promise<Player> {
  const session = await newPlayer(tag);
  const username = fresh(tag.slice(0, 8));
  const set = await rpc(session, "set_username", { name: username });
  assert.equal(set.body?.status, "ok", JSON.stringify(set.body));
  return { ...session, username };
}

/** An account that never signs in, for filling places (the stack allows 30 sign-ins in 5 minutes). */
async function account(tag: string): Promise<string> {
  const made = await asService("/auth/v1/admin/users", {
    body: { email: newEmail(tag), password: `pw-${Math.random().toString(36).slice(2)}`, email_confirm: true },
  });
  assert.ok(ok(made.status), JSON.stringify(made.body));
  return made.body.id;
}

async function partyOf(leader: Player, members: Player[], options: { name?: string; max_members?: number; milestone?: number } = {}): Promise<string> {
  const made = await rpc(leader, "create_party", { name: options.name ?? "Friday circle", ...options });
  assert.equal(made.status, 200, JSON.stringify(made.body));
  for (const member of members) {
    assert.deepEqual((await rpc(leader, "invite_member", { party_id: made.body, username: member.username })).body, { status: "sent" });
    assert.deepEqual((await rpc(member, "respond_invite", { party_id: made.body, accept: true })).body, { status: "active" });
  }
  return made.body;
}

function post(who: Session | undefined, party: string, amount: unknown, extra: Record<string, unknown> = {}) {
  return call("/rest/v1/darood_entries", {
    token: who?.accessToken,
    body: { party_id: party, amount, local_date: day(), tz: "UTC", ...extra },
    headers: { Prefer: "return=representation" },
  });
}

// One cast, shared, to keep under the stack's sign-in limit.
let castPromise: Promise<{ leader: Player; member: Player; invitee: Player; outsider: Player }> | undefined;
function cast() {
  castPromise ??= (async () => {
    const [leader, member, invitee, outsider] = await Promise.all(["dl", "dm", "di", "do"].map((t) => player(`darood_${t}`)));
    return { leader, member, invitee, outsider };
  })();
  return castPromise;
}

localTest("a non-member reads nothing of a party, and signed out reads nothing at all", async () => {
  const { leader, member, invitee, outsider } = await cast();
  const party = await partyOf(leader, [member]);
  await rpc(leader, "invite_member", { party_id: party, username: invitee.username });
  assert.equal((await post(member, party, 313)).status, 201);

  const paths = [
    `/rest/v1/parties?id=eq.${party}`,
    `/rest/v1/party_members?party_id=eq.${party}`,
    `/rest/v1/darood_entries?party_id=eq.${party}`,
    `/rest/v1/party_stats?party_id=eq.${party}`,
    `/rest/v1/party_member_stats?party_id=eq.${party}`,
  ];
  for (const path of paths) {
    const seen = await call(path, { token: outsider.accessToken });
    assert.equal(seen.status, 200, path);
    assert.deepEqual(seen.body, [], `${path}: ${JSON.stringify(seen.body)}`);
    assert.equal((await call(path)).status, 401, `${path} signed out`);
  }
  // Nor the members' usernames: an outsider reads only their own.
  const names = (await call("/rest/v1/usernames?select=username", { token: outsider.accessToken })).body;
  assert.deepEqual(names, [{ username: outsider.username }]);
});

localTest("an invitee sees the party and their invitation, but no entries or stats", async () => {
  const { leader, member, invitee } = await cast();
  const party = await partyOf(leader, [member]);
  await rpc(leader, "invite_member", { party_id: party, username: invitee.username });
  await post(member, party, 100);

  const see = async (path: string) => (await call(path, { token: invitee.accessToken })).body;
  assert.equal((await see(`/rest/v1/parties?id=eq.${party}`)).length, 1);
  assert.deepEqual((await see(`/rest/v1/party_members?party_id=eq.${party}&select=user_id,status`)), [{ user_id: invitee.userId, status: "invited" }]);
  assert.deepEqual(await see(`/rest/v1/darood_entries?party_id=eq.${party}`), []);
  assert.deepEqual(await see(`/rest/v1/party_stats?party_id=eq.${party}`), []);
  // The leader's name, to say who invited them; not the members'.
  const names = (await see("/rest/v1/usernames?select=username")).map((n: any) => n.username).sort();
  assert.deepEqual(names, [invitee.username, leader.username].sort());
  assert.ok(!names.includes(member.username));
});

localTest("members see their party, its members, its entries and its stats", async () => {
  const { leader, member } = await cast();
  const party = await partyOf(leader, [member], { milestone: 100000 });
  await post(leader, party, 100);
  await post(leader, party, 50, { local_date: day(2) });
  await post(member, party, 313, { local_date: day(1) });

  const stats = (await call(`/rest/v1/party_stats?party_id=eq.${party}&select=total,entry_count,member_count,milestone`, { token: member.accessToken })).body;
  assert.deepEqual(stats, [{ total: 463, entry_count: 3, member_count: 2, milestone: 100000 }]);
  const perMember = (await call(`/rest/v1/party_member_stats?party_id=eq.${party}&select=username,total,active_days&order=total.desc`, { token: member.accessToken })).body;
  assert.deepEqual(perMember, [
    { username: member.username, total: 313, active_days: 1 },
    { username: leader.username, total: 150, active_days: 2 },
  ]);
  assert.equal((await call(`/rest/v1/darood_entries?party_id=eq.${party}`, { token: leader.accessToken })).body.length, 3);
});

localTest("a party holds 2 to 30 people: the 31st can't join", async () => {
  const { leader, outsider } = await cast();
  for (const max_members of [1, 31]) {
    const refused = await rpc(leader, "create_party", { name: "Too big or small", max_members });
    assert.equal(refused.status, 400, `${max_members}: ${JSON.stringify(refused.body)}`);
  }

  // 30 places: the leader and 29 others (filled directly, as accounts that never sign in).
  const party = await partyOf(leader, [], { max_members: 30 });
  const others = await Promise.all(Array.from({ length: 29 }, (_, i) => account(`seat${i}`)));
  for (const userId of others) {
    const seated = await asService("/rest/v1/party_members", { body: { party_id: party, user_id: userId, status: "active", joined_at: new Date().toISOString() } });
    assert.equal(seated.status, 201, JSON.stringify(seated.body));
  }
  const thirtyFirst = await rpc(leader, "invite_member", { party_id: party, username: outsider.username });
  assert.deepEqual([thirtyFirst.status, thirtyFirst.body.message], [400, "party_full"]);
  // Not even the server's own key can seat a 31st.
  const forced = await asService("/rest/v1/party_members", { body: { party_id: party, user_id: outsider.userId, status: "active" } });
  assert.deepEqual([forced.status, forced.body.message], [400, "party_full"]);

  // A party of 2 is full with its leader and one invitation.
  const pair = await partyOf(leader, [], { max_members: 2 });
  const third = await account("third");
  await asService("/rest/v1/party_members", { body: { party_id: pair, user_id: third, status: "invited" } });
  assert.equal((await rpc(leader, "invite_member", { party_id: pair, username: outsider.username })).body.message, "party_full");
});

localTest("entries are final, posted only by active members as themselves", async () => {
  const { leader, member, invitee, outsider } = await cast();
  const party = await partyOf(leader, [member]);
  await rpc(leader, "invite_member", { party_id: party, username: invitee.username });

  const mine = await post(member, party, 300);
  assert.equal(mine.status, 201);
  assert.equal(mine.body[0].user_id, member.userId);
  const id = mine.body[0].id;

  assert.equal((await post(member, party, 300, { user_id: leader.userId })).status, 403, "as someone else");
  assert.equal((await post(invitee, party, 300)).status, 403, "invited, not active");
  assert.equal((await post(outsider, party, 300)).status, 403, "outsider");
  assert.equal((await post(undefined, party, 300)).status, 401, "signed out");

  const edit = await call(`/rest/v1/darood_entries?id=eq.${id}`, { method: "PATCH", token: member.accessToken, body: { amount: 100000 } });
  assert.equal(edit.status, 403);
  const remove = await call(`/rest/v1/darood_entries?id=eq.${id}`, { method: "DELETE", token: member.accessToken });
  assert.equal(remove.status, 403);
  const leaderRemove = await call(`/rest/v1/darood_entries?id=eq.${id}`, { method: "DELETE", token: leader.accessToken });
  assert.equal(leaderRemove.status, 403);
  assert.deepEqual((await call(`/rest/v1/darood_entries?id=eq.${id}&select=amount`, { token: member.accessToken })).body, [{ amount: 300 }]);

  // A member who left or was removed can't post; their entries stay in the total.
  await rpc(member, "leave_party", { party_id: party });
  assert.equal((await post(member, party, 1)).status, 403);
  assert.equal((await call(`/rest/v1/party_stats?party_id=eq.${party}&select=total`, { token: leader.accessToken })).body[0].total, 300);
});

localTest("entries are whole numbers of 1 to 100,000, on a real recent day, 500,000 a day at most", async () => {
  const { leader } = await cast();
  const party = await partyOf(leader, []);
  for (const amount of [0, -5, 100001]) {
    const refused = await post(leader, party, amount);
    assert.deepEqual([refused.status, refused.body.code], [400, "23514"], String(amount));
  }
  for (const amount of [300.5, "300 today", "1e3"]) {
    assert.equal((await post(leader, party, amount)).status, 400, String(amount));
  }
  assert.equal((await post(leader, party, 1)).status, 201);
  assert.equal((await post(leader, party, 100000, { local_date: day(1) })).status, 201);

  // The day: today or up to 7 days back, in a real time zone.
  assert.equal((await post(leader, party, 1, { local_date: day(-1) })).body.message, "bad_day");
  assert.equal((await post(leader, party, 1, { local_date: day(8) })).body.message, "bad_day");
  assert.equal((await post(leader, party, 1, { local_date: day(7) })).status, 201);
  assert.equal((await post(leader, party, 1, { tz: "Mars/Olympus" })).body.message, "bad_time_zone");

  // 500,000 a day: today already has 1, so four more 100,000s and 99,999 fit, then nothing.
  for (const amount of [100000, 100000, 100000, 100000, 99999]) assert.equal((await post(leader, party, amount)).status, 201);
  assert.equal((await post(leader, party, 1)).body.message, "daily_limit");
  assert.equal((await post(leader, party, 1, { local_date: day(2) })).status, 201, "another day is its own");
  const other = await partyOf(leader, []);
  assert.equal((await post(leader, other, 100000)).status, 201, "another party is its own");
});

localTest("only the leader invites, removes, renames and sets the milestone; nobody writes the tables directly", async () => {
  const { leader, member, outsider } = await cast();
  const party = await partyOf(leader, [member]);

  assert.equal((await rpc(member, "invite_member", { party_id: party, username: outsider.username })).body.message, "not_leader");
  assert.equal((await rpc(member, "remove_member", { party_id: party, user_id: leader.userId })).body.message, "not_leader");
  assert.equal((await rpc(member, "set_milestone", { party_id: party, target: 5000 })).body.message, "not_leader");

  const rename = (who: Session, name: string) =>
    call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: who.accessToken, body: { name }, headers: { Prefer: "return=representation" } });
  assert.deepEqual((await rename(member, "Taken over")).body, []);
  assert.equal((await rename(leader, "Renamed")).body[0].name, "Renamed");
  assert.equal((await rename(leader, "x")).status, 400);
  for (const body of [{ max_members: 30 }, { leader_id: member.userId }, { completed_at: new Date().toISOString() }, { deleted_at: new Date().toISOString() }]) {
    const refused = await call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: leader.accessToken, body });
    assert.equal(refused.status, 403, JSON.stringify(body));
  }
  assert.equal((await call("/rest/v1/parties", { token: leader.accessToken, body: { name: "Direct", leader_id: leader.userId } })).status, 403);
  assert.equal((await call(`/rest/v1/parties?id=eq.${party}`, { method: "DELETE", token: leader.accessToken })).status, 403);
  assert.equal((await call("/rest/v1/party_members", { token: outsider.accessToken, body: { party_id: party, user_id: outsider.userId, status: "active" } })).status, 403);
  assert.equal((await call(`/rest/v1/party_members?party_id=eq.${party}&user_id=eq.${member.userId}`, { method: "PATCH", token: leader.accessToken, body: { status: "removed" } })).status, 403);

  // The milestone: above the total, set by the leader, never emptied.
  await post(member, party, 500);
  assert.equal((await rpc(leader, "set_milestone", { party_id: party, target: 500 })).body.message, "milestone_too_low");
  assert.equal((await rpc(leader, "set_milestone", { party_id: party, target: 501 })).body.milestone, 501);
  const emptied = await call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: leader.accessToken, body: { milestone: null } });
  assert.equal(emptied.body.message, "milestone_too_low");

  assert.deepEqual((await rpc(leader, "remove_member", { party_id: party, user_id: member.userId })).body, { status: "removed" });
  assert.equal((await rpc(leader, "remove_member", { party_id: party, user_id: leader.userId })).body.message, "not_a_member");
});

localTest("invitations: the same answer for unknown names, then accepted, declined or renewed", async () => {
  const { leader, member, invitee } = await cast();
  const party = await partyOf(leader, []);
  const members = async () => (await asService(`/rest/v1/party_members?party_id=eq.${party}&select=user_id,status`)).body.length;

  assert.deepEqual((await rpc(leader, "invite_member", { party_id: party, username: "nobody_has_this_name" })).body, { status: "sent" });
  assert.equal(await members(), 1, "nothing stored for an unknown name");
  assert.deepEqual((await rpc(leader, "invite_member", { party_id: party, username: invitee.username.toUpperCase() })).body, { status: "sent" });
  assert.deepEqual((await rpc(leader, "invite_member", { party_id: party, username: invitee.username })).body, { status: "sent" });
  assert.equal(await members(), 2);
  assert.deepEqual((await rpc(leader, "invite_member", { party_id: party, username: leader.username })).body, { status: "already_member" });

  assert.deepEqual((await rpc(invitee, "respond_invite", { party_id: party, accept: false })).body, { status: "declined" });
  assert.equal((await rpc(invitee, "respond_invite", { party_id: party, accept: true })).body.message, "no_invitation");
  assert.deepEqual((await rpc(leader, "invite_member", { party_id: party, username: invitee.username })).body, { status: "sent" });
  assert.deepEqual((await rpc(invitee, "respond_invite", { party_id: party, accept: true })).body, { status: "active" });
  assert.equal((await rpc(member, "respond_invite", { party_id: party, accept: true })).body.message, "no_invitation");

  // A player without a username can't start a party.
  const nameless = await newPlayer("darood_nameless");
  assert.equal((await rpc(nameless, "create_party", { name: "Mine" })).body.message, "username_required");
});

localTest("members leave, the leader hands over first, and a deleted leader's party passes on", async () => {
  const [leader, first, second] = await Promise.all(["ll", "lf", "ls"].map((t) => player(`darood_${t}`)));
  const party = await partyOf(leader, [first, second]);
  assert.equal((await rpc(leader, "leave_party", { party_id: party })).body.message, "leader_must_hand_over");
  assert.deepEqual((await rpc(second, "leave_party", { party_id: party })).body, { status: "left" });
  assert.equal((await rpc(second, "leave_party", { party_id: party })).body.message, "not_a_member");

  assert.equal((await rpc(leader, "delete_user_account", {})).status, 204);
  const leads = (await asService(`/rest/v1/parties?id=eq.${party}&select=leader_id`)).body[0].leader_id;
  assert.equal(leads, first.userId);
  const leaderRows = (await asService(`/rest/v1/party_members?party_id=eq.${party}&role=eq.leader&select=user_id`)).body;
  assert.deepEqual(leaderRows, [{ user_id: first.userId }]);
});

localTest("a party finishes once, when its total first reaches the milestone, and for good", async () => {
  const { leader, member } = await cast();
  const party = await partyOf(leader, [member], { milestone: 1000 });
  const finished = async () => (await asService(`/rest/v1/parties?id=eq.${party}&select=completed_at`)).body[0].completed_at;

  await post(member, party, 600);
  assert.equal(await finished(), null);
  const together = await Promise.all([post(member, party, 300), post(leader, party, 300)]);
  assert.deepEqual(together.map((t) => t.status), [201, 201]);
  const stamp = await finished();
  assert.equal(typeof stamp, "string");

  assert.equal((await post(leader, party, 1)).body.message, "party_closed");
  assert.equal((await rpc(leader, "set_milestone", { party_id: party, target: 5000 })).body.message, "party_closed");
  const undo = await asService(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", body: { completed_at: null } });
  assert.equal(undo.body.message, "completion_is_final");
  assert.equal(await finished(), stamp);
  assert.equal((await call(`/rest/v1/party_stats?party_id=eq.${party}&select=total`, { token: leader.accessToken })).body[0].total, 1200);
});
