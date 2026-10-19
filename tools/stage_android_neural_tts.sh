#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
mkdir -p "$ROOT/app/libs" "$ROOT/app/src/main/assets"
TMP="$(mktemp -d)"
trap 'rm -rf "$TMP"' EXIT
curl --retry 4 -fL "https://github.com/k2-fsa/sherpa-onnx/releases/download/v1.13.8/sherpa-onnx-1.13.8.aar" -o "$TMP/sherpa.aar"
echo "633c24321e06b1fe79feafa03ea16cbc0f8a286641e2da3559bac91bdb13bd96  $TMP/sherpa.aar" | sha256sum -c -
cp "$TMP/sherpa.aar" "$ROOT/app/libs/sherpa-onnx.aar"
curl --retry 4 -fL "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models/vits-piper-ru_RU-dmitri-medium.tar.bz2" -o "$TMP/model.tar.bz2"
tar -tjf "$TMP/model.tar.bz2" | python3 -c 'import sys; [(_ for _ in ()).throw(ValueError("unsafe path")) for n in sys.stdin if n.startswith("/") or ".." in n.strip().split("/")]'
tar -xjf "$TMP/model.tar.bz2" -C "$ROOT/app/src/main/assets"
MODEL="$ROOT/app/src/main/assets/vits-piper-ru_RU-dmitri-medium"
test -s "$MODEL/ru_RU-dmitri-medium.onnx"
test -s "$MODEL/tokens.txt"
test -d "$MODEL/espeak-ng-data"
echo "Offline Russian TTS staged"
