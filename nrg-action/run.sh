#!/usr/bin/env bash
#
# nrg-action runtime — composite GitHub Action shim around the NRG CLI.
#
# Inputs are passed via INPUT_* env vars (set by action.yml). This script:
#   1. Validates inputs
#   2. Resolves the NRG version (`latest` -> GitHub Releases API tag_name)
#   3. Downloads & extracts the release zip into $RUNNER_TEMP (idempotent)
#   4. Snapshots *.md sibling hashes
#   5. Invokes NRG once per --file
#   6. Diffs hashes, writes `version` and `changed-files` outputs
#
set -euo pipefail

# === inputs (defaults match action.yml) ===
INPUT_FILE="${INPUT_FILE:-}"
INPUT_FILES="${INPUT_FILES:-}"
INPUT_CHARSET="${INPUT_CHARSET:-UTF-8}"
INPUT_MODE="${INPUT_MODE:-generate}"
INPUT_NRG_VERSION="${INPUT_NRG_VERSION:-latest}"
INPUT_LOG_LEVEL="${INPUT_LOG_LEVEL:-info}"

die() { echo "::error::$*" >&2; exit 1; }

# Hash all *.md files that sit next to any *.src.md in FILES[]'s parent dirs.
# Output: lines of "<path><TAB><md5>". Missing files are absent (treated as new).
# Uses md5sum (Linux) with macOS `md5 -q` fallback.
snapshot_md_hashes() {
  local f dir
  for f in "${FILES[@]}"; do
    dir="$(dirname "$f")"
    [[ -d "$dir" ]] || continue
    find "$dir" -maxdepth 1 -type f -name '*.md' ! -name '*.src.md' -print0 \
      | while IFS= read -r -d '' p; do
          if command -v md5sum >/dev/null 2>&1; then
            printf '%s\t%s\n' "$p" "$(md5sum "$p" | awk '{print $1}')"
          else
            printf '%s\t%s\n' "$p" "$(md5 -q "$p")"
          fi
        done
  done | sort
}

# === 1. validate inputs ===
case "$INPUT_MODE" in
  generate|check|validate) ;;
  *) die "Invalid mode: '$INPUT_MODE' (expected: generate | check | validate)" ;;
esac

if [[ -n "$INPUT_FILE" && -n "$INPUT_FILES" ]]; then
  die "Inputs 'file' and 'files' are mutually exclusive — set only one."
fi
if [[ -z "$INPUT_FILE" && -z "$INPUT_FILES" ]]; then
  die "One of 'file' or 'files' must be provided."
fi

FILES=()
if [[ -n "$INPUT_FILE" ]]; then
  FILES+=("$INPUT_FILE")
else
  while IFS= read -r line; do
    [[ -n "$line" ]] && FILES+=("$line")
  done <<< "$INPUT_FILES"
fi
[[ ${#FILES[@]} -gt 0 ]] || die "No source files resolved from 'file' / 'files'."

# === 2. resolve version ===
if [[ "$INPUT_NRG_VERSION" == "latest" ]]; then
  AUTH_HEADER=()
  if [[ -n "${GH_TOKEN:-}" ]]; then
    AUTH_HEADER=(-H "Authorization: Bearer $GH_TOKEN")
  fi
  API="https://api.github.com/repos/nanolaba/readme-generator/releases/latest"
  if ! RESPONSE="$(curl -fsSL "${AUTH_HEADER[@]+"${AUTH_HEADER[@]}"}" "$API" 2>&1)"; then
    die "Failed to query $API: $RESPONSE"
  fi
  VERSION="$(printf '%s' "$RESPONSE" | jq -r '.tag_name')"
  [[ -n "$VERSION" && "$VERSION" != "null" ]] || die "Could not parse tag_name from API response"
  echo "Resolved nrg-version=latest to $VERSION"
else
  VERSION="$INPUT_NRG_VERSION"
fi

# Normalize: tag always has 'v' prefix; STRIPPED never does.
TAG="$VERSION"
[[ "$TAG" == v* ]] || TAG="v$TAG"
STRIPPED="${TAG#v}"

# === 3. download & extract (idempotent within a job) ===
NRG_HOME="${RUNNER_TEMP:-/tmp}/nrg-$TAG"
mkdir -p "$NRG_HOME"

if [[ ! -f "$NRG_HOME/nrg.jar" ]]; then
  ZIP_URL="https://github.com/nanolaba/readme-generator/releases/download/$TAG/readme-generator-$STRIPPED.zip"
  echo "Downloading $ZIP_URL"
  curl -fsSL -o "$NRG_HOME/nrg.zip" "$ZIP_URL" || die "Failed to download $ZIP_URL"
  unzip -qo "$NRG_HOME/nrg.zip" -d "$NRG_HOME"
  rm -f "$NRG_HOME/nrg.zip"
  FOUND_JAR="$(find "$NRG_HOME" -name 'nrg.jar' -print 2>/dev/null | head -n1)"
  [[ -n "$FOUND_JAR" ]] || die "nrg.jar not found inside $ZIP_URL"
  if [[ "$FOUND_JAR" != "$NRG_HOME/nrg.jar" ]]; then
    cp -f "$FOUND_JAR" "$NRG_HOME/nrg.jar"
  fi
fi
JAR="$NRG_HOME/nrg.jar"

# === 4. pre-run snapshot ===
PRE_HASHES_FILE="${RUNNER_TEMP:-/tmp}/nrg-pre-hashes.txt"
snapshot_md_hashes > "$PRE_HASHES_FILE"

# === 5. run NRG ===
# Invoke main class explicitly via -cp to remain compatible with older release
# jars whose manifest may not advertise the correct Main-Class.
ARGS=()
case "$INPUT_MODE" in
  check)    ARGS+=(--check) ;;
  validate) ARGS+=(--validate) ;;
esac

EXIT_CODE=0
for f in "${FILES[@]}"; do
  java -cp "$JAR" com.nanolaba.nrg.NRG \
    -f "$f" \
    --charset "$INPUT_CHARSET" \
    --log-level "$INPUT_LOG_LEVEL" \
    "${ARGS[@]+"${ARGS[@]}"}" \
    || EXIT_CODE=$?
done

# === 6. post-run: outputs ===
POST_HASHES_FILE="${RUNNER_TEMP:-/tmp}/nrg-post-hashes.txt"
snapshot_md_hashes > "$POST_HASHES_FILE"

# `diff` exits 1 when files differ; that's not an error here, so swallow it.
# Hash format is "<path>\t<md5>"; sed strips the diff prefix and the tab+hash
# suffix to preserve paths that may contain whitespace.
CHANGED="$( { diff "$PRE_HASHES_FILE" "$POST_HASHES_FILE" || true; } \
  | sed -n 's/^[<>] \(.*\)\t[a-f0-9]*$/\1/p' | sort -u )"

if [[ -n "${GITHUB_OUTPUT:-}" ]]; then
  DELIM="NRGEOF_$(openssl rand -hex 8 2>/dev/null || date +%s%N)"
  {
    echo "version=$VERSION"
    printf 'changed-files<<%s\n%s\n%s\n' "$DELIM" "$CHANGED" "$DELIM"
  } >> "$GITHUB_OUTPUT"
fi

exit "$EXIT_CODE"
