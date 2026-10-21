// A study party's chat on the server (migrations 20261019000050 to 20261019000058,
// docs/study-chat.md): who reads and writes messages and files, deleting, the private bucket, and
// the limits. Every rule the app relies on is checked against the real database and storage.
import assert from "node:assert/strict";
import { asService, call, localTest, newPlayer, sql, stack, type Session } from "./stack.ts";

type Player = Session & { username: string };

const fresh = (prefix: string) => `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
const returning = { Prefer: "return=representation" };

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

async function studyParty(leader: Player, members: Player[]): Promise<string> {
  const made = await rpc(leader, "create_party", { name: fresh("Chat"), party_type: "study", max_members: 10 });
  assert.equal(made.status, 200, JSON.stringify(made.body));
  if (members.length > 0) sql(`DELETE FROM private.invite_log WHERE inviter = '${leader.userId}'`);
  for (const member of members) {
    assert.deepEqual((await rpc(leader, "invite_member", { party_id: made.body, username: member.username })).body, { status: "sent" });
    assert.deepEqual((await rpc(member, "respond_invite", { party_id: made.body, accept: true })).body, { status: "active" });
  }
  return made.body;
}

let castPromise: Promise<{ leader: Player; member: Player; other: Player; outsider: Player }> | undefined;
function cast() {
  castPromise ??= (async () => {
    const [leader, member, other, outsider] = await Promise.all(["cl", "cm", "co", "cx"].map((t) => player(`chat_${t}`)));
    return { leader, member, other, outsider };
  })();
  return castPromise;
}

const send = (who: Session | undefined, party: string, body: Record<string, unknown>) =>
  call("/rest/v1/chat_messages", { token: who?.accessToken, body: { party_id: party, ...body }, headers: returning });
const messagesOf = async (who: Session | undefined, party: string) =>
  (await call(`/rest/v1/chat_messages?party_id=eq.${party}&select=body,deleted_at&order=created_at`, { token: who?.accessToken })).body;

/** A file row as request-upload makes it (the service role): pending unless said otherwise. */
function fileRow(party: string, uploader: string, extra: { status?: string; size?: number; name?: string } = {}): string {
  const id = crypto.randomUUID();
  const reason = extra.status === "rejected" ? "'bad_type'" : "NULL";
  sql(
    `INSERT INTO public.chat_files (id, party_id, uploader_id, storage_path, display_name, mime, size_bytes, status, reject_reason) ` +
      `VALUES ('${id}', '${party}', '${uploader}', '${party}/${crypto.randomUUID()}', '${extra.name ?? "notes.pdf"}', 'application/pdf', ` +
      `${extra.size ?? 1000}, '${extra.status ?? "pending"}', ${reason})`,
  );
  return id;
}

/** Clears a member's uploads (the daily count) between tests that share them. */
const forgetUploads = (who: Session) => sql(`DELETE FROM public.chat_files WHERE uploader_id = '${who.userId}'`);

localTest("chat: only a study party's active members read it, and send as themselves", async () => {
  const { leader, member, outsider } = await cast();
  const party = await studyParty(leader, [member]);
  const elsewhere = await studyParty(outsider, []);

  assert.equal((await send(member, party, { body: "As the leader", sender_id: leader.userId })).status, 403, "who sent it is the server's to say");
  const sent = await send(member, party, { body: "Anyone done question 3?" });
  assert.equal(sent.status, 201, JSON.stringify(sent.body));
  assert.equal(sent.body[0].sender_id, member.userId);
  assert.deepEqual(await messagesOf(leader, party), [{ body: "Anyone done question 3?", deleted_at: null }]);

  assert.deepEqual(await messagesOf(outsider, party), [], "an outsider reads nothing");
  assert.equal((await call(`/rest/v1/chat_messages?party_id=eq.${party}`)).status, 401, "nor the signed out");
  assert.equal((await send(outsider, party, { body: "Hello" })).status, 403, "nor writes");
  assert.equal((await send(member, elsewhere, { body: "Into another party" })).status, 403, "a member of A writes nothing in B");

  // A client writes text only, and a reply stays in its party.
  assert.equal((await send(member, party, { kind: "system", body: "file_rejected" })).status, 403);
  assert.equal((await send(member, party, { kind: "file" })).status, 403);
  const theirs = (await send(outsider, elsewhere, { body: "Mine" })).body[0];
  assert.equal((await send(member, party, { body: "Re", reply_to: theirs.id })).body.message, "bad_reply");
  assert.equal((await send(leader, party, { body: "Yes, it's 42", reply_to: sent.body[0].id })).status, 201);

  // Nobody edits a message.
  const edit = await call(`/rest/v1/chat_messages?id=eq.${sent.body[0].id}`, { method: "PATCH", token: member.accessToken, body: { body: "Changed" }, headers: returning });
  assert.equal(edit.status, 403);

  // The limits on text.
  for (const body of ["", " padded ", "x".repeat(2001)]) assert.equal((await send(member, party, { body })).status, 400, JSON.stringify(body.slice(0, 10)));
  assert.equal((await send(member, party, { body: "x".repeat(2000) })).status, 201);

  // Removed, a member reads nothing at once.
  assert.deepEqual((await rpc(leader, "remove_member", { party_id: party, user_id: member.userId })).body, { status: "removed" });
  assert.deepEqual(await messagesOf(member, party), []);
  assert.equal((await send(member, party, { body: "Still here?" })).status, 403);
});

localTest("chat: the sender deletes their own message, the leader any; nobody else", async () => {
  const { leader, member, other } = await cast();
  const party = await studyParty(leader, [member, other]);
  const mine = (await send(member, party, { body: "Oops" })).body[0];
  const others = (await send(other, party, { body: "Not yours" })).body[0];

  assert.equal((await rpc(member, "delete_chat_message", { message_id: others.id })).body.message, "not_allowed");
  assert.equal((await rpc(member, "delete_chat_message", { message_id: mine.id })).status, 204);
  assert.equal((await rpc(member, "delete_chat_message", { message_id: mine.id })).status, 204, "again changes nothing");
  assert.equal((await rpc(leader, "delete_chat_message", { message_id: others.id })).status, 204, "the leader deletes any");
  const left = await messagesOf(other, party);
  assert.equal(left.length, 2, "the rows stay, for replies");
  assert.ok(left.every((m: { body: string | null; deleted_at: string | null }) => m.body === null && m.deleted_at !== null), "their text gone for everyone");

  // A file message's file goes with it.
  const file = fileRow(party, member.userId, { status: "ready" });
  const fileMessage = crypto.randomUUID();
  sql(`INSERT INTO public.chat_messages (id, party_id, sender_id, kind, file_id) VALUES ('${fileMessage}', '${party}', '${member.userId}', 'file', '${file}')`);
  assert.equal((await call(`/rest/v1/chat_files?id=eq.${file}&select=id`, { token: other.accessToken })).body.length, 1);
  assert.equal((await rpc(member, "delete_chat_message", { message_id: fileMessage })).status, 204);
  assert.deepEqual((await call(`/rest/v1/chat_files?id=eq.${file}&select=id`, { token: other.accessToken })).body, [], "gone for everyone");
  forgetUploads(member);
});

localTest("chat files: members read ready ones, uploaders their own pending; never the storage path", async () => {
  const { leader, member, other, outsider } = await cast();
  const party = await studyParty(leader, [member, other]);
  const pending = fileRow(party, member.userId);
  const ready = fileRow(party, member.userId, { status: "ready", name: "Past paper.pdf" });
  const filesOf = async (who: Session) => (await call(`/rest/v1/chat_files?party_id=eq.${party}&select=id&order=created_at`, { token: who.accessToken })).body.map((f: { id: string }) => f.id);

  assert.deepEqual(await filesOf(member), [pending, ready]);
  assert.deepEqual(await filesOf(other), [ready], "another's pending file is theirs alone");
  assert.deepEqual(await filesOf(outsider), []);
  assert.equal((await call(`/rest/v1/chat_files?party_id=eq.${party}&select=storage_path`, { token: member.accessToken })).status, 403, "the path is the server's");

  // No client makes, changes or deletes a file row.
  const made = await call("/rest/v1/chat_files", {
    token: member.accessToken,
    body: { party_id: party, storage_path: `${party}/${crypto.randomUUID()}`, display_name: "x.pdf", mime: "application/pdf", size_bytes: 10 },
  });
  assert.equal(made.status, 403);
  assert.equal((await call(`/rest/v1/chat_files?id=eq.${ready}`, { method: "PATCH", token: leader.accessToken, body: { status: "ready" } })).status, 403);
  assert.equal((await call(`/rest/v1/chat_files?id=eq.${ready}`, { method: "DELETE", token: member.accessToken })).status, 403);

  // What a file row may hold.
  assert.throws(() => fileRow(party, member.userId, { name: "../../etc/passwd" }), /chat_file_name/);
  assert.throws(() => fileRow(party, member.userId, { size: 20 * 1024 * 1024 + 1 }), /chat_file_size/);
  assert.throws(
    () => sql(`INSERT INTO public.chat_files (party_id, uploader_id, storage_path, display_name, mime, size_bytes) VALUES ('${party}', '${member.userId}', '${party}/${crypto.randomUUID()}', 'a.exe', 'application/x-msdownload', 10)`),
    /chat_file_mime/,
  );
  assert.throws(
    () => sql(`INSERT INTO public.chat_files (party_id, uploader_id, storage_path, display_name, mime, size_bytes) VALUES ('${party}', '${member.userId}', '00000000-0000-0000-0000-000000000000/${crypto.randomUUID()}', 'a.pdf', 'application/pdf', 10)`),
    /chat_file_path/,
    "a path in another party",
  );
  forgetUploads(member);
});

localTest("chat: the bucket is shut to every client; the server alone reads and writes it", async () => {
  const { leader, member } = await cast();
  const party = await studyParty(leader, [member]);
  const path = `${party}/${crypto.randomUUID()}`;
  const upload = await fetch(`${stack().apiUrl}/storage/v1/object/study-files/${path}`, {
    method: "POST",
    headers: { apikey: stack().publishableKey, Authorization: `Bearer ${member.accessToken}`, "Content-Type": "application/pdf" },
    body: "%PDF-1.4 hello",
  });
  assert.ok([400, 403].includes(upload.status), `a member uploads nothing directly (${upload.status})`);

  // The server puts a file there; a member still can't read or list it, or make it public.
  const put = await fetch(`${stack().apiUrl}/storage/v1/object/study-files/${path}`, {
    method: "POST",
    headers: { apikey: stack().serviceKey, Authorization: `Bearer ${stack().serviceKey}`, "Content-Type": "application/pdf" },
    body: "%PDF-1.4 hello",
  });
  assert.equal(put.status, 200, await put.text());
  const read = await fetch(`${stack().apiUrl}/storage/v1/object/authenticated/study-files/${path}`, {
    headers: { apikey: stack().publishableKey, Authorization: `Bearer ${member.accessToken}` },
  });
  assert.ok([400, 403, 404].includes(read.status), `nor reads it directly (${read.status})`);
  const listed = await call("/storage/v1/object/list/study-files", { token: member.accessToken, body: { prefix: party } });
  assert.deepEqual(listed.body, [], "nor lists it");
  const open = await fetch(`${stack().apiUrl}/storage/v1/object/public/study-files/${path}`);
  assert.ok([400, 404].includes(open.status), `no public URL (${open.status})`);
  assert.equal(sql("SELECT public FROM storage.buckets WHERE id = 'study-files'"), "f");
  await asService(`/storage/v1/object/study-files/${path}`, { method: "DELETE" });
});

localTest("chat: a party holds 500 MB, a member uploads 20 a day and sends 30 a minute", async () => {
  const { leader, member, other } = await cast();
  const party = await studyParty(leader, [member, other]);
  forgetUploads(member);
  forgetUploads(other);

  // 500 MB: 24 files of 20 MB are 480; a 25th just under 20 fits, and then 2 bytes more don't.
  for (let i = 0; i < 12; i++) fileRow(party, member.userId, { size: 20 * 1024 * 1024, status: "ready" });
  for (let i = 0; i < 12; i++) fileRow(party, other.userId, { size: 20 * 1024 * 1024, status: "ready" });
  fileRow(party, leader.userId, { size: 20 * 1024 * 1024 - 1, status: "ready" });
  assert.throws(() => fileRow(party, leader.userId, { size: 2 }), /party_storage_full/);
  fileRow(party, leader.userId, { size: 1, status: "rejected" });
  const used = await call(`/rest/v1/study_chat_storage?party_id=eq.${party}&select=used_bytes,files`, { token: leader.accessToken });
  assert.deepEqual(used.body, [{ used_bytes: 500 * 1024 * 1024 - 1, files: 25 }]);
  sql(`DELETE FROM public.chat_files WHERE party_id = '${party}'`);

  // 20 uploads a day, rejected ones counted.
  for (let i = 0; i < 19; i++) fileRow(party, member.userId);
  fileRow(party, member.userId, { status: "rejected" });
  assert.throws(() => fileRow(party, member.userId), /too_many_uploads/);
  fileRow(party, other.userId);

  // 30 messages a minute (what they sent in the tests before doesn't count here).
  sql(`DELETE FROM public.chat_messages WHERE sender_id IN ('${other.userId}', '${member.userId}')`);
  for (let i = 0; i < 30; i++) assert.equal((await send(other, party, { body: `Message ${i}` })).status, 201);
  const thirtyFirst = await send(other, party, { body: "One too many" });
  assert.equal(thirtyFirst.status, 400);
  assert.equal(thirtyFirst.body.message, "too_many_messages");
  assert.equal((await send(member, party, { body: "Mine still go" })).status, 201);
  forgetUploads(member);
  forgetUploads(other);
});

localTest("chat: a member's read state is their own, and only moves on", async () => {
  const { leader, member, outsider } = await cast();
  const party = await studyParty(leader, [member]);
  const first = await rpc(member, "mark_chat_read", { party_id: party });
  assert.equal(first.status, 200, JSON.stringify(first.body));
  sql(`UPDATE public.chat_reads SET last_read_at = now() + interval '1 hour' WHERE user_id = '${member.userId}' AND party_id = '${party}'`);
  const again = await rpc(member, "mark_chat_read", { party_id: party });
  assert.ok(Date.parse(again.body) > Date.parse(first.body) + 30 * 60_000, "never back");
  assert.equal((await rpc(outsider, "mark_chat_read", { party_id: party })).body.message, "not_a_member");
  assert.equal((await call(`/rest/v1/chat_reads?party_id=eq.${party}&select=user_id`, { token: leader.accessToken })).body.length, 0, "another's isn't read");
});

localTest("chat files: only the leader pins a ready file", async () => {
  const { leader, member } = await cast();
  const party = await studyParty(leader, [member]);
  const ready = fileRow(party, member.userId, { status: "ready" });
  assert.equal((await rpc(member, "pin_chat_file", { file_id: ready, pinned: true })).body.message, "not_allowed");
  assert.equal((await rpc(leader, "pin_chat_file", { file_id: ready, pinned: true })).status, 204);
  assert.equal(sql(`SELECT pinned FROM public.chat_files WHERE id = '${ready}'`), "t");
  const pending = fileRow(party, member.userId);
  assert.equal((await rpc(leader, "pin_chat_file", { file_id: pending, pinned: true })).body.message, "not_allowed");
  forgetUploads(member);
});
