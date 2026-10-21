// Run with: node --test "supabase/functions/**/*.test.ts"   (Node 22.18+ runs the TypeScript directly)
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleRequestUpload, type RequestUploadDeps } from "./handler.ts";
import { cleanName, nameFitsType } from "../_shared/study_files.ts";

const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const PARTY = "5f0c1d2e-3a4b-4c5d-8e9f-0a1b2c3d4e5f";
const TOKEN = "user-access-token";

function deps(refusal?: string) {
  const asked: { user: string; party: string; name: string; mime: string; size: number }[] = [];
  const signed: string[] = [];
  const d: RequestUploadDeps = {
    getUserId: async (token) => (token === TOKEN ? USER : null),
    requestRow: async (user, party, name, mime, size) => {
      asked.push({ user, party, name, mime, size });
      return refusal ? { ok: false, error: refusal } : { ok: true, data: { file_id: "f1", storage_path: `${party}/abc` } };
    },
    signUpload: async (path) => {
      signed.push(path);
      return `https://example.test/upload/${path}?token=t`;
    },
  };
  return { d, asked, signed };
}

const ask = (body: unknown, token: string | null = TOKEN, method = "POST") =>
  new Request("https://example.test/functions/v1/request-upload", {
    method,
    headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
    body: method === "POST" ? JSON.stringify(body) : undefined,
  });

const pdf = { party_id: PARTY, name: "Past paper.pdf", mime: "application/pdf", size: 120_000 };

test("a member's request gets a pending file and an upload URL for the server's own path", async () => {
  const { d, asked, signed } = deps();
  const res = await handleRequestUpload(ask(pdf), d);
  assert.equal(res.status, 200);
  const body = await res.json();
  assert.equal(body.file_id, "f1");
  assert.equal(body.expires_in, 300);
  assert.equal(body.display_name, "Past paper.pdf");
  assert.deepEqual(asked, [{ user: USER, party: PARTY, name: "Past paper.pdf", mime: "application/pdf", size: 120_000 }]);
  assert.deepEqual(signed, [`${PARTY}/abc`], "the path is the database's, never the phone's");
});

test("only a signed-in account may ask, and only by POST", async () => {
  for (const token of [null, "not-a-token"]) {
    const { d, asked } = deps();
    const res = await handleRequestUpload(ask(pdf, token), d);
    assert.equal(res.status, 401);
    assert.equal(asked.length, 0);
  }
  assert.equal((await handleRequestUpload(ask(pdf, TOKEN, "GET"), deps().d)).status, 405);
});

test("what isn't allowed goes no further than the function", async () => {
  const cases: [Record<string, unknown>, number, string][] = [
    [{ ...pdf, party_id: "x" }, 400, "bad_request"],
    [{ ...pdf, mime: "application/x-msdownload", name: "setup.exe" }, 400, "bad_type"],
    [{ ...pdf, mime: "application/zip", name: "notes.zip" }, 400, "bad_type"],
    [{ ...pdf, mime: "application/vnd.ms-word.document.macroEnabled.12", name: "notes.docm" }, 400, "bad_type"],
    // An app renamed, declared as what its name says it isn't.
    [{ ...pdf, name: "setup.exe" }, 400, "bad_type"],
    [{ ...pdf, name: "notes.pdf.exe" }, 400, "bad_type"],
    [{ ...pdf, size: 20 * 1024 * 1024 + 1 }, 413, "too_large"],
    [{ ...pdf, size: 0 }, 400, "bad_request"],
    [{ ...pdf, size: 1.5 }, 400, "bad_request"],
    [{ ...pdf, name: "///" }, 400, "bad_name"],
    [{ ...pdf, name: "x".repeat(1001) }, 400, "bad_name"],
  ];
  for (const [body, status, error] of cases) {
    const { d, asked } = deps();
    const res = await handleRequestUpload(ask(body), d);
    assert.equal(res.status, status, JSON.stringify(body).slice(0, 80));
    assert.deepEqual(await res.json(), { error });
    assert.equal(asked.length, 0, "the database wasn't asked");
  }
  const bad = await handleRequestUpload(new Request("https://example.test", { method: "POST", headers: { Authorization: `Bearer ${TOKEN}` }, body: "{" }), deps().d);
  assert.equal(bad.status, 400);
});

test("the database's refusals come back with their reason", async () => {
  for (const [refusal, status] of [["not_a_member", 403], ["party_storage_full", 413], ["too_many_uploads", 429], ["bad_name", 400]] as const) {
    const { d, signed } = deps(refusal);
    const res = await handleRequestUpload(ask(pdf), d);
    assert.equal(res.status, status);
    assert.deepEqual(await res.json(), { error: refusal });
    assert.equal(signed.length, 0, "no URL for a refused file");
  }
});

test("a name is cleaned of folders, path and control characters, and kept short", () => {
  assert.equal(cleanName("../../etc/passwd.pdf"), "passwd.pdf");
  assert.equal(cleanName("C:\\Users\\me\\Week 3 notes.docx"), "Week 3 notes.docx");
  assert.equal(cleanName("in\u0000voice\u202e.pdf"), "invoice.pdf");
  assert.equal(cleanName('a<b>c:d"e|f?g*.txt'), "abcdefg.txt");
  assert.equal(cleanName("  ...hidden.pdf  "), "hidden.pdf");
  assert.equal(cleanName("   "), null);
  assert.equal(cleanName(".."), null);
  const long = cleanName(`${"y".repeat(200)}.pdf`)!;
  assert.equal(long.length, 120);
  assert.ok(long.endsWith(".pdf"));
  assert.ok(nameFitsType("photo.JPEG", "image/jpeg"));
  assert.ok(!nameFitsType("photo.png", "image/jpeg"));
});
