from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware

from api.auth_routes import router as auth_router
from api.mood_routes import router as mood_router
from core.config import get_settings

settings = get_settings()

app = FastAPI(title="Mood Map API", version="1.0.0")

if settings.cors_origins:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=settings.cors_origins,
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

app.include_router(auth_router, prefix="/api/auth")
app.include_router(mood_router, prefix="/api/mood")


@app.get("/")
def root():
    return {"message": "Mood Map API", "version": "1.0.0"}


@app.get("/health")
def health():
    """Liveness: process is up (use for load balancer / Cloud Run / ECS health checks)."""
    return {"status": "ok"}


@app.get("/health/ready")
def health_ready():
    """Readiness: database is reachable (optional stricter check for orchestration)."""
    from database.db_connection import get_db_connection

    db = get_db_connection()
    if db is None:
        raise HTTPException(status_code=503, detail="Database unavailable")
    try:
        db.close()
    except Exception:
        pass
    return {"status": "ok", "database": True}


if __name__ == "__main__":
    import uvicorn

    # Bind all interfaces so phones on the same LAN can reach the API (not only localhost).
    uvicorn.run("main:app", host="0.0.0.0", port=8000, reload=True)
