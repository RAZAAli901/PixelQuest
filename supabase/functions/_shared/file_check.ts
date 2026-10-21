// What an uploaded file really is, from its bytes (docs/study-chat.md, How a file is shared, step 3).
// Its name and declared type are only claims: the content decides. Anything not clearly one of the
// allowed types is rejected, and so is an allowed type carrying macros or embedded programs.
// No runtime-specific imports: the same code runs in Deno and under Node for the tests.

export type Verdict = { ok: true; mime: string } | { ok: false; reason: Rejection };

/**
 * Why a file was rejected, for its uploader: empty, too big, a type that isn't allowed (or isn't
 * what it claimed), an Office file with macros or embedded programs, or text that isn't text.
 */
export type Rejection = "empty" | "too_large" | "bad_type" | "type_mismatch" | "macros" | "embedded" | "bad_text";

const DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
const XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
const PPTX = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

const startsWith = (bytes: Uint8Array, magic: number[], at = 0) =>
  bytes.length >= at + magic.length && magic.every((b, i) => bytes[at + i] === b);

const ascii = (text: string) => [...text].map((c) => c.charCodeAt(0));

/** Whether [bytes] holds [text] as UTF-16LE, as OLE stream names are written. */
function hasUtf16(bytes: Uint8Array, text: string): boolean {
  const needle = new Uint8Array(text.length * 2);
  for (let i = 0; i < text.length; i++) needle[i * 2] = text.charCodeAt(i);
  outer: for (let i = 0; i + needle.length <= bytes.length; i++) {
    for (let j = 0; j < needle.length; j++) if (bytes[i + j] !== needle[j]) continue outer;
    return true;
  }
  return false;
}

/** An old Office (OLE) file: which one, by its main stream; rejected with macros or embedded objects. */
function inspectOle(bytes: Uint8Array): Verdict {
  if (hasUtf16(bytes, "_VBA_PROJECT") || hasUtf16(bytes, "Macros") || hasUtf16(bytes, "_VBA_PROJECT_CUR")) {
    return { ok: false, reason: "macros" };
  }
  // A packaged object (Packager) or an object pool can carry a program inside.
  if (hasUtf16(bytes, "\u0001Ole10Native") || hasUtf16(bytes, "ObjectPool")) return { ok: false, reason: "embedded" };
  if (hasUtf16(bytes, "WordDocument")) return { ok: true, mime: "application/msword" };
  if (hasUtf16(bytes, "Workbook") || hasUtf16(bytes, "Book")) return { ok: true, mime: "application/vnd.ms-excel" };
  if (hasUtf16(bytes, "PowerPoint Document")) return { ok: true, mime: "application/vnd.ms-powerpoint" };
  // Other OLE files (an .msi, an Outlook message) aren't allowed.
  return { ok: false, reason: "bad_type" };
}

/** The names in a zip's central directory, or null if it isn't a well-formed zip. */
export function zipNames(bytes: Uint8Array): string[] | null {
  const view = new DataView(bytes.buffer, bytes.byteOffset, bytes.byteLength);
  // The end of central directory record: within the last 64 KB (a comment's at most that).
  let end = -1;
  for (let i = bytes.length - 22; i >= Math.max(0, bytes.length - 22 - 65_535); i--) {
    if (view.getUint32(i, true) === 0x06054b50) {
      end = i;
      break;
    }
  }
  if (end < 0) return null;
  const count = view.getUint16(end + 10, true);
  const size = view.getUint32(end + 12, true);
  let at = view.getUint32(end + 16, true);
  if (count > 10_000 || at + size > bytes.length) return null;
  const names: string[] = [];
  const decoder = new TextDecoder("utf-8", { fatal: false });
  for (let n = 0; n < count; n++) {
    if (at + 46 > bytes.length || view.getUint32(at, true) !== 0x02014b50) return null;
    const nameLength = view.getUint16(at + 28, true);
    const extraLength = view.getUint16(at + 30, true);
    const commentLength = view.getUint16(at + 32, true);
    if (at + 46 + nameLength > bytes.length) return null;
    names.push(decoder.decode(bytes.subarray(at + 46, at + 46 + nameLength)));
    at += 46 + nameLength + extraLength + commentLength;
  }
  return names;
}

/** A new Office file (a zip of parts): which one, by its main part; rejected with macros or programs. */
function inspectOoxml(bytes: Uint8Array): Verdict {
  const names = zipNames(bytes);
  if (!names) return { ok: false, reason: "bad_type" };
  const lower = names.map((n) => n.toLowerCase());
  if (lower.some((n) => n.endsWith("vbaproject.bin") || n.endsWith("vbadata.xml") || n.includes("/activex/"))) {
    return { ok: false, reason: "macros" };
  }
  if (lower.some((n) => /\.(exe|dll|com|bat|cmd|js|jse|vbs|vbe|wsf|ps1|msi|scr|jar|apk|sh|lnk)$/.test(n) || /\/embeddings\/.*\.bin$/.test(n))) {
    return { ok: false, reason: "embedded" };
  }
  // Every Office file names its parts' types; without it, it's an ordinary zip (an archive).
  if (!lower.includes("[content_types].xml")) return { ok: false, reason: "bad_type" };
  if (lower.includes("word/document.xml")) return { ok: true, mime: DOCX };
  if (lower.includes("xl/workbook.xml")) return { ok: true, mime: XLSX };
  if (lower.includes("ppt/presentation.xml")) return { ok: true, mime: PPTX };
  return { ok: false, reason: "bad_type" };
}

/** Text, as TXT and CSV are: valid UTF-8, with no control characters but tab and line ends. */
function isText(bytes: Uint8Array): boolean {
  let text: string;
  try {
    text = new TextDecoder("utf-8", { fatal: true }).decode(bytes);
  } catch {
    return false;
  }
  // deno-lint-ignore no-control-regex
  return !/[\u0000-\u0008\u000b\u000c\u000e-\u001f\u007f]/.test(text);
}

/** What [bytes] is, judged against the type it was [declared] as. */
export function inspect(bytes: Uint8Array, declared: string, maxBytes: number): Verdict {
  if (bytes.length === 0) return { ok: false, reason: "empty" };
  if (bytes.length > maxBytes) return { ok: false, reason: "too_large" };
  let found: Verdict;
  if (startsWith(bytes, ascii("%PDF-"))) found = { ok: true, mime: "application/pdf" };
  else if (startsWith(bytes, [0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a])) found = { ok: true, mime: "image/png" };
  else if (startsWith(bytes, [0xff, 0xd8, 0xff])) found = { ok: true, mime: "image/jpeg" };
  else if (startsWith(bytes, [0xd0, 0xcf, 0x11, 0xe0, 0xa1, 0xb1, 0x1a, 0xe1])) found = inspectOle(bytes);
  else if (startsWith(bytes, [0x50, 0x4b, 0x03, 0x04]) || startsWith(bytes, [0x50, 0x4b, 0x05, 0x06])) found = inspectOoxml(bytes);
  else if (declared === "text/plain" || declared === "text/csv") {
    return isText(bytes) ? { ok: true, mime: declared } : { ok: false, reason: "bad_text" };
  } else found = { ok: false, reason: "bad_type" };
  if (!found.ok) return found;
  // What it is must be what it said it was (a PNG sent as a PDF is refused, not quietly kept).
  return found.mime === declared ? found : { ok: false, reason: "type_mismatch" };
}

/** The SHA-256 of [bytes], in hex. */
export async function sha256(bytes: Uint8Array): Promise<string> {
  const digest = await crypto.subtle.digest("SHA-256", bytes);
  return [...new Uint8Array(digest)].map((b) => b.toString(16).padStart(2, "0")).join("");
}
