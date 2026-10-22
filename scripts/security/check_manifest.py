#!/usr/bin/env python3
"""Fail on an exported component without a reason (S042), or a permission nobody decided on (S049).

Reads the merged manifest of a build (the app's and its libraries'). Every exported activity,
service, receiver or provider must be protected by a permission, or be listed below with the reason
it's exported (docs/security/MANIFEST_AUDIT.md). A new one fails the build until someone decides.
Every permission the app asks for must be listed below with what uses it, so a library can't add one
unnoticed.

    python3 scripts/security/check_manifest.py app/build/intermediates/merged_manifest/release/processReleaseMainManifest/AndroidManifest.xml
"""
import sys
import xml.etree.ElementTree as ET

A = "{http://schemas.android.com/apk/res/android}"

# Exported on purpose, without a permission of its own.
ALLOWED = {
    "com.pixelquest.app.MainActivity": "the launcher: the home-screen icon starts it",
}

# Every permission in the merged manifest, and what needs it (docs/security/MANIFEST_AUDIT.md).
PERMISSIONS = {
    "android.permission.INTERNET": "the cloud: sign-in, sync, chat, chess",
    "android.permission.ACCESS_NETWORK_STATE": "NetworkUtils and WorkManager's network constraint",
    "android.permission.POST_NOTIFICATIONS": "reminders and the other notifications (Android 13+)",
    "android.permission.SCHEDULE_EXACT_ALARM": "quest and prayer reminders on time",
    "android.permission.RECEIVE_BOOT_COMPLETED": "BootReceiver re-arms the alarms",
    "android.permission.USE_FULL_SCREEN_INTENT": "the PROMPT reminder's full-screen screen",
    "android.permission.ACCESS_COARSE_LOCATION": "prayer times by city, asked for only when turned on",
    "android.permission.WAKE_LOCK": "WorkManager",
    "android.permission.FOREGROUND_SERVICE": "WorkManager's expedited sync on Android 11 and older",
    "com.google.android.c2dm.permission.RECEIVE": "Firebase push for chess",
    "com.pixelquest.app.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION": "androidx: guards the app's own runtime receivers",
}


def main(path: str) -> int:
    root = ET.parse(path).getroot()
    app = root.find("application")
    problems = []
    for perm in root.findall("uses-permission") + root.findall("uses-permission-sdk-23"):
        name = perm.get(A + "name")
        if name not in PERMISSIONS:
            problems.append(f"uses-permission {name}: not decided (add it to PERMISSIONS with what needs it, or remove it)")
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
    print(f"{len(problems)} problem(s) in the exported components and permissions.")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1]))
