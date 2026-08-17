#!/usr/bin/env bash

set -euo pipefail

repo_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
report_dir="$repo_root/build/reports/annual-update"

usage() {
  cat <<'EOF'
Usage:
  tools/annual-update.sh run --year YEAR --data-revision COMMIT [options]
  tools/annual-update.sh prepare-pr --year YEAR [--config FILE]
  tools/annual-update.sh publish-pr --year YEAR --data-revision COMMIT --base BRANCH

Commands:
  run         Validate inputs and data, generate the annual update twice to
              prove determinism, and run all annual verification gates.
  prepare-pr  Write the allowed changes patch and public verification summary.
  publish-pr  Apply a prepared handoff, push annual-update/YEAR, and create or
              update its draft pull request. Requires gh authentication.

Run options:
  --config FILE              Annual configuration (default: annual/YEAR.json)
  --version-name VERSION     Assert the configured versionName
  --staged-data-dir DIR      Validate this checkout and install it as iBurn-Data
  --require-secret           Require the restricted staff unlock code

Environment variables IBURN_UNLOCK_CODE, IBURN_API_URL, and MAPBOX_API_KEY are
passed through to Gradle. For local use they may instead be Gradle properties.
EOF
}

die() {
  printf 'annual-update: %s\n' "$*" >&2
  exit 1
}

require_value() {
  test "$#" -ge 2 || die "$1 requires a value"
}

absolute_path() {
  case "$1" in
    /*) printf '%s\n' "$1" ;;
    *) printf '%s/%s\n' "$repo_root" "$1" ;;
  esac
}

validate_revision() {
  local data_dir=$1
  local revision=$2
  local ref
  local reachable=false

  [[ "$revision" =~ ^[0-9a-f]{40}$ ]] || die 'data revision must be a full lowercase 40-character commit id'
  git -C "$data_dir" rev-parse --git-dir >/dev/null 2>&1 || die "data checkout is not a Git repository: $data_dir"
  test "$(git -C "$data_dir" rev-parse HEAD)" = "$revision" || die "data checkout HEAD does not match $revision"

  while IFS= read -r ref; do
    if git -C "$data_dir" merge-base --is-ancestor "$revision" "$ref"; then
      reachable=true
      break
    fi
  done < <(git -C "$data_dir" for-each-ref --format='%(refname)' refs/remotes)
  test "$reachable" = true || die 'data revision is not reachable from a remote-tracking ref'
}

manifest_sha256() {
  local manifest=$1
  if command -v sha256sum >/dev/null 2>&1; then
    sha256sum "$manifest" | awk '{print $1}'
  else
    shasum -a 256 "$manifest" | awk '{print $1}'
  fi
}

run_update() {
  local year=''
  local revision=''
  local config=''
  local version_name=''
  local data_dir="$repo_root/iBurn-Data"
  local staged_data_dir=''
  local require_secret=false
  local first_manifest
  local second_manifest

  while test "$#" -gt 0; do
    case "$1" in
      --year) require_value "$@"; year=$2; shift 2 ;;
      --data-revision) require_value "$@"; revision=$2; shift 2 ;;
      --config) require_value "$@"; config=$2; shift 2 ;;
      --version-name) require_value "$@"; version_name=$2; shift 2 ;;
      --staged-data-dir) require_value "$@"; staged_data_dir=$2; shift 2 ;;
      --require-secret) require_secret=true; shift ;;
      -h|--help) usage; exit 0 ;;
      *) die "unknown run option: $1" ;;
    esac
  done

  test -n "$year" || die 'run requires --year'
  test -n "$revision" || die 'run requires --data-revision'
  test -n "$config" || config="annual/$year.json"
  config=$(absolute_path "$config")
  test -f "$config" || die "annual configuration does not exist: $config"
  test "$(jq -r .year "$config")" = "$year" || die "configuration year does not match $year"
  if test -n "$version_name"; then
    test "$(jq -r .versionName "$config")" = "$version_name" || die "configuration versionName does not match $version_name"
  fi

  if test -n "$staged_data_dir"; then
    staged_data_dir=$(absolute_path "$staged_data_dir")
    validate_revision "$staged_data_dir" "$revision"
    if test -d "$data_dir"; then
      rmdir "$data_dir" || die "refusing to replace non-empty data directory: $data_dir"
    fi
    mv "$staged_data_dir" "$data_dir"
  else
    validate_revision "$data_dir" "$revision"
  fi

  cd "$repo_root"
  if test "$require_secret" = true; then
    ./gradlew :iBurn:validateAnnualSecret -PannualConfig="$config"
  fi
  ./gradlew :iBurn:annualUpdate -PannualConfig="$config" -PdataRevision="$revision"
  first_manifest=$(manifest_sha256 "$report_dir/annual-manifest.json")
  ./gradlew :iBurn:annualUpdate --rerun-tasks -PannualConfig="$config" -PdataRevision="$revision"
  second_manifest=$(manifest_sha256 "$report_dir/annual-manifest.json")
  test "$first_manifest" = "$second_manifest" || die 'annual manifest changed between identical update runs'
  ./gradlew :iBurn:verifyAnnualUpdate -PannualConfig="$config" -PdataRevision="$revision"
}

prepare_pr() {
  local year=''
  local config=''
  local config_pathspec
  local handoff="$report_dir/pr-handoff"

  while test "$#" -gt 0; do
    case "$1" in
      --year) require_value "$@"; year=$2; shift 2 ;;
      --config) require_value "$@"; config=$2; shift 2 ;;
      -h|--help) usage; exit 0 ;;
      *) die "unknown prepare-pr option: $1" ;;
    esac
  done

  test -n "$year" || die 'prepare-pr requires --year'
  test -n "$config" || config="annual/$year.json"
  config=$(absolute_path "$config")
  case "$config" in
    "$repo_root"/*) config_pathspec=${config#"$repo_root"/} ;;
    *) die 'pull request configuration must be inside the repository' ;;
  esac
  test -f "$report_dir/verification-report.json" || die 'verification report is missing; run the update first'
  mkdir -p "$handoff"
  cd "$repo_root"
  git diff --binary -- "$config_pathspec" iBurn-Data iBurn/src/main/assets/js iBurn/src/main/assets/map > "$handoff/changes.patch"
  jq '{
    year,
    versionName,
    sourceRecords,
    databaseRows: (.database.rowCounts | to_entries | map(.value) | add),
    status
  }' "$report_dir/verification-report.json" > "$handoff/summary.json"
}

publish_pr() {
  local year=''
  local revision=''
  local base=''
  local handoff="$report_dir/pr-handoff"
  local summary="$handoff/summary.json"
  local branch
  local body="$report_dir/pull-request.md"

  while test "$#" -gt 0; do
    case "$1" in
      --year) require_value "$@"; year=$2; shift 2 ;;
      --data-revision) require_value "$@"; revision=$2; shift 2 ;;
      --base) require_value "$@"; base=$2; shift 2 ;;
      -h|--help) usage; exit 0 ;;
      *) die "unknown publish-pr option: $1" ;;
    esac
  done

  test -n "$year" || die 'publish-pr requires --year'
  test -n "$revision" || die 'publish-pr requires --data-revision'
  test -n "$base" || die 'publish-pr requires --base'
  test -f "$handoff/changes.patch" || die "handoff patch is missing: $handoff/changes.patch"
  test -f "$summary" || die "handoff summary is missing: $summary"
  test -s "$handoff/changes.patch" || return 0
  branch="annual-update/$year"

  cd "$repo_root"
  git checkout -B "$branch"
  git apply --index "$handoff/changes.patch"
  git diff --cached --quiet && return 0
  git -c user.name=github-actions -c user.email=github-actions@github.com \
    commit -m "Automate $year annual update"
  git push --force-with-lease origin "$branch"

  printf '%s\n' \
    "## Annual update $year" \
    '' \
    "- Data revision: \`$revision\`" \
    "- Version: \`$(jq -r .versionName "$summary")\`" \
    "- Art: $(jq -r .sourceRecords.art "$summary")" \
    "- Camps: $(jq -r .sourceRecords.camps "$summary")" \
    "- Events: $(jq -r .sourceRecords.events "$summary")" \
    "- Database rows: $(jq -r .databaseRows "$summary")" \
    "- Verification: $(jq -r .status "$summary")" \
    '' \
    'Restricted JSON, media, database, and APK outputs are workflow artifacts and are not included in this PR.' \
    > "$body"

  if gh pr view "$branch" >/dev/null 2>&1; then
    gh pr edit "$branch" --title "Annual update $year" --body-file "$body"
  else
    gh pr create --draft --base "$base" --head "$branch" --title "Annual update $year" --body-file "$body"
  fi
}

command=${1:-}
test -n "$command" || { usage >&2; exit 2; }
shift
case "$command" in
  run) run_update "$@" ;;
  prepare-pr) prepare_pr "$@" ;;
  publish-pr) publish_pr "$@" ;;
  -h|--help|help) usage ;;
  *) die "unknown command: $command" ;;
esac
