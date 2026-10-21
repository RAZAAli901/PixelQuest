// Sharing files in a study chat, end to end on the local stack (docs/study-chat.md, How a file is
// shared): request-upload, the upload itself, finalize-upload and get-download-url, against the
// real database, storage and Edge Functions. Every file here is hostile but one.
// Needs `npx supabase functions serve` (docs/LOCAL_SUPABASE.md), as gemini_proxy.test.ts does.
import assert from "node:assert/strict";
import { asService, call, localTest, newPlayer, sql, stack, type Session } from "./stack.ts";

type Player = Session & { username: string };

const fresh = (prefix: string) => `${prefix}_${Math.random().toString(36).slice(2, 10)}`;
const enc = new TextEncoder();
const PDF = "application/pdf";
const DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

function rpc(who: Session | undefined, fn: string, body: unknown) {
  return call(`/rest/v1/rpc/${fn}`, { token: who?.accessToken, body });
}

async function player(tag: string): Promise<Player> {
  const session = await newPlayer(tag);
  const username = fresh(tag.slice(0, 8));
  assert.equal((await rpc(session, "set_username", { name: username })).body?.status, "ok");
  return { ...session, username };
}

let castPromise: Promise<{ leader: Player; member: Player; outsider: Player }> | undefined;
function cast() {
  castPromise ??= (async () => {
    const [leader, member, outsider] = await Promise.all(["fl", "fm", "fx"].map((t) => player(`files_${t}`)));
    return { leader, member, outsider };
  })();
  return castPromise;
}

async function studyParty(leader: Player, members: Player[]): Promise<string> {
  const made = await rpc(leader, "create_party", { name: fresh("Files"), party_type: "study", max_members: 10 });
  assert.equal(made.status, 200, JSON.stringify(made.body));
  sql(`DELETE FROM private.invite_log WHERE inviter = '${leader.userId}'`);
  for (const member of members) {
    await rpc(leader, "invite_member", { party_id: made.body, username: member.username });
    await rpc(member, "respond_invite", { party_id: made.body, accept: true });
  }
  return made.body;
}

const fn = (name: string, who: Session | undefined, body: unknown) => call(`/functions/v1/${name}`, { token: who?.accessToken, body });

/** The phone's upload: the bytes straight to the signed URL, as the type declared. */
async function put(path: string, bytes: Uint8Array, mime: string) {
  assert.match(path, /^\/storage\/v1\//, "a path on the project, for the app to put its own address in front");
  const res = await fetch(`${stack().apiUrl}${path}`, { method: "PUT", headers: { "Content-Type": mime, apikey: stack().publishableKey }, body: bytes });
  return { status: res.status, text: await res.text() };
}

/** request-upload, upload and finalize-upload in a row; what finalize said. */
async function share(who: Session, party: string, name: string, mime: string, bytes: Uint8Array, declared = bytes.length) {
  const asked = await fn("request-upload", who, { party_id: party, name, mime, size: declared });
  assert.equal(asked.status, 200, JSON.stringify(asked.body));
  const up = await put(asked.body.upload_url, bytes, mime);
  assert.equal(up.status, 200, up.text);
  const done = await fn("finalize-upload", who, { file_id: asked.body.file_id });
  return { file: asked.body.file_id as string, url: asked.body.upload_url as string, done };
}

/** Clears a member's uploads (their daily count), and the file messages pointing at them. */
const forget = (who: Session) =>
  sql(
    `DELETE FROM public.chat_messages WHERE file_id IN (SELECT id FROM public.chat_files WHERE uploader_id = '${who.userId}'); ` +
      `DELETE FROM public.chat_files WHERE uploader_id = '${who.userId}'`,
  );

const storedPath = (file: string) => sql(`SELECT storage_path FROM public.chat_files WHERE id = '${file}'`);
const stillStored = async (file: string) => (await asService(`/storage/v1/object/info/study-files/${storedPath(file)}`)).status === 200;

/** A zip of stored entries (as an Office file is), for the hostile cases. */
function zip(entries: Record<string, string>): Uint8Array {
  const parts: number[] = [];
  const central: number[] = [];
  const u16 = (n: number) => [n & 0xff, (n >> 8) & 0xff];
  const u32 = (n: number) => [n & 0xff, (n >> 8) & 0xff, (n >> 16) & 0xff, (n >>> 24) & 0xff];
  for (const [name, content] of Object.entries(entries)) {
    const n = [...enc.encode(name)];
    const d = [...enc.encode(content)];
    const offset = parts.length;
    parts.push(...u32(0x04034b50), ...u16(20), ...u16(0), ...u16(0), ...u16(0), ...u16(0), ...u32(0), ...u32(d.length), ...u32(d.length), ...u16(n.length), ...u16(0), ...n, ...d);
    central.push(...u32(0x02014b50), ...u16(20), ...u16(20), ...u16(0), ...u16(0), ...u16(0), ...u16(0), ...u32(0), ...u32(d.length), ...u32(d.length), ...u16(n.length), ...u16(0), ...u16(0), ...u16(0), ...u16(0), ...u32(0), ...u32(offset), ...n);
  }
  const count = Object.keys(entries).length;
  const end = [...u32(0x06054b50), ...u16(0), ...u16(0), ...u16(count), ...u16(count), ...u32(central.length), ...u32(parts.length), ...u16(0)];
  return new Uint8Array([...parts, ...central, ...end]);
}

localTest("files: a real PDF is shared, posted, and opened by a member as an attachment for 60 seconds", async () => {
  const { leader, member, outsider } = await cast();
  const party = await studyParty(leader, [member]);
  const bytes = enc.encode("%PDF-1.7\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n");
  const { file, url, done } = await share(member, party, "Week 3/../Past paper.pdf", PDF, bytes);
  assert.equal(done.status, 200, JSON.stringify(done.body));
  assert.equal(done.body.status, "ready");

  // Its row: what it really is, its hash, its cleaned name; and its message, as the uploader's.
  const row = (await call(`/rest/v1/chat_files?id=eq.${file}&select=display_name,mime,size_bytes,sha256,status`, { token: leader.accessToken })).body[0];
  assert.deepEqual({ ...row, sha256: row.sha256.length }, { display_name: "Past paper.pdf", mime: PDF, size_bytes: bytes.length, sha256: 64, status: "ready" });
  const message = (await call(`/rest/v1/chat_messages?party_id=eq.${party}&kind=eq.file&select=sender_id,file_id`, { token: leader.accessToken })).body;
  assert.deepEqual(message, [{ sender_id: member.userId, file_id: file }]);

  // Uploaded once: the same URL can't overwrite it.
  assert.notEqual((await put(url, enc.encode("%PDF-1.7 replaced"), PDF)).status, 200);

  // A member opens it: a short-lived URL, served as an attachment.
  const link = await fn("get-download-url", leader, { file_id: file });
  assert.equal(link.status, 200, JSON.stringify(link.body));
  assert.equal(link.body.expires_in, 60);
  const got = await fetch(`${stack().apiUrl}${link.body.url}`);
  assert.equal(got.status, 200);
  assert.match(got.headers.get("content-disposition") ?? "", /attachment/);
  assert.deepEqual(new Uint8Array(await got.arrayBuffer()), bytes);

  // Nobody else does, and asking twice for the result changes nothing.
  assert.equal((await fn("get-download-url", outsider, { file_id: file })).status, 403);
  assert.equal((await fn("get-download-url", undefined, { file_id: file })).status, 401);
  assert.deepEqual((await fn("finalize-upload", member, { file_id: file })).body, { status: "ready" });
  assert.equal((await fn("finalize-upload", leader, { file_id: file })).status, 403, "only its uploader finalizes it");
});

localTest("files: a disguised program, a zip, a macro document and a lying size are rejected and deleted", async () => {
  const { leader, member } = await cast();
  const party = await studyParty(leader, [member]);
  forget(member);
  const cases: [string, string, Uint8Array, string, number?][] = [
    // An app renamed .pdf.
    ["Notes.pdf", PDF, new Uint8Array([0x4d, 0x5a, 0x90, 0x00, 0x03, 0x00, 0x00, 0x00]), "bad_type"],
    // A zip named as a PDF, and as a Word file.
    ["Answers.pdf", PDF, zip({ "answers.pdf": "%PDF-1.4", "run.exe": "MZ" }), "embedded"],
    ["Answers.docx", DOCX, zip({ "notes.txt": "hello" }), "bad_type"],
    // A Word file with macros.
    ["Essay.docx", DOCX, zip({ "[Content_Types].xml": "<Types/>", "word/document.xml": "<w/>", "word/vbaProject.bin": "VBA" }), "macros"],
    // A PNG sent as a PDF.
    ["Scan.pdf", PDF, new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]), "type_mismatch"],
    // More bytes than declared (the party's storage counts the declared size).
    ["Big.pdf", PDF, enc.encode("%PDF-1.4 and a lot more than ten bytes"), "size_mismatch", 10],
  ];
  for (const [name, mime, bytes, reason, declared] of cases) {
    const { file, done } = await share(member, party, name, mime, bytes, declared);
    assert.deepEqual(done.body, { status: "rejected", reason }, name);
    assert.equal(await stillStored(file), false, `${name}'s bytes are deleted`);
    // Its uploader sees why; nobody else sees it at all.
    const mine = (await call(`/rest/v1/chat_files?id=eq.${file}&select=status,reject_reason`, { token: member.accessToken })).body;
    assert.deepEqual(mine, [{ status: "rejected", reject_reason: reason }]);
    assert.deepEqual((await call(`/rest/v1/chat_files?id=eq.${file}&select=id`, { token: leader.accessToken })).body, []);
    assert.equal((await fn("get-download-url", member, { file_id: file })).status, 403);
  }
  assert.deepEqual((await call(`/rest/v1/chat_messages?party_id=eq.${party}&kind=eq.file&select=id`, { token: leader.accessToken })).body, [], "nothing posted");
  forget(member);
});

localTest("files: what request-upload refuses before anything's stored", async () => {
  const { leader, member, outsider } = await cast();
  const party = await studyParty(leader, [member]);
  const ask = (who: Session | undefined, body: Record<string, unknown>) =>
    fn("request-upload", who, { party_id: party, name: "Notes.pdf", mime: PDF, size: 1000, ...body });

  assert.equal((await ask(undefined, {})).status, 401);
  assert.deepEqual((await ask(outsider, {})).body, { error: "not_a_member" });
  assert.deepEqual((await ask(member, { size: 20 * 1024 * 1024 + 1 })).body, { error: "too_large" }, "oversized");
  assert.deepEqual((await ask(member, { size: 0 })).body, { error: "bad_request" }, "empty");
  assert.deepEqual((await ask(member, { name: "setup.exe", mime: "application/x-msdownload" })).body, { error: "bad_type" });
  assert.deepEqual((await ask(member, { name: "notes.zip", mime: "application/zip" })).body, { error: "bad_type" });
  assert.deepEqual((await ask(member, { name: "macro.docm", mime: "application/vnd.ms-word.document.macroEnabled.12" })).body, { error: "bad_type" });
  assert.deepEqual((await ask(member, { name: "invoice.pdf.exe" })).body, { error: "bad_type" });
  assert.deepEqual((await ask(member, { name: "x".repeat(1001) + ".pdf" })).body, { error: "bad_name" }, "a huge name");
  const long = await ask(member, { name: "y".repeat(500) + ".pdf" });
  assert.equal(long.status, 200);
  assert.equal(long.body.display_name.length, 120);
  assert.ok(long.body.display_name.endsWith(".pdf"));

  // Empty bytes for a declared size: rejected at finalize.
  const empty = await ask(member, { size: 5 });
  await put(empty.body.upload_url, new Uint8Array(0), PDF);
  assert.deepEqual((await fn("finalize-upload", member, { file_id: empty.body.file_id })).body, { status: "rejected", reason: "empty" });
  forget(member);
});

localTest("files: an upload arriving after its 5 minutes is refused, and cleanup lets stale ones go", async () => {
  const { leader, member } = await cast();
  const party = await studyParty(leader, [member]);
  const asked = await fn("request-upload", member, { party_id: party, name: "Late.pdf", mime: PDF, size: 9 });
  await put(asked.body.upload_url, enc.encode("%PDF-1.4\n"), PDF);
  sql(`UPDATE public.chat_files SET created_at = now() - interval '6 minutes' WHERE id = '${asked.body.file_id}'`);
  assert.deepEqual((await fn("finalize-upload", member, { file_id: asked.body.file_id })).body, { status: "rejected", reason: "upload_expired" });

  // Never finished, an hour on: the next request's cleanup rejects it, and its bytes go.
  const stale = await fn("request-upload", member, { party_id: party, name: "Stale.pdf", mime: PDF, size: 9 });
  await put(stale.body.upload_url, enc.encode("%PDF-1.4\n"), PDF);
  sql(`UPDATE public.chat_files SET created_at = now() - interval '2 hours' WHERE id = '${stale.body.file_id}'`);
  assert.equal(await stillStored(stale.body.file_id), true);
  const cleaned = await call("/functions/v1/cleanup-uploads", { body: {}, headers: { Authorization: `Bearer ${stack().serviceKey}` }, apiKey: stack().serviceKey });
  assert.equal(cleaned.status, 200, JSON.stringify(cleaned.body));
  assert.equal(sql(`SELECT status || ':' || reject_reason FROM public.chat_files WHERE id = '${stale.body.file_id}'`), "rejected:upload_expired");
  assert.equal(await stillStored(stale.body.file_id), false);
  assert.equal((await call("/functions/v1/cleanup-uploads", { token: member.accessToken, body: {} })).status, 401, "nobody else runs it");
  forget(member);
});

localTest("files: a removed member loses messages and files at once, and a download link expires", async () => {
  const { leader, member } = await cast();
  const party = await studyParty(leader, [member]);
  forget(leader);
  const bytes = enc.encode("%PDF-1.7\n1 0 obj<<>>endobj\n%%EOF\n");
  const { file, done } = await share(leader, party, "Revision.pdf", PDF, bytes);
  assert.equal(done.body.status, "ready", JSON.stringify(done.body));

  // A member's link works now.
  const link = await fn("get-download-url", member, { file_id: file });
  assert.equal(link.status, 200);
  assert.equal((await fetch(`${stack().apiUrl}${link.body.url}`)).status, 200);

  // Removed: no messages, no file rows, no new link, at once.
  assert.deepEqual((await rpc(leader, "remove_member", { party_id: party, user_id: member.userId })).body, { status: "removed" });
  assert.deepEqual((await call(`/rest/v1/chat_messages?party_id=eq.${party}&select=id`, { token: member.accessToken })).body, []);
  assert.deepEqual((await call(`/rest/v1/chat_files?party_id=eq.${party}&select=id`, { token: member.accessToken })).body, []);
  assert.equal((await fn("get-download-url", member, { file_id: file })).status, 403);
  assert.equal((await fn("request-upload", member, { party_id: party, name: "x.pdf", mime: PDF, size: 10 })).status, 403);

  // A signed link stops working once its time is up (one signed for a second, here).
  const path = storedPath(file);
  const short = await asService(`/storage/v1/object/sign/study-files/${path}`, { body: { expiresIn: 1 } });
  assert.equal(short.status, 200, JSON.stringify(short.body));
  await new Promise((r) => setTimeout(r, 2500));
  const late = await fetch(`${stack().apiUrl}/storage/v1${short.body.signedURL}`);
  assert.ok([400, 401, 403].includes(late.status), `an expired link is refused (${late.status})`);
  forget(leader);
});
