#!/usr/bin/env bash
#
# Turns a fresh copy of this template into a specific club's project:
# renames the Java package, Maven coordinates and application name.
#
#   ./scripts/init-club.sh <java-package> <artifact-id>
#   ./scripts/init-club.sh org.rivalsunited.api rivals-united-api
#
# Run it once, from the project root, before your first commit. The club's
# display name, league settings and secrets are configuration (.env), not
# code, so they are not touched here.
set -euo pipefail

if [[ $# -ne 2 ]]; then
  sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'
  exit 1
fi

NEW_PACKAGE="$1"
NEW_ARTIFACT="$2"
OLD_PACKAGE="com.clubsite.api"
OLD_ARTIFACT="club-api"

if [[ ! "$NEW_PACKAGE" =~ ^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*)+$ ]]; then
  echo "Package must be lowercase and dotted, e.g. org.myclub.api" >&2
  exit 1
fi
if [[ ! "$NEW_ARTIFACT" =~ ^[a-z][a-z0-9-]*$ ]]; then
  echo "Artifact id must be lowercase letters, digits and hyphens, e.g. my-club-api" >&2
  exit 1
fi
if [[ "$NEW_PACKAGE" == "$OLD_PACKAGE" || "$NEW_PACKAGE" == "$OLD_PACKAGE".* ]]; then
  echo "Pick a package outside $OLD_PACKAGE" >&2
  exit 1
fi
if [[ ! -d "src/main/java/${OLD_PACKAGE//.//}" ]]; then
  echo "Run this from the project root of an untouched template (src/main/java/${OLD_PACKAGE//.//} not found)." >&2
  exit 1
fi

NEW_GROUP="${NEW_PACKAGE%.*}"

# perl -pi works the same on Linux and macOS, unlike sed -i.
for root in src/main/java src/test/java; do
  new_dir="$root/${NEW_PACKAGE//.//}"
  mkdir -p "$new_dir"
  cp -R "$root/${OLD_PACKAGE//.//}/." "$new_dir/"
  rm -rf "$root/${OLD_PACKAGE//.//}"
  # Remove now-empty parent directories of the old package.
  find "$root" -type d -empty -delete
done

grep -rl "$OLD_PACKAGE" src | xargs perl -pi -e "s/\Q$OLD_PACKAGE\E/$NEW_PACKAGE/g"

perl -0pi -e "s#<groupId>com.clubsite</groupId>#<groupId>$NEW_GROUP</groupId>#; s#<artifactId>$OLD_ARTIFACT</artifactId>#<artifactId>$NEW_ARTIFACT</artifactId>#; s#<name>$OLD_ARTIFACT</name>#<name>$NEW_ARTIFACT</name>#" pom.xml
perl -pi -e "s/^spring.application.name=.*/spring.application.name=$NEW_ARTIFACT/" src/main/resources/application.properties
perl -pi -e "s/defaultValue=\"$OLD_ARTIFACT\"/defaultValue=\"$NEW_ARTIFACT\"/" src/main/resources/logback-spring.xml

echo "Renamed $OLD_PACKAGE -> $NEW_PACKAGE and $OLD_ARTIFACT -> $NEW_ARTIFACT."
echo "Next: cp .env.example .env, fill it in, then ./mvnw spring-boot:run (or docker compose up)."
