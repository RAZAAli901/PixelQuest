#!/usr/bin/env bash
set -euo pipefail

# Benchmark PixelQuest cold startup latency using adb and am start-activity.
# Usage: ./scripts/benchmark-startup.sh [iterations=5]

PACKAGE="com.pixelquest.app"
ACTIVITY="com.pixelquest.app.MainActivity"
ITERATIONS="${1:-5}"

echo "=========================================="
echo "PixelQuest Startup Benchmark Harness"
echo "Package: $PACKAGE"
echo "Activity: $ACTIVITY"
echo "Iterations: $ITERATIONS"
echo "=========================================="

# Ensure adb is reachable and device is connected
if ! command -v adb >/dev/null 2>&1; then
    echo "Error: adb command not found in PATH." >&2
    exit 1
fi

DEVICE_COUNT=$(adb devices | grep -v "List" | grep "device$" | wc -l || true)
if [ "$DEVICE_COUNT" -lt 1 ]; then
    echo "Error: No active Android device or emulator detected via adb." >&2
    exit 1
fi

echo "1. Triggering ProfileInstaller compilation (speed-profile)..."
adb shell cmd package compile -m speed-profile -f "$PACKAGE" || true

echo "2. Running cold start measurements..."
declare -a TIMES=()
TOTAL=0

for i in $(seq 1 "$ITERATIONS"); do
    # Force stop to ensure clean cold start
    adb shell am force-stop "$PACKAGE"
    sleep 1

    # Measure startup using am start-activity -W
    OUTPUT=$(adb shell am start-activity -W -n "$PACKAGE/$ACTIVITY" 2>&1)
    TOTAL_TIME=$(echo "$OUTPUT" | grep -i "TotalTime:" | awk '{print $2}' | tr -d '\r')

    if [ -n "$TOTAL_TIME" ]; then
        echo "  Run $i: ${TOTAL_TIME} ms"
        TIMES+=("$TOTAL_TIME")
        TOTAL=$((TOTAL + TOTAL_TIME))
    else
        echo "  Run $i: Failed to capture TotalTime"
    fi
    sleep 1
done

COUNT=${#TIMES[@]}
if [ "$COUNT" -gt 0 ]; then
    AVG=$((TOTAL / COUNT))
    echo "=========================================="
    echo "Benchmark Summary ($COUNT runs):"
    echo "  Average Cold Start: ${AVG} ms"
    echo "=========================================="
else
    echo "No successful startup runs recorded."
fi
