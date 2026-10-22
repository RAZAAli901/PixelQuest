#!/usr/bin/env python3
"""Fail on an exported component without a reason (S042).

Reads the merged manifest of a build (the app's and its libraries'). Every exported activity,
service, receiver or provider must be protected by a permission, or be listed below with the reason
it's exported (docs/security/MANIFEST_AUDIT.md). A new one fails the build until someone decides.

    python3 scripts/security/check_manifest.py app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
"""
import sys
import xml.etree.ElementTree as ET

A = "{http://schemas.android.com/apk/res/android}"

# Exported on purpose, without a permission of its own.
ALLOWED = {
    "com.pixelquest.app.MainActivity": "the launcher: the home-screen icon starts it",
}


def main(path: str) -> int:
    app = ET.parse(path).getroot().find("application")
    problems = []
    for tag in ("activity", "activity-alias", "service", "receiver", "provider"):
        for c in app.findall(tag):
            name = c.get(A + "name")
            exported = c.get(A + "exported")
            if exported is None and c.find("intent-filter") is not None and tag != "provider":
                problems.append(f"{tag} {name}: no android:exported (required for components with intent filters)")
                continue
            if exported != "true":
                continue
            if c.get(A + "permission") or (tag == "provider" and (c.get(A + "readPermission") or c.get(A + "writePermission"))):
                continue
            if name not in ALLOWED:
                problems.append(f"{tag} {name}: exported with no permission and no reason")
    for p in problems:
        print("MANIFEST:", p)
    print(f"{len(problems)} problem(s) in the exported components.")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
