#!/usr/bin/env bash
set -euo pipefail

shopt -s nocasematch

function usage() {
  echo "usage: ./release.sh [release version] [next version]"
  echo "       release version : optional and should be of the form major.minor.patch"
  echo "                         e.g. 1.3.0-SNAPSHOT OR can be keyword MAJOR or MINOR"
  echo "                         in which case this script will increment that corresponding component"
  echo "                         of the current version"
  echo "                         (defaults to current POM version without the SNAPSHOT)"
  echo "       next version    : optional and should be of the form major.minor.patch-SNAPSHOT"
  echo "                         e.g. 1.3.0-SNAPSHOT OR can be keyword MAJOR or MINOR"
  echo "                         in which case this script will increment that corresponding component"
  echo "                         of the release version"
  echo "                         (defaults to current release version with patch increment)"
}

VERSION=${1:-}
NEXT_VERSION=${2:-}

POM_VERSION=$(mvn -q -Dexec.executable=echo -Dexec.args='${project.version}' --non-recursive exec:exec)

if [[ "$VERSION" =~ ^[0-9]+.[0-9]+.[0-9]+$ ]]; then
  IFS="." read -r MAJOR MINOR PATCH <<< "${VERSION}"
elif [[ "$NEXT_VERSION" =~ ^major$ ]]; then
  NEXT_MAJOR=$((MAJOR + 1))
  NEXT_VERSION="${NEXT_MAJOR}.0.0-SNAPSHOT"
elif [[ "$NEXT_VERSION" =~ ^minor$ ]]; then
  NEXT_MINOR=$((MINOR + 1))
  NEXT_VERSION="${MAJOR}.${NEXT_MINOR}.0-SNAPSHOT"
elif [ -z "$VERSION" ]; then
  VERSION=${POM_VERSION%-SNAPSHOT}
  IFS="." read -r MAJOR MINOR PATCH <<< "${VERSION}"
else
  usage
  exit 1
fi

if [[ "$NEXT_VERSION" =~ ^[0-9]+.[0-9]+.[0-9]+-SNAPSHOT$ ]]; then
  NEXT_VERSION=${NEXT_VERSION}
elif [[ "$NEXT_VERSION" =~ ^major$ ]]; then
  NEXT_MAJOR=$((MAJOR + 1))
  NEXT_VERSION="${NEXT_MAJOR}.0.0-SNAPSHOT"
elif [[ "$NEXT_VERSION" =~ ^minor$ ]]; then
  NEXT_MINOR=$((MINOR + 1))
  NEXT_VERSION="${MAJOR}.${NEXT_MINOR}.0-SNAPSHOT"
elif [[ "$NEXT_VERSION" =~ ^patch$ ]]; then
  NEXT_PATCH=$((PATCH + 1))
  NEXT_VERSION="${MAJOR}.${MINOR}.${NEXT_PATCH}-SNAPSHOT"
elif [ -z "$NEXT_VERSION" ]; then
  NEXT_PATCH=$((PATCH + 1))
  NEXT_VERSION="${MAJOR}.${MINOR}.${NEXT_PATCH}-SNAPSHOT"
else
  usage
  exit 1
fi

export TAG="v${VERSION}"
echo "checking that ${TAG} does NOT already exist"
git fetch --tags
if git rev-parse "refs/tags/${TAG}" >/dev/null 2>&1; then
    echo "tag ${TAG} already exists"
    exit 1
fi

echo "preparing release $VERSION ($NEXT_VERSION)"

echo "checking no uncommitted changes..."
if ! git diff --quiet || ! git diff --cached --quiet; then
  echo "uncommitted or staged changes detected, commit or stash first"
  exit 1
fi

echo "re-generating CHANGELOG..."
PREVIOUS_TAG=$(grep -m1 '^## v[0-9]\+\.[0-9]\+\.[0-9]\+' CHANGELOG.md | sed -E 's/^## (v[0-9]+\.[0-9]+\.[0-9]+).*/\1/')
CURRENT_DATE=$(date +%Y-%m-%d)
ARGS="-Dchangelog.from=${PREVIOUS_TAG} -Dchangelog.tag=${TAG} -Dchangelog.date=${CURRENT_DATE}"
mvn -pl . git-changelog-maven-plugin:git-changelog "${ARGS}" > target/release.log 2>&1 || {
    cat target/release.log
    echo "failed to re-generate CHANGELOG"
    exit 1
}
cat target/CHANGELOG.new.md > target/CHANGELOG.md
cat ./CHANGELOG.md | grep -v "# Changelog" >> target/CHANGELOG.md
cat -s  target/CHANGELOG.md > ./CHANGELOG.md

echo "updating README..."
find . -type f -name "README.md" -exec \
    sed -i -E \
    -e "s|<version>.+[0-9]</version>|<version>${VERSION}</version>|" \
    -e "s|<version>.+-SNAPSHOT</version>|<version>${NEXT_VERSION}</version>|" \
    {} \;

echo "updating pom versions to ${VERSION}..."
mvn -B versions:set -DnewVersion="${VERSION}" > target/release.log 2>&1 || {
    cat target/release.log
    mvn -B versions:rollback
    echo "failed update pom versions to ${VERSION}"
    exit 1
}
mvn -B versions:commit

echo "running tests..."
mvn -B verify > target/release-tests.log 2>&1 || {
    cat target/release-tests.log
    echo "tests failed"
    exit 1
}

echo "checking no SNAPSHOT dependencies..."
if mvn -q dependency:list | grep SNAPSHOT; then
  echo "snapshot dependencies detected, cannot release"
  exit 1
fi

git commit -am "build: release ${VERSION}..."
RELEASE_SHA=$(git rev-parse HEAD)

echo "updating pom versions to ${NEXT_VERSION}..."
mvn -B versions:set -DnewVersion="${NEXT_VERSION}"> target/release.log 2>&1 || {
    cat target/release.log
    mvn -B versions:rollback
    echo "failed update pom versions to ${NEXT_VERSION}"
    exit 1
}
mvn -B versions:commit

git commit -am "build: next dev version ${NEXT_VERSION}"

echo
echo "Release $VERSION prepared successfully."
echo "Next steps: "
echo "  1. Push commits and raise a PR."
echo "  2. Once that PR is approved then create a GitHub release for a new tag ${TAG} with target ${RELEASE_SHA}."
echo