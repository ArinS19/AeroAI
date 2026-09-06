# AeroAI — Backend → Frontend Handoff

This document lets a frontend developer or AI coding agent build the AeroAI frontend
**without reading any backend source code**. It is the complete, implementation-ready
contract for how the frontend must talk to the Spring Boot backend.

The authoritative product/UX specification remains `IMPLEMENT_v2.md` at the repo root —
this document describes *how the backend that already exists actually behaves*, so treat
this file as binding for wire formats, and `IMPLEMENT_v2.md` as binding for product/UX
behavior, page structure, and design direction.

---

## A. Backend startup instructions

```bash
# 1. Start infrastructure (from repo root)
docker compose up -d postgres redis

# 2. Start the ML service (from repo root)
python -m uvicorn ml.api.main:app --reload --port 8000

# 3. Start the backend (from backend/)
cd backend
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or, for the fully containerized stack: `docker compose up --build` (starts postgres,
redis, ml, backend — not yet the frontend).

Health checks:
- Backend: `GET http://localhost:8080/actuator/health` → `{"status":"UP"}`
- ML service: `GET http://127.0.0.1:8000/health` → `{"status":"ok"}`

## B. Backend URL / port

- Base URL (dev): `http://localhost:8080`
- All REST endpoints are prefixed `/api`
- WebSocket/STOMP endpoint: `http://localhost:8080/ws` (SockJS-wrapped **and** available as
  a raw WebSocket upgrade at the same path)

## C. Required environment variables (frontend-visible only)

The frontend must only ever read **public**, non-secret configuration. Recommended Vite
env vars (see `.env.example` at repo root):

```text
VITE_API_BASE_URL=http://localhost:8080
VITE_WS_URL=http://localhost:8080/ws
VITE_MAP_TILE_URL=https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png
```

Never read/expose `JWT_SECRET`, `DATABASE_*`, `REDIS_*`, or `OPENSKY_*` from the frontend —
those exist only in the backend's environment.

## D. Authentication flow

- Email + password only. No OAuth/social login.
- On success, `POST /api/auth/register` and `POST /api/auth/login` both return a JWT
  (`token`) and its declared lifetime in seconds (`expiresInSeconds`, default 3600 = 60
  minutes).
- Store the token (e.g. `localStorage` or in-memory + `localStorage` fallback) and send it
  on every authenticated request as:

  ```text
  Authorization: Bearer <token>
  ```

- There is **no refresh-token flow**. When the token expires, the backend returns `401`
  on the next authenticated call — the frontend must detect this and prompt re-login
  (redirect to `/login` with a return-to path per Section 61), not silently retry.
- `POST /api/auth/logout` is a stateless no-op on the backend (JWT has no server-side
  session to invalidate) — the frontend's actual "logout" behavior is discarding the
  stored token client-side. Still call the endpoint for symmetry/future-proofing.

## E. Register endpoint

```text
POST /api/auth/register
Auth: none
Content-Type: application/json
```

Request body:

```json
{
  "email": "user@example.com",
  "password": "password123",
  "confirmPassword": "password123"
}
```

Validation rules (enforced server-side, return `400 VALIDATION_ERROR` if violated):
- `email` must be a syntactically valid email address.
- `password` must be **at least 8 characters and contain at least one letter and one
  number**.
- `confirmPassword` must exactly match `password` (checked server-side too, not just
  client-side).

Response `201 Created`:

```json
{
  "token": "eyJhbGciOiJIUzUxMiJ9...",
  "tokenType": "Bearer",
  "expiresInSeconds": 3600,
  "user": {
    "id": 1,
    "email": "user@example.com",
    "createdAt": "2026-09-06T06:04:47.407930Z",
    "trackedFlightCount": 0
  }
}
```

Errors:
- `400 VALIDATION_ERROR` — invalid email/password shape.
- `409 CONFLICT` — an account with this email already exists (message: *"An account with
  this email already exists."*). Do not attempt to distinguish "email taken" from other
  errors via a different shape — this is the one and only conflict shape.

## F. Login endpoint

```text
POST /api/auth/login
Auth: none
Content-Type: application/json
```

Request body:

```json
{ "email": "user@example.com", "password": "password123" }
```

Response `200 OK`: same `AuthResponse` shape as Register (§E).

Errors:
- `401 UNAUTHORIZED` with the **exact same generic message** for both "no such account"
  and "wrong password": *"Invalid email or password."* — never reveal which case it was.

## G. Logout behavior

```text
POST /api/auth/logout
Auth: Bearer token optional (endpoint doesn't require/validate it)
```

Response: `204 No Content`. The frontend must independently discard the stored token and
reset any authenticated UI state — the backend does not track sessions to invalidate.

## H. JWT requirements

- Header: `Authorization: Bearer <token>`
- Algorithm: HMAC-SHA (HS512), opaque to the frontend — never decode/inspect the token's
  claims client-side to make authorization decisions; only use it as a bearer credential.
- Expiry: 60 minutes by default (`expiresInSeconds` in the auth response tells you the
  actual configured value — don't hardcode 60).
- On `401` from any endpoint, treat the session as expired: clear the stored token and
  redirect to `/login`.

## I. Every frontend-consumable REST endpoint

All endpoints are prefixed `/api`. Every non-2xx JSON response uses the **error envelope**
described in section AD below.

### Auth

| Method | Path | Auth | Body | Response |
|---|---|---|---|---|
| POST | `/api/auth/register` | none | `RegisterRequest` | `201` `AuthResponse` |
| POST | `/api/auth/login` | none | `LoginRequest` | `200` `AuthResponse` |
| POST | `/api/auth/logout` | none | — | `204` |

### Account

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/api/me` | **required** | `200` `UserDto` |
| GET | `/api/me/flights` | **required** | `200` `TrackedFlightDto[]` |

`UserDto`:
```json
{ "id": 1, "email": "user@example.com", "createdAt": "2026-09-06T06:04:47.407930Z", "trackedFlightCount": 2 }
```

### Airspaces (city catalogue — Section 13.2)

| Method | Path | Auth | Query params | Response |
|---|---|---|---|---|
| GET | `/api/airspaces` | none | `q` (optional, city/country/alias search) | `200` `AirspaceDto[]` |
| GET | `/api/airspaces/{cityId}` | none | — | `200` `AirspaceDto` (includes `nearbyAirports`) |

`cityId` is a stable lowercase-hyphenated slug (e.g. `mumbai`, `new-delhi`, `abu-dhabi`).
The full seeded catalogue has 60 major cities (25 Indian + 35 international).

`AirspaceDto`:
```json
{
  "cityId": "mumbai",
  "cityName": "Mumbai",
  "country": "India",
  "latitude": 19.076,
  "longitude": 72.8777,
  "defaultZoom": 9,
  "lamin": 17.28, "lomin": 71.08, "lamax": 20.88, "lomax": 74.68,
  "displayRegion": "Maharashtra, India",
  "aliases": ["Bombay"],
  "nearbyAirports": [
    { "id": 1, "name": "Chhatrapati Shivaji Maharaj International Airport", "iata": "BOM", "icao": "VABB", "latitude": 19.0896, "longitude": 72.8656 }
  ]
}
```

`nearbyAirports` is only populated on the **detail** endpoint (`GET /api/airspaces/{cityId}`),
not the list endpoint (empty array there — avoids an expensive N+1 lookup on the catalogue
listing).

Example error (unknown city):
```json
{ "timestamp": "2026-09-06T06:16:50.900513300Z", "status": 404, "error": "NOT_FOUND", "message": "Airspace 'not-a-real-city' was not found.", "path": "/api/airspaces/not-a-real-city" }
```

### Airports (Section 29/30)

| Method | Path | Auth | Query params | Response |
|---|---|---|---|---|
| GET | `/api/airports` | none | `page` (default 0), `size` (default 50, max 100) | `200` Spring `Page<AirportDto>` |
| GET | `/api/airports/search?q=` | none | `q` (required, max 100 chars) | `200` `AirportDto[]` (max 50 results) |
| GET | `/api/airports/{id}` | none | — | `200` `AirportDto` |

`AirportDto`:
```json
{ "id": 1, "name": "Chhatrapati Shivaji Maharaj International Airport", "iata": "BOM", "icao": "VABB", "city": "Mumbai", "location": "Maharashtra", "country": "India", "latitude": 19.0896, "longitude": 72.8656 }
```

The `page` response from `GET /api/airports` is Spring Data's standard `Page<T>` JSON shape:
```json
{ "content": [ /* AirportDto[] */ ], "totalElements": 155, "totalPages": 4, "number": 0, "size": 50, ... }
```

**Note (honest limitation — read this before building Arrivals/Departures UI):**
Airport "live arrivals/departures" (Section 30) is **not currently exposed as a distinct
endpoint**. OpenSky's live state vectors carry no origin/destination/schedule information
at all — there is no reliable data source backing an arrivals/departures list today. Do not
build this table against a live endpoint; if/when a reliable route-association source is
added to the backend, this handoff document will be updated with the new endpoint. Until
then, render this section of the Airport Details page as `Unavailable` per Section 3.1
rather than inventing data.

### Flights (Section 27/28)

| Method | Path | Auth | Query params | Response |
|---|---|---|---|---|
| GET | `/api/flights` | none | `airspaceId` (default: the server's configured default city), `status` (`SCHEDULED\|IN_FLIGHT\|LANDED\|UNKNOWN`), `airline` (substring match) | `200` `FlightListItemDto[]` |
| GET | `/api/flights/current` | none | `airspaceId` | `200` `AircraftDto[]` — use this for the **map** (raw per-aircraft telemetry) |
| GET | `/api/flights/search?q=` | none | `q` (callsign or ICAO24 substring, max 50 results, exact matches ranked first) | `200` `FlightListItemDto[]` |
| GET | `/api/flights/{icao24}` | optional | — | `200` `FlightDetailDto` (`tracked` field reflects the caller's own tracking state if a valid JWT is sent, else `false`) |
| GET | `/api/flights/{icao24}/trail?minutes=` | none | `minutes` — **must be exactly one of** `5, 15, 30, 60` | `200` `TrailResponseDto` |

`{icao24}` is the 6-hex-character ICAO24 identifier (e.g. `8014e8`), **not** a numeric ID —
this is intentional, since it's the one truly stable aircraft identifier (Section 18.2).

Requesting `airspaceId` on any flights endpoint also opportunistically "activates" that
city in the backend's bounded ingestion scheduler (Section 18.4) — the first request or
two for a newly-selected city may return an empty/stale snapshot while the backend spins
up ingestion for it; keep polling/resubscribing rather than treating an empty result as a
permanent failure.

`AircraftDto` (used by `/api/flights/current` **and** WebSocket added/updated entries —
see section W):
```json
{
  "icao24": "8014e8",
  "callsign": "AIC9HA",
  "latitude": 18.6862,
  "longitude": 73.046,
  "altitude": 11277.6,
  "speed": 225.33,
  "heading": 162.19,
  "verticalRate": 0.0,
  "onGround": false,
  "status": "IN_FLIGHT",
  "lastUpdate": "2026-09-06T06:14:22Z"
}
```
Any field may be `null` — render it as the literal string **"Unavailable"**, never `0`/`--`.

`FlightListItemDto` (Flights page table rows / search results):
```json
{
  "icao24": "801659",
  "callsign": "AIC9HA",
  "airline": "Air India",
  "origin": null,
  "destination": null,
  "status": "IN_FLIGHT",
  "altitude": 868.68,
  "speed": 128.64,
  "lastUpdate": "2026-09-06T06:16:16Z"
}
```
`airline` is populated only when the callsign's 3-letter ICAO prefix matches a small known
airline lookup table (~40 major carriers) — otherwise `null`/Unavailable. `origin`/
`destination` are currently always `null` (see the honest-limitation note above); the field
shape (`AirportRefDto`, see below) is stable and will populate automatically if a reliable
route source is added later — the frontend does not need to change when that happens.

`FlightDetailDto` (flight details panel/page, Section 28):
```json
{
  "icao24": "8014e8",
  "callsign": "AIC9HA",
  "airline": "Air India",
  "aircraftRegistration": null,
  "aircraftType": null,
  "origin": null,
  "destination": null,
  "departureTime": null,
  "arrivalTime": null,
  "status": "IN_FLIGHT",
  "currentPosition": {
    "latitude": 18.6862, "longitude": 73.046, "altitude": 11277.6,
    "speed": 225.33, "heading": 162.19, "verticalRate": 0.0,
    "lastUpdate": "2026-09-06T06:14:22Z"
  },
  "distanceTravelledKm": null,
  "distanceRemainingKm": null,
  "estimatedArrival": null,
  "trailAvailable": true,
  "weather": {
    "temperatureCelsius": 26.6, "conditionDescription": "Slight rain showers",
    "windSpeedKmh": 11.6, "windDirectionDegrees": 240.0, "humidityPercent": 86.0,
    "visibilityKm": null, "pressureHpa": 1013.7, "cloudCoverPercent": 94.0, "precipitationMm": 0.4
  },
  "prediction": { "available": false, "predictedDelayMinutes": null, "delayProbability": null, "delayOver15Minutes": null, "category": null, "disclaimer": "Machine-learning estimate, not an airline guarantee." },
  "conflicts": [],
  "tracked": false
}
```

`AirportRefDto` shape (used for `origin`/`destination` whenever they ARE available):
```json
{ "id": 1, "name": "...", "iata": "BOM", "icao": "VABB", "latitude": 19.09, "longitude": 72.87 }
```

`distanceTravelledKm` requires ≥2 recorded points since the aircraft's current In-Flight
session began — `null` until then. `distanceRemainingKm`/`estimatedArrival` additionally
require a reliable `destination` (currently never populated — see limitation above), so
today they are always `null`; render as **"ETA unavailable"** / **"Unavailable"** per
Section 25.4/25.3.

`conflicts` is an array of `ConflictRiskDto` (empty array = no MEDIUM/HIGH risk currently
detected for this aircraft — treat as implicit **LOW**, not "Unavailable"):
```json
{
  "otherIcao24": "bbbbbb",
  "otherCallsign": "TEST2",
  "riskLevel": "HIGH",
  "horizontalDistanceKm": 0.0,
  "altitudeDifferenceM": 0.0,
  "closureRateMs": -400.0,
  "dcpaHorizontalM": 0.0,
  "altitudeDiffAtTcpaM": 0.0,
  "timeToClosestApproachSeconds": 5.25,
  "calculatedAt": "2026-09-06T06:20:09.045Z",
  "disclaimer": "Research/decision-support estimate only. Not an official ATC, TCAS, or collision-avoidance warning."
}
```

`TrailResponseDto`:
```json
{ "icao24": "8014e8", "minutes": 15, "available": true, "points": [ { "lat": 18.6862, "lon": 73.046, "timestamp": 1788675262 } ] }
```
`timestamp` inside `points` is **Unix epoch seconds** (not ISO-8601) — convert for display.
`available: false` (with empty `points`) means "Trail unavailable / collecting data"
(Section 14) — never render a fabricated straight line.

### Tracking (Section 41/42)

| Method | Path | Auth | Body | Response |
|---|---|---|---|---|
| POST | `/api/tracked-flights` | **required** | `{ "icao24": "8014e8", "callsign": "AIC9HA" }` (`callsign` optional) | `201` `TrackedFlightDto` |
| DELETE | `/api/tracked-flights/{id}` | **required** | — | `204` |

`id` here is the numeric `TrackedFlightDto.id` (not the ICAO24). Tracking the same aircraft
twice is idempotent — the existing tracking record is returned rather than creating a
duplicate. Attempting to delete another user's tracked-flight record returns `404
NOT_FOUND` (never reveals whether the ID exists at all, per Section 40).

`TrackedFlightDto`:
```json
{ "id": 2, "icao24": "8014e8", "callsign": "AIC9HA", "status": "IN_FLIGHT", "predictedDelayMinutes": null, "delayCategory": null, "lastUpdate": "2026-09-06T06:14:22Z", "dataAvailable": true }
```
`dataAvailable: false` means the tracked aircraft has temporarily dropped out of live
state — render **"Data temporarily unavailable"** (Section 42), and do NOT offer an
"Untrack" affordance that implies the relationship itself is gone; it isn't.

### Alerts (Section 43)

| Method | Path | Auth | Response |
|---|---|---|---|
| GET | `/api/alerts` | **required** | `200` `AlertDto[]` (newest first) |
| GET | `/api/alerts/unread-count` | **required** | `200` `{ "unreadCount": 2 }` |
| PATCH | `/api/alerts/{id}/read` | **required** | `200` `AlertDto` |
| PATCH | `/api/alerts/read-all` | **required** | `204` |

`AlertDto`:
```json
{ "id": 5, "trackedFlightId": 2, "type": "STATUS_CHANGE", "message": "Flight AIC9HA status changed to LANDED.", "createdAt": "2026-09-06T09:00:00Z", "read": false }
```
`type` is one of: `STATUS_CHANGE`, `DELAY_CATEGORY_CHANGE`, `CONFLICT_RISK_CHANGE`,
`DATA_UNAVAILABLE`. Poll `GET /api/alerts/unread-count` every ~30s (or reuse an existing
WebSocket connection cadence) to keep the notification bell badge live without a full page
refresh.

### Analytics (Section 44)

| Method | Path | Auth | Query params | Response |
|---|---|---|---|---|
| GET | `/api/analytics` | none | `range` = `today` \| `7d` (default `today`); `airspaceId` (default: server's default city) | `200` `AnalyticsDto` |

`AnalyticsDto`:
```json
{
  "range": "today",
  "airspaceId": "mumbai",
  "liveTraffic": {
    "totalAircraftVisible": 14,
    "byAltitudeBand": { "10000-30000ft": 5, "30000ft+": 3, "0-10000ft": 5, "UNAVAILABLE": 1 },
    "byStatus": { "IN_FLIGHT": 13, "UNKNOWN": 1 },
    "arrivalsInRange": 2,
    "departuresInRange": 39
  },
  "flightStats": {
    "flightsMonitored": 0,
    "activeFlights": 14,
    "completedFlights": 0,
    "averagePredictedDelayMinutes": null,
    "delayCategoryDistribution": {}
  },
  "conflictStats": {
    "potentialConflicts": 0,
    "mediumRiskCount": 0,
    "highRiskCount": 0,
    "recentConflictEvents": [ { "icao24": null, "otherIcao24": "bbbbbb", "riskLevel": "HIGH", "calculatedAt": "..." } ]
  },
  "systemHealth": {
    "lastOpenSkyUpdate": "2026-09-06T06:16:35Z",
    "freshnessLabel": "LIVE",
    "openSkyStatus": "UP",
    "openMeteoStatus": "UP",
    "mlServiceStatus": "UP",
    "totalTrackedFlights": 0
  }
}
```
`flightStats`/`arrivalsInRange`/`departuresInRange` reflect the same OpenSky data
limitations described above (no reliable schedule source) — treat low/zero values as
honest, not as a bug. `freshnessLabel` is one of `LIVE` / `DELAYED` / `UNAVAILABLE` — reuse
this exact vocabulary for the live-data indicator (Section 17), don't invent your own
thresholds client-side.

## J–P. (Flight/Airspace/Airport/Search/Prediction/Analytics/Alert APIs)

All covered above in section I — there is one unified REST surface, not separate
sub-APIs. Prediction and conflict data are embedded inside `FlightDetailDto`, not separate
top-level endpoints (there's no `GET /api/predictions/{id}` — predictions are always
fetched as part of a flight's detail response).

## Q. User/account APIs

Covered in section I ("Account"). There is no password-reset, profile-photo, or
email-change endpoint — intentionally out of scope (Section 39.3).

## R. Weather APIs

Weather is never a standalone endpoint — it's embedded in `FlightDetailDto.weather` (see
above). There is no `/api/weather` route and no Weather nav item/page (Section 31).

## S. Conflict-risk APIs

Embedded in `FlightDetailDto.conflicts` (see above) and summarized in
`AnalyticsDto.conflictStats`. There is no standalone conflict-list endpoint.

## T. WebSocket endpoint

```text
ws://localhost:8080/ws       (raw WebSocket upgrade)
http://localhost:8080/ws     (SockJS-wrapped, for environments that block raw WS)
```

Protocol: **STOMP** over WebSocket/SockJS (`@stomp/stompjs` + optionally `sockjs-client`
on the frontend).

## U. WebSocket connection procedure

```text
1. GET /api/flights/current?airspaceId={cityId}   -- render initial markers from this REST snapshot
2. Open STOMP connection to /ws (SockJS or raw WS)
3. SUBSCRIBE to /topic/aircraft/{cityId}           -- same cityId as step 1
4. Apply incoming delta messages (added/updated/removed) to the in-memory marker map
```

Example STOMP frames (verified against the running backend):

```text
>>> CONNECT
    accept-version:1.2
    heart-beat:0,0

<<< CONNECTED
    version:1.2
    heart-beat:0,0

>>> SUBSCRIBE
    id:sub-0
    destination:/topic/aircraft/mumbai

<<< MESSAGE
    destination:/topic/aircraft/mumbai
    content-type:application/json
    subscription:sub-0

    { "timestamp": "...", "added": [ ... ], "updated": [ ... ], "removed": [ ... ] }
```

No authentication is required to subscribe — the live map is public (Section 2.1).

## V. WebSocket message format

Exactly (Section 47):

```json
{
  "timestamp": "2026-09-06T06:20:09.045707900Z",
  "added": [ /* AircraftDto[] — see section I */ ],
  "updated": [ /* AircraftDto[] — full record, not a partial diff */ ],
  "removed": [ "b4c5d6" ]
}
```

- `added`/`updated` entries use the **full** `AircraftDto` shape (never a partial-field
  diff) — merge by replacing the entire cached record for that `icao24`.
- `removed` is a flat array of ICAO24 strings.
- Empty deltas (nothing changed) are **not broadcast at all** — don't expect a message
  every poll cycle; absence of a message for a while is normal, not a disconnect signal.
- Key all markers/state by `icao24` (stable identifier) — never by array index or callsign.

## W. Exact added/updated/removed aircraft delta structure

Already fully specified in sections I (`AircraftDto`) and V above — this is one unified
schema, not three different ones.

## X. (duplicate of W — see above)

## Y. Reconnection behavior

The backend does not send any special "you were disconnected" signal — reconnection is a
pure client-side STOMP/WebSocket concern:

1. Detect connection loss via the STOMP client's `onWebSocketClose`/disconnect callback.
2. Show **"Connection interrupted — reconnecting…"** (Section 52).
3. Retry with exponential backoff: start at 1s, double up to a 30s cap, reset to 1s on
   successful reconnect.
4. On reconnect, **re-fetch `GET /api/flights/current?airspaceId={cityId}` first** to close
   any gap, THEN resubscribe to `/topic/aircraft/{cityId}`.
5. Resume applying deltas as normal.

## Z. Live/stale/unavailable semantics

Use `AnalyticsDto.systemHealth.freshnessLabel` (or compute the same thing client-side from
a `lastOpenSkyUpdate`/similar timestamp if a dedicated freshness field is added to a future
endpoint) with these exact thresholds/labels (Section 17 — do not invent different
wording):

| Age of last successful update | Label | Display |
|---|---|---|
| ≤ 20s | `LIVE` | `LIVE` / `Updated Xs ago` |
| 20s–90s | `DELAYED` | `DATA DELAYED` / `Last update Xs ago` |
| > 90s or never | `UNAVAILABLE` | `LIVE DATA UNAVAILABLE` |

Compute the ticking "Xs ago" text client-side every second from the backend-provided
timestamp compared to the browser's own clock — never let the backend's label go stale in
a static render.

## AA. Timestamp format

Every timestamp crossing the REST/WebSocket boundary is **UTC ISO-8601** with an explicit
`Z`/offset suffix, e.g. `2026-09-06T09:15:30Z` or with fractional seconds
`2026-09-06T06:04:47.407930100Z`. The one exception: `TrailResponseDto.points[].timestamp`
is a **Unix epoch integer in seconds** (not a string) — convert accordingly. Render all
timestamps in the browser's local timezone by default (`Intl.DateTimeFormat`/`date-fns`),
except the Analytics "Today" boundary, which is anchored server-side to
`Asia/Kolkata` regardless of viewer timezone (Section 44/45).

## AB. Units

All aircraft telemetry (`AircraftDto`, `FlightDetailDto.currentPosition`) is in **raw
OpenSky-native units**:

| Field | Unit |
|---|---|
| `latitude`/`longitude` | decimal degrees (WGS84) |
| `altitude` | **meters** |
| `speed` | **meters/second** |
| `heading` | degrees clockwise from true north (0–360) |
| `verticalRate` | meters/second (positive = climbing) |

Distances (`distanceTravelledKm`/`distanceRemainingKm`/conflict `horizontalDistanceKm`)
are in **kilometers**. Conflict altitude fields (`altitudeDifferenceM`,
`dcpaHorizontalM`, `altitudeDiffAtTcpaM`) are in **meters**. `closureRateMs` is
**meters/second** (negative = closing, positive = opening). `timeToClosestApproachSeconds`
is seconds. Weather fields are metric (`°C`, `km/h`, `%`, `hPa`, `mm`).

Converting units for **display only** (e.g. m/s → km/h, meters → feet) is fine and expected
— just never re-derive a value the backend already computed as a *decision* (ETA, distance
remaining, conflict risk level, status, prediction category). See section AH.

## AC. Enum values

| Enum | Values |
|---|---|
| Flight status | `SCHEDULED`, `IN_FLIGHT`, `LANDED`, `UNKNOWN` |
| Conflict risk level | `LOW` (implicit/absent — never sent as a literal string in `conflicts[]`), `MEDIUM`, `HIGH` |
| Prediction category | `Low`, `Moderate`, `High` (note: **mixed case**, from the ML service directly — not upper-case like the other enums) |
| Alert type | `STATUS_CHANGE`, `DELAY_CATEGORY_CHANGE`, `CONFLICT_RISK_CHANGE`, `DATA_UNAVAILABLE` |
| Analytics freshness label | `LIVE`, `DELAYED`, `UNAVAILABLE` |
| System health status | `UP`, `DEGRADED`, `UNKNOWN` |

`?status=` filter values on `GET /api/flights` must be one of the flight-status enum
values above (case-insensitive) — anything else returns `400 VALIDATION_ERROR`.

## AD. REST error envelope

Every non-2xx JSON response uses exactly this shape:

```json
{
  "timestamp": "2026-09-06T09:15:30Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Flight not found.",
  "path": "/api/flights/abc123"
}
```

`error` is a stable machine-readable code the frontend can branch on:
`VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`,
`INTERNAL_ERROR`, `UPSTREAM_UNAVAILABLE`. `message` is safe, human-readable text — never a
raw stack trace. Always branch on `error`, never on parsing `message` text.

## AE. Pagination/filtering/sorting behavior

- `GET /api/airports` is the only paginated endpoint (Spring `Page<T>` shape, `page`/`size`
  query params, 0-indexed, `size` capped at 100 server-side).
- `GET /api/flights` / `/api/flights/search` / `/api/airports/search` return **plain
  arrays**, not paginated — the underlying dataset is bounded (a single active airspace's
  current aircraft is at most a few hundred; airport search is capped to 50 results
  server-side). Implement client-side virtualization if a table needs it (Section 27), but
  don't build server-side pagination controls for these.
- Search matching: case-insensitive, substring for names/cities, exact-match-first ranking
  for callsigns/ICAO24/IATA/ICAO codes (Section 26). The backend already applies this — do
  not re-implement fuzzy matching client-side.

## AF. City/airspace URL/query behavior

- Every flights/analytics endpoint accepts `airspaceId` as the city slug (e.g. `mumbai`).
  Omitting it uses the backend's configured default city.
- The frontend should mirror the selected city in its own URL (`/map?airspace=mumbai`,
  Section 61) so refresh/bookmark/share works — this is a frontend routing concern, not
  something the backend enforces.
- Switching cities: fetch the new city's `GET /api/flights/current?airspaceId=newCity`
  snapshot AND resubscribe the WebSocket topic to `/topic/aircraft/newCity` — unsubscribe
  the old topic first so you don't keep receiving updates for the previous city. Do this
  atomically from the UI's perspective (Section 13.2.1) — don't render a frame that mixes
  aircraft from two cities.
- The very first request for a city that hasn't been actively polled recently may return
  an empty or stale snapshot while the backend's bounded scheduler (Section 18.4) spins it
  up — show the normal "Live data unavailable"/loading state, not an error, and keep
  polling.

## AG. Expected frontend behavior for loading/error/empty states

Follow Section 52 exactly:

| Situation | Copy |
|---|---|
| Loading | "Loading live traffic…" / "Loading flight…" / "Loading airport…" |
| Empty results | "No aircraft found for the current filters." / "No matching flights or airports found." / "No tracked flights yet." |
| Weather unavailable | "Weather: Unavailable" |
| Prediction unavailable | "AI Delay Prediction: Unavailable" |
| Live data delayed | "LIVE DATA DELAYED" / "Last update: …" |
| WebSocket disconnected | "Connection interrupted — reconnecting…" |

Never render a blank/broken screen — every data-fetching component needs explicit
loading/empty/error handling.

## AH. Calculations already performed by the backend — do NOT recalculate on the frontend

The frontend must render these values as-is, never re-derive them:

- **Distance travelled** (`distanceTravelledKm`) — haversine sum over recorded trail
  points since the current flight session began.
- **Distance remaining** (`distanceRemainingKm`) — haversine to destination, rounded to
  nearest 10km, always prefixed `~` in the UI.
- **AeroAI ETA** (`estimatedArrival`) — derived from remaining distance ÷ effective speed.
- **Flight status** (`status`) — the `SCHEDULED`/`IN_FLIGHT`/`LANDED`/`UNKNOWN` normalization,
  including the 2-consecutive-cycle Landed-confirmation rule.
- **Conflict risk** (`riskLevel`, `dcpaHorizontalM`, `tcpaSeconds`, `closureRateMs`, etc.) —
  the full TCPA/DCPA calculation.
- **Prediction category/probability** (`prediction.category`, `prediction.delayProbability`) —
  from the ML service, passed through unchanged.
- **Data freshness label** (`LIVE`/`DELAYED`/`UNAVAILABLE`) — computed from the trusted
  backend timestamp, not a client-side guess (the "Xs ago" ticking text is the only thing
  computed client-side, from that same backend timestamp).

The frontend MAY do pure unit/display conversions (m/s→km/h, meters→feet, UTC→local
timezone) — that's presentation, not re-derivation.

## AI. ML-generated values and their disclaimer requirements

Every AI-generated value (`prediction.predictedDelayMinutes`, `delayProbability`,
`delayOver15Minutes`, `category`) must display the **"AI" badge** (tooltip:
*"Machine-learning estimate, not an airline guarantee."* — this exact string is returned
in `prediction.disclaimer`, reuse it verbatim rather than hardcoding your own copy).
Calculated values (distance/ETA/conflict) get the **"Calculated"/"Est." badge** instead
(tooltip strings are provided per-DTO where applicable, e.g.
`ConflictRiskDto.disclaimer` = *"Research/decision-support estimate only. Not an official
ATC, TCAS, or collision-avoidance warning."*). Never mix badges on the same value. Do not
invent a second "confidence %" alongside `delayProbability` — there is exactly one
probability value in the contract.

## AJ. CORS/development configuration

The backend's CORS policy allows only the origins listed in `CORS_ALLOWED_ORIGINS`
(comma-separated, default `http://localhost:5173,http://localhost:3000` for local Vite
dev). If your dev server runs on a different port, add it to that env var (never a
wildcard `*`, and the backend does not accept one). Credentials (`Authorization` header)
are allowed cross-origin for these configured origins.

## AK. Exact commands to start all required services

```bash
# From the repo root:
docker compose up -d postgres redis
python -m uvicorn ml.api.main:app --reload --port 8000

# In a second terminal, from backend/:
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Or, once the frontend exists: `docker compose up --build` for the full non-frontend stack,
then run the frontend's own dev server separately (e.g. `npm run dev`) pointed at
`VITE_API_BASE_URL=http://localhost:8080`.

## AL. Frontend integration checklist

- [ ] Read `IMPLEMENT_v2.md` for the full product/UX spec (pages, layout, visual identity,
      navigation, disclaimers) — this handoff document covers only the API contract.
- [ ] Set up an API client layer (`api/` → `services/`) that never calls `fetch`/`axios`
      directly from components (Section 10).
- [ ] Implement the auth flow (register/login/JWT storage/401-triggers-relogin) first —
      most other features are usable without login, but tracking/alerts need it.
- [ ] Implement the city/airspace selector backed by `GET /api/airspaces` — never hardcode
      a city list (Rule 9).
- [ ] Implement the map: initial snapshot via `GET /api/flights/current`, then STOMP
      subscribe to `/topic/aircraft/{cityId}` for live deltas, keyed by `icao24`.
- [ ] Implement WebSocket reconnection with the exact backoff/re-sync procedure in section
      Y.
- [ ] Implement the live-data freshness indicator using the exact LIVE/DELAYED/UNAVAILABLE
      thresholds and copy in section Z.
- [ ] Implement Flights/Airports pages using the endpoints in section I, honoring the
      known "origin/destination/arrivals/departures are currently Unavailable" limitation
      rather than working around it with invented data.
- [ ] Implement Track/Untrack + My Flights, respecting the auth-required gates and the
      generic "Create an account to track flights" prompt for anonymous users (Section 41).
- [ ] Implement the notification bell backed by `GET /api/alerts` +
      `/api/alerts/unread-count`.
- [ ] Implement Analytics using `GET /api/analytics?range=today|7d`.
- [ ] Apply the AI/Calculated badge conventions in section AI everywhere a derived or
      ML-generated value is shown.
- [ ] Verify no page ever fabricates a value the backend reports as `null`/absent — render
      the literal string **"Unavailable"** instead (Section 3.1) — this is tested for real
      in `BACKEND_IMPLEMENTATION_STATUS.md` and must remain true on the frontend too.
