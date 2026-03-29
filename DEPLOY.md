# Deploying Mood Map (AWS & GCP)

This guide focuses on the **FastAPI backend** in `backend/`. The Android app should use your **HTTPS** API URL in `MOOD_MAP_API_BASE_URL` for release builds.

## 1. Environment variables (production)

Set these in your host’s secret manager or console (never commit real values).

| Variable | Required | Notes |
|----------|----------|--------|
| `DB_HOST` | Yes for TCP | MySQL host (local, RDS, or Cloud SQL **private IP**). **Not used** when `CLOUD_SQL_CONNECTION_NAME` is set (Cloud Run socket). |
| `CLOUD_SQL_CONNECTION_NAME` | GCP Cloud Run + Cloud SQL | `project:region:instance`. Enables Unix socket `/cloudsql/...` (attach instance on the Cloud Run service). |
| `DB_USER` | Yes | |
| `DB_PASSWORD` | Yes | From Secrets Manager / Secret Manager |
| `DB_NAME` | Yes | |
| `JWT_SECRET` | Yes | ≥32 random bytes; required when `APP_ENV=production` |
| `APP_ENV` | Yes | `production` |
| `GOOGLE_WEB_CLIENT_ID` | For Google Sign-In | Web OAuth client ID (same as Android `local.properties`) |
| `CORS_ORIGINS` | If you add a web admin | Comma-separated `https://` origins |

Optional:

| Variable | Default | Notes |
|----------|---------|--------|
| `JWT_ACCESS_TOKEN_EXPIRE_MINUTES` | `10080` | |
| `PORT` | `8080` | Set automatically on **Cloud Run** and **App Runner** |

## 2. Health checks

- **`GET /health`** — process alive (liveness).
- **`GET /health/ready`** — can open a MySQL connection (readiness). Point orchestration readiness probes here once the DB is wired.

## 3. Local Docker (sanity check)

From `backend/`:

```bash
docker compose up --build
```

API: `http://localhost:8000`  
MySQL: `localhost:3306` (schema loaded from `sql/schema.sql` on first DB volume init).

Stop: `docker compose down` (add `-v` to reset the DB volume).

## 4. Build the container image

From the **repository root** (matches Cloud Build / default Dockerfile trigger):

```bash
docker build -t moodmap-api:latest .
```

Or with context **`backend/`** (uses `backend/Dockerfile`):

```bash
docker build -t moodmap-api:latest -f backend/Dockerfile backend
```

## 5. Google Cloud Platform (Cloud Run + Cloud SQL)

### 5.1 One-command deploy (after the image exists)

1. Grant the **Cloud Run service account** the role **Cloud SQL Client** on the project (Console → IAM, or):

   ```bash
   PROJECT_NUMBER=$(gcloud projects describe PROJECT_ID --format='value(projectNumber)')
   gcloud projects add-iam-policy-binding PROJECT_ID \
     --member="serviceAccount:${PROJECT_NUMBER}-compute@developer.gserviceaccount.com" \
     --role="roles/cloudsql.client"
   ```

2. Create secrets in **Secret Manager** (e.g. `moodmap-db-password`, `moodmap-jwt-secret`) and reference them on deploy.

3. Deploy (adjust names; see `gcp/deploy-cloud-run.sample.sh` for a filled-out template):

   ```bash
   gcloud run deploy moodmap-api \
     --image REGION-docker.pkg.dev/PROJECT_ID/REPO/moodmap-api:latest \
     --region REGION \
     --platform managed \
     --allow-unauthenticated \
     --add-cloudsql-instances PROJECT_ID:REGION:INSTANCE \
     --set-env-vars "APP_ENV=production,CLOUD_SQL_CONNECTION_NAME=PROJECT_ID:REGION:INSTANCE,DB_USER=moodmap,DB_NAME=moodmap,GOOGLE_WEB_CLIENT_ID=YOUR_WEB_CLIENT_ID" \
     --set-secrets "DB_PASSWORD=moodmap-db-password:latest,JWT_SECRET=moodmap-jwt-secret:latest"
   ```

   Use the same `PROJECT_ID:REGION:INSTANCE` string for **`--add-cloudsql-instances`** and **`CLOUD_SQL_CONNECTION_NAME`**.

### 5.2 High-level flow

1. Create a **MySQL** instance in **Cloud SQL** (same region you will use for Cloud Run).
2. Create database + user; load schema with **`backend/sql/schema.sql`** only (Cloud Storage import, or paste/run in **Cloud SQL Studio**).

   **Which file to import?**

   | Situation | File |
   |-----------|------|
   | **New Cloud SQL / empty DB** (your case) | `backend/sql/schema.sql` |
   | Legacy DB with column `password` (not `password_hash`) | `backend/sql/migration_legacy_password_column.sql` |
   | Already has `password_hash`, missing `google_sub` (MySQL 8.0.12+) | `backend/sql/migration_add_google_auth.sql` |

   Importing **`migration_legacy_password_column.sql`** on a fresh instance fails: there is no `password` column to rename. Upload **`schema.sql`** to your bucket and import that (or run it in the Studio query editor).
3. **Secret Manager**: store `DB_PASSWORD`, `JWT_SECRET`, etc.
4. **Artifact Registry**: create a Docker repository.
5. **Cloud Build**: the repo has a **`Dockerfile` at the root** (build context `.`) so the default **Dockerfile** trigger works. **`backend/Dockerfile`** is for local builds from `backend/` only (e.g. `docker compose` there). Optionally point the trigger at **`cloudbuild.yaml`** to build, push, and set **`_IMAGE_NAME`** for Artifact Registry.
6. Build and push the image locally if needed (replace `REGION`, `PROJECT`, `REPO`):

   ```bash
   docker tag moodmap-api:latest REGION-docker.pkg.dev/PROJECT/REPO/moodmap-api:latest
   docker push REGION-docker.pkg.dev/PROJECT/REPO/moodmap-api:latest
   ```

7. **Cloud Run** → Create service → container image above.
   - **Authentication**: allow unauthenticated invocations *only if* you want a public API (typical for mobile backends; you still protect routes with JWT).
   - **Cloud SQL**: under **Connections** add your MySQL instance. Set env **`CLOUD_SQL_CONNECTION_NAME`** to `project:region:instance` (same string as in the console “Connection name”). The API connects via **`mysql-connector` + Unix socket** `/cloudsql/...` (no `DB_HOST` needed for this path). For **private IP** without the connector, omit `CLOUD_SQL_CONNECTION_NAME`, use a **VPC connector**, and set **`DB_HOST`** to the instance private IP. See [Connect from Cloud Run to Cloud SQL](https://cloud.google.com/sql/docs/mysql/connect-run).
   - **Secrets**: inject `DB_PASSWORD`, `JWT_SECRET` from Secret Manager; set `GOOGLE_WEB_CLIENT_ID` as plain env (or secret if you prefer).
   - **Variables**: `APP_ENV=production`, `DB_USER`, `DB_NAME`, and **`CLOUD_SQL_CONNECTION_NAME`** when using the built-in socket.

8. After deploy, Cloud Run gives an **HTTPS** URL. Put that base URL + trailing slash in Android **`MOOD_MAP_API_BASE_URL`** for release.

**Note:** Connecting Cloud Run to Cloud SQL is the fiddliest step. Alternatives: **private IP + VPC connector**, or run the API on **GCE** / **GKE** with a simpler network path. The Dockerfile itself is the same.

## 6. Amazon Web Services

### Option A — App Runner (simplest)

1. Create **RDS MySQL** (private subnets + security group).
2. Run `schema.sql` on RDS.
3. Push image to **ECR**:

   ```bash
   aws ecr get-login-password --region REGION | docker login --username AWS --password-stdin ACCOUNT.dkr.ecr.REGION.amazonaws.com
   docker tag moodmap-api:latest ACCOUNT.dkr.ecr.REGION.amazonaws.com/moodmap-api:latest
   docker push ACCOUNT.dkr.ecr.REGION.amazonaws.com/moodmap-api:latest
   ```

4. **App Runner** → New service → deploy from ECR.
5. Add environment variables and (if required) **VPC connector** so the service can reach RDS private IP.
6. App Runner provides **HTTPS**. Use that URL in the Android app.

### Option B — ECS Fargate / EKS / EC2

Use the same container image; configure target group health check on **`/health`** (or **`/health/ready`** once DB is reachable). Terminate TLS at **ALB** or **API Gateway**.

## 7. Android release

1. Set `MOOD_MAP_API_BASE_URL` to **`https://your-api-host/`** (must be `https` for release Gradle check).
2. Build signed **release** AAB/APK in Android Studio.
3. Ensure your **Maps** and **OAuth** client restrictions include the **release** signing SHA-1.

## 8. Security checklist (before public traffic)

- [ ] `APP_ENV=production` and strong `JWT_SECRET`
- [ ] MySQL not publicly exposed; strong DB password
- [ ] TLS only (handled by Cloud Run / App Runner / ALB)
- [ ] Rotate any keys that ever lived in chat or repos
- [ ] Privacy policy + Play Data safety (if publishing on Google Play)

## 9. CI (GitHub Actions)

On push/PR that touch `backend/` or the root **`Dockerfile`**, **`.github/workflows/backend-docker.yml`** runs `docker build` from the **repo root** (image is not pushed). Add `docker login` + `push: true` and your registry URL when you wire ECR or Artifact Registry.

## 10. What this repo does *not* include

- Terraform / CloudFormation or a full push-to-prod pipeline (extend the workflow above).
- Automatic migrations on startup (run SQL against RDS/Cloud SQL when you change schema).
- Rate limiting and WAF (add at API gateway or reverse proxy).

The **Dockerfile** and **`docker-compose.yml`** are the portable baseline for both AWS and GCP.
