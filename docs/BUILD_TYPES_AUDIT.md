# Build types and flavours audit

Reviewed 2026-10-22 for Q018.

## Build types

The project uses only two build types:
- `debug`: talks to development Supabase when `SUPABASE_DEV_URL` is set, enables debug logging, disables minification.
- `release`: enables R8 code shrinking (`isMinifyEnabled = true`) and resource shrinking (`isShrinkResources = true`), proxies Gemini via server Edge Function, and signs with release keystore.

No intermediate, staging, or abandoned build types exist.

## Flavours

No product flavours or flavour dimensions are defined. The single application ID `com.pixelquest.app` serves all variants.

## Removed flags

- `vectorDrawables.useSupportLibrary = true`: removed as `minSdk = 24` natively supports VectorDrawables without support library overhead (introduced in API 21).
