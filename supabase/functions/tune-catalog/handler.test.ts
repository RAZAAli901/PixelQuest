import test from "node:test";
import assert from "node:assert/strict";
import { handleTuneCatalog, validateCatalog, type Catalog } from "./handler.ts";
import { generatePresignedGetUrl, type R2Config } from "./r2.ts";

const sampleCatalogJson = JSON.stringify({
  version: 1,
  tunes: [
    {
      id: "calm-bells-v1",
      name: "Calm Bells",
      description: "Soft bell pattern",
      categories: ["quest", "namaz"],
      objectKey: "tunes/calm-bells-v1.ogg",
      previewObjectKey: "tunes/previews/calm-bells-v1.ogg",
      mimeType: "audio/ogg",
      bytes: 182340,
      durationMillis: 12000,
      sha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    },
    {
      id: "malicious-traversal",
      name: "Hacker Tune",
      description: "Tries directory traversal",
      categories: ["quest"],
      objectKey: "tunes/../../etc/passwd",
      previewObjectKey: "tunes/previews/hack.ogg",
      mimeType: "audio/ogg",
      bytes: 100,
      durationMillis: 1000,
      sha256: "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855",
    },
  ],
});

const fakeR2: R2Config = {
  accountId: "acc123",
  bucket: "test-bucket",
  accessKeyId: "key123",
  secretAccessKey: "secret123",
};

test("validateCatalog filters invalid entries and traversal attempts", () => {
  const catalog = validateCatalog(JSON.parse(sampleCatalogJson));
  assert.equal(catalog.tunes.length, 1);
  assert.equal(catalog.tunes[0].id, "calm-bells-v1");
});

test("refuses unauthorized requests", async () => {
  const req = new Request("http://localhost/tune-catalog", { method: "GET" });
  const res = await handleTuneCatalog(req, {
    getUserId: async () => null,
    fetchCatalog: async () => sampleCatalogJson,
    r2Config: fakeR2,
  });
  assert.equal(res.status, 401);
});

test("allows only authenticated GET requests when R2 is configured", async () => {
  const req = new Request("http://localhost/tune-catalog", {
    method: "POST",
    headers: { Authorization: "Bearer valid-token" },
  });
  const res = await handleTuneCatalog(req, {
    getUserId: async () => "user-uuid",
    fetchCatalog: async () => sampleCatalogJson,
    r2Config: fakeR2,
  });
  assert.equal(res.status, 405);
});

test("does not issue URLs when R2 credentials are absent", async () => {
  const req = new Request("http://localhost/tune-catalog", {
    method: "GET",
    headers: { Authorization: "Bearer valid-token" },
  });
  const res = await handleTuneCatalog(req, {
    getUserId: async () => "user-uuid",
    fetchCatalog: async () => sampleCatalogJson,
    r2Config: null,
  });
  assert.equal(res.status, 503);
});

test("returns catalog with preview URLs for authenticated user", async () => {
  const req = new Request("http://localhost/tune-catalog", {
    method: "GET",
    headers: { Authorization: "Bearer valid-token" },
  });
  const res = await handleTuneCatalog(req, {
    getUserId: async (t) => (t === "valid-token" ? "user-uuid" : null),
    fetchCatalog: async () => sampleCatalogJson,
    r2Config: fakeR2,
    signUrl: (k) => `https://signed.r2/${k}?sig=123`,
  });

  assert.equal(res.status, 200);
  const data = await res.json() as Catalog;
  assert.equal(data.tunes.length, 1);
  assert.equal(data.tunes[0].id, "calm-bells-v1");
  assert.ok((data.tunes[0] as any).previewUrl.includes("signed.r2"));
});

test("returns specific tune with downloadUrl when tuneId query param is provided", async () => {
  const req = new Request("http://localhost/tune-catalog?tuneId=calm-bells-v1", {
    method: "GET",
    headers: { Authorization: "Bearer valid-token" },
  });
  const res = await handleTuneCatalog(req, {
    getUserId: async () => "user-uuid",
    fetchCatalog: async () => sampleCatalogJson,
    r2Config: fakeR2,
    signUrl: (k) => `https://signed.r2/${k}?sig=download`,
  });

  assert.equal(res.status, 200);
  const data = await res.json() as any;
  assert.equal(data.tune.id, "calm-bells-v1");
  assert.ok(data.tune.downloadUrl.includes("tunes/calm-bells-v1.ogg"));
});

test("returns 404 for unknown tuneId", async () => {
  const req = new Request("http://localhost/tune-catalog?tuneId=non-existent", {
    method: "GET",
    headers: { Authorization: "Bearer valid-token" },
  });
  const res = await handleTuneCatalog(req, {
    getUserId: async () => "user-uuid",
    fetchCatalog: async () => sampleCatalogJson,
    r2Config: fakeR2,
  });

  assert.equal(res.status, 404);
});

test("returns empty catalog safely when catalog.json is missing or corrupt", async () => {
  const req = new Request("http://localhost/tune-catalog", {
    method: "GET",
    headers: { Authorization: "Bearer valid-token" },
  });
  const res = await handleTuneCatalog(req, {
    getUserId: async () => "user-uuid",
    fetchCatalog: async () => null,
    r2Config: fakeR2,
  });

  assert.equal(res.status, 200);
  const data = await res.json() as any;
  assert.deepEqual(data.tunes, []);
});

test("generates valid AWS V4 presigned GET URL", () => {
  const now = new Date("2026-11-18T20:00:00Z");
  const url = generatePresignedGetUrl(fakeR2, "tunes/sample.ogg", 300, now);
  assert.ok(url.startsWith("https://test-bucket.acc123.r2.cloudflarestorage.com/tunes/sample.ogg"));
  assert.ok(url.includes("X-Amz-Algorithm=AWS4-HMAC-SHA256"));
  assert.ok(url.includes("X-Amz-Signature="));
});
