#!/usr/bin/env bash
# Fails if Flyway SQL migrations contain duplicate version numbers.
set -euo pipefail

MIGRATION_DIR="backend/src/main/resources/db/migration"

if [[ ! -d "$MIGRATION_DIR" ]]; then
  echo "::error::flyway-duplicate-version-guard: migration directory not found: $MIGRATION_DIR" >&2
  exit 1
fi

shopt -s nullglob
migrations=("$MIGRATION_DIR"/V*.sql)

if ((${#migrations[@]} == 0)); then
  echo "::warning::flyway-duplicate-version-guard: no versioned SQL migrations found in $MIGRATION_DIR"
  exit 0
fi

entries=()

for migration_path in "${migrations[@]}"; do
  filename="$(basename "$migration_path")"

  if [[ "$filename" =~ ^V([0-9]+(\.[0-9]+)?)__.*\.sql$ ]]; then
    version="${BASH_REMATCH[1]}"
  else
    continue
  fi

  entries+=("$version $filename")
done

duplicate_lines="$(
  printf '%s\n' "${entries[@]}" | awk '
    {
      version = $1
      file = $2
      if (version in seen) {
        print "version " version " -> " seen[version] " and " file
      } else {
        seen[version] = file
      }
    }
  '
)"

if [[ -n "$duplicate_lines" ]]; then
  echo "::error::flyway-duplicate-version-guard: duplicate migration versions detected:" >&2
  while IFS= read -r line; do
    [[ -z "$line" ]] && continue
    echo "  - $line" >&2
  done <<<"$duplicate_lines"
  exit 1
fi

echo "flyway-duplicate-version-guard: checked ${#migrations[@]} migration files — OK"
