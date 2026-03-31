#!/usr/bin/env bash
# Reports DORA deployment/incident events to backend admin API.
# Usage examples:
#   scripts/report-dora-event.sh deployment --status SUCCESS --service backend --environment production --change-id "$GITHUB_SHA"
#   scripts/report-dora-event.sh incident --status OPEN --service backend --environment production --summary "API elevated error rate"
set -euo pipefail

print_usage() {
  cat <<'EOF'
Usage:
  report-dora-event.sh <deployment|incident> [options]

Common options:
  --service <name>             Service name (default: backend)
  --environment <env>          Environment (default: production)
  --summary <text>             Optional short summary
  --api-base-url <url>         Optional API base URL override
  --token <jwt>                Optional bearer token override

Deployment options:
  --status <SUCCESS|FAILED>    Required
  --change-id <id>             Optional (e.g. commit SHA or release ID)
  --change-started-at <iso>    Optional ISO-8601
  --deployed-at <iso>          Optional ISO-8601 (default now)

Incident options:
  --status <OPEN|RESOLVED>     Required
  --incident-started-at <iso>  Optional ISO-8601 (default now)
  --restored-at <iso>          Optional ISO-8601; required for RESOLVED
  --caused-by-change <bool>    Optional true/false (default true)

Environment variables:
  DORA_API_BASE_URL            Preferred API base URL (fallback: API_BASE_URL, default http://localhost:8080)
  DORA_ADMIN_BEARER_TOKEN      Preferred admin bearer token (fallback: ADMIN_JWT_TOKEN)
EOF
}

if [[ $# -lt 1 ]]; then
  print_usage
  exit 1
fi

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  print_usage
  exit 0
fi

mode="$1"
shift

service_name="backend"
environment_name="production"
summary=""
status=""
change_id=""
change_started_at=""
deployed_at=""
incident_started_at=""
restored_at=""
caused_by_change="true"
api_base_url="${DORA_API_BASE_URL:-${API_BASE_URL:-http://localhost:8080}}"
bearer_token="${DORA_ADMIN_BEARER_TOKEN:-${ADMIN_JWT_TOKEN:-}}"

if ! command -v python3 >/dev/null 2>&1; then
  echo "python3 is required to build safe JSON payloads." >&2
  exit 1
fi

while [[ $# -gt 0 ]]; do
  case "$1" in
    --service) service_name="${2:-}"; shift 2 ;;
    --environment) environment_name="${2:-}"; shift 2 ;;
    --summary) summary="${2:-}"; shift 2 ;;
    --status) status="${2:-}"; shift 2 ;;
    --change-id) change_id="${2:-}"; shift 2 ;;
    --change-started-at) change_started_at="${2:-}"; shift 2 ;;
    --deployed-at) deployed_at="${2:-}"; shift 2 ;;
    --incident-started-at) incident_started_at="${2:-}"; shift 2 ;;
    --restored-at) restored_at="${2:-}"; shift 2 ;;
    --caused-by-change) caused_by_change="${2:-}"; shift 2 ;;
    --api-base-url) api_base_url="${2:-}"; shift 2 ;;
    --token) bearer_token="${2:-}"; shift 2 ;;
    -h|--help) print_usage; exit 0 ;;
    *)
      echo "Unknown option: $1" >&2
      print_usage
      exit 1
      ;;
  esac
done

if [[ -z "${bearer_token}" ]]; then
  echo "Missing admin bearer token. Set DORA_ADMIN_BEARER_TOKEN or ADMIN_JWT_TOKEN." >&2
  exit 1
fi

if [[ "${caused_by_change}" != "true" && "${caused_by_change}" != "false" ]]; then
  echo "--caused-by-change must be true or false." >&2
  exit 1
fi

if [[ "${mode}" == "deployment" ]]; then
  if [[ "${status}" != "SUCCESS" && "${status}" != "FAILED" ]]; then
    echo "Deployment status must be SUCCESS or FAILED." >&2
    exit 1
  fi
  if [[ -z "${deployed_at}" ]]; then
    deployed_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  fi
  if [[ -z "${change_started_at}" ]]; then
    change_started_at="${deployed_at}"
  fi

  payload=$(SERVICE_NAME="${service_name}" ENVIRONMENT_NAME="${environment_name}" STATUS_VALUE="${status}" CHANGE_ID_VALUE="${change_id}" CHANGE_STARTED_AT="${change_started_at}" DEPLOYED_AT="${deployed_at}" SUMMARY_VALUE="${summary}" python3 - <<'PY'
import json
import os

change_id = os.environ.get("CHANGE_ID_VALUE", "").strip() or None
summary = os.environ.get("SUMMARY_VALUE", "").strip() or None
payload = {
    "serviceName": os.environ["SERVICE_NAME"],
    "environment": os.environ["ENVIRONMENT_NAME"],
    "status": os.environ["STATUS_VALUE"],
    "changeId": change_id,
    "changeStartedAt": os.environ["CHANGE_STARTED_AT"],
    "deployedAt": os.environ["DEPLOYED_AT"],
    "summary": summary,
}
print(json.dumps(payload))
PY
)
  endpoint="${api_base_url%/}/api/v1/admin/metrics/dora/deployments"
elif [[ "${mode}" == "incident" ]]; then
  if [[ "${status}" != "OPEN" && "${status}" != "RESOLVED" ]]; then
    echo "Incident status must be OPEN or RESOLVED." >&2
    exit 1
  fi
  if [[ -z "${incident_started_at}" ]]; then
    incident_started_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  fi
  if [[ "${status}" == "RESOLVED" && -z "${restored_at}" ]]; then
    restored_at="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"
  fi

  payload=$(SERVICE_NAME="${service_name}" ENVIRONMENT_NAME="${environment_name}" STATUS_VALUE="${status}" INCIDENT_STARTED_AT="${incident_started_at}" RESTORED_AT_VALUE="${restored_at}" CAUSED_BY_CHANGE_VALUE="${caused_by_change}" SUMMARY_VALUE="${summary}" python3 - <<'PY'
import json
import os

restored_at = os.environ.get("RESTORED_AT_VALUE", "").strip() or None
summary = os.environ.get("SUMMARY_VALUE", "").strip() or None
payload = {
    "serviceName": os.environ["SERVICE_NAME"],
    "environment": os.environ["ENVIRONMENT_NAME"],
    "status": os.environ["STATUS_VALUE"],
    "incidentStartedAt": os.environ["INCIDENT_STARTED_AT"],
    "restoredAt": restored_at,
    "causedByChange": os.environ["CAUSED_BY_CHANGE_VALUE"] == "true",
    "summary": summary,
}
print(json.dumps(payload))
PY
)
  endpoint="${api_base_url%/}/api/v1/admin/metrics/dora/incidents"
else
  echo "First argument must be 'deployment' or 'incident'." >&2
  print_usage
  exit 1
fi

echo "Reporting DORA ${mode} event to ${endpoint}"
curl --silent --show-error --fail-with-body \
  -X POST "${endpoint}" \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer ${bearer_token}" \
  -d "${payload}" >/dev/null
echo "DORA ${mode} event reported successfully."
