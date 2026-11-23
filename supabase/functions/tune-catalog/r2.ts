import { createHmac, createHash } from "node:crypto";

export interface R2Config {
  accountId: string;
  bucket: string;
  accessKeyId: string;
  secretAccessKey: string;
}

export function toHex(buf: ArrayBuffer | Uint8Array | Buffer): string {
  const bytes = buf instanceof Uint8Array ? buf : new Uint8Array(buf);
  let hex = "";
  for (let i = 0; i < bytes.length; i++) {
    hex += bytes[i].toString(16).padStart(2, "0");
  }
  return hex;
}

export function sha256Hex(data: string | Uint8Array): string {
  return createHash("sha256").update(data).digest("hex");
}

function hmac(key: Uint8Array | string, data: string): Buffer {
  return createHmac("sha256", key).update(data).digest();
}

/**
 * Generates an AWS Signature Version 4 presigned GET URL for Cloudflare R2.
 */
export function generatePresignedGetUrl(
  config: R2Config,
  objectKey: string,
  expiresInSeconds: number = 300,
  now: Date = new Date()
): string {
  const cleanedKey = objectKey.startsWith("/") ? objectKey.slice(1) : objectKey;
  const host = `${config.bucket}.${config.accountId}.r2.cloudflarestorage.com`;
  const endpoint = `https://${host}`;

  const amzDate = now.toISOString().replace(/[:-]|\.\d{3}/g, ""); // YYYYMMDDTHHMMSSZ
  const dateStamp = amzDate.slice(0, 8); // YYYYMMDD
  const region = "auto";
  const service = "s3";
  const credentialScope = `${dateStamp}/${region}/${service}/aws4_request`;

  const queryParams = new URLSearchParams();
  queryParams.set("X-Amz-Algorithm", "AWS4-HMAC-SHA256");
  queryParams.set("X-Amz-Credential", `${config.accessKeyId}/${credentialScope}`);
  queryParams.set("X-Amz-Date", amzDate);
  queryParams.set("X-Amz-Expires", expiresInSeconds.toString());
  queryParams.set("X-Amz-SignedHeaders", "host");

  // Canonical query string must be sorted by param key
  const sortedParams = new URLSearchParams([...queryParams.entries()].sort((a, b) => a[0].localeCompare(b[0])));
  const canonicalQueryString = sortedParams.toString();

  const canonicalUri = `/${encodeURI(cleanedKey)}`;
  const canonicalHeaders = `host:${host}\n`;
  const signedHeaders = "host";
  const payloadHash = "UNSIGNED-PAYLOAD";

  const canonicalRequest = [
    "GET",
    canonicalUri,
    canonicalQueryString,
    canonicalHeaders,
    signedHeaders,
    payloadHash,
  ].join("\n");

  const stringToSign = [
    "AWS4-HMAC-SHA256",
    amzDate,
    credentialScope,
    sha256Hex(canonicalRequest),
  ].join("\n");

  // Signing key derivation
  const kDate = hmac(`AWS4${config.secretAccessKey}`, dateStamp);
  const kRegion = hmac(kDate, region);
  const kService = hmac(kRegion, service);
  const kSigning = hmac(kService, "aws4_request");

  const signature = createHmac("sha256", kSigning).update(stringToSign).digest("hex");

  return `${endpoint}${canonicalUri}?${canonicalQueryString}&X-Amz-Signature=${signature}`;
}
