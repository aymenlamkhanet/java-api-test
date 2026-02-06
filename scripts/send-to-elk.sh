#!/bin/bash
# ==========================================
# Script pour envoyer les logs Jenkins à ELK
# ==========================================
# Usage: ./send-to-elk.sh <stage> <status> <message> [metrics_json]

ELASTICSEARCH_URL="${ELASTICSEARCH_URL:-http://elasticsearch:9200}"
ELK_INDEX="${ELK_INDEX:-jenkins-pipeline-logs}"

STAGE="$1"
STATUS="$2"
MESSAGE="$3"
METRICS="${4:-{}}"

TIMESTAMP=$(date -u +"%Y-%m-%dT%H:%M:%S.000Z")
DATE_INDEX=$(date +"%Y.%m.%d")

# Déterminer le niveau de log
LEVEL="INFO"
if [ "$STATUS" = "FAILED" ] || [ "$STATUS" = "ERROR" ]; then
    LEVEL="ERROR"
elif [ "$STATUS" = "WARNING" ]; then
    LEVEL="WARN"
fi

# Construire le payload JSON
JSON_PAYLOAD=$(cat <<EOF
{
    "@timestamp": "${TIMESTAMP}",
    "pipeline": "${JOB_NAME:-unknown}",
    "build_number": "${BUILD_NUMBER:-0}",
    "build_url": "${BUILD_URL:-}",
    "git_commit": "${GIT_COMMIT:-${SHORT_SHA:-unknown}}",
    "git_branch": "${GIT_BRANCH:-unknown}",
    "stage": "${STAGE}",
    "status": "${STATUS}",
    "level": "${LEVEL}",
    "message": "${MESSAGE}",
    "node": "${NODE_NAME:-master}",
    "executor": "${EXECUTOR_NUMBER:-0}",
    "metrics": ${METRICS}
}
EOF
)

# Envoyer à Elasticsearch
curl -s -X POST "${ELASTICSEARCH_URL}/${ELK_INDEX}-${DATE_INDEX}/_doc" \
    -H "Content-Type: application/json" \
    -d "${JSON_PAYLOAD}" > /dev/null 2>&1

if [ $? -eq 0 ]; then
    echo "✅ Log envoyé à ELK: [${STAGE}] ${STATUS}"
else
    echo "⚠️ Erreur envoi ELK (non bloquant)"
fi
