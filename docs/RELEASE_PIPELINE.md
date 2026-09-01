# PixelQuest GitHub Actions Release Pipeline Documentation

This document outlines the workflow and secrets configuration for the PixelQuest Automated Release Pipeline (`.github/workflows/release.yml`).

## Trigger Conditions
- Any git tag push matching `v*` (e.g. `v1.0.0`, `v1.0.0-rc1`).

## Required GitHub Repository Secrets
To build and publish signed release APKs, the following secrets must be set in GitHub Repository Settings -> Secrets and variables -> Actions:

| Secret Name | Description | Example / Value |
|-------------|-------------|-----------------|
| `KEYSTORE_BASE64` | Base64-encoded string of `pixelquest-release.jks` | `MIIK...` |
| `KEYSTORE_PASSWORD` | Passphrase for the release keystore | `your_store_password` |
| `KEY_ALIAS` | Key alias inside the keystore | `pixelquest` |
| `KEY_PASSWORD` | Passphrase for the key alias | `your_key_password` |
| `GITHUB_TOKEN` | Automatic GitHub token provided by Actions | Default `${{ secrets.GITHUB_TOKEN }}` |

## Local Test Tag Verification Script
```bash
# Push test tag to trigger release workflow run
git tag v1.0.0-rc1
git push origin v1.0.0-rc1
```
