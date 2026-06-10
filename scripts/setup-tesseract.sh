#!/usr/bin/env bash
# Download Tesseract tessdata into infra/tesseract/tessdata
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TESS_DIR="$ROOT/infra/tesseract/tessdata"
BASE_URL="https://github.com/tesseract-ocr/tessdata/raw/main"
CHECK_ONLY=false
SKIP_ENG=false

while [[ $# -gt 0 ]]; do
  case "$1" in
    --check-only) CHECK_ONLY=true ;;
    --skip-eng) SKIP_ENG=true ;;
    *) echo "Unknown option: $1" >&2; exit 1 ;;
  esac
  shift
done

mkdir -p "$TESS_DIR"

files=(chi_sim.traineddata)
if [[ "$SKIP_ENG" != true ]]; then
  files+=(eng.traineddata)
fi

min_bytes() {
  case "$1" in
    chi_sim.traineddata) echo 40000000 ;;
    eng.traineddata) echo 1000000 ;;
    *) echo 1 ;;
  esac
}

file_ok() {
  local name="$1"
  local path="$TESS_DIR/$name"
  local min
  min="$(min_bytes "$name")"
  [[ -f "$path" ]] && [[ "$(wc -c < "$path")" -ge "$min" ]]
}

echo "Tessdata directory: $TESS_DIR"
echo

if [[ "$CHECK_ONLY" == true ]]; then
  ok=true
  for name in "${files[@]}"; do
    if file_ok "$name"; then
      size_mb=$(awk "BEGIN {printf \"%.1f\", $(wc -c < "$TESS_DIR/$name")/1024/1024}")
      echo "[OK] $name (${size_mb} MB)"
    else
      echo "[MISSING] $name"
      ok=false
    fi
  done
  echo
  if [[ "$ok" == true ]]; then
    echo "Tessdata ready."
    exit 0
  fi
  echo "Run without --check-only to download."
  exit 1
fi

for name in "${files[@]}"; do
  path="$TESS_DIR/$name"
  if file_ok "$name"; then
    echo "Skip $name (already present)"
    continue
  fi
  url="$BASE_URL/$name"
  echo "Downloading $name ..."
  echo "  $url"
  curl -fsSL "$url" -o "$path"
  if ! file_ok "$name"; then
    echo "Download failed or file too small: $path" >&2
    exit 1
  fi
  size_mb=$(awk "BEGIN {printf \"%.1f\", $(wc -c < "$path")/1024/1024}")
  echo "  Done (${size_mb} MB)"
done

echo
echo "Tessdata installed. Set ingest.ocr.enabled=true and data-path=./infra/tesseract/tessdata"
