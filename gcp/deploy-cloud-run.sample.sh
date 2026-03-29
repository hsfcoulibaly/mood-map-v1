#!/usr/bin/env bash
# Copy to deploy-cloud-run.sh, fill the variables, then: chmod +x deploy-cloud-run.sh && ./deploy-cloud-run.sh
# Prereqs: gcloud auth, Cloud SQL MySQL + DB user, schema.sql applied, Artifact Registry image built (e.g. Cloud Build).

set -euo pipefail

REGION="us-central1"
PROJECT_ID="your-gcp-project"
REPO="moodmap"
SERVICE="moodmap-api"
# Cloud SQL instance connection name (Console → Cloud SQL → instance → Connection name)
CLOUD_SQL_CONNECTION_NAME="${PROJECT_ID}:${REGION}:your-instance"

IMAGE="${REGION}-docker.pkg.dev/${PROJECT_ID}/${REPO}/moodmap-api:latest"

gcloud config set project "${PROJECT_ID}"

gcloud run deploy "${SERVICE}" \
  --image "${IMAGE}" \
  --region "${REGION}" \
  --platform managed \
  --allow-unauthenticated \
  --add-cloudsql-instances "${CLOUD_SQL_CONNECTION_NAME}" \
  --set-env-vars "APP_ENV=production,CLOUD_SQL_CONNECTION_NAME=${CLOUD_SQL_CONNECTION_NAME},DB_USER=moodmap,DB_NAME=moodmap,GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com" \
  --set-secrets "DB_PASSWORD=YOUR_DB_PASSWORD_SECRET:latest,JWT_SECRET=YOUR_JWT_SECRET:latest"

echo "Done. Set Android MOOD_MAP_API_BASE_URL to the HTTPS URL shown above."
