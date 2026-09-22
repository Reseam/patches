#!/usr/bin/env bash
# SPDX-FileCopyrightText: 2026 AunAli K. <hello@auna.li>
# SPDX-License-Identifier: GPL-3.0-or-later
set -euo pipefail

# Run from the patches repository root, after installing the current combined test APK.
# ANDROID_SERIAL can select a device. No network requests leave the test process.
: "${ANDROID_HOME:?Set ANDROID_HOME}"
module=apps/youtube/extensions/dislike
./gradlew :apps:youtube:extensions:dislike:deviceRegressionJar --console=plain
build_tools="${ANDROID_HOME}/build-tools/${ANDROID_BUILD_TOOLS:-36.0.0}"
android_jar="${ANDROID_HOME}/platforms/${ANDROID_PLATFORM:-android-36}/android.jar"
"$build_tools/d8" --min-api 29 --lib "$android_jar" \
    --output "$module/build/device-regression/test.zip" \
    "$module/build/device-regression/ryd-device-regression.jar"
apk_path=$(adb shell pm path app.reseam.android.youtube | tr -d '\r' | sed -n 's/^package://p' | head -n 1)
test -n "$apk_path"
device_dir=$(adb shell mktemp -d /data/local/tmp/ryd-regression.XXXXXX | tr -d '\r')
# Delete only the two files and the unique directory this script created.
trap 'adb shell rm -f "$device_dir/test.zip"; adb shell rmdir "$device_dir"' EXIT
adb push "$module/build/device-regression/test.zip" "$device_dir/test.zip"
adb shell chmod 444 "$device_dir/test.zip"
test_command="CLASSPATH=$device_dir/test.zip:$apk_path app_process /system/bin app.reseam.youtube.dislike.RydDeviceRegression"
if [ "${RYD_TEST_ROOT:-0}" = 1 ]; then
    # Some Android 16 builds deny shell access to the font config needed by Canvas tests.
    adb shell su -c "$test_command"
else
    adb shell "$test_command"
fi
