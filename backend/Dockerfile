# Mood Map API — deploy to GCP Cloud Run, AWS App Runner / ECS / Lightsail, or any Docker host.
FROM python:3.12-slim-bookworm

WORKDIR /app

ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    PIP_NO_CACHE_DIR=1

# mysql-connector-python uses wheels; no build tools required.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY . .

# Cloud Run and AWS App Runner inject PORT; default 8080 matches many platforms.
ENV PORT=8080
EXPOSE 8080

# Production: no --reload. Workers: scale by running more containers (recommended for serverless).
CMD ["sh", "-c", "exec uvicorn main:app --host 0.0.0.0 --port ${PORT:-8080} --proxy-headers --forwarded-allow-ips='*'"]
