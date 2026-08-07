#!/usr/bin/env bash
set -euo pipefail

mkdir -p qa/screenshots qa/test-results

{
  echo "size=$(adb shell wm size | tr -d '\r')"
  echo "density=$(adb shell wm density | tr -d '\r')"
  echo "android_release=$(adb shell getprop ro.build.version.release | tr -d '\r')"
  echo "android_api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')"
  echo "abi=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')"
} > qa/EMULATOR_DEVICE.txt

./gradlew connectedDebugAndroidTest --stacktrace

adb pull /sdcard/Android/data/cl.habitosqa.app/files/screenshots/. qa/screenshots/
for file in \
  01_empty_light.png \
  02_habits_light.png \
  03_history_light.png \
  04_habits_dark.png; do
  test -s "qa/screenshots/$file"
done
if [ -s qa/screenshots/05_large_text.png ]; then
  echo "Large-text screenshot captured."
fi

adb uninstall cl.habitosqa.app >/dev/null 2>&1 || true
adb logcat -c
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop cl.habitosqa.app
adb shell am start -W -n cl.habitosqa.app/.MainActivity | tee qa/SMOKE_START.txt
sleep 3
PID="$(adb shell pidof cl.habitosqa.app | tr -d '\r')"
test -n "$PID"
adb shell dumpsys activity activities > qa/ACTIVITY_DUMPSYS.txt
grep -q "cl.habitosqa.app/.MainActivity" qa/ACTIVITY_DUMPSYS.txt
adb logcat -d -v threadtime > qa/logcat.txt
if grep -E -A8 -B3 "FATAL EXCEPTION|ANR in cl\.habitosqa\.app" qa/logcat.txt | grep -q "cl.habitosqa.app"; then
  echo "Crash/ANR attributable to Hábitos QA detected."
  exit 1
fi
{
  echo "process_id=$PID"
  echo "status=stable"
} > qa/SMOKE_RESULT.txt

reset_viewport() {
  adb shell wm size reset >/dev/null 2>&1 || true
  adb shell wm density reset >/dev/null 2>&1 || true
}
trap reset_viewport EXIT
adb shell wm size 880x1562
adb shell wm density 440
adb shell am force-stop cl.habitosqa.app
adb shell am start -W -n cl.habitosqa.app/.MainActivity > qa/SMALL_VIEWPORT_START.txt
sleep 2
test -n "$(adb shell pidof cl.habitosqa.app | tr -d '\r')"
adb exec-out screencap -p > qa/screenshots/06_compact_viewport.png
reset_viewport
trap - EXIT
