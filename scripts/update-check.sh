#!/usr/bin/env bash
# Tries a real update on a device or emulator, without touching the installed PixelQuest or its data.
#
# Builds two debug APKs, of <from> (a tag or commit, e.g. v1.1.0) and <to> (default: the working
# tree's HEAD), both as a separate app, com.pixelquest.app.updatecheck. Install the first, play,
# install the second over it, and check that the progress came through. See
# docs/UPDATES_AND_BACKUPS.md ("Trying an update on a device").
#
# Usage: scripts/update-check.sh <from> [to]
# Needs: git, a JDK and the Android SDK (as for any build), and local.properties in the repo.
set -euo pipefail

from=${1:?"usage: scripts/update-check.sh <from> [to]"}
to=${2:-HEAD}
repo=$(git rev-parse --show-toplevel)
out="$repo/build/update-check"
mkdir -p "$out"

build() { # <ref> <label>
  local ref=$1 label=$2 wt
  wt=$(mktemp -d "${TMPDIR:-/tmp}/pq-update-check-XXXXXX")
  git -C "$repo" worktree add --detach "$wt" "$ref" >/dev/null
  cp "$repo/local.properties" "$wt/" 2>/dev/null || true
  [ -f "$wt/gradle.properties" ] || cp "$repo/gradle.properties" "$wt/"
  # Older build scripts trip over java.util.Properties in today's Gradle; import it instead.
  if grep -q "java.util.Properties()" "$wt/app/build.gradle.kts"; then
    sed -i "s/java\.util\.Properties()/Properties()/" "$wt/app/build.gradle.kts"
    sed -i "1i import java.util.Properties" "$wt/app/build.gradle.kts"
  fi
  sed -i 's/applicationId = "com.pixelquest.app"/applicationId = "com.pixelquest.app.updatecheck"/' "$wt/app/build.gradle.kts"
  echo "Building $ref as com.pixelquest.app.updatecheck..."
  (cd "$wt" && ./gradlew --no-daemon -q :app:assembleDebug)
  cp "$(find "$wt/app/build/outputs/apk/debug" -name '*.apk' | head -1)" "$out/$label.apk"
  git -C "$repo" worktree remove --force "$wt" >/dev/null 2>&1 || rm -rf "$wt"
  git -C "$repo" worktree prune
}

build "$from" from
build "$to" to

cat <<EOF

Built:
  $out/from.apk  ($from)
  $out/to.apk    ($to)

On the device or emulator:
  adb install "$out/from.apk"
  adb shell pm grant com.pixelquest.app.updatecheck android.permission.POST_NOTIFICATIONS
  # Open "PixelQuest" (the second icon), finish onboarding if shown, complete a quest or two, note the XP.
  adb install -r "$out/to.apk"
  # Open it again: the same XP and quests, and no onboarding for a player who has played.
  adb uninstall com.pixelquest.app.updatecheck
EOF
