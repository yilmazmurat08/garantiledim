#!/usr/bin/env bash
# Emülatörde: ekran turu testini çalıştırır, görüntüleri çeker, sonra imzalı
# release APK'yı kurup açılışta çökmediğini kontrol eder.
set -uo pipefail

PKG=com.garantiledim.app
OUT=ci-out
mkdir -p "$OUT"
status=0

adb wait-for-device
adb shell settings put global window_animation_scale 0
adb shell settings put global transition_animation_scale 0
adb shell settings put global animator_duration_scale 0

adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb install -r -g app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk

adb logcat -c
adb shell am instrument -w -r \
  -e class "$PKG.ScreenshotTourTest" \
  "$PKG.test/androidx.test.runner.AndroidJUnitRunner" | tee "$OUT/instrumentation.txt"
if ! grep -q "OK (" "$OUT/instrumentation.txt"; then
  echo "::error::Ekran turu testi başarısız"
  status=1
fi
adb logcat -d > "$OUT/logcat-debug.txt"

mkdir -p "$OUT/screenshots"
adb exec-out run-as "$PKG" tar cf - -C files/screenshots . | tar xf - -C "$OUT/screenshots" || true
adb exec-out screencap -p > "$OUT/screenshots/zz-son-durum.png" || true

# Release APK (R8 ile küçültülmüş, test anahtarıyla imzalı) açılış kontrolü
adb uninstall "$PKG" || true
adb install -r app/build/outputs/apk/release/app-release.apk
adb logcat -c
adb shell am start -W -n "$PKG/.MainActivity"
sleep 8
adb exec-out screencap -p > "$OUT/screenshots/release-acilis.png" || true
adb logcat -d > "$OUT/logcat-release.txt"
if [ -z "$(adb shell pidof "$PKG")" ] || grep -q "FATAL EXCEPTION" "$OUT/logcat-release.txt"; then
  echo "::error::Release APK açılışta çöktü"
  grep -A 30 "FATAL EXCEPTION" "$OUT/logcat-release.txt" || true
  status=1
fi

ls -la "$OUT/screenshots"
exit $status
