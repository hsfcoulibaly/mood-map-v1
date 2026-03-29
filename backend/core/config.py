import os
from dataclasses import dataclass
from functools import lru_cache

from dotenv import load_dotenv

load_dotenv()


@dataclass(frozen=True)
class Settings:
    db_host: str
    db_user: str
    db_password: str
    db_name: str
    # When set (e.g. Cloud Run + Cloud SQL), connect via /cloudsql/PROJECT:REGION:INSTANCE
    cloud_sql_connection_name: str
    jwt_secret: str
    jwt_access_token_expire_minutes: int
    cors_origins: list[str]
    is_production: bool
    google_web_client_id: str


@lru_cache
def get_settings() -> Settings:
    app_env = (os.getenv("APP_ENV") or "development").lower()
    is_production = app_env == "production"
    secret = os.getenv("JWT_SECRET", "").strip()
    if is_production and len(secret) < 32:
        raise RuntimeError(
            "APP_ENV=production requires JWT_SECRET with at least 32 characters."
        )
    if not secret:
        secret = "dev-only-insecure-secret-change-for-nonlocal-testing-min-32-ch"
    expire_raw = os.getenv("JWT_ACCESS_TOKEN_EXPIRE_MINUTES", "10080")
    try:
        expire_mins = max(5, int(expire_raw))
    except ValueError:
        expire_mins = 10080
    cors_raw = os.getenv("CORS_ORIGINS") or ""
    origins = [o.strip() for o in cors_raw.split(",") if o.strip()]
    return Settings(
        db_host=os.getenv("DB_HOST", "127.0.0.1"),
        db_user=os.getenv("DB_USER", "root"),
        db_password=os.getenv("DB_PASSWORD", ""),
        db_name=os.getenv("DB_NAME", "moodmap"),
        cloud_sql_connection_name=os.getenv("CLOUD_SQL_CONNECTION_NAME", "").strip(),
        jwt_secret=secret,
        jwt_access_token_expire_minutes=expire_mins,
        cors_origins=origins,
        is_production=is_production,
        google_web_client_id=os.getenv("GOOGLE_WEB_CLIENT_ID", "").strip(),
    )
