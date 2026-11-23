import { json, bearerToken, type Fetch } from "../_shared/study_files.ts";
import { generatePresignedGetUrl, type R2Config } from "./r2.ts";

export interface TuneEntry {
  id: string;
  name: string;
  description: string;
  categories: string[];
  objectKey: string;
  previewObjectKey: string;
  mimeType: string;
  bytes: number;
  durationMillis: number;
  sha256: string;
}

export interface Catalog {
  version: number;
  tunes: TuneEntry[];
}

export interface TuneWithUrls extends TuneEntry {
  downloadUrl?: string;
  previewUrl?: string;
}

const ID_REGEX = /^[a-z0-9-]+$/;
const SHA256_REGEX = /^[a-f0-9]{64}$/;
const ALLOWED_MIME_TYPES = new Set(["audio/ogg", "audio/mpeg", "audio/wav"]);
const ALLOWED_CATEGORIES = new Set(["quest", "namaz"]);
const MAX_BYTES = 5 * 1024 * 1024; // 5 MB max per tune
const MAX_DURATION_MS = 60 * 1000; // 60 seconds max

export function isValidTuneEntry(entry: unknown): entry is TuneEntry {
  if (!entry || typeof entry !== "object") return false;
  const t = entry as Record<string, unknown>;

  if (typeof t.id !== "string" || !ID_REGEX.test(t.id)) return false;
  if (typeof t.name !== "string" || t.name.trim().length === 0 || t.name.length > 100) return false;
  if (typeof t.description !== "string" || t.description.length > 200) return false;
  if (!Array.isArray(t.categories) || t.categories.length === 0 || t.categories.some((c) => typeof c !== "string" || !ALLOWED_CATEGORIES.has(c))) return false;

  if (typeof t.objectKey !== "string" || !/^tunes\/[a-zA-Z0-9._/-]+$/.test(t.objectKey) || t.objectKey.includes("..") || t.objectKey.includes("//")) return false;
  if (typeof t.previewObjectKey !== "string" || !/^tunes\/[a-zA-Z0-9._/-]+$/.test(t.previewObjectKey) || t.previewObjectKey.includes("..") || t.previewObjectKey.includes("//")) return false;

  if (typeof t.mimeType !== "string" || !ALLOWED_MIME_TYPES.has(t.mimeType)) return false;
  if (typeof t.bytes !== "number" || t.bytes <= 0 || t.bytes > MAX_BYTES) return false;
  if (typeof t.durationMillis !== "number" || t.durationMillis <= 0 || t.durationMillis > MAX_DURATION_MS) return false;
  if (typeof t.sha256 !== "string" || !SHA256_REGEX.test(t.sha256)) return false;

  return true;
}

export function validateCatalog(data: unknown): Catalog {
  if (!data || typeof data !== "object") {
    return { version: 1, tunes: [] };
  }
  const obj = data as Record<string, unknown>;
  const version = typeof obj.version === "number" ? obj.version : 1;
  const rawTunes = Array.isArray(obj.tunes) ? obj.tunes : [];

  const tunes: TuneEntry[] = [];
  const seenIds = new Set<string>();

  for (const item of rawTunes) {
    if (isValidTuneEntry(item) && !seenIds.has(item.id)) {
      seenIds.add(item.id);
      tunes.push(item);
    }
  }

  return { version, tunes };
}

export interface Dependencies {
  getUserId: (token: string) => Promise<string | null>;
  fetchCatalog: () => Promise<string | null>;
  r2Config: R2Config | null;
  signUrl?: (objectKey: string) => string;
}

export async function handleTuneCatalog(req: Request, deps: Dependencies): Promise<Response> {
  if (req.method !== "GET") {
    return json(405, { error: "method_not_allowed" });
  }

  const token = bearerToken(req);
  if (!token) {
    return json(401, { error: "unauthorized" });
  }

  const userId = await deps.getUserId(token);
  if (!userId) {
    return json(401, { error: "unauthorized" });
  }
  if (!deps.r2Config) return json(503, { error: "not_configured" });

  // Load catalog.json
  let rawJson: string | null = null;
  try {
    rawJson = await deps.fetchCatalog();
  } catch (_e) {
    rawJson = null;
  }

  // If the bucket or catalog.json is empty or unavailable, return a valid empty catalog (docs/R2_TUNE_LIBRARY_PLAN.md §Security)
  let catalog: Catalog = { version: 1, tunes: [] };
  if (rawJson) {
    try {
      catalog = validateCatalog(JSON.parse(rawJson));
    } catch (_e) {
      catalog = { version: 1, tunes: [] };
    }
  }

  const sign = deps.signUrl ?? ((key: string) => {
    if (!deps.r2Config) return "";
    return generatePresignedGetUrl(deps.r2Config, key, 300);
  });

  // If request is requesting a specific tune download URL
  const url = new URL(req.url);
  const tuneId = url.searchParams.get("tuneId");
  if (tuneId) {
    const tune = catalog.tunes.find((t) => t.id === tuneId);
    if (!tune) {
      return json(404, { error: "tune_not_found" });
    }
    const downloadUrl = sign(tune.objectKey);
    const previewUrl = sign(tune.previewObjectKey);
    return json(200, {
      tune: {
        ...tune,
        downloadUrl,
        previewUrl,
      },
    });
  }

  // Return full catalog with preview URLs signed
  const tunesWithPreviews = catalog.tunes.map((t) => ({
    ...t,
    previewUrl: sign(t.previewObjectKey),
  }));

  return json(200, {
    version: catalog.version,
    tunes: tunesWithPreviews,
  });
}
