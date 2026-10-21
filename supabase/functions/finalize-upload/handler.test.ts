// Run with: node --test "supabase/functions/**/*.test.ts"   (Node 22.18+ runs the TypeScript directly)
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleFinalize, type FinalizeDeps, type PendingUpload } from "./handler.ts";

const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const FILE = "7d1e2f3a-4b5c-4d6e-8f9a-0b1c2d3e4f5a";
const TOKEN = "user-access-token";
const PDF = new TextEncoder().encode("%PDF-1.7\n1 0 obj << >> endobj\n%%EOF\n");

function deps(options: { upload?: Partial<PendingUpload>; refusal?: string; stored?: Uint8Array | null; readyRefusal?: string } = {}) {
  const removed: string[] = [];
  const rejected: { file: string; reason: string }[] = [];
  const ready: { mime: string; size: number; sha: string }[] = [];
  const upload: PendingUpload = {
    party_id: "p1", storage_path: "p1/obj", mime: "application/pdf", size_bytes: PDF.length, status: "pending",
    created_at: "2026-10-21T10:00:00Z", uploaded_at: "2026-10-21T10:01:00Z", ...options.upload,
  };
  const d: FinalizeDeps = {
    getUserId: async (token) => (token === TOKEN ? USER : null),
    loadUpload: async () => (options.refusal ? { ok: false, error: options.refusal } : { ok: true, data: upload }),
    readObject: async () => ("stored" in options ? options.stored! : PDF),
    removeObject: async (path) => {
      removed.push(path);
    },
    markReady: async (_user, _file, mime, size, sha) => {
      ready.push({ mime, size, sha });
      return options.readyRefusal ? { ok: false, error: options.readyRefusal } : { ok: true, data: "m1" };
    },
    markRejected: async (file, reason) => {
      rejected.push({ file, reason });
    },
  };
  return { d, removed, rejected, ready };
}

const finalize = (d: FinalizeDeps, body: unknown = { file_id: FILE }, token: string | null = TOKEN) =>
  handleFinalize(
    new Request("https://example.test/functions/v1/finalize-upload", {
      method: "POST",
      headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify(body),
    }),
    d,
  );

test("a real PDF, as declared and in time, becomes ready with its own hash, and its message is posted", async () => {
  const { d, ready, removed } = deps();
  const res = await finalize(d);
  assert.equal(res.status, 200);
  assert.deepEqual(await res.json(), { status: "ready", message_id: "m1" });
  assert.equal(ready.length, 1);
  assert.equal(ready[0].mime, "application/pdf");
  assert.equal(ready[0].size, PDF.length);
  assert.match(ready[0].sha, /^[0-9a-f]{64}$/);
  assert.deepEqual(removed, []);
});

test("only its uploader, signed in, finalizes it", async () => {
  assert.equal((await finalize(deps().d, { file_id: FILE }, null)).status, 401);
  assert.equal((await finalize(deps({ refusal: "not_yours" }).d)).status, 403);
  assert.equal((await finalize(deps({ refusal: "no_such_file" }).d)).status, 404);
  assert.equal((await finalize(deps().d, { file_id: "../x" })).status, 400);
});

test("not uploaded yet: the phone may try again, nothing's rejected", async () => {
  for (const options of [{ upload: { uploaded_at: null } }, { stored: null }]) {
    const { d, rejected } = deps(options);
    const res = await finalize(d);
    assert.equal(res.status, 409);
    assert.deepEqual(rejected, []);
  }
});

test("asked again after it's decided: the same answer, no second check", async () => {
  const { d, ready } = deps({ upload: { status: "ready" } });
  assert.deepEqual(await (await finalize(d)).json(), { status: "ready" });
  assert.equal(ready.length, 0);
});

test("late, the wrong size, or not what it claimed: rejected, and its bytes deleted", async () => {
  const cases: [Parameters<typeof deps>[0], string][] = [
    [{ upload: { uploaded_at: "2026-10-21T10:05:01Z" } }, "upload_expired"],
    [{ upload: { size_bytes: 10 } }, "size_mismatch"],
    [{ stored: new Uint8Array(0), upload: { size_bytes: 1 } }, "empty"],
    [{ stored: new Uint8Array([0x4d, 0x5a, 0x90, 0x00]), upload: { size_bytes: 4 } }, "bad_type"],
    [{ stored: new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a]), upload: { size_bytes: 8 } }, "type_mismatch"],
  ];
  for (const [options, reason] of cases) {
    const { d, removed, rejected, ready } = deps(options);
    const res = await finalize(d);
    assert.deepEqual(await res.json(), { status: "rejected", reason }, reason);
    assert.deepEqual(removed, ["p1/obj"]);
    assert.deepEqual(rejected, [{ file: FILE, reason }]);
    assert.equal(ready.length, 0);
  }
});

test("its uploader gone from the party meanwhile: not kept", async () => {
  const { d, removed, rejected } = deps({ readyRefusal: "not_a_member" });
  assert.deepEqual(await (await finalize(d)).json(), { status: "rejected", reason: "not_a_member" });
  assert.deepEqual(removed, ["p1/obj"]);
  assert.deepEqual(rejected, [{ file: FILE, reason: "not_a_member" }]);
});
