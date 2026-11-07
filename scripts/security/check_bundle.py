#!/usr/bin/env python3
"""Validate Android App Bundle (.aab) format, structure, targetSdk, and launcher icon for Google Play.

Usage:
    python3 scripts/security/check_bundle.py app/build/outputs/bundle/release/app-release.aab
"""
import os
import sys
import zipfile

EXPECTED_TARGET_SDK = "36"
EXPECTED_COMPILE_SDK = "36"
EXPECTED_ICON = 'android:icon="@mipmap/ic_launcher"'
EXPECTED_ROUND_ICON = 'android:roundIcon="@mipmap/ic_launcher_round"'
MAX_BUNDLE_BYTES = 150 * 1024 * 1024  # 150 MB Play limit; PixelQuest is ~9 MB


def verify_bundle(aab_path: str) -> None:
    if not os.path.isfile(aab_path):
        raise FileNotFoundError(f"App Bundle not found at {aab_path}")

    size = os.path.getsize(aab_path)
    print(f"Bundle size: {size:,} bytes ({size / (1024 * 1024):.2f} MB)")
    if size > MAX_BUNDLE_BYTES:
        raise ValueError(f"Bundle size ({size} bytes) exceeds limit ({MAX_BUNDLE_BYTES} bytes)")

    if not zipfile.is_zipfile(aab_path):
        raise ValueError(f"File at {aab_path} is not a valid zip archive")

    with zipfile.ZipFile(aab_path, 'r') as zf:
        namelist = set(zf.namelist())

        required_entries = [
            "BundleConfig.pb",
            "base/manifest/AndroidManifest.xml",
        ]
        for req in required_entries:
            if req not in namelist:
                raise ValueError(f"Missing required bundle entry: {req}")

        # Verify dex files exist in base module
        has_dex = any(name.startswith("base/dex/classes") and name.endswith(".dex") for name in namelist)
        if not has_dex:
            raise ValueError("No dex classes found in base/dex/ within App Bundle")

        # Verify res or base resources
        has_res = any(name.startswith("base/res/") for name in namelist)
        if not has_res:
            raise ValueError("No resources found in base/res/ within App Bundle")

        print(f"Verified App Bundle structure: {len(namelist)} entries found in archive.")


def verify_build_configuration(repo_root: str = ".") -> None:
    toml_path = os.path.join(repo_root, "gradle", "libs.versions.toml")
    build_gradle = os.path.join(repo_root, "app", "build.gradle.kts")

    if not os.path.isfile(toml_path):
        raise FileNotFoundError(f"libs.versions.toml not found at {toml_path}")
    if not os.path.isfile(build_gradle):
        raise FileNotFoundError(f"build.gradle.kts not found at {build_gradle}")

    with open(toml_path, "r", encoding="utf-8") as f:
        toml_content = f.read()

    target_match = f'targetSdk = "{EXPECTED_TARGET_SDK}"' in toml_content
    compile_match = f'compileSdk = "{EXPECTED_COMPILE_SDK}"' in toml_content

    if not target_match:
        raise ValueError(f'targetSdk != "{EXPECTED_TARGET_SDK}" in {toml_path}')
    if not compile_match:
        raise ValueError(f'compileSdk != "{EXPECTED_COMPILE_SDK}" in {toml_path}')

    print(f"Verified compileSdk={EXPECTED_COMPILE_SDK} and targetSdk={EXPECTED_TARGET_SDK} in gradle/libs.versions.toml")


def verify_manifest_icon(repo_root: str = ".") -> None:
    manifest = os.path.join(repo_root, "app", "src", "main", "AndroidManifest.xml")
    if not os.path.isfile(manifest):
        raise FileNotFoundError(f"AndroidManifest.xml not found at {manifest}")

    with open(manifest, "r", encoding="utf-8") as f:
        content = f.read()

    if EXPECTED_ICON not in content:
        raise ValueError(f"Missing launcher icon '{EXPECTED_ICON}' in {manifest}")
    if EXPECTED_ROUND_ICON not in content:
        raise ValueError(f"Missing round launcher icon '{EXPECTED_ROUND_ICON}' in {manifest}")

    print(f"Verified launcher icons ({EXPECTED_ICON}, {EXPECTED_ROUND_ICON}) in AndroidManifest.xml")


def main() -> int:
    aab_path = sys.argv[1] if len(sys.argv) > 1 else "app/build/outputs/bundle/release/app-release.aab"
    try:
        verify_build_configuration()
        verify_manifest_icon()
        verify_bundle(aab_path)
        print("PLAY READINESS BUNDLE VALIDATION PASSED!")
        return 0
    except Exception as e:
        print(f"PLAY READINESS VALIDATION FAILED: {e}", file=sys.stderr)
        return 1


if __name__ == "__main__":
    sys.exit(main())
