# AeroAI — AI-based Air Traffic Monitoring and Prediction System

AeroAI is a public, Flightradar-style aviation intelligence platform: live air-traffic
monitoring by major-city airspace, flight/airport information, AI-based delay prediction,
basic mathematical conflict-risk detection, personalized flight tracking, alerts, and
analytics.

**This repository currently contains a completed backend implementation.** The frontend
has not yet been implemented (see `BACKEND_IMPLEMENTATION_STATUS.md` for the current
project phase and `BACKEND_FRONTEND_HANDOFF.md` for everything a frontend developer/agent
needs to build against this backend without reading its source).

The full specification this backend implements is `IMPLEMENT_v2.md` at the repo root.

## Repository layout

```text
AATMP_Project/
├── IMPLEMENT_v2.md                 # Master implementation specification (source of truth)
├── backend/                        # Spring Boot modular monolith (COMPLETE)
├── ml/                              # Existing FastAPI ML service + frozen model artifacts
├── frontend/                        # Not yet implemented
├── docker-compose.yml               # postgres + redis + ml + backend
├── .env.example                     # Copy to .env and fill in real values
├── BACKEND_FRONTEND_HANDOFF.md      # API contract for frontend implementation
└── BACKEND_IMPLEMENTATION_STATUS.md # What was built, verified, and how
```

## Runtime requirements

- Java 21 (LTS)
- Maven 3.9.x
- Python 3.11.x (3.12 also verified working) for the ML service
- Docker Desktop / Docker Engine 24.x+ with Compose v2
- PostgreSQL 16.x
- Redis 7.2.x

## Environment setup

```bash
cp .env.example .env
# then fill in real values, especially JWT_SECRET and (optionally) OpenSky credentials
```

For local development, `OPENSKY_CREDENTIALS_FILE` may point at an external
`credentials.json` (OAuth2 `clientId`/`clientSecret`) produced by the OpenSky account
page. That file lives **outside** this repository and must never be committed. Without
it, the backend still runs, using OpenSky's reduced-limit anonymous access.

## Running the infrastructure (PostgreSQL + Redis)

```bash
docker compose up -d postgres redis
```

(Or reuse already-running local Postgres/Redis containers/services — the backend just
needs a reachable instance matching `DATABASE_URL`/`REDIS_HOST`.)

## Running the ML service

From the repository root:

```bash
python -m uvicorn ml.api.main:app --reload --port 8000
```

Health check: `GET http://127.0.0.1:8000/health` → `{"status": "ok"}`

**The ML models/service are frozen** — do not retrain, replace, or modify
`ml/api/main.py` or the three `.pkl` artifacts in `ml/models/`.

## Running the backend

```bash
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Health check: `GET http://localhost:8080/actuator/health` → `{"status": "UP"}`

Flyway migrations run automatically on startup (schema + reference-data seed for
airspaces/airports). No manual DB setup is required beyond having an empty `aatmp`
database reachable.

## Running everything with Docker Compose

```bash
docker compose up --build
```

This builds and starts `postgres`, `redis`, `ml`, and `backend`. The frontend is not yet
part of this compose file (see the status doc).

## Tests

```bash
cd backend
mvn test
```

Tests exercise the **real** local PostgreSQL/Redis/FastAPI services (not mocks) wherever
practical — see `BACKEND_IMPLEMENTATION_STATUS.md` for full results and any tests that are
skipped when a dependency isn't reachable.

## Configuration reference

Every tunable value (poll intervals, thresholds, retention windows, conflict-detection
parameters, etc.) is documented in `IMPLEMENT_v2.md` Section 94 and bound to
`aeroai.*` Spring properties in `backend/src/main/resources/application.yml`. Never a
hardcoded magic number.

## Scheduler/rate-limit policy (Section 18.4)

By default, exactly **one** city airspace (`AIRSPACE_DEFAULT_CITY_ID=mumbai`) is actively
polled from OpenSky at a time (`AIRSPACE_MAX_ACTIVE_CITY_COUNT=1`), so the free API budget
is never multiplied by the size of the city catalogue. Requesting a different city's
current traffic promotes it into the active-polling set (bounded by the same count);
increase `AIRSPACE_MAX_ACTIVE_CITY_COUNT` if you have a higher OpenSky rate allowance.
