#!/usr/bin/env bash
set -euo pipefail
apk=${1:-app/build/outputs/apk/release/app-release.apk}
sdk=${ANDROID_HOME:-${ANDROID_SDK_ROOT:-}}
if [[ -z "$sdk" ]]; then
  printf '%s\n' 'Set ANDROID_HOME or ANDROID_SDK_ROOT to your Android SDK.' >&2
  exit 1
fi
"$sdk/build-tools/36.0.0/apksigner" verify --verbose --print-certs "$apk"
"$sdk/cmdline-tools/latest/bin/apkanalyzer" manifest print "$apk"
"$sdk/cmdline-tools/latest/bin/apkanalyzer" dex packages --defined-only "$apk"
sha256sum "$apk"
