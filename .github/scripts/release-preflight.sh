#!/usr/bin/env bash
#
# Release pre-flight guard (issue #54).
#
# v1.2 was tagged from a commit whose POMs still said 1.2-SNAPSHOT, so the release shipped
# readme-generator-1.2-SNAPSHOT.zip and every nrg-action consumer got a 404 on
# releases/download/v1.2/readme-generator-1.2.zip. This script makes that mistake loud:
#
#   1. every POM carries the same version and it is not a -SNAPSHOT;
#   2. (--tag vX.Y)   the tag being released is exactly "v" + that version;
#   3. (--build)      the release zip builds as readme-generator-X.Y.zip and the jar inside
#                     reports X.Y; a .sha256 file is written next to it;
#   4. (--published)  the zip and its .sha256 are downloadable from the URLs nrg-action
#                     builds, and they agree.
#
# Usage (from the repo root, before tagging):
#   .github/scripts/release-preflight.sh --build
#   .github/scripts/release-preflight.sh --tag v1.3 --build
#   .github/scripts/release-preflight.sh --tag v1.3 --published      # after the upload
#
# Set MVN to the Maven executable when `mvn` is not on PATH.
#
set -euo pipefail

MVN="${MVN:-mvn}"
REPO_SLUG="${REPO_SLUG:-nanolaba/readme-generator}"
HELP_PLUGIN="org.apache.maven.plugins:maven-help-plugin:3.4.0"
POMS=(pom.xml nrg/pom.xml nrg-maven-plugin/pom.xml)

TAG=""
BUILD=false
PUBLISHED=false

fail() {
  # ::error:: is rendered as an annotation by GitHub Actions and is harmless noise elsewhere.
  echo "::error::release pre-flight: $*" >&2
  exit 1
}

# "<hex>  <file>" in the format `sha256sum -c` reads; shasum covers macOS.
sha256_line() {
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$1"
  else
    shasum -a 256 "$1"
  fi
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --tag)       [[ $# -ge 2 ]] || fail "--tag needs a value"; TAG="$2"; shift 2 ;;
    --build)     BUILD=true; shift ;;
    --published) PUBLISHED=true; shift ;;
    -h|--help)   sed -n '2,23p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *)           fail "unknown argument: $1" ;;
  esac
done

cd "$(dirname "$0")/../.."

# === 1. POM versions ===
VERSION=""
for pom in "${POMS[@]}"; do
  v="$("$MVN" -q -N -f "$pom" "$HELP_PLUGIN:evaluate" -Dexpression=project.version -DforceStdout | tr -d '\r\n')"
  [[ -n "$v" ]] || fail "could not read project.version from $pom"
  echo "$pom: $v"
  if [[ -z "$VERSION" ]]; then
    VERSION="$v"
  elif [[ "$v" != "$VERSION" ]]; then
    fail "$pom is at $v but pom.xml is at $VERSION — the modules are out of sync"
  fi
done

if [[ "$VERSION" == *-SNAPSHOT* ]]; then
  fail "project.version is $VERSION — pin the POMs to the release version (drop -SNAPSHOT) and commit before tagging"
fi

# === 2. tag ===
if [[ -n "$TAG" && "$TAG" != "v$VERSION" ]]; then
  fail "tag $TAG does not match project.version $VERSION (expected tag v$VERSION)"
fi

ASSET="readme-generator-$VERSION.zip"

# === 3. release asset ===
if [[ "$BUILD" == true ]]; then
  "$MVN" --batch-mode --no-transfer-progress -pl nrg -am clean package \
    -DskipTests -Dgpg.skip=true -Dmaven.javadoc.skip=true

  [[ -f "nrg/target/$ASSET" ]] || fail "nrg/target/$ASSET was not produced"
  stray="$(find nrg/target -maxdepth 1 -name '*-SNAPSHOT*' -print)"
  [[ -z "$stray" ]] || fail "SNAPSHOT-stamped artifacts in nrg/target: $stray"

  stamped="$(unzip -p "nrg/target/$ASSET" "readme-generator-$VERSION/version.txt" | tr -d '\r\n')"
  [[ "$stamped" == "$VERSION" ]] || fail "version.txt inside $ASSET says '$stamped', expected '$VERSION'"

  reported="$(java -cp nrg/target/nrg.jar com.nanolaba.nrg.NRG --version | head -n1 | tr -d '\r')"
  [[ "$reported" == *" $VERSION" ]] || fail "nrg.jar reports '$reported', expected version $VERSION"

  # Published next to the zip so that nrg-action can verify what it downloads.
  (cd nrg/target && sha256_line "$ASSET" > "$ASSET.sha256")

  echo "Built nrg/target/$ASSET ($reported)"
  echo "sha256 $(awk '{print $1}' "nrg/target/$ASSET.sha256") — upload both $ASSET and $ASSET.sha256"
fi

# === 4. published asset ===
if [[ "$PUBLISHED" == true ]]; then
  URL="https://github.com/$REPO_SLUG/releases/download/v$VERSION/$ASSET"
  tmp="$(mktemp -d)"
  trap 'rm -rf "$tmp"' EXIT
  curl -fsSL --retry 5 --retry-delay 10 --retry-all-errors -o "$tmp/$ASSET" "$URL" \
    || fail "$URL is not downloadable — upload $ASSET to the v$VERSION release (nrg-action builds exactly this URL)"
  curl -fsSL --retry 5 --retry-delay 10 --retry-all-errors -o "$tmp/$ASSET.sha256" "$URL.sha256" \
    || fail "$URL.sha256 is not downloadable — upload $ASSET.sha256 next to the zip (nrg-action verifies against it)"
  published="$(awk 'NR==1 {print $1}' "$tmp/$ASSET.sha256")"
  actual="$(cd "$tmp" && sha256_line "$ASSET" | awk '{print $1}')"
  [[ "$published" == "$actual" ]] \
    || fail "$ASSET.sha256 on the release says $published but the published zip is $actual"
  echo "Published asset is reachable and matches its checksum: $URL ($actual)"
fi

echo "Release pre-flight passed for $VERSION"
