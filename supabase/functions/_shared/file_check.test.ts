// Run with: node --test "supabase/functions/**/*.test.ts"   (Node 22.18+ runs the TypeScript directly)
// Hostile files, made byte by byte: what each really is decides, never its name or declared type.
import { test } from "node:test";
import assert from "node:assert/strict";
import { inspect, sha256, zipNames } from "./file_check.ts";
import { cleanName, MAX_FILE_BYTES } from "./study_files.ts";

const DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
const XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
const PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
const enc = new TextEncoder();

/** A zip of stored (uncompressed) entries, well-formed enough for any reader of its directory. */
export function zip(entries: Record<string, string>): Uint8Array {
  const locals: Uint8Array[] = [];
  const centrals: Uint8Array[] = [];
  let offset = 0;
  for (const [name, content] of Object.entries(entries)) {
    const n = enc.encode(name);
    const data = enc.encode(content);
    const local = new Uint8Array(30 + n.length + data.length);
    const lv = new DataView(local.buffer);
    lv.setUint32(0, 0x04034b50, true);
    lv.setUint16(4, 20, true);
    lv.setUint32(18, data.length, true);
    lv.setUint32(22, data.length, true);
    lv.setUint16(26, n.length, true);
    local.set(n, 30);
    local.set(data, 30 + n.length);
    const central = new Uint8Array(46 + n.length);
    const cv = new DataView(central.buffer);
    cv.setUint32(0, 0x02014b50, true);
    cv.setUint16(4, 20, true);
    cv.setUint16(6, 20, true);
    cv.setUint32(20, data.length, true);
    cv.setUint32(24, data.length, true);
    cv.setUint16(28, n.length, true);
    cv.setUint32(42, offset, true);
    central.set(n, 46);
    locals.push(local);
    centrals.push(central);
    offset += local.length;
  }
  const centralSize = centrals.reduce((s, c) => s + c.length, 0);
  const end = new Uint8Array(22);
  const ev = new DataView(end.buffer);
  ev.setUint32(0, 0x06054b50, true);
  ev.setUint16(8, centrals.length, true);
  ev.setUint16(10, centrals.length, true);
  ev.setUint32(12, centralSize, true);
  ev.setUint32(16, offset, true);
  const out = new Uint8Array(offset + centralSize + 22);
  let at = 0;
  for (const part of [...locals, ...centrals, end]) {
    out.set(part, at);
    at += part.length;
  }
  return out;
}

/** An OLE (old Office) file's start, with these stream names in its directory, as UTF-16LE. */
export function ole(...streams: string[]): Uint8Array {
  const head = [0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1];
  const names = streams.flatMap((s) => [...s].flatMap((c) => [c.charCodeAt(0), 0]).concat([0, 0]));
  return new Uint8Array([...head, ...new Array(504).fill(0), ...names]);
}

const office = (main: string, extra: Record<string, string> = {}) =>
  zip({ "[Content_Types].xml": "<Types/>", "_rels/.rels": "<Relationships/>", [main]: "<x/>", ...extra });

const pdf = enc.encode("%PDF-1.7\n%âã\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n");
const png = new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 0, 0, 0, 13]);
const jpeg = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, 0, 16, 0x4a, 0x46, 0x49, 0x46]);
const exe = new Uint8Array([0x4d, 0x5a, 0x90, 0x00, 0x03, 0x00, 0x00, 0x00, 0x04, 0x00]);
const elf = new Uint8Array([0x7f, 0x45, 0x4c, 0x46, 2, 1, 1, 0]);

test("each allowed type passes as what it is", () => {
  assert.deepEqual(inspect(pdf, "application/pdf", MAX_FILE_BYTES), { ok: true, mime: "application/pdf" });
  assert.deepEqual(inspect(png, "image/png", MAX_FILE_BYTES), { ok: true, mime: "image/png" });
  assert.deepEqual(inspect(jpeg, "image/jpeg", MAX_FILE_BYTES), { ok: true, mime: "image/jpeg" });
  assert.deepEqual(inspect(office("word/document.xml"), DOCX, MAX_FILE_BYTES), { ok: true, mime: DOCX });
  assert.deepEqual(inspect(office("xl/workbook.xml"), XLSX, MAX_FILE_BYTES), { ok: true, mime: XLSX });
  assert.deepEqual(inspect(office("ppt/presentation.xml"), PPTX, MAX_FILE_BYTES), { ok: true, mime: PPTX });
  assert.deepEqual(inspect(ole("Root Entry", "WordDocument", "1Table"), "application/msword", MAX_FILE_BYTES), { ok: true, mime: "application/msword" });
  assert.deepEqual(inspect(ole("Root Entry", "Workbook"), "application/vnd.ms-excel", MAX_FILE_BYTES), { ok: true, mime: "application/vnd.ms-excel" });
  assert.deepEqual(inspect(ole("Root Entry", "PowerPoint Document"), "application/vnd.ms-powerpoint", MAX_FILE_BYTES), { ok: true, mime: "application/vnd.ms-powerpoint" });
  assert.deepEqual(inspect(enc.encode("Name,Score\nAria,10\n"), "text/csv", MAX_FILE_BYTES), { ok: true, mime: "text/csv" });
  assert.deepEqual(inspect(enc.encode("Chapter 3: waves\tété السلام\r\n"), "text/plain", MAX_FILE_BYTES), { ok: true, mime: "text/plain" });
});

test("a program renamed .pdf, .txt or .docx is refused", () => {
  for (const declared of ["application/pdf", "text/plain", "text/csv", DOCX, "image/png"]) {
    for (const program of [exe, elf]) {
      const verdict = inspect(program, declared, MAX_FILE_BYTES);
      assert.equal(verdict.ok, false, declared);
    }
  }
  assert.deepEqual(inspect(exe, "application/pdf", MAX_FILE_BYTES), { ok: false, reason: "bad_type" });
  assert.deepEqual(inspect(exe, "text/plain", MAX_FILE_BYTES), { ok: false, reason: "bad_text" });
});

test("a zip with a .pdf (or .docx) name is refused: archives aren't allowed", () => {
  const archive = zip({ "report.pdf": "%PDF-1.4", "setup.exe": "MZ" });
  assert.deepEqual(inspect(archive, "application/pdf", MAX_FILE_BYTES), { ok: false, reason: "embedded" });
  const plain = zip({ "notes.txt": "hello" });
  assert.deepEqual(inspect(plain, "application/pdf", MAX_FILE_BYTES), { ok: false, reason: "bad_type" });
  assert.deepEqual(inspect(plain, DOCX, MAX_FILE_BYTES), { ok: false, reason: "bad_type" });
  // Cut short: no directory to read.
  assert.deepEqual(inspect(archive.subarray(0, 40), DOCX, MAX_FILE_BYTES), { ok: false, reason: "bad_type" });
  assert.equal(zipNames(new Uint8Array([0x50, 0x4b, 0x03, 0x04])), null);
});

test("macro documents and embedded programs are refused, old and new", () => {
  assert.deepEqual(inspect(office("word/document.xml", { "word/vbaProject.bin": "VBA" }), DOCX, MAX_FILE_BYTES), { ok: false, reason: "macros" });
  assert.deepEqual(inspect(office("xl/workbook.xml", { "xl/vbaProject.bin": "VBA" }), XLSX, MAX_FILE_BYTES), { ok: false, reason: "macros" });
  assert.deepEqual(inspect(office("word/document.xml", { "word/activeX/activeX1.xml": "<x/>" }), DOCX, MAX_FILE_BYTES), { ok: false, reason: "macros" });
  assert.deepEqual(inspect(office("word/document.xml", { "word/embeddings/oleObject1.bin": "x" }), DOCX, MAX_FILE_BYTES), { ok: false, reason: "embedded" });
  assert.deepEqual(inspect(office("ppt/presentation.xml", { "ppt/media/run.exe": "MZ" }), PPTX, MAX_FILE_BYTES), { ok: false, reason: "embedded" });
  assert.deepEqual(inspect(ole("Root Entry", "WordDocument", "Macros", "VBA", "_VBA_PROJECT"), "application/msword", MAX_FILE_BYTES), { ok: false, reason: "macros" });
  assert.deepEqual(inspect(ole("Root Entry", "Workbook", "_VBA_PROJECT_CUR"), "application/vnd.ms-excel", MAX_FILE_BYTES), { ok: false, reason: "macros" });
  assert.deepEqual(inspect(ole("Root Entry", "WordDocument", "ObjectPool", "\u0001Ole10Native"), "application/msword", MAX_FILE_BYTES), { ok: false, reason: "embedded" });
  // An installer is OLE too, and isn't any document.
  assert.deepEqual(inspect(ole("Root Entry", "䡀㬿"), "application/msword", MAX_FILE_BYTES), { ok: false, reason: "bad_type" });
});

test("what it is must be what it said: a PNG sent as a PDF, a .docx sent as a spreadsheet", () => {
  assert.deepEqual(inspect(png, "application/pdf", MAX_FILE_BYTES), { ok: false, reason: "type_mismatch" });
  assert.deepEqual(inspect(office("word/document.xml"), XLSX, MAX_FILE_BYTES), { ok: false, reason: "type_mismatch" });
  assert.deepEqual(inspect(pdf, "text/plain", MAX_FILE_BYTES), { ok: false, reason: "type_mismatch" });
  assert.deepEqual(inspect(ole("Root Entry", "WordDocument"), "application/vnd.ms-excel", MAX_FILE_BYTES), { ok: false, reason: "type_mismatch" });
});

test("empty, oversized, and text that isn't text", () => {
  assert.deepEqual(inspect(new Uint8Array(0), "application/pdf", MAX_FILE_BYTES), { ok: false, reason: "empty" });
  assert.deepEqual(inspect(new Uint8Array(11), "text/plain", 10), { ok: false, reason: "too_large" });
  assert.deepEqual(inspect(new Uint8Array([0x68, 0x00, 0x69]), "text/plain", MAX_FILE_BYTES), { ok: false, reason: "bad_text" });
  assert.deepEqual(inspect(new Uint8Array([0xc3, 0x28]), "text/csv", MAX_FILE_BYTES), { ok: false, reason: "bad_text" }, "not UTF-8");
  assert.deepEqual(inspect(enc.encode("ok\u001b[31mred"), "text/plain", MAX_FILE_BYTES), { ok: false, reason: "bad_text" }, "terminal escapes");
});

test("a long, hostile name is cut down and cleaned to its last part (request-upload refuses one over 1,000 characters)", () => {
  const name = cleanName(`${"../".repeat(400)}${"‮".repeat(50)}${"a".repeat(5000)}.pdf`)!;
  assert.equal(name.length, 120);
  assert.ok(name.endsWith(".pdf"));
  assert.ok(!/[\\/‮]/.test(name));
});

test("its hash is the bytes' own", async () => {
  assert.equal(await sha256(enc.encode("abc")), "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
});
