"""Smoke tests for the FastAPI app (no MySQL required). Run from `backend/`:  python -m pytest tests/ -v"""

from fastapi.testclient import TestClient

from main import app

client = TestClient(app)


def test_health():
    r = client.get("/health")
    assert r.status_code == 200
    assert r.json() == {"status": "ok"}


def test_root():
    r = client.get("/")
    assert r.status_code == 200
    body = r.json()
    assert body.get("message") == "Mood Map API"
    assert body.get("version")


def test_mood_comfort():
    r = client.get("/api/mood/comfort", params={"mood": "happy"})
    assert r.status_code == 200
    data = r.json()
    assert "message" in data
    assert isinstance(data["message"], str)
    assert len(data["message"]) > 0


def test_health_ready_with_or_without_database():
    """In CI without MySQL this is 503; with DB configured locally it may be 200."""
    r = client.get("/health/ready")
    assert r.status_code in (200, 503)
    if r.status_code == 200:
        assert r.json().get("database") is True
