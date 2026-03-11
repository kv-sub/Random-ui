# ─────────────────────────────────────────────────────────────────────────────
# Synthetic Data Agent — Streamlit UI
# Build context: workspace root (one level above this file)
#
# Build (from insurance-claim-system/ with compose):
#   docker compose --profile test up --build
#
# Build standalone (from workspace root):
#   docker build -f synthetic-agent/Dockerfile -t synthetic-agent .
# ─────────────────────────────────────────────────────────────────────────────
FROM python:3.12-slim

WORKDIR /app

# Install dependencies first (layer-cache friendly)
COPY synthetic-agent/requirements.txt requirements.txt
RUN pip install --no-cache-dir -r requirements.txt

# Copy agent source from workspace root
COPY main.py main.py

# Non-root user for security
RUN addgroup --system agentgroup && adduser --system --ingroup agentgroup agentuser
USER agentuser

EXPOSE 8501

HEALTHCHECK --interval=30s --timeout=10s --start-period=20s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8501/_stcore/health')"

ENTRYPOINT ["streamlit", "run", "main.py", \
            "--server.port=8501", \
            "--server.address=0.0.0.0", \
            "--server.headless=true", \
            "--browser.gatherUsageStats=false"]
