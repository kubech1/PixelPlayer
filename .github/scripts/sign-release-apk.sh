#!/usr/bin/env bash
set -euo pipefail

unsigned_apk="$1"
signed_apk="$2"
build_tools_version="$(ls "$ANDROID_HOME/build-tools" | sort -V | tail -n 1)"
build_tools="$ANDROID_HOME/build-tools/$build_tools_version"

badging="$("$build_tools/aapt2" dump badging "$unsigned_apk")"
native_code="$(sed -n 's/^native-code: //p' <<< "$badging")"
if [[ "$native_code" != "'arm64-v8a'" ]]; then
  echo "Expected only arm64-v8a in $unsigned_apk, found: ${native_code:-no native libraries}" >&2
  exit 1
fi
if grep -q '^application-debuggable' <<< "$badging"; then
  echo "The release APK must not be debuggable" >&2
  exit 1
fi

: "${RELEASE_KEYSTORE_BASE64:?Set PIXELPLAY_RELEASE_KEYSTORE_BASE64 in GitHub Actions secrets}"
: "${RELEASE_STORE_PASSWORD:?Set PIXELPLAY_RELEASE_STORE_PASSWORD in GitHub Actions secrets}"
: "${RELEASE_KEY_ALIAS:?Set PIXELPLAY_RELEASE_KEY_ALIAS in GitHub Actions secrets}"
: "${RELEASE_KEY_PASSWORD:?Set PIXELPLAY_RELEASE_KEY_PASSWORD in GitHub Actions secrets}"

keystore="$(mktemp)"
trap 'rm -f -- "$keystore"' EXIT
printf '%s' "$RELEASE_KEYSTORE_BASE64" | base64 --decode > "$keystore"

"$build_tools/apksigner" sign \
  --ks "$keystore" --ks-key-alias "$RELEASE_KEY_ALIAS" \
  --ks-pass env:RELEASE_STORE_PASSWORD --key-pass env:RELEASE_KEY_PASSWORD \
  --out "$signed_apk" "$unsigned_apk"
"$build_tools/apksigner" verify --verbose "$signed_apk"
