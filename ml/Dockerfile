# AeroAI FastAPI ML service. Deployment structure preserved exactly per
# IMPLEMENT_v2.md Section 12: ml/api/ + ml/models/ + this Dockerfile only.
# Build context MUST be the repository root so `ml/models/*.pkl` resolves.
FROM python:3.11-slim
WORKDIR /srv

COPY ml/api/requirements.txt ml/api/requirements.txt
RUN pip install --no-cache-dir -r ml/api/requirements.txt

COPY ml/api ml/api
COPY ml/models ml/models

EXPOSE 8000
CMD ["python", "-m", "uvicorn", "ml.api.main:app", "--host", "0.0.0.0", "--port", "8000"]
