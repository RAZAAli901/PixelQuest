// Run with: node --test "supabase/functions/**/*.test.ts"   (Node 22.18+ runs the TypeScript directly)
import { test } from "node:test";
import assert from "node:assert/strict";
import { handleDownload, type DownloadDeps } from "./handler.ts";

const USER = "3f2b8c1e-9a4d-4e6f-8b2a-1c3d5e7f9a0b";
const FILE = "7d1e2f3a-4b5c-4d6e-8f9a-0b1c2d3e4f5a";
const TOKEN = "user-access-token";

function deps(allowed = true) {
  const signed: { path: string; seconds: number; name: string }[] = [];
  const d: DownloadDeps = {
    getUserId: async (token) => (token === TOKEN ? USER : null),
    loadFile: async () =>
      allowed
        ? { ok: true, data: { storage_path: "p1/obj", display_name: "Past paper.pdf", mime: "application/pdf", size_bytes: 1200 } }
        : { ok: false, error: "not_allowed" },
    signDownload: async (path, seconds, name) => {
      signed.push({ path, seconds, name });
      return `https://example.test/sign/${path}?token=t&download=${encodeURIComponent(name)}`;
    },
  };
  return { d, signed };
}

const open = (d: DownloadDeps, body: unknown = { file_id: FILE }, token: string | null = TOKEN) =>
  handleDownload(
    new Request("https://example.test/functions/v1/get-download-url", {
      method: "POST",
      headers: { "Content-Type": "application/json", ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: JSON.stringify(body),
    }),
    d,
  );

test("a member gets a 60-second URL that downloads it as an attachment", async () => {
  const { d, signed } = deps();
  const res = await open(d);
  assert.equal(res.status, 200);
  assert.equal(res.headers.get("Cache-Control"), "no-store");
  const body = await res.json();
  assert.equal(body.expires_in, 60);
  assert.equal(body.name, "Past paper.pdf");
  assert.match(body.url, /download=Past%20paper\.pdf/);
  assert.deepEqual(signed, [{ path: "p1/obj", seconds: 60, name: "Past paper.pdf" }]);
});

test("anyone else gets nothing, and learns nothing from the answer", async () => {
  const { d, signed } = deps(false);
  const res = await open(d);
  assert.equal(res.status, 403);
  assert.deepEqual(await res.json(), { error: "not_allowed" });
  assert.equal(signed.length, 0);
  assert.equal((await open(deps().d, { file_id: FILE }, null)).status, 401);
  assert.equal((await open(deps().d, { file_id: "nope" })).status, 400);
});
