// Supabase Edge Function entry point for tune-catalog.
import { authUserId } from "../_shared/study_files.ts";
import { handleTuneCatalog } from "./handler.ts";
import { generatePresignedGetUrl, type R2Config } from "./r2.ts";

const supabaseUrl = Deno.env.get("SUPABASE_URL") ?? "";
const anonKey = Deno.env.get("SUPABASE_ANON_KEY") ?? "";
const serviceKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY") ?? "";

const r2Config: R2Config | null = (() => {
  const accountId = Deno.env.get("R2_ACCOUNT_ID");
  const bucket = Deno.env.get("R2_BUCKET");
  const accessKeyId = Deno.env.get("R2_ACCESS_KEY_ID");
  const secretAccessKey = Deno.env.get("R2_SECRET_ACCESS_KEY");
  if (!accountId || !bucket || !accessKeyId || !secretAccessKey) {
    return null;
  }
  return { accountId, bucket, accessKeyId, secretAccessKey };
})();

async function fetchCatalogFromR2(): Promise<string | null> {
  if (!r2Config) return null;
  const catalogUrl = generatePresignedGetUrl(r2Config, "catalog.json", 60);
  const res = await fetch(catalogUrl);
  if (!res.ok) return null;
  return await res.text();
}

Deno.serve((req: Request) =>
  handleTuneCatalog(req, {
    getUserId: authUserId(fetch, supabaseUrl, anonKey || serviceKey),
    fetchCatalog: fetchCatalogFromR2,
    r2Config,
  })
);
