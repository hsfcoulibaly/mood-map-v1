# Mood Map (Hacklanta)

Android client plus FastAPI backend for a campus **mood map**: anonymous pins on a map, AI companion chat, journal, recovery stories, and counselor-style insights. The UX is aligned with the original web hackathon project [hacklanta-the-Cognitive-Coders](https://github.com/HassanCoulibaly/hacklanta-the-Cognitive-Coders) (React + Leaflet + Firebase + Node APIs). This repo implements **native Android (Kotlin / Jetpack Compose)** and a **Python** API for accounts.

## Repository layout

| Path | Description |
|------|-------------|
| `mobile/` | Android app (`applicationId`: `hacklanta.moodmap`) |
| `backend/` | FastAPI app: JWT auth, email/password + Google Sign-In, MySQL |
| `backend/sql/` | MySQL schema and migrations |
| `backend/Dockerfile` | Container image for AWS / GCP / any Docker host |
| [`DEPLOY.md`](DEPLOY.md) | **AWS & GCP deployment** (Cloud Run, App Runner, Docker Compose) |
| `_web-ref/` | Optional local clone of the web repo for reference only (listed in root `.gitignore` so it is not committed) |

## Backend (FastAPI)

### Requirements

- Python 3.11+ recommended  
- MySQL 5.7+ / 8.x  
- Dependencies: see `backend/requirements.txt`

### Setup

1. Create a database and apply SQL:

   - New database: run `backend/sql/schema.sql`  
   - Existing tables that used a column named `password`: run `backend/sql/migration_legacy_password_column.sql` first, then `migration_add_google_auth.sql` if `google_sub` is still missing.

2. Configure environment:

   ```bash
   cd backend
   cp .env.example .env
   ```

   Edit `.env` with `DB_*`, `JWT_SECRET` (use a long random value in production), and `GOOGLE_WEB_CLIENT_ID` (OAuth **Web client** ID from Google Cloud Console, used to verify Android ID tokens).

3. Install and run:

   ```bash
   pip install -r requirements.txt
   python main.py
   ```

   By default the app listens on `0.0.0.0:8000` so devices on your LAN can reach it. Health checks: `GET /health` (liveness), `GET /health/ready` (database readiness).

### Docker (production-style)

From repo root:

```bash
docker build -t moodmap-api:latest ./backend
```

Local stack (API + MySQL): see `backend/docker-compose.yml` and the full guide in **[DEPLOY.md](DEPLOY.md)** (GCP Cloud Run, AWS App Runner / ECR, env vars, TLS).

### API surface (high level)

- `POST /api/auth/register`, `POST /api/auth/login`, `POST /api/auth/google`  
- `GET /api/mood/comfort?mood=...` (simple comfort strings)  
- `GET /` — API metadata

## Android app

### Requirements

- Android Studio (recent stable) with SDK 36 / Compose  
- JDK 11+

### Configuration (`mobile/local.properties`)

This file is **not** committed. Set at least:

| Property | Purpose |
|----------|---------|
| `sdk.dir` | Android SDK path (often filled by Android Studio) |
| `MOOD_MAP_API_BASE_URL` | Base URL with trailing slash, e.g. `http://10.0.2.2:8000/` for emulator → host, or `http://<your-lan-ip>:8000/` for a physical device |
| `GOOGLE_WEB_CLIENT_ID` | Same **Web** OAuth client ID as `GOOGLE_WEB_CLIENT_ID` in backend `.env` |
| `GEMINI_API_KEY` | For on-device Gemini (AI chat, companion, insights, journal summary) |
| `MAPS_API_KEY` | [Maps SDK for Android](https://developers.google.com/maps/documentation/android-sdk/start) key (enable “Maps SDK for Android” on your Google Cloud project) |

Release builds expect `MOOD_MAP_API_BASE_URL` to use **https** (see `validateReleaseApiConfiguration` in `mobile/app/build.gradle.kts`).

### Debug networking

Debug builds use a network security config that allows cleartext HTTP so you can talk to a dev server on your PC. Release builds are intended for HTTPS-only API URLs.

### Windows / OneDrive builds

If Gradle fails deleting files under `mobile/app/build`, OneDrive or another process may be locking the build tree. This project sets the **app module build directory** to `%LOCALAPPDATA%\MoodMapGradle\app` to shorten paths and avoid syncing heavy intermediates. Debug APK output: `%LOCALAPPDATA%\MoodMapGradle\app\outputs\apk\debug\`. If problems persist: **File → Invalidate Caches**, `gradlew --stop`, delete leftover `mobile/app/build` after closing Android Studio, then rebuild.

### Running

Open the `mobile/` folder in Android Studio, sync Gradle, run the `app` configuration on an emulator or device.

After sign-in, the app uses a **bottom navigation** shell: Map, Insights, AI chat, Journal, Recovery stories, Menu (crisis copy + sign out).

## Security notes

- Never commit `backend/.env` or `mobile/local.properties`.  
- Rotate any API keys or database passwords that were ever shared or checked into history.  
- Restrict Google Maps and OAuth keys in Google Cloud Console (Android package + SHA-1 for Maps; authorized clients for OAuth).

## Git / IDE hygiene

- **`mobile/.idea/`** should stay untracked. If it was already added to Git, remove it from the index once:  
  `git rm -r --cached mobile/.idea`  
  (then commit; local IDE settings remain on your machine).
- **`_web-ref/`** is ignored at the repo root; do not commit a full clone of the web project.

## Automated checks (CI / local)

Run these **one at a time** (two Gradle commands in parallel can corrupt the shared Kotlin cache under `%LOCALAPPDATA%\MoodMapGradle\`):

**Android** (from `mobile/`):

```bash
gradlew.bat --stop
gradlew.bat clean testDebugUnitTest lintDebug assembleDebug
```

- **Unit tests:** `testDebugUnitTest`  
- **Lint:** `lintDebug` (report: `%LOCALAPPDATA%\MoodMapGradle\app\reports\lint-results-debug.html`)  
- **Instrumented tests** (needs a device or emulator): `gradlew.bat connectedDebugAndroidTest`

**Backend** (from `backend/`):

```bash
pip install -r requirements-dev.txt
python -m pytest tests/ -v
python -m compileall -q .
```

Smoke tests hit `/`, `/health`, and `/api/mood/comfort` only (no MySQL). Full auth flows need a running database.

## Related project

- Web prototype and feature ideas: [HassanCoulibaly/hacklanta-the-Cognitive-Coders](https://github.com/HassanCoulibaly/hacklanta-the-Cognitive-Coders)

## License

Specify your team’s license here if you add one (e.g. MIT to match the web repo).
