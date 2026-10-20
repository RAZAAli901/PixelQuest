// Parties on the server (migrations 20261019000004 to 20261019000043): who reads what, the limits,
// the leader's rights, invitations, leaving, and finishing. Every rule the app relies on is checked
// here against the real database, row-level security and functions. Darood Parties first; then Study
// parties, which share the same core (docs/party-core.md) and none of Darood's own rules, and their
// own items, completions, study time and weekly points (docs/study-party.md). They share
// one file, and one cast of players, to keep under the stack's sign-in limit.
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

async function partyOf(leader: Player, members: Player[], options: { name?: string; max_members?: number; milestone?: number; party_type?: string } = {}): Promise<string> {
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

localTest("privacy: an outsider sees an opted-in party's race line, and no name, id or entry anywhere", async () => {
  const { leader, member, outsider } = await cast();
  const party = await partyOf(leader, [member], { name: fresh("Private"), milestone: 5000 });
  for (const [who, amount] of [[leader, 313], [member, 1234]] as const) assert.equal((await post(who, party, amount)).status, 201);
  await call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: leader.accessToken, body: { is_public_race: true } });
  const secrets = [leader.userId, member.userId, leader.username, member.username, party];
  // The entries' amounts too, except on the board, whose totals belong to many parties (theirs included).
  const amounts = ["1234", "313"];

  // Everything an outsider might read, every way they might ask.
  const reads: [string, unknown][] = [];
  for (const path of [
    `/rest/v1/parties?select=*`,
    `/rest/v1/party_members?select=*`,
    `/rest/v1/darood_entries?select=*`,
    `/rest/v1/party_stats?select=*`,
    `/rest/v1/party_member_stats?select=*`,
    `/rest/v1/party_day_stats?select=*`,
    `/rest/v1/usernames?select=*`,
    `/rest/v1/race_board?select=*`,
  ]) {
    const answer = await call(path, { token: outsider.accessToken });
    reads.push([path, answer.body]);
  }
  // An amount is shown only as a number of its own: inside a random id or a timestamp ("...6313254..."), it's chance.
  const shows = (text: string, secret: string) =>
    amounts.includes(secret) ? new RegExp(`(?<![\\w.-])${secret}(?![\\w.])`).test(text) : text.includes(secret);
  for (const [path, body] of reads) {
    const text = JSON.stringify(body);
    const hidden = path.startsWith("/rest/v1/race_board") ? secrets : [...secrets, ...amounts];
    for (const secret of hidden) assert.ok(!shows(text, secret), `${path} shows ${secret}: ${text.slice(0, 300)}`);
  }

  // The board shows the line, with its six columns and mine, and only those.
  const line = ((reads.find(([path]) => path.startsWith("/rest/v1/race_board"))![1]) as Record<string, unknown>[])
    .find((row) => row.total === 1547);
  assert.ok(line, "the party's line is there");
  assert.deepEqual(Object.keys(line!).sort(), ["completed_at", "member_count", "milestone", "mine", "name", "progress", "total"]);
  assert.equal(line!.mine, false);

  // Asking the board for more gets nothing more: no hidden columns, nothing to embed.
  for (const path of [
    "/rest/v1/race_board?select=id",
    "/rest/v1/race_board?select=leader_id",
    "/rest/v1/race_board?select=*,party_members(*)",
    "/rest/v1/race_board?select=*,parties(*)",
  ]) {
    const answer = await call(path, { token: outsider.accessToken });
    assert.ok(answer.status >= 400, `${path} answered ${answer.status}: ${JSON.stringify(answer.body).slice(0, 200)}`);
  }
  // Nor can it be filtered by what it doesn't show.
  assert.ok((await call(`/rest/v1/race_board?id=eq.${party}`, { token: outsider.accessToken })).status >= 400);
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

// Study parties: the same core, none of Darood's own (20261019000031 to 20261019000034).

// The new form takes a member limit as well as the kind: the app always sends one.
const study = { party_type: "study", max_members: 10 };

localTest("a study party starts with its leader, within the same limits, and with no milestone (20261019000034)", async () => {
  const { leader, outsider } = await cast();
  const party = await partyOf(leader, [], { name: "Physics group", ...study, max_members: 6 });
  const row = (await call(`/rest/v1/parties?id=eq.${party}&select=party_type,max_members,milestone,completed_at,is_public_race`, { token: leader.accessToken })).body;
  assert.deepEqual(row, [{ party_type: "study", max_members: 6, milestone: null, completed_at: null, is_public_race: false }]);
  const members = (await call(`/rest/v1/party_members?party_id=eq.${party}&select=user_id,role,status`, { token: leader.accessToken })).body;
  assert.deepEqual(members, [{ user_id: leader.userId, role: "leader", status: "active" }]);

  for (const body of [{ max_members: 1 }, { max_members: 31 }, { name: "x" }]) {
    const refused = await rpc(leader, "create_party", { name: "Study circle", ...study, max_members: 6, ...body });
    assert.equal(refused.status, 400, JSON.stringify(body));
  }
  const unknown = await rpc(leader, "create_party", { name: "Chess club", max_members: 6, party_type: "chess" });
  assert.deepEqual([unknown.status, unknown.body.message], [400, "unknown_party_type"]);
  // The 1.4 form, without a kind, still starts a Darood Party.
  const old = await rpc(leader, "create_party", { name: "Old form", max_members: 6, milestone: 1000 });
  assert.deepEqual((await call(`/rest/v1/parties?id=eq.${old.body}&select=party_type,milestone`, { token: leader.accessToken })).body, [{ party_type: "darood", milestone: 1000 }]);
  // Signed out, nothing starts.
  assert.equal((await call("/rest/v1/rpc/create_party", { body: { name: "Anyone", max_members: 6, ...study } })).status, 401);

  // 30 places, as in a Darood Party: the leader and 29 others, then the 31st is refused.
  const full = await partyOf(leader, [], { ...study, max_members: 30 });
  const others = await Promise.all(Array.from({ length: 29 }, (_, i) => account(`studyseat${i}`)));
  for (const userId of others) {
    const seated = await asService("/rest/v1/party_members", { body: { party_id: full, user_id: userId, status: "active", joined_at: new Date().toISOString() } });
    assert.equal(seated.status, 201, JSON.stringify(seated.body));
  }
  const thirtyFirst = await rpc(leader, "invite_member", { party_id: full, username: outsider.username });
  assert.deepEqual([thirtyFirst.status, thirtyFirst.body.message], [400, "party_full"]);
});

localTest("a study party's reading, invitations and leader work as a Darood Party's", async () => {
  const { leader, member, invitee, outsider } = await cast();
  const party = await partyOf(leader, [member], study);
  await rpc(leader, "invite_member", { party_id: party, username: invitee.username });

  // An outsider reads nothing; signed out, nothing at all.
  for (const path of [`/rest/v1/parties?id=eq.${party}`, `/rest/v1/party_members?party_id=eq.${party}`]) {
    assert.deepEqual((await call(path, { token: outsider.accessToken })).body, [], path);
    assert.equal((await call(path)).status, 401, `${path} signed out`);
  }
  // The invitee sees the party and their own row; the members see everyone.
  assert.equal((await call(`/rest/v1/parties?id=eq.${party}`, { token: invitee.accessToken })).body.length, 1);
  assert.deepEqual((await call(`/rest/v1/party_members?party_id=eq.${party}&select=user_id`, { token: invitee.accessToken })).body, [{ user_id: invitee.userId }]);
  assert.equal((await call(`/rest/v1/party_members?party_id=eq.${party}`, { token: member.accessToken })).body.length, 3);

  // Only the leader invites, removes and renames; its kind never changes.
  assert.equal((await rpc(member, "invite_member", { party_id: party, username: outsider.username })).body.message, "not_leader");
  assert.equal((await rpc(member, "remove_member", { party_id: party, user_id: leader.userId })).body.message, "not_leader");
  const rename = (who: Session, name: string) =>
    call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: who.accessToken, body: { name }, headers: { Prefer: "return=representation" } });
  assert.deepEqual((await rename(member, "Taken over")).body, []);
  assert.equal((await rename(leader, "Exam group")).body[0].name, "Exam group");
  assert.equal((await call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: leader.accessToken, body: { party_type: "darood" } })).status, 403, "its kind never changes");

  // Answering, handing over, leaving, deleting: the same.
  assert.deepEqual((await rpc(invitee, "respond_invite", { party_id: party, accept: false })).body, { status: "declined" });
  assert.equal((await rpc(leader, "leave_party", { party_id: party })).body.message, "leader_must_hand_over");
  assert.deepEqual((await rpc(leader, "transfer_leadership", { party_id: party, user_id: member.userId })).body, { leader_id: member.userId });
  assert.deepEqual((await rpc(leader, "leave_party", { party_id: party })).body, { status: "left" });
  assert.equal((await rpc(leader, "delete_party", { party_id: party })).body.message, "not_leader");
  assert.deepEqual((await rpc(member, "delete_party", { party_id: party })).body, { status: "deleted" });
  assert.deepEqual((await call(`/rest/v1/parties?id=eq.${party}`, { token: member.accessToken })).body, []);
});

localTest("a study party has none of Darood's own: no entries, milestone or race board (20261019000033)", async () => {
  const { leader, member, outsider } = await cast();
  const party = await partyOf(leader, [member], { name: fresh("Quiet"), ...study });

  const entry = await post(member, party, 33);
  assert.deepEqual([entry.status, entry.body.message], [400, "not_a_darood_party"]);
  const viaFunction = await rpc(member, "add_darood_entry", { party_id: party, amount: 33, local_date: day(), tz: "UTC", client_id: crypto.randomUUID() });
  assert.equal(viaFunction.body.message, "not_a_darood_party");

  const milestone = await rpc(leader, "set_milestone", { party_id: party, target: 5000 });
  assert.equal(milestone.status, 400);
  assert.match(milestone.body.message, /darood_only_columns/);
  const race = await call(`/rest/v1/parties?id=eq.${party}`, { method: "PATCH", token: leader.accessToken, body: { is_public_race: true } });
  assert.equal(race.status, 400);
  assert.match(race.body.message, /darood_only_columns/);

  const name = (await call(`/rest/v1/parties?id=eq.${party}&select=name`, { token: leader.accessToken })).body[0].name;
  const board = (await call("/rest/v1/race_board?select=name", { token: outsider.accessToken })).body;
  assert.ok(!board.some((line: { name: string }) => line.name === name));
});

// A study party's own: items, completions, study time, weekly points (20261019000035 to 20261019000043).

/** [tz]'s date now, as yyyy-MM-dd. */
const todayIn = (tz: string) => new Intl.DateTimeFormat("en-CA", { timeZone: tz }).format(new Date());

/** The Monday of [date]'s week (yyyy-MM-dd), as the scores count it. */
function mondayOf(date: string): string {
  const d = new Date(`${date}T00:00:00Z`);
  d.setUTCDate(d.getUTCDate() - ((d.getUTCDay() + 6) % 7));
  return d.toISOString().slice(0, 10);
}

const returning = { Prefer: "return=representation" };

function addItem(who: Session, party: string, body: Record<string, unknown>) {
  return call("/rest/v1/study_items", { token: who.accessToken, body: { party_id: party, ...body }, headers: returning });
}

function complete(who: Session, item: string, tz = "UTC", extra: Record<string, unknown> = {}) {
  return call("/rest/v1/study_completions", { token: who.accessToken, body: { item_id: item, local_date: todayIn(tz), tz, ...extra }, headers: returning });
}

function logStudy(who: Session, party: string, minutes: number, tz = "UTC", extra: Record<string, unknown> = {}) {
  return call("/rest/v1/study_sessions", { token: who.accessToken, body: { party_id: party, minutes, local_date: todayIn(tz), tz, ...extra }, headers: returning });
}

const inDays = (days: number) => new Date(Date.now() + days * 86_400_000).toISOString();

localTest("a study party's items, completions, study time and scores are its active members' alone", async () => {
  const { leader, member, invitee, outsider } = await cast();
  const party = await partyOf(leader, [member], { name: fresh("Study"), ...study });
  await rpc(leader, "invite_member", { party_id: party, username: invitee.username });
  const item = (await addItem(member, party, { kind: "assignment", title: "Essay" })).body[0].id;
  assert.equal((await complete(member, item)).status, 201);
  assert.equal((await logStudy(member, party, 30)).status, 201);

  const paths = [
    `/rest/v1/study_items?party_id=eq.${party}`,
    `/rest/v1/study_completions?party_id=eq.${party}`,
    `/rest/v1/study_sessions?party_id=eq.${party}`,
    `/rest/v1/study_week_scores?party_id=eq.${party}`,
    `/rest/v1/study_week_winners?party_id=eq.${party}`,
  ];
  for (const path of paths) {
    for (const who of [invitee, outsider]) assert.deepEqual((await call(path, { token: who.accessToken })).body, [], `${path} for ${who.username}`);
    assert.equal((await call(path)).status, 401, `${path} signed out`);
  }
  for (const path of paths.slice(0, 4)) assert.equal((await call(path, { token: leader.accessToken })).body.length, 1, path);

  // Outsiders write nothing either; nor does a Darood Party take study items.
  assert.equal((await addItem(outsider, party, { kind: "assignment", title: "Spam" })).status, 403);
  assert.equal((await logStudy(outsider, party, 30)).status, 403);
  assert.equal((await complete(outsider, item)).status, 403);
  const darood = await partyOf(leader, [], { name: fresh("Darood") });
  assert.equal((await addItem(leader, darood, { kind: "assignment", title: "Not here" })).status, 403);
});

localTest("items: any member adds one, only its creator or the leader edits it, only the leader pins", async () => {
  const { leader, member } = await cast();
  const party = await partyOf(leader, [member], { name: fresh("Items"), ...study });

  const mine = (await addItem(member, party, { kind: "assignment", title: "Lab report", subject: "Physics" })).body[0];
  assert.equal(mine.created_by, member.userId);
  const theirs = (await addItem(leader, party, { kind: "quiz", title: "Quiz 1", due_at: inDays(3) })).body[0];
  const edit = (who: Session, id: string, body: Record<string, unknown>) =>
    call(`/rest/v1/study_items?id=eq.${id}`, { method: "PATCH", token: who.accessToken, body, headers: returning });

  assert.equal((await edit(member, mine.id, { title: "Lab report 2" })).body[0].title, "Lab report 2");
  assert.deepEqual((await edit(member, theirs.id, { title: "Taken" })).body, [], "not the member's to edit");
  assert.equal((await edit(leader, mine.id, { notes: "Due after lunch" })).body[0].notes, "Due after lunch");
  assert.equal((await edit(member, mine.id, { pinned: true })).body.message, "not_leader");
  assert.equal((await addItem(member, party, { kind: "assignment", title: "Pinned", pinned: true })).body.message, "not_leader");
  assert.equal((await edit(leader, mine.id, { pinned: true })).body[0].pinned, true);
  assert.equal((await edit(member, mine.id, { created_by: leader.userId })).status, 403, "who added it is for good");

  // The limits: a quiz needs a date; a title is 2 to 80; notes up to 500.
  for (const body of [{ kind: "quiz", title: "No date" }, { kind: "assignment", title: "x" }, { kind: "assignment", title: "Long", notes: "n".repeat(501) }, { kind: "exam", title: "Exam" }]) {
    assert.equal((await addItem(member, party, body)).status, 400, JSON.stringify(body));
  }

  assert.deepEqual((await call(`/rest/v1/study_items?id=eq.${theirs.id}`, { method: "DELETE", token: member.accessToken, headers: returning })).body, []);
  assert.equal((await call(`/rest/v1/study_items?id=eq.${mine.id}`, { method: "DELETE", token: member.accessToken, headers: returning })).body.length, 1);
});

localTest("completions: your own, once, undone within 24 hours, then final", async () => {
  const { leader, member } = await cast();
  const party = await partyOf(leader, [member], { name: fresh("Done"), ...study });
  const item = (await addItem(leader, party, { kind: "assignment", title: "Essay" })).body[0].id;

  const done = await complete(member, item, "Asia/Karachi");
  assert.equal(done.status, 201);
  assert.deepEqual([done.body[0].user_id, done.body[0].party_id], [member.userId, party]);
  assert.equal((await complete(member, item)).status, 409, "once each");
  assert.equal((await complete(member, item, "UTC", { tz: "Mars/Olympus" })).status, 400, "a real time zone");
  const sneaky = await call("/rest/v1/study_completions", { token: leader.accessToken, body: { item_id: item, user_id: member.userId, local_date: todayIn("UTC"), tz: "UTC" } });
  assert.equal(sneaky.status, 403, "never for someone else");

  const undo = (who: Session) => call(`/rest/v1/study_completions?item_id=eq.${item}&user_id=eq.${member.userId}`, { method: "DELETE", token: who.accessToken, headers: returning });
  assert.deepEqual((await undo(leader)).body, [], "not the leader's to undo");
  assert.equal((await undo(member)).body.length, 1, "undone within a day");
  assert.equal((await complete(member, item)).status, 201);
  sql(`UPDATE public.study_completions SET completed_at = now() - interval '25 hours' WHERE item_id = '${item}'`);
  assert.deepEqual((await undo(member)).body, [], "final after 24 hours");
  assert.equal((await call(`/rest/v1/study_completions?item_id=eq.${item}`, { method: "PATCH", token: member.accessToken, body: { local_date: todayIn("UTC") } })).status, 403);
});

localTest("points: 10 an assignment, 15 a quiz, +5 a day early, study time 1 per 10 minutes and 12 a day", async () => {
  const { leader, member } = await cast();
  const party = await partyOf(leader, [member], { name: fresh("Points"), ...study });
  const essay = (await addItem(leader, party, { kind: "assignment", title: "Essay" })).body[0].id;
  const quiz = (await addItem(leader, party, { kind: "quiz", title: "Quiz 1", due_at: inDays(3) })).body[0].id;
  const lateQuiz = (await addItem(leader, party, { kind: "quiz", title: "Quiz 2", due_at: inDays(0.5) })).body[0].id;
  for (const item of [essay, quiz, lateQuiz]) assert.equal((await complete(member, item)).status, 201);
  // 25 + 40 minutes today: 65, so 6 points.
  for (const minutes of [25, 40]) assert.equal((await logStudy(member, party, minutes)).status, 201);
  // Out of bounds: 4 and 241 minutes, a day in the future, 8 days back.
  for (const extra of [{ minutes: 4 }, { minutes: 241 }]) assert.equal((await logStudy(member, party, extra.minutes)).status, 400);
  const future = new Date(Date.now() + 2 * 86_400_000).toISOString().slice(0, 10);
  const longAgo = new Date(Date.now() - 9 * 86_400_000).toISOString().slice(0, 10);
  for (const local_date of [future, longAgo]) assert.equal((await logStudy(member, party, 30, "UTC", { local_date })).body.message, "bad_day");

  const scores = (await call(`/rest/v1/study_week_scores?party_id=eq.${party}&user_id=eq.${member.userId}&select=week_start,points,item_points,bonus_points,study_points,items_completed,study_minutes`, { token: leader.accessToken })).body;
  assert.deepEqual(scores, [{ week_start: mondayOf(todayIn("UTC")), points: 51, item_points: 40, bonus_points: 5, study_points: 6, items_completed: 3, study_minutes: 65 }]);

  // 200 minutes in a day is 20 points' worth, capped at 12.
  assert.equal((await logStudy(member, party, 200)).status, 201);
  const capped = (await call(`/rest/v1/study_week_scores?party_id=eq.${party}&user_id=eq.${member.userId}&select=study_points`, { token: leader.accessToken })).body;
  assert.deepEqual(capped, [{ study_points: 12 }]);

  // The values are the ones the app mirrors (StudyPointsConfig).
  const config = (await call("/rest/v1/study_points_config?select=*", { token: member.accessToken })).body;
  assert.deepEqual(config, [{ assignment: 10, quiz: 15, early_bonus: 5, minutes_per_point: 10, daily_study_max: 12 }]);
});

localTest("a week is each member's own: Monday to Sunday by the day they recorded, in their time zone", async () => {
  const { leader, member } = await cast();
  const party = await partyOf(leader, [member], { name: fresh("Weeks"), ...study });
  // The same moment, Sunday 18 October 2026 at 22:00 UTC: Monday the 19th in Karachi, still Sunday in New York.
  sql(`BEGIN;
    ALTER TABLE public.study_sessions DISABLE TRIGGER study_session_guard;
    INSERT INTO public.study_sessions (party_id, user_id, minutes, local_date, tz, created_at) VALUES
      ('${party}', '${leader.userId}', 30, '2026-10-19', 'Asia/Karachi', '2026-10-18T22:00:00Z'),
      ('${party}', '${member.userId}', 30, '2026-10-18', 'America/New_York', '2026-10-18T22:00:00Z');
    ALTER TABLE public.study_sessions ENABLE TRIGGER study_session_guard;
    COMMIT;`);
  const weeks = (await call(`/rest/v1/study_week_scores?party_id=eq.${party}&select=user_id,week_start,points&order=week_start`, { token: member.accessToken })).body;
  assert.deepEqual(weeks, [
    { user_id: member.userId, week_start: "2026-10-12", points: 3 },
    { user_id: leader.userId, week_start: "2026-10-19", points: 3 },
  ]);

  // Closing: once a week has ended everywhere; the top scorer, ties all, recorded once.
  assert.equal((await rpc(member, "close_study_week", { party_id: party, week_start: mondayOf(todayIn("UTC")) })).body.message, "week_not_over");
  assert.equal((await rpc(member, "close_study_week", { party_id: party, week_start: "2026-10-13" })).body.message, "bad_week");
  const closed = (await rpc(member, "close_study_week", { party_id: party, week_start: "2026-10-12" })).body;
  assert.deepEqual(closed, { status: "closed", winners: [{ user_id: member.userId, points: 3 }] });
  assert.equal((await rpc(leader, "close_study_week", { party_id: party, week_start: "2026-10-12" })).body.status, "already_closed");
  assert.deepEqual((await call(`/rest/v1/study_week_winners?party_id=eq.${party}&select=user_id,points`, { token: leader.accessToken })).body, [{ user_id: member.userId, points: 3 }]);
  const { outsider } = await cast();
  assert.equal((await rpc(outsider, "close_study_week", { party_id: party, week_start: "2026-10-12" })).body.message, "not_a_member");
});
