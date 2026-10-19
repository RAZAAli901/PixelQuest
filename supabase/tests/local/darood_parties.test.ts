// Darood Parties on the server (migrations 20261019000004 to 20261019000022): who reads what, the
// limits, the leader's rights, invitations, leaving, and finishing. Every rule the app relies on is
// checked here against the real database, row-level security and functions.
import assert from "node:assert/strict";
import { asService, call, localTest, newEmail, newPlayer, sql, type Session } from "./stack.ts";

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
  // The suite shares one leader, who invites more than 20 times an hour across it: its count starts
  // over for each party here (the limit has its own test).
  if (members.length > 0) sql(`DELETE FROM private.invite_log WHERE inviter = '${leader.userId}'`);
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

localTest("the leader hands over to an active member, then can leave; nobody else can (20261019000027)", async () => {
  const { leader, member, invitee, outsider } = await cast();
  const party = await partyOf(leader, [member]);
  await rpc(leader, "invite_member", { party_id: party, username: invitee.username });
  const hand = (who: Session, to: string) => rpc(who, "transfer_leadership", { party_id: party, user_id: to });

  assert.equal((await hand(member, member.userId)).body.message, "not_leader");
  assert.equal((await hand(outsider, outsider.userId)).status, 403);
  for (const to of [invitee.userId, outsider.userId, leader.userId]) {
    const refused = await hand(leader, to);
    assert.deepEqual([refused.status, refused.body.message], [400, "not_a_member"], to);
  }
  assert.equal((await call("/rest/v1/rpc/transfer_leadership", { body: { party_id: party, user_id: member.userId } })).status, 401);

  assert.deepEqual((await hand(leader, member.userId)).body, { leader_id: member.userId });
  const roles = (await asService(`/rest/v1/party_members?party_id=eq.${party}&status=eq.active&select=user_id,role&order=role`)).body;
  assert.deepEqual(roles, [{ user_id: member.userId, role: "leader" }, { user_id: leader.userId, role: "member" }]);
  assert.equal((await asService(`/rest/v1/parties?id=eq.${party}&select=leader_id`)).body[0].leader_id, member.userId);

  // The new leader leads; the old one is a member, who can now leave.
  assert.equal((await rpc(leader, "set_milestone", { party_id: party, target: 5000 })).body.message, "not_leader");
  assert.equal((await rpc(member, "set_milestone", { party_id: party, target: 5000 })).body.milestone, 5000);
  assert.equal((await hand(leader, leader.userId)).body.message, "not_leader");
  assert.deepEqual((await rpc(leader, "leave_party", { party_id: party })).body, { status: "left" });
  assert.equal((await rpc(member, "leave_party", { party_id: party })).body.message, "leader_must_hand_over");
});

localTest("only the leader deletes a party, and then it's gone for everyone (20261019000028)", async () => {
  const { leader, member, invitee, outsider } = await cast();
  const party = await partyOf(leader, [member]);
  await rpc(leader, "invite_member", { party_id: party, username: invitee.username });
  assert.equal((await post(member, party, 300)).status, 201);

  assert.equal((await rpc(member, "delete_party", { party_id: party })).body.message, "not_leader");
  assert.equal((await rpc(outsider, "delete_party", { party_id: party })).status, 403);
  assert.equal((await call("/rest/v1/rpc/delete_party", { body: { party_id: party } })).status, 401);

  assert.deepEqual((await rpc(leader, "delete_party", { party_id: party })).body, { status: "deleted" });
  assert.deepEqual((await rpc(leader, "delete_party", { party_id: party })).body, { status: "deleted" }, "again: the same answer");

  for (const who of [leader, member, invitee]) {
    for (const path of [`/rest/v1/parties?id=eq.${party}`, `/rest/v1/darood_entries?party_id=eq.${party}`, `/rest/v1/party_stats?party_id=eq.${party}`, `/rest/v1/party_member_stats?party_id=eq.${party}`]) {
      assert.deepEqual((await call(path, { token: who.accessToken })).body, [], `${path} for ${who.username}`);
    }
  }
  assert.equal((await post(member, party, 1)).body.message, "party_closed");
  assert.equal((await rpc(leader, "invite_member", { party_id: party, username: outsider.username })).body.message, "not_leader");
  assert.equal((await rpc(invitee, "respond_invite", { party_id: party, accept: true })).body.message, "no_invitation");
  assert.equal((await rpc(leader, "set_milestone", { party_id: party, target: 5000 })).body.message, "not_leader");

  // Hidden, not erased: the entries are still there.
  assert.equal((await asService(`/rest/v1/darood_entries?party_id=eq.${party}&select=amount`)).body.length, 1);
});

localTest("a party's day: each poster's total for a day, for its members alone (20261019000029)", async () => {
  const { leader, member, invitee, outsider } = await cast();
  const party = await partyOf(leader, [member]);
  await rpc(leader, "invite_member", { party_id: party, username: invitee.username });
  for (const [who, amount, back] of [[leader, 300, 0], [leader, 13, 0], [member, 2000, 0], [member, 50, 1]] as const) {
    assert.equal((await post(who, party, amount, { local_date: day(back) })).status, 201);
  }
  const today = `/rest/v1/party_day_stats?party_id=eq.${party}&local_date=eq.${day()}&select=user_id,total,entry_count&order=total`;

  assert.deepEqual((await call(today, { token: member.accessToken })).body, [
    { user_id: leader.userId, total: 313, entry_count: 2 },
    { user_id: member.userId, total: 2000, entry_count: 1 },
  ]);
  assert.deepEqual((await call(`/rest/v1/party_day_stats?party_id=eq.${party}&local_date=eq.${day(1)}&select=user_id,total`, { token: leader.accessToken })).body, [
    { user_id: member.userId, total: 50 },
  ]);
  for (const who of [invitee, outsider]) assert.deepEqual((await call(today, { token: who.accessToken })).body, [], "not theirs to see");
  assert.equal((await call(today)).status, 401);
  assert.equal((await call("/rest/v1/party_day_stats", { method: "POST", token: leader.accessToken, body: { party_id: party } })).status >= 400, true);

  await rpc(leader, "delete_party", { party_id: party });
  assert.deepEqual((await call(today, { token: leader.accessToken })).body, [], "gone with the party");
});

localTest("the race board: opted-in parties with a milestone, name and progress only (20261019000030)", async () => {
  const { leader, member, outsider } = await cast();
  const racing = await partyOf(leader, [member], { name: fresh("Racers"), milestone: 1000 });
  const quiet = await partyOf(leader, [], { name: fresh("Quiet"), milestone: 1000 });
  const aimless = await partyOf(leader, [], { name: fresh("Aimless") });
  assert.equal((await post(member, racing, 250)).status, 201);
  const race = (who: Session, party: string, on: boolean) =>
    call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: who.accessToken, body: { is_public_race: on }, headers: { Prefer: "return=representation" } });
  const nameOf = async (party: string) => (await asService(`/rest/v1/parties?id=eq.${party}&select=name`)).body[0].name;
  const board = async (who: Session) => (await call("/rest/v1/race_board?select=*", { token: who.accessToken })).body as Record<string, unknown>[];

  // Off by default: nothing shows.
  assert.equal((await asService(`/rest/v1/parties?id=eq.${racing}&select=is_public_race`)).body[0].is_public_race, false);
  const racingName = await nameOf(racing);
  assert.ok(!(await board(outsider)).some((row) => row.name === racingName));

  // Only the leader turns it on; a member's update changes nothing.
  assert.deepEqual((await race(member, racing, true)).body, []);
  assert.equal((await race(leader, racing, true)).body[0].is_public_race, true);
  assert.equal((await race(leader, aimless, true)).status, 200);
  const row = (await board(outsider)).find((r) => r.name === racingName);
  assert.deepEqual(row, { name: racingName, member_count: 2, total: 250, milestone: 1000, progress: 0.25, completed_at: null, mine: false });
  assert.equal((await board(member)).find((r) => r.name === racingName)?.mine, true, "a member's own is marked");

  // Never the quiet one, nor one without a milestone; never anything signed out.
  const names = (await board(outsider)).map((r) => r.name);
  assert.ok(!names.includes(await nameOf(quiet)));
  assert.ok(!names.includes(await nameOf(aimless)));
  assert.equal((await call("/rest/v1/race_board?select=*")).status, 401);
  assert.equal((await call("/rest/v1/race_board", { method: "POST", token: leader.accessToken, body: { name: "x" } })).status >= 400, true);

  // Off again, or deleted: gone.
  await race(leader, racing, false);
  assert.ok(!(await board(outsider)).some((r) => r.name === racingName));
  await race(leader, racing, true);
  await rpc(leader, "delete_party", { party_id: racing });
  assert.ok(!(await board(outsider)).some((r) => r.name === racingName));
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

localTest("an entry sent twice counts once, even if the first answer was lost (20261019000024)", async () => {
  const { leader, member } = await cast();
  const party = await partyOf(leader, [member], { milestone: 1000 });
  const send = (amount: number, client_id: string) =>
    rpc(member, "add_darood_entry", { party_id: party, amount, local_date: day(), tz: "UTC", client_id });
  const key = crypto.randomUUID();

  const first = await send(300, key);
  assert.equal(first.status, 200, JSON.stringify(first.body));
  const again = await send(300, key);
  assert.equal(again.body.id, first.body.id, "the same entry comes back");

  // Two copies at once: one row.
  const twin = crypto.randomUUID();
  const both = await Promise.all([send(200, twin), send(200, twin)]);
  assert.equal(both[0].body.id, both[1].body.id, JSON.stringify(both.map((b) => b.body)));
  const total = async () => (await call(`/rest/v1/party_stats?party_id=eq.${party}&select=total`, { token: leader.accessToken })).body[0].total;
  assert.equal(await total(), 500);

  // The entry that finishes the party, sent again after its answer was lost, is found, not refused.
  const finisher = crypto.randomUUID();
  assert.equal((await send(500, finisher)).status, 200);
  const retried = await send(500, finisher);
  assert.equal(retried.status, 200, JSON.stringify(retried.body));
  assert.equal(await total(), 1000);
  // A new entry after finishing is refused as before.
  assert.equal((await send(1, crypto.randomUUID())).body.message, "party_closed");

  // Keys are per account: another member's same key is their own entry.
  const shared = crypto.randomUUID();
  const other = await partyOf(leader, [member]);
  assert.equal((await rpc(leader, "add_darood_entry", { party_id: other, amount: 1, local_date: day(), tz: "UTC", client_id: shared })).status, 200);
  assert.equal((await rpc(member, "add_darood_entry", { party_id: other, amount: 1, local_date: day(), tz: "UTC", client_id: shared })).status, 200);
  assert.equal((await call(`/rest/v1/party_stats?party_id=eq.${other}&select=total`, { token: leader.accessToken })).body[0].total, 2);
});

localTest("entries and memberships are published for live updates (20261019000025)", () => {
  const published = sql("SELECT string_agg(tablename, ',' ORDER BY tablename) FROM pg_publication_tables WHERE pubname = 'supabase_realtime' AND schemaname = 'public'");
  for (const table of ["darood_entries", "party_members"]) assert.ok(published.split(",").includes(table), published);
  // Nothing else of the game is published: profiles, usernames and parties stay out.
  for (const table of ["profiles", "usernames", "parties"]) assert.ok(!published.split(",").includes(table), published);
});

localTest("at most 20 invitations an hour per account, unknown names included (20261019000026)", async () => {
  const [leader, friend] = await Promise.all(["rl", "rf"].map((t) => player(`darood_${t}`)));
  const first = await partyOf(leader, [], { max_members: 30 });
  const second = await partyOf(leader, [], { max_members: 30 });
  // 20 across two parties, made-up names among them.
  for (let i = 0; i < 20; i++) {
    const answer = await rpc(leader, "invite_member", { party_id: i % 2 ? first : second, username: `nobody_${i}_${Date.now() % 1e5}` });
    assert.deepEqual(answer.body, { status: "sent" }, String(i));
  }
  const twentyFirst = await rpc(leader, "invite_member", { party_id: first, username: friend.username });
  assert.deepEqual([twentyFirst.status, twentyFirst.body.message], [400, "too_many_invites"]);
  // Another leader isn't held back by this one.
  const own = await partyOf(friend, []);
  assert.deepEqual((await rpc(friend, "invite_member", { party_id: own, username: leader.username })).body, { status: "sent" });
  // An hour on, it's allowed again.
  sql(`UPDATE private.invite_log SET at = now() - interval '61 minutes' WHERE inviter = '${leader.userId}'`);
  assert.deepEqual((await rpc(leader, "invite_member", { party_id: first, username: friend.username })).body, { status: "sent" });
});

