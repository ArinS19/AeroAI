# AeroAI — AI-based Air Traffic Monitoring and Prediction System

# IMPLEMENT.md — MASTER IMPLEMENTATION SPECIFICATION (v2 — Refined)

> **This file is the single source of truth for implementation.** Build the complete application end-to-end from this specification, in the order given in Section 87. Do not silently expand scope (Section 2.3 is binding). Where a choice is not explicitly pinned below, choose the simplest, robust, maintainable, free/open-source option that satisfies every constraint in this document — and record the choice in code comments/README so it doesn't drift.
>
> **Refinement notes (read once, then ignore):** This revision does not remove or weaken any requirement from the original spec. It (a) pins exact library/runtime versions so "currently supported" isn't a moving target, (b) replaces every "reasonable"/"suitable"/"where appropriate" instruction that had a natural concrete default with that default, spelled out as a named, overridable config value, (c) fully specifies the mathematics for conflict detection, ETA, and distance so two engineers implementing independently produce the same numbers, (d) fully specifies wire formats (REST error envelope, WebSocket delta schema, ML request/response) byte-for-byte, and (e) adds a config reference table (Section 94) and a glossary (Section 95) so no term is implementation-ambiguous. Every numbered section from the original document is preserved; nothing was deleted.

---

# 1. PRODUCT DEFINITION

Build **AeroAI — AI-based Air Traffic Monitoring and Prediction System**.

AeroAI is a public, Flightradar-style aviation intelligence website focused on live air-traffic monitoring by major-city airspace, flight information, airport information, AI-based flight-delay prediction, basic mathematical conflict-risk detection, personalized flight tracking, alerts, and analytics.

The primary exploration model is **city-first airspace monitoring**: users choose a major city from a large searchable city catalogue, and AeroAI loads the configured airspace surrounding that city, centers the map on it, highlights relevant nearby airports, and tracks aircraft currently observed in that selected airspace. The city catalogue is reference data, not a hardcoded set of frontend buttons.

This is an **educational/research decision-support platform**. It is not an official ATC system, TCAS/collision-avoidance system, certified aviation safety system, airline operations system, or source of official aviation instructions. This framing must appear:

- In the footer of every page (persistent, short form): *"AeroAI is an educational research platform. Not an official ATC, TCAS, or airline system. Data may be delayed or unavailable."*
- In an expandable "About / Disclaimer" section reachable from the footer, containing the full text of Section 15's disclaimer rules (see Rule 16 in Section 89).
- Inline next to any AI-generated or calculated value the first time it appears in a given panel (e.g., a small "AI estimate" or "AeroAI calculated" tag next to the value itself, not just a global disclaimer).

The product must be useful without registration. Authentication exists only for personalization. No feature described in Section 2.1 may ever be placed behind a login wall during implementation, including via a "soft paywall," teaser, or blurred-preview pattern.

---

# 2. NON-NEGOTIABLE SCOPE

## 2.1 Public functionality

Anyone, with zero accounts and zero cookies beyond what's functionally required, can:

- Open the website.
- Explore live traffic.
- Choose a major city/airspace and view traffic around that city.
- Search flights.
- View flight details.
- Search and view airports.
- View airport arrivals and departures.
- Browse/search the supported major-city airspace catalogue.
- View relevant weather.
- View AI delay prediction when prediction data is available.
- View conflict-risk information when calculable.
- View analytics.

## 2.2 Authenticated functionality

An account is required only to:

- Track/save a flight.
- View My Flights.
- Receive alerts for tracked flights.
- Access/manage the user's account.

## 2.3 Explicitly out of scope

Do NOT implement, and reject any future instruction that reintroduces, the following without an explicit, written change to this file:

- RBAC.
- Admin roles.
- ATC roles/dashboards.
- Airport-manager roles.
- Analyst roles.
- Runway assignment.
- Runway recommendation.
- Audit-log systems.
- Complex report generation.
- Email notifications.
- Paid notification services.
- Chatbot/AI assistant.
- Generic AI-generated aviation prose.
- Booking.
- Payments.
- Airline management.
- Unnecessary ML models (i.e., any model beyond the two described in Section 32).
- Unnecessary microservices (i.e., any service beyond: frontend, Spring Boot backend, FastAPI ML service, PostgreSQL, Redis).
- OAuth / social login (Section 39 pins email+password only).
- Push notifications requiring a third-party push service (browser Notifications API + in-app panel only — see Section 43).
- Server-side rendering / SSR frameworks (Vite SPA only — see Section 8).

If a request during implementation would add any item on this list, stop and flag it instead of proceeding.

---

# 3. CORE PRODUCT PRINCIPLES

## 3.1 Never fabricate data

Never invent an airline, flight number, callsign, aircraft type, registration, origin, destination, schedule, ETA, altitude, speed, heading, weather value, historical position, delay value, or conflict event.

If reliable data is unavailable, display exactly the string:

> **Unavailable**

(Not "N/A", not "--", not "0", not an empty cell — the literal word "Unavailable" so it is unambiguous across the whole UI. Style it distinctly, e.g. muted/italic gray text, so it never reads as a real value at a glance.)

If a calculation cannot be performed reliably (insufficient inputs, inputs outside validity bounds, stale beyond threshold), display **Unavailable** rather than a best-effort guess, a zero, or a cached stale value silently reused past its freshness window.

## 3.2 Data categories

Maintain this three-way distinction in the domain model (DTOs, not just documentation) so the UI can label things correctly and testing can verify no category leaks into another.

### External data (sourced as-is, normalized only)

- OpenSky live aircraft/flight state data.
- Open-Meteo weather data.
- Airport reference data (static reference dataset — see Section 59).

### Calculated data (AeroAI-derived via deterministic math, not ML)

- Flight trail.
- Distance travelled.
- Distance remaining.
- AeroAI ETA estimate.
- Normalized flight status.
- Conflict risk.
- Relative movement.
- Rate of closure.
- Closest-approach estimates.
- Traffic statistics.
- Data freshness.

### AI-generated data (produced by the two ML models in Section 32)

- Predicted delay in minutes.
- Probability of >15-minute delay.
- Delay category.

**UI labeling convention** (binding): External data gets no badge. Calculated data gets a small "Calculated" or "Est." affordance (tooltip: "Calculated by AeroAI from live data"). AI-generated data gets an "AI" badge (tooltip: "Machine-learning estimate, not an airline guarantee"). Never mix badges on the same value.

---

# 4. VISUAL IDENTITY

## 4.1 Brand

Visible product name: **AeroAI**

Subtitle: **AI-based Air Traffic Monitoring and Prediction System**

Do not use "AATMP" as visible branding anywhere in the UI, page titles, or meta tags. It may appear only in internal code comments/repo docs if useful for the team.

## 4.2 Design direction

Use a:

- Dark aviation/radar aesthetic.
- Black/very dark charcoal base — pin as `#0A0E12` (base background) / `#12171D` (elevated surface).
- Cyan/blue primary accents — pin as `#22D3EE` (primary cyan) / `#3B82F6` (secondary blue), used for interactive elements, live indicators, and primary CTAs.
- Small touches of dark green — pin as `#14532D`/`#22C55E` used sparingly (e.g., "on ground"/"landed"/low-risk states), never as the dominant color.
- Clean and minimal visual language.
- Professional aviation-software feel (think: dense, data-forward, monospace figures for numeric telemetry).
- Subtle animations/transitions (150–250ms ease, no bouncy easing, no parallax gimmicks).

Semantic state colors (pin these, use consistently everywhere the same concept appears):

```text
LOW risk / on-time / landed      → green   #22C55E
MEDIUM risk / minor delay        → amber   #F59E0B
HIGH risk / major delay / error  → red     #EF4444
Stale / delayed data             → amber   #F59E0B
Unavailable                      → gray    #6B7280
```

Do NOT make it look like a generic AI SaaS website. Avoid:

- Purple AI gradients.
- Excessive neon.
- Excessive glow.
- Rainbow UI.
- Excessive glassmorphism.
- Giant decorative effects.
- Robot/brain AI branding/iconography.

Typography: use a single geometric/humanist sans-serif for UI text (e.g., Inter or system-ui stack) and a tabular-figure monospace or tabular-lining numeral style for all numeric telemetry (altitude, speed, coordinates) so digits align in tables. Do not use a display/decorative font.

## 4.3 Logo

Create a simple aviation/radar-inspired AeroAI logo using an aircraft silhouette, radar arc, flight path, or minimal AeroAI mark, in the pinned primary cyan on the dark base. Provide it as an SVG (scalable, theme-color-driven via CSS variables where practical) plus exported PNG favicons at 16×16, 32×32, 180×180 (apple-touch-icon), and a maskable 512×512 for PWA-style home-screen use. Keep it professional — no gradients beyond a two-stop subtle cyan-to-blue if used at all.

---

# 5. GLOBAL NAVIGATION

Primary navigation:

```text
Map | Flights | Airports | Analytics | My Flights
```

There is **no Weather navigation item** and **no separate Weather page**. Weather is supporting data only (Section 31).

The header must contain, left to right on desktop:

1. AeroAI logo (links to `/`).
2. Main navigation (the five items above).
3. Global search (Section 26), always visible on desktop, collapsible to an icon-triggered overlay on mobile.
4. Login/Register buttons when logged out, OR: notification bell (Section 43.3) + account menu (avatar/email initial) when logged in.

Header height fixed and unobtrusive (≤64px desktop, ≤56px mobile), sticky on scroll only on non-Map pages (the Map page's own overlay controls take priority — see Section 13.1).

---

# 6. HOMEPAGE

The homepage should immediately feel like a live aviation platform rather than a corporate landing page. It must load and become interactive quickly — treat the hero live-traffic element as the largest above-the-fold element, not a static screenshot.

Include:

- AeroAI branding (logo + subtitle).
- Global search, front and center or in the header — reachable without scrolling.
- Login/register (header).
- Prominent **Explore Live Traffic** CTA linking to `/map`.
- A strong live-traffic visual/experience directly on the homepage: either (a) an embedded, live, real (not decorative/fake) mini version of the map showing actual current aircraft over India, or (b) a live-updating counter/strip of real current traffic stats (aircraft count, active flights) pulled from the same backend endpoints as Analytics — never a static illustration or stock photo standing in for "live."
- Supporting sections (short, scannable, each with a link to the relevant page) for:
  - Live flight tracking.
  - Flight intelligence (search + details).
  - AI delay prediction.
  - Conflict-risk detection.
  - Airport information.
  - Analytics.

The live traffic experience is the identity of the product — it is the single most important thing on the page, visually and in terms of load priority.

---

# 7. RESPONSIVE BEHAVIOR

## Desktop

Desktop (≥1024px) is the primary target and gets full information density (side panels, dense tables, multi-column layouts).

## Mobile

The application must still fully function on phones (down to 360px width). It does not require a separate native mobile application.

On mobile (< 768px, with a defined tablet transitional range 768–1023px):

- Collapse navigation into a hamburger/bottom-nav pattern appropriately; five primary items must remain reachable within one tap.
- Keep search usable: a full-width search entry point, results in a full-screen overlay.
- Keep map usable: pinch-zoom, tap-to-select, no controls stacked on top of each other.
- Aircraft remain selectable via tap with an adequate hit target (minimum 32×32px effective touch area even if the rendered icon is smaller).
- Desktop side panels (flight details, conflict details) become bottom sheets or full-screen overlays with a clear close affordance.
- Tables (Flights page, Arrivals/Departures) may horizontally scroll with a visible scroll affordance, or convert to stacked compact cards — pick one pattern and use it consistently across all tables, do not mix.
- Login/register remains usable as full-width forms.
- My Flights remains usable as a card list.
- Account remains usable.
- Analytics remains readable: charts stack vertically, KPI cards go to a 2-column grid.
- Do not cause page-level horizontal overflow anywhere (verify with a real device or emulator at 360px, 390px, 414px widths as part of Section 76 tests).

---

# 8. TECHNOLOGY STACK

Pin exact major.minor versions below. Patch versions should track the latest patch of the pinned minor at implementation time (security patches only — do not jump minors without re-verifying compatibility across the stack).

## Frontend

- React **18.3.x**
- TypeScript **5.4.x** (preferred; if plain JS is used instead, all guidance below still applies minus type annotations)
- Vite **5.x** (SPA build, no SSR)
- Tailwind CSS **3.4.x**
- Leaflet **1.9.4**
- React Leaflet **4.2.x** (the React 18-compatible major version) as the React/Leaflet integration
- Charting: **Recharts** (React-native, free, lightweight — satisfies "lightweight charting library" from the original spec) for all Analytics charts (Section 44.5)
- State/data fetching: React Query (TanStack Query) **5.x** for REST caching/loading/error state; plain React context or Zustand for small global UI state (auth session, selected filters) — do not introduce Redux for this scope
- Routing: React Router **6.x**

## Backend

- Java **21** (LTS)
- Spring Boot **3.3.x**
- Spring Web
- Spring Data JPA (with Hibernate as provided by Spring Boot's managed version)
- Spring Security **6.x** (for authentication/authorization — see Section 39/40/50)
- Spring WebSocket (STOMP over WebSocket, or raw WebSocket — pin **STOMP over SockJS/WebSocket** for built-in broadcast/topic semantics, since the live feed is a broadcast to many subscribed clients)
- PostgreSQL JDBC driver (`org.postgresql:postgresql`, latest stable matching PostgreSQL 16)
- Spring Data Redis (Lettuce client, Spring Boot managed version)
- JWT library: `io.jsonwebtoken:jjwt` (jjwt-api/impl/jackson) latest **0.12.x**
- Password hashing: **BCrypt** via Spring Security's `BCryptPasswordEncoder` (strength 10, configurable)
- Build tool: **Maven** (or Gradle if the team prefers — pick one and use it consistently; examples below assume Maven)

## ML

Use the existing Python service, pinned:

- Python **3.11.x**
- FastAPI **0.111.x**
- Uvicorn **0.30.x** (standard extras)
- Pandas **2.2.x**
- NumPy **1.26.x** (compatible with the pinned pandas/scikit-learn/xgboost combination — verify at install time with `pip check`)
- Scikit-learn — pin to the **exact version the existing `.pkl` artifacts were trained/serialized with** (record this in `ml/api/requirements.txt`; loading a joblib/pickle model with a mismatched scikit-learn minor version is a common silent-breakage risk — verify by loading and running a smoke prediction against a known input/output pair before deployment)
- XGBoost — same rule: pin to the version the models were trained with
- Joblib **1.4.x**

## Persistence/cache

- PostgreSQL **16.x**
- Redis **7.2.x**

## External APIs

- OpenSky Network REST API (Section 18)
- Open-Meteo API (Section 31)

## Infrastructure

- Docker **24.x+** / Docker Compose **v2** (the `docker compose` CLI plugin syntax, not legacy `docker-compose`)

Use mutually compatible, currently supported versions. Prefer stable/LTS runtimes rather than bleeding-edge releases. Do not introduce a dependency solely because it is fashionable. Before finalizing any version pin above, do a quick compatibility check (e.g. Spring Boot 3.3.x + Java 21 + Spring Security 6.x is a known-good combination as of this spec's writing) since these ecosystems move quickly.

---

# 9. ARCHITECTURE

Use a **modular monolith Spring Boot backend** plus the separate Python ML service. Do not split every feature into a separate microservice — one Spring Boot deployable, one FastAPI deployable, that's it.

The frontend must communicate only with Spring Boot. It must never call OpenSky, Open-Meteo, or FastAPI directly, and must never hold PostgreSQL/Redis credentials.

```text
React + Tailwind + Leaflet
            |
            v
      Spring Boot API
       /     |      \
      /      |       \
 OpenSky Open-Meteo FastAPI ML
      |       |       |
      +-------+-------+
              |
       Redis + PostgreSQL
```

More specifically:

```text
                         +------------------+
                         |    OpenSky API   |
                         +--------+---------+
                                  |
                             ~15 sec polling
                                  |
                                  v
                    +--------------------------+
                    |       Spring Boot        |
                    |                          |
                    | Auth                     |
                    | Flights                  |
                    | Airports                 |
                    | Weather                  |
                    | Tracking                 |
                    | Alerts                   |
                    | Conflict                 |
                    | Analytics                |
                    | OpenSky                  |
                    | Open-Meteo               |
                    | Prediction client        |
                    | WebSocket                |
                    +------+------------+------+
                           |            |
                    +------v---+   +---v----------+
                    |  Redis   |   | PostgreSQL   |
                    | Live     |   | Users        |
                    | state    |   | Tracking     |
                    | Trails   |   | Alerts       |
                    | Cache    |   | History      |
                    | Pred.    |   | Airports     |
                    +-----+----+   | Predictions  |
                          |        +--------------+
                          |
                       WebSocket
                          |
                          v
                 +---------------------+
                 | React + Leaflet     |
                 +---------------------+

                         Spring Boot
                              |
                              | REST
                              v
                     +----------------+
                     | FastAPI ML     |
                     | XGBoost        |
                     | Random Forest  |
                     +----------------+
```

React must never directly access:

- OpenSky.
- Open-Meteo.
- Redis.
- PostgreSQL.
- FastAPI.

Enforce this at the network level too where practical (Docker network isolation: only the backend service is on the same network as `postgres`, `redis`, and `ml`; the frontend/reverse-proxy is the only thing exposed and it only talks to `backend`).


---

# 10. FRONTEND STRUCTURE

Use a maintainable structure similar to:

```text
frontend/
├── src/
│   ├── app/                 # app shell, providers, router config
│   ├── components/
│   │   ├── layout/          # header, footer, nav, page shells
│   │   ├── map/             # Leaflet map, markers, trails, filters, controls
│   │   ├── flights/         # flight table, flight card, side panel, search results
│   │   ├── airports/        # airport list, airport detail, arrivals/departures table
│   │   ├── analytics/       # KPI cards, charts
│   │   ├── alerts/          # notification bell, panel, item
│   │   └── common/          # buttons, inputs, badges, empty/loading/error states
│   ├── pages/
│   │   ├── Home/
│   │   ├── Map/
│   │   ├── Flights/
│   │   ├── FlightDetails/
│   │   ├── Airports/
│   │   ├── AirportDetails/
│   │   ├── Analytics/
│   │   ├── MyFlights/
│   │   ├── Account/
│   │   ├── Login/
│   │   └── Register/
│   ├── hooks/                # useWebSocket, useLiveAircraft, useAuth, useTrail, ...
│   ├── services/             # thin wrappers per domain (flightsService, authService, ...)
│   ├── api/                  # low-level fetch/axios client, interceptors, error normalization
│   ├── store/                # auth/session state, UI-only global state
│   ├── types/                # TypeScript types mirroring backend DTOs (Section 48)
│   ├── utils/                # date/time formatting, geo math for client-side display only
│   └── styles/                # Tailwind config, theme tokens (Section 4.2 pinned colors)
└── ...
```

Optimize exact organization if needed while preserving separation of responsibilities: no component directly calls `fetch`/`axios` — everything goes through `services/` → `api/`. No page component contains derived-math logic that belongs on the backend (Sections 25, 36) — the frontend renders values the backend already computed; it does not re-derive ETA, distance, or conflict risk itself.

---

# 11. BACKEND STRUCTURE

Use a structure similar to:

```text
backend/
└── src/
    └── main/
        ├── java/
        │   └── com/aeroai/backend/
        │       ├── config/         # SecurityConfig, WebSocketConfig, RedisConfig, CorsConfig, SchedulingConfig
        │       ├── auth/           # User entity, AuthController, AuthService, JwtFilter, JwtService
        │       ├── flight/         # Flight domain: controller, service, repository (history), DTOs
        │       ├── airport/        # Airport reference data: controller, service, repository, DTOs
        │       ├── weather/        # Open-Meteo client, normalization, caching
        │       ├── tracking/       # TrackedFlight entity, controller, service, repository
        │       ├── alert/          # Alert entity, controller, service, repository, trigger logic
        │       ├── conflict/       # Conflict detection engine (Section 36), DTOs
        │       ├── analytics/      # Aggregation queries, controller, DTOs
        │       ├── opensky/        # OpenSky client, scheduled ingestion job, response mapping
        │       ├── openmeteo/      # Open-Meteo client (separate package from `weather` domain logic if useful)
        │       ├── prediction/     # FastAPI client, request/response mapping, cache
        │       ├── websocket/      # STOMP config, broadcast service, delta builder
        │       └── common/         # ApiError envelope (Section 46.1), exception handlers, geo-math utils, base entities
        └── resources/
            ├── application.yml
            ├── application-dev.yml
            ├── application-docker.yml
            └── db/migration/       # Flyway migrations (V1__init.sql, V2__..., etc.)
```

Use controllers, services, repositories, DTOs, clients, configuration, and exception handling appropriately: **controllers** validate input and translate to/from DTOs only; **services** hold business logic; **repositories** are Spring Data JPA interfaces, no business logic; **clients** (`opensky`, `openmeteo`, `prediction`) wrap external HTTP calls with timeouts, retries, and error translation so the rest of the codebase never sees raw external-API exceptions.

Use **Flyway** for schema migrations (free, standard with Spring Boot) rather than `hibernate.ddl-auto=update` in anything beyond local dev, so the schema in Section 22 has a versioned, reproducible history.

---

# 12. ML STRUCTURE

Preserve the existing ML deployment structure:

```text
ml/
├── api/
│   ├── main.py
│   └── requirements.txt
└── models/
    ├── final_delay_regression_model.pkl
    ├── final_delay_classification_model.pkl
    └── classification_threshold.pkl
```

Do not require training scripts/data/plots for deployment — the deployed `ml/` directory is exactly the two folders above plus a Dockerfile.

The FastAPI service must load models using paths relative to `main.py` (e.g. `Path(__file__).resolve().parent.parent / "models" / "final_delay_regression_model.pkl"`), not fragile current-working-directory assumptions (`open("models/...")` breaks depending on where `uvicorn` is invoked from). Load all three artifacts once at process startup (module-level or FastAPI `lifespan`/startup event), not per-request, and fail fast (process exits with a clear log message) if any artifact is missing or fails to deserialize — do not silently start a service that will 500 on every request.

---

# 13. MAP EXPERIENCE

The Map page is the primary product experience.

## 13.1 Layout

Use a full-screen Leaflet map (fills the viewport below the header, or edge-to-edge with the header as a floating overlay — pick one and apply consistently).

The map must dominate the screen — no more than ~15% of viewport width should be permanently occupied by non-map chrome on desktop (e.g. a filter rail), and on mobile, non-map chrome should be collapsible to near-zero.

Controls, search, filters, and panels must be positioned so they do not overlap each other or obstruct essential map functionality. Concretely: top-left = search/filter trigger, top-right = live-data indicator + zoom/reset/fullscreen/locate-me controls stacked vertically, right side (desktop) or bottom sheet (mobile) = flight details side panel when an aircraft is selected, so the search/filter cluster and the details panel never occupy the same screen region simultaneously.

## 13.2 Initial viewport

Default view: **India + enough surrounding region to show international traffic** — pin the default bounding box/center+zoom as a config value (Section 94), e.g. center `[22.0, 79.0]`, zoom `5` on a standard Leaflet zoom scale, tuned so most of the Indian subcontinent plus parts of the Middle East/Southeast Asia are visible on a typical 16:9 desktop viewport.

Allow free panning/zooming outside India — do not set `maxBounds` restricting the map region. Do not restrict the map to India.

## 13.2.1 City/Airspace selector UX

Provide a prominent **Choose Airspace** control on the Map page. It may be a searchable command menu, compact selector, or left/top overlay, but it must remain easy to access without covering the aircraft map.

The selector must support:

- Search by city name.
- Search by country name.
- Search by major-city aliases where the reference dataset contains them.
- A curated/popular subset for quick access, backed by the same complete reference dataset.
- Keyboard selection on desktop and a full-screen/bottom-sheet selector on mobile.
- A clear indication of the currently selected city.
- Loading state while the selected airspace is being prepared.
- Empty state when no city matches.
- An explicit error state if the selected city's airspace cannot be loaded.
- A clear transition when switching cities so aircraft from the previous airspace are not visually presented as traffic belonging to the new city.

When the selected city changes, the frontend must replace the active airspace context atomically: selected city metadata, map viewport, nearby-airport highlights, current-aircraft snapshot, filters, and WebSocket/live-update subscription state must all correspond to the same city. Do not briefly combine aircraft from two different city airspaces.

The selector should feel like the main entry point to the monitoring experience, not like a generic location dropdown.

### 13.2.2 City airspace model

Each supported major city/airspace reference record must contain at minimum:

```text
cityId
cityName
country
latitude
longitude
defaultZoom
lamin
lomin
lamax
lomax
```

It may additionally contain:

```text
displayRegion
aliases
nearbyAirportIds
```

`nearbyAirportIds` is a convenience/reference relationship only. Nearby airports must still exist in the authoritative `airports` table; never invent airport records.

The airspace bounding box must be explicit reference data. Do not dynamically invent a radius or bounding box from the city name in the frontend.

### 13.2.3 Nearby airports for the selected city

When a city is selected, highlight airports associated with that city's configured region/nearby-airport references. If no explicit relationship is seeded, the backend may derive nearby airports deterministically from airport coordinates and the city's configured geographic bounds/radius, but it must use real seeded airport records only.

Nearby airport markers must be visually distinct from aircraft markers and must not be confused with live aircraft.

Clicking a highlighted airport opens the existing airport detail flow or provides the existing **View Live Traffic Around Airport** action from Section 30.

## 13.3 Aircraft markers

Use aircraft-style icons (a simple rotatable plane silhouette SVG matching the brand cyan) rather than generic dots.

Where available show:

- Aircraft icon.
- Heading/orientation (icon rotated to match).
- Callsign (per the zoom rules in 13.4).
- Current position (marker location).

Rotate icon based on heading using a CSS `transform: rotate(Ndeg)` (or Leaflet's built-in icon rotation plugin/utility) applied to the icon element, with `0deg` = north-up matching the aircraft's actual heading value in degrees clockwise from true north.

If heading is unavailable, use a neutral orientation (icon pointing north, `0deg`) or the last reliable heading if one was recorded within the stale threshold (Section 57) — never interpolate/invent a heading.

## 13.4 Callsign labels

Use intelligent zoom behavior, with concrete zoom-level bands (Leaflet zoom levels, tune during implementation but start here):

- Zoom ≤ 6 ("zoomed out"): icons only, no text labels (avoids clutter over a whole subcontinent).
- Zoom 7–9 ("medium zoom"): show callsign labels only for the selected aircraft and any aircraft involved in an active conflict-risk pair (Section 37).
- Zoom ≥ 10 ("zoomed in"): show callsign labels for all visible aircraft, using label collision avoidance (skip/offset overlapping labels) so text doesn't overlap.

Keep the map clean at every zoom level — verify visually with a dense traffic scenario (50+ aircraft) as part of Section 76 tests.

## 13.5 Aircraft movement

Default external refresh target: approximately **15 seconds**, configurable via `OPENSKY_POLL_INTERVAL_SECONDS` (Section 49/94).

When previous/current positions are reliable (same aircraft identifier, timestamps within a plausible gap, no impossible jump per Section 56 validation), interpolate smoothly between the last two known points over the interval between updates (e.g. a linear or great-circle interpolation animated client-side over ~the polling interval, so motion looks continuous rather than a jump-cut every 15s).

When interpolation is unreliable (first sighting of an aircraft, a gap longer than ~2× the polling interval, or a validation failure), snap to the new position instead of animating a potentially misleading path.

Never interpolate across clearly invalid data (Section 56) — validate before animating, not after.

---

# 14. MAP TRAILS

Build trails from actually collected successive coordinates — never synthesize intermediate points beyond the interpolation described in 13.5 for *display smoothing*; the trail itself is the sequence of real observed points.

```text
P1 -> P2 -> P3 -> P4 -> P5
```

The user can choose visible trail duration:

- 5 minutes.
- 15 minutes.
- 30 minutes.
- 1 hour.

The backend/cache may hold more recent points as needed (Redis retains up to the maximum duration, 1 hour, per aircraft — see Section 20), but the UI shows only the selected interval by filtering client-side or requesting the specific window server-side (prefer server-side filtering via a query parameter, e.g. `GET /api/flights/{id}/trail?minutes=30`, so payload size scales with what's actually shown).

Validate each point before it enters the trail:

- Valid latitude (`-90..90`).
- Valid longitude (`-180..180`).
- Valid timestamp (not in the future beyond small clock-skew tolerance, not older than the point it's being appended after).
- Stable aircraft identifier (ICAO24 matches).
- No unnecessary duplicate point (skip if identical lat/lon/timestamp to the last stored point, or if movement since the last point is below a negligible-movement threshold *and* time since the last point is below a minimum spacing threshold — both configurable, e.g. skip if <50m movement and <5s elapsed).
- Reject obviously impossible jumps when appropriate: if implied ground speed between two consecutive points exceeds a generous sanity ceiling (e.g. ~1500 km/h, comfortably above any commercial aircraft's true airspeed, as a guard against corrupted coordinates) treat the newer point as a fresh trail start rather than connecting it to the prior point.

A new aircraft with insufficient collected history may show:

> Trail unavailable / collecting data

Never fabricate a trail (no drawing a straight line "guess" from origin airport to current position, for example).

---

# 15. MAP FILTERS

Required filters:

1. Aircraft type (from OpenSky's reported type/category where available; "Unavailable" bucket if not reported).
2. Airline / callsign (derived from callsign prefix or associated flight-level data where reliable).
3. Airport / region / selected airspace (filter to aircraft within a radius of a selected airport, or within the currently selected city's authoritative airspace bounding box).
4. Flight status (Scheduled / In Flight / Landed / Unknown — Section 24).
5. Tracked flights (logged-in only).

Filters combine logically (AND across filter categories, OR within multi-select values of the same category).

Example:

```text
Airline = Air India
Status = In Flight
Region = India
```

means all selected conditions apply.

Provide:

> Clear All

If logged out and Tracked Flights is selected:

> Sign in to view tracked flights.

with Login/Register actions inline, and do not silently ignore the filter selection — communicate why nothing changed.

Do not show a filter if reliable data for it does not exist (e.g., if aircraft-type data coverage is too sparse to be useful, hide or clearly mark that filter as limited rather than presenting a broken-feeling filter).

---

# 16. MAP CONTROLS

Provide, as always-visible or single-tap-reachable controls:

1. Zoom +.
2. Zoom -.
3. Reset/center map (returns to the selected city's configured viewport from 13.2; if no city is selected, uses `AIRSPACE_DEFAULT_CITY_ID`).
4. Choose Airspace (opens the major-city selector from Section 13.2.1).
5. Fullscreen (uses the Fullscreen API where supported, graceful no-op/hide on unsupported browsers).
5. Locate me (uses `navigator.geolocation`, centers map on user location if permission granted; if denied/unavailable, fail silently with no error toast spam — just don't move the map).
6. Traffic filters (opens the filter panel from Section 15).
7. Live-data indicator (Section 17).

There is **NO map-style toggle** (no satellite/street/dark-mode-only-for-map switcher — the map always uses the single pinned tile style, Section 73).

---

# 17. LIVE DATA INDICATOR

Compute freshness as `now - lastSuccessfulOpenSkyUpdateTimestamp` using the trusted backend timestamp (Section 65), not client clock guesses beyond formatting.

Show, when fresh (age ≤ `LIVE_THRESHOLD_SECONDS`, default 20s — slightly above the 15s poll interval to allow for normal jitter):

```text
LIVE
Updated 8s ago
```

If stale (age > `LIVE_THRESHOLD_SECONDS` but ≤ `UNAVAILABLE_THRESHOLD_SECONDS`, default 90s):

```text
DATA DELAYED
Last update 58s ago
```

If unavailable (age > `UNAVAILABLE_THRESHOLD_SECONDS`, or no successful update has ever occurred):

```text
LIVE DATA UNAVAILABLE
```

Never call stale data live — the frontend must compute this state from the real timestamp on every render tick (e.g. a 1-second ticking clock updating the "Xs ago" text), not cache the label at fetch time.


---

# 18. OPENSKY INGESTION

OpenSky is the primary live aircraft data source.

Centralize all OpenSky communication in Spring Boot, in a single scheduled job (`@Scheduled(fixedDelayString = "${opensky.poll-interval-seconds}000")` or equivalent), never triggered by an incoming browser request. Never make one OpenSky request per browser user — all connected clients share the same ingested, cached state.

## 18.1 Data

Use reliable fields when available, mapped from the OpenSky `states/all` (or bounding-box-scoped `states/all?lamin=...&lomin=...&lamax=...&lomax=...`) response vector:

- ICAO24 (index 0 — unique aircraft address, the stable identifier).
- Callsign (index 1, trimmed of trailing whitespace OpenSky pads it with).
- Latitude / Longitude (indices 6/5).
- Altitude — prefer barometric altitude (index 7); fall back to geometric altitude (index 13) only if barometric is null, and mark the source used internally for debugging.
- Ground speed / velocity (index 9).
- Heading / true track (index 10).
- Vertical rate (index 11).
- On-ground state (index 8, boolean).
- Last contact/update timestamp (index 4, `last_contact`, Unix epoch seconds).
- Country/origin information where available (`origin_country`, index 2).

Only use fields actually available — treat any null/missing field as `Unavailable` at the DTO level rather than defaulting to `0` (a `null` speed must never render as "0 km/h").

## 18.2 Aircraft vs flight identity

Do not treat every state vector as a complete commercial-flight record.

Distinguish:

### Aircraft identity (from the live state vector, always available if OpenSky returns the aircraft)

- ICAO24.
- Callsign.
- Registration/type where reliably available (OpenSky's state vector does not include registration/type directly — if this is sourced, it must come from a separate reliable reference join, e.g. a static ICAO24-to-registration/type reference table if the team builds one; otherwise mark Unavailable rather than guessing from callsign patterns).

### Flight information (higher-level, not guaranteed present for every aircraft)

- Origin.
- Destination.
- Flight number.
- Scheduled departure.
- Scheduled arrival.
- Other flight-level metadata.

Only associate flight-level information when reliable — if the callsign cannot be confidently mapped to a known route (no reliable schedule source is specified in this spec beyond what's derivable from OpenSky + the static airport reference data), show those fields as Unavailable rather than guessing from callsign prefix alone.

## 18.3 Polling

Use approximately 15 seconds (`OPENSKY_POLL_INTERVAL_SECONDS`, default `15`) as the default configurable ingestion interval.

Use OpenSky OAuth2 client-credentials authentication for authenticated access. Since March 18, 2026, OpenSky no longer supports Basic Auth with username/password for programmatic REST API access. The local development credentials are provided as the OpenSky `credentials.json` file containing `clientId` and `clientSecret`; do not copy this file into the repository and do not commit it. The backend should support a configurable credentials-file path (for local development) and environment-injected client ID/secret (for deployment), with the credentials-file approach taking precedence only when explicitly configured. Obtain and cache the OAuth2 access token server-side and refresh it before expiry; never expose the token or client secret to the frontend. Without credentials, anonymous requests may be supported at reduced limits. Keep the polling interval configurable and enforce provider rate/credit limits and 429 backoff rather than assuming unlimited authenticated access.

Cache shared results in Redis (Section 20) immediately after each successful fetch. Avoid duplicate external calls — a single scheduled job instance should hold a lock (e.g. a simple `@Scheduled` on a single backend instance, or a Redis-based distributed lock if the backend is ever horizontally scaled) so multiple backend instances don't each poll OpenSky independently.

## 18.4 Geographic focus and selected city airspace

Prioritize **major-city airspaces** rather than treating the entire India bounding box as the primary live-feed unit.

Each city in the airspace reference dataset has an explicit bounding box (`lamin`, `lomin`, `lamax`, `lomax`). OpenSky requests for the live map use the selected city's bounding box rather than requesting the entire global `states/all` payload.

The ingestion architecture must remain centralized: the browser never calls OpenSky and there is never one OpenSky request per browser user. The backend owns the active city-airspace ingestion/cache state.

Because a large city catalogue can contain more cities than the OpenSky request budget can support simultaneously, the system must **not** attempt to poll every catalogue city every 15 seconds. Instead:

- Maintain a bounded active-airspace set; the default configuration uses one active live city stream so the free API budget is not multiplied by the number of supported cities.
- Reuse cached/current state for cities that were recently ingested.
- When a city is selected, serve its most recent valid backend snapshot immediately when available.
- The backend may promote a requested city into the active ingestion slot according to a simple bounded scheduler/queue, subject to the configured OpenSky polling interval and provider rate limits.
- The scheduler must enforce a provider-request budget rather than assuming that `OPENSKY_POLL_INTERVAL_SECONDS` can be multiplied indefinitely across cities. If demand exceeds the free request budget, reduce refresh frequency/rotation and expose accurate freshness instead of making excess requests.
- Never create a separate external request for every connected user selecting the same city; requests for the same city must share one backend ingestion stream and Redis snapshot.
- If a selected city has no sufficiently recent successful snapshot, show the city normally but mark its traffic as `Unavailable`/delayed according to Section 17 rather than fabricating traffic.
- The scheduler must bound the number of simultaneously active city airspaces and document the chosen policy in the README.

The original India-focused bounding box remains available as a reference/default configuration where needed for broad analytics or fallback behavior, but it is no longer the primary Map-page live-airspace query.

Users may still pan/zoom outside the selected airspace on the map, but the backend is not obligated to ingest global live state to support that. Clearly document that aircraft outside the currently ingested city airspace may be absent/no live markers.

## 18.5 Failure handling

If OpenSky fails (network error, timeout, non-2xx response, malformed payload):

- Do not crash the ingestion job or the application.
- Preserve last valid state temporarily in Redis (do not clear it just because the current poll failed).
- Mark data stale (update an internal "last successful fetch" timestamp only on success, so Section 17's freshness calculation naturally reflects the failure).
- Show accurate freshness to the frontend via the existing timestamp mechanism.
- Retry automatically on the next scheduled interval; additionally apply a short exponential backoff (e.g. cap consecutive-failure backoff at 2× the normal interval) only if consecutive failures exceed a small threshold (e.g. 3), so a single transient blip doesn't change behavior but a sustained outage doesn't hammer OpenSky.

If stale for too long (exceeds `UNAVAILABLE_THRESHOLD_SECONDS`, Section 17), show live data unavailable.

Never silently present stale data as live.

---

# 19. REAL-TIME WEBSOCKET ARCHITECTURE

Use STOMP over WebSocket (with SockJS fallback for environments that block raw WebSocket) via Spring's WebSocket support.

Use a city/airspace-aware topic structure, e.g. `/topic/aircraft/{cityId}`, so clients receive deltas only for the currently selected airspace. The backend must publish each ingestion delta to the corresponding city topic. A small shared system topic may be used for global freshness/health metadata if needed, but aircraft traffic remains scoped by city airspace.

Initial load:

```text
React
  ↓
GET /api/flights?airspaceId={cityId} (current state, REST)
  ↓
Render map with current markers
  ↓
Open WebSocket / STOMP connection, subscribe to /topic/aircraft/{cityId}
  ↓
Receive incremental updates
```

After ingestion:

```text
OpenSky
  ↓
Spring Boot (scheduled job)
  ↓
Validate + normalize
  ↓
Redis (current state written)
  ↓
Delta computed (diff against previous cycle's state)
  ↓
Broadcast to /topic/aircraft
  ↓
React
```

Do not reload the entire page or map every 15 seconds. Prefer delta messages (full schema in Section 47) containing:

- Added aircraft.
- Updated aircraft.
- Removed aircraft.
- Relevant changed fields only for "updated" entries where practical (or the full current record per updated aircraft if diffing individual fields adds more complexity than it saves — either is acceptable, but pick one and keep the WebSocket payload schema in Section 47 as the contract either way).

Frontend identifies aircraft using stable identifiers (ICAO24) as the React key/map-marker key, so re-renders update in place rather than remounting.

## 19.1 WebSocket reconnect

If disconnected:

1. Detect connection loss (STOMP client `onWebSocketClose`/`onDisconnect` callback).
2. Show `Connection interrupted — reconnecting…` (Section 52).
3. Retry automatically with exponential backoff (e.g. start at 1s, double up to a cap of 30s, reset to 1s on successful reconnect).
4. On reconnect, immediately re-fetch current state via `GET /api/flights` (or an equivalent snapshot endpoint) to close any gap that occurred while disconnected, before resubscribing to the live topic.
5. Resume live updates.

No manual refresh should be required.

---

# 20. REDIS

Redis is the fast current-state/cache layer.

Use for:

- Current aircraft state.
- Recent trail points (up to the maximum selectable duration, 1 hour — Section 14).
- Cached prediction results.
- Short-lived aggregate/cache data where useful (e.g. a rolling counter of currently-visible aircraft for the homepage/analytics "live" stat, refreshed alongside ingestion rather than recomputed per request).

Conceptual keys, with concrete data structures and TTLs:

```text
aircraft:{airspaceId}:{icao24} → Redis HASH of the current normalized state fields; TTL 90s (UNAVAILABLE_THRESHOLD_SECONDS), refreshed on every successful ingestion cycle so an aircraft that stops reporting naturally expires from "current state" without a manual delete

airspace:{airspaceId}:meta → Redis HASH containing the selected airspace metadata and latest successful ingestion timestamp

airspace:{airspaceId}:aircraft → Redis SET/index of currently-live ICAO24 identifiers for that city airspace
trail:{airspaceId}:{icao24} → Redis Sorted Set (ZSET), score = timestamp (epoch seconds), member = JSON-encoded {lat, lon, timestamp}; trimmed via ZREMRANGEBYSCORE to drop members older than 3600s (1 hour) on each write
prediction:{flightOrAircraftId} → Redis STRING (JSON-encoded prediction DTO); TTL = PREDICTION_CACHE_TTL_SECONDS (default 900s / 15 min), invalidated early only if meaningful inputs change (Section 35)
```

Use TTLs/cleanup mechanisms as specified above so Redis self-cleans. Do not let Redis accumulate infinite historical data — the 1-hour ZSET trim and 90s aircraft-state TTL are the enforcement mechanism, not a background cleanup job for these particular keys (a background cleanup job is still needed for PostgreSQL history per Section 67).

Redis is not the only persistent copy of critical user information — user/tracking/alert data lives in PostgreSQL (Section 21); Redis holding a transient cache of it (if ever done for performance) must never be the sole source.

---

# 21. POSTGRESQL

PostgreSQL is the persistent data store.

Use for:

- Users.
- Tracked flights.
- Alerts.
- Airport reference data.
- Flight history (bounded to the 7-day retention window, Section 23).
- Relevant prediction history (a durable record of predictions made, distinct from the transient Redis prediction cache used for fast repeated reads).
- Persistent analytics source data (Analytics queries read from PostgreSQL's history/prediction/conflict-event tables, not from ephemeral Redis state, so numbers are stable and auditable within the retention window).

Do not use PostgreSQL as the primary mechanism for every 15-second live-map update — that traffic goes through Redis + WebSocket (Sections 19–20). PostgreSQL does receive periodic history writes (e.g. one row per aircraft per ingestion cycle, or downsampled — see Section 23) but it is not queried synchronously to render the live map.


---

# 22. DATABASE SCHEMA

Keep the schema lean and aligned with the actual scope. Column types below are PostgreSQL types; adapt naming to the team's JPA/Flyway conventions (snake_case columns shown, matching typical Flyway SQL migrations).

## 22.1 users

```sql
id              BIGSERIAL PRIMARY KEY
email           VARCHAR(255) NOT NULL UNIQUE
password_hash   VARCHAR(255) NOT NULL   -- BCrypt hash, never plaintext
created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
updated_at      TIMESTAMPTZ NOT NULL DEFAULT now()
```

Email must be unique (enforced via `UNIQUE` constraint, case-insensitively normalized to lowercase before storage/comparison).

## 22.2 tracked_flights

```sql
id                    BIGSERIAL PRIMARY KEY
user_id               BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
aircraft_identifier   VARCHAR(24) NOT NULL   -- ICAO24
callsign              VARCHAR(16)
tracked_at            TIMESTAMPTZ NOT NULL DEFAULT now()
active                BOOLEAN NOT NULL DEFAULT true
```

Use a foreign key to `users` with `ON DELETE CASCADE` so deleting a user cleans up their tracking rows. Index `(user_id, active)` for My Flights lookups and `(aircraft_identifier)` for reverse lookups (e.g. "is anyone tracking this aircraft" for alert fan-out).

Current live data must not depend on this stale snapshot — `callsign` here is a convenience/display cache captured at tracking time, not the source of truth for the flight's current callsign (which always comes from live/current state).

## 22.3 flight_history

```sql
id                    BIGSERIAL PRIMARY KEY
aircraft_identifier   VARCHAR(24) NOT NULL   -- ICAO24
callsign              VARCHAR(16)
"timestamp"           TIMESTAMPTZ NOT NULL
latitude              DOUBLE PRECISION NOT NULL
longitude             DOUBLE PRECISION NOT NULL
altitude              DOUBLE PRECISION
speed                 DOUBLE PRECISION
heading               DOUBLE PRECISION
vertical_rate         DOUBLE PRECISION
on_ground             BOOLEAN
status                VARCHAR(16)   -- SCHEDULED | IN_FLIGHT | LANDED | UNKNOWN
```

Index `(aircraft_identifier, "timestamp")` (composite, for trail/history range queries) and a standalone index on `"timestamp"` (for retention cleanup queries in Section 67).

## 22.4 predictions

```sql
id                              BIGSERIAL PRIMARY KEY
flight_or_aircraft_identifier   VARCHAR(24) NOT NULL
predicted_delay_minutes         DOUBLE PRECISION NOT NULL
delay_probability               DOUBLE PRECISION NOT NULL   -- 0.0–1.0
category                        VARCHAR(16) NOT NULL         -- Low | Moderate | High
created_at                      TIMESTAMPTZ NOT NULL DEFAULT now()
```

Do not store a prediction every 15 seconds unless genuinely necessary — write a row only when a new prediction is actually computed (i.e., on cache miss/refresh per Section 35), not on every read.

## 22.5 alerts

```sql
id                   BIGSERIAL PRIMARY KEY
user_id              BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE
tracked_flight_id    BIGINT REFERENCES tracked_flights(id) ON DELETE CASCADE
type                 VARCHAR(32) NOT NULL   -- e.g. STATUS_CHANGE | DELAY_CATEGORY_CHANGE | CONFLICT_RISK_CHANGE | DATA_UNAVAILABLE
message              TEXT NOT NULL
created_at           TIMESTAMPTZ NOT NULL DEFAULT now()
read                 BOOLEAN NOT NULL DEFAULT false
```

Index `(user_id, read, created_at DESC)` to power the notification panel (Section 43.3) efficiently.

## 22.6 airspaces

```sql
id              BIGSERIAL PRIMARY KEY
city_name       VARCHAR(255) NOT NULL
country         VARCHAR(255) NOT NULL
latitude        DOUBLE PRECISION NOT NULL
longitude       DOUBLE PRECISION NOT NULL
default_zoom    INTEGER NOT NULL
lamin           DOUBLE PRECISION NOT NULL
lomin           DOUBLE PRECISION NOT NULL
lamax           DOUBLE PRECISION NOT NULL
lomax           DOUBLE PRECISION NOT NULL
display_region  VARCHAR(255)
```

Use a unique constraint/index for the canonical city/country identity. Optional aliases and nearby-airport relationships may be stored in normalized reference tables or a simple seed/reference structure if needed; do not overengineer this dataset.

Airspace rows are static/slow-changing reference data, not user-generated data and not live aircraft state.

Seed the airspace catalogue with a genuinely broad set of major cities across the supported regions, not just the example/default city. The initial catalogue should be large enough to make the selector useful as a real city browser, while remaining a static reference dataset that can be expanded without changing frontend component logic. Do not invent airport or geographic metadata; every bounding box must be based on the seeded city reference data chosen for the project.

## 22.7 airports

```sql
id          BIGSERIAL PRIMARY KEY
name        VARCHAR(255) NOT NULL
iata        VARCHAR(3)
icao        VARCHAR(4)
city        VARCHAR(255)
"location"  VARCHAR(255)   -- free-text region/state, distinct from city if useful
country     VARCHAR(255)
latitude    DOUBLE PRECISION NOT NULL
longitude   DOUBLE PRECISION NOT NULL
```

Use suitable indexes/unique constraints: unique index on `iata` (where not null), unique index on `icao` (where not null), and a `pg_trgm`-backed (or simple `ILIKE`-friendly) index on `name`/`city` to support fast partial-match search (Section 26).

---

# 23. HISTORY AND RETENTION

Persistent flight history retention: **7 days** (`HISTORY_RETENTION_DAYS`, default `7`).

Use this for:

- Recent trails (the PostgreSQL `flight_history` table is the durable backstop; Redis's 1-hour ZSET is the fast path for the actively-displayed trail durations in Section 14).
- Flight history (e.g. "was this aircraft flying yesterday" style lookups if ever surfaced).
- Analytics ("Last 7 Days" range, Section 44).
- Completed-flight information.
- Relevant conflict calculations (a short recent window of history may inform closing-rate smoothing if instantaneous OpenSky velocity is noisy — optional refinement, not required for a correct v1).

Delete/archive records older than the retention window via a scheduled cleanup job (Section 67), e.g. a nightly `DELETE FROM flight_history WHERE "timestamp" < now() - interval '7 days'`.

Do not retain unlimited flight-position data.

Completed flight records should also be retained only within the defined 7-day history window — there is no separate "keep completed flights forever" carve-out.

---

# 24. FLIGHT STATUS

Normalize flight state into exactly these four values (stored/transmitted as an enum, e.g. `SCHEDULED | IN_FLIGHT | LANDED | UNKNOWN`):

## In Flight

Use reliable recent position + airborne state: `on_ground = false` (or absent-but-inferred-airborne from altitude/speed) AND the last state update is within the freshness threshold (Section 65).

## Landed

Use a combination of:

- On-ground state (`on_ground = true` reported by OpenSky), AND
- Recent observations confirming this over more than a single sample (require the on-ground state to persist across at least 2 consecutive successful polling cycles, e.g. ~30s, before flipping status to Landed — this guards against a single noisy/incorrect sample),
- Airport proximity where reliable (optional corroborating signal: current/last position within a configurable radius, e.g. 5km, of a known airport from the reference table),
- State transitions (the aircraft was previously In Flight and has now met the on-ground criteria above — a status can only become Landed via a transition from In Flight or Unknown, never fabricated from a first-ever sighting that happens to already be on-ground with no prior airborne evidence, which more likely indicates the aircraft was always parked/taxiing and unrelated to a tracked flight event).

Do not mark landed merely because one update disappears (see Section 57 — disappearance and Landed are different things and must not be conflated).

## Scheduled

Reserved for flight-level records where a schedule is reliably known but the aircraft has not yet appeared in live state (only assignable where the product actually has a reliable schedule source per Section 18.2 — if no such source exists for a given flight, do not force a Scheduled status; use Unknown instead).

## Unknown

If evidence is insufficient for any of the above (first sighting with ambiguous state, prolonged data gaps not yet past the stale-removal threshold, contradictory signals), use Unknown rather than guessing.

---

# 25. DERIVED FLIGHT DATA

All derived data in this section is computed server-side (Spring Boot), never re-derived independently by the frontend, so there is exactly one source of truth for the numbers shown.

## 25.1 Current position

Show:

- Latitude.
- Longitude.
- Altitude.
- Speed.
- Heading.
- Vertical rate.
- Last update.

Each individually falls back to `Unavailable` if its underlying OpenSky field is null.

## 25.2 Distance travelled

Calculate from actual observed trajectory segments using the **haversine great-circle formula** between each consecutive pair of stored trail/history points for the current flight (since the aircraft was first observed in the current flight "session" — i.e., since the last transition into In Flight status):

```text
distance(P1,P2) + distance(P2,P3) + distance(P3,P4) + ...

haversine(p1, p2):
  R = 6371.0088  km (mean Earth radius)
  φ1, φ2 = lat1, lat2 in radians
  Δφ = (lat2 - lat1) in radians
  Δλ = (lon2 - lon1) in radians
  a = sin²(Δφ/2) + cos(φ1) * cos(φ2) * sin²(Δλ/2)
  c = 2 * atan2(√a, √(1-a))
  distance = R * c
```

Sum this across the actually-recorded segments. If fewer than 2 valid points exist for the current flight session, distance travelled is `Unavailable`.

## 25.3 Distance remaining

When destination is reliable (Section 18.2's flight-information reliability bar), calculate the haversine great-circle distance (same formula as 25.2) from the current position to the destination airport's reference coordinates (Section 59).

Label as an estimate:

```text
Distance remaining: ~420 km
```

Round to the nearest 10km (or similar sensible precision) and always prefix with `~` to visually reinforce it's an estimate. Do not claim it is the exact airline flight-path distance (real routes follow airways, not great circles, and this is disclosed via the "Calculated" badge from Section 3.2 plus a tooltip: *"Straight-line estimate, not the actual flight path."*).

## 25.4 Estimated arrival time

Where enough information exists (reliable destination + reliable current position + a usable current/recent speed value that is non-zero and airborne), calculate an AeroAI ETA:

```text
remaining_distance_km = haversine(current_position, destination)          -- Section 25.3
effective_speed_kmh   = current ground speed, OR a short trailing average
                        (e.g. last 3–5 valid speed samples) if the
                        instantaneous value is noisy/near-zero while
                        still airborne
eta_hours             = remaining_distance_km / effective_speed_kmh
eta_timestamp         = now() + eta_hours (converted to duration)
```

Display as an AeroAI estimate, not official airline ETA (badge + tooltip per Section 3.2). Clamp implausible results (e.g. `effective_speed_kmh` below a sane minimum threshold like 50 km/h while airborne, indicating noisy/invalid data) to `Unavailable` rather than displaying a wildly large ETA.

If unreliable (any required input missing or fails the clamp above):

> ETA unavailable

---

# 26. FLIGHT SEARCH

Provide one universal global search system backing the header search box, results grouped by type.

Search flights by:

- Callsign.
- Flight identifier/number where available.
- ICAO24.
- Aircraft registration where available.

Search airports by:

- Name.
- IATA.
- ICAO.
- City.

Search major airspaces/cities by:

- City name.
- Country.
- Configured aliases.

Search airlines/callsigns where reliable (matches airline-name substrings against known callsign-prefix-to-airline mappings if such a reference exists; otherwise this search dimension degrades gracefully to callsign-prefix matching only).

Search behavior:

- Case-insensitive (normalize query and indexed fields to a common case, e.g. via `ILIKE` in PostgreSQL or a lowercased comparison against the live Redis aircraft set).
- Trim whitespace from the query before matching.
- Partial matches (substring, not just prefix, for name/city fields; prefix-preferred for callsign/ICAO/IATA codes since those are short structured codes).
- Exact matches prioritized (rank exact matches above partial matches in the combined result ordering).
- Fast — target sub-200ms server response for typical queries against the current in-memory/Redis aircraft set plus indexed PostgreSQL airport table; debounce the frontend request (Section 53) at ~250–300ms after the last keystroke.
- Duplicate-free (de-duplicate by stable identifier before returning).

Group results where useful:

```text
AIRSPACES
FLIGHTS
AIRPORTS
AIRCRAFT
```

Clicking a result navigates to the appropriate object (`/flights/{id}` or `/airports/{id}`).

Do not fabricate search results — an empty/no-match query returns an explicit empty state (Section 52), never a "closest guess."

---

# 27. FLIGHTS PAGE

The Flights page is: **Search + filters + dense aviation-style table.**

Possible columns:

```text
CALLSIGN
AIRLINE
FROM
TO
STATUS
ALTITUDE
SPEED
DELAY RISK
LAST UPDATED
```

Only show columns with reliable data for the current dataset — if, e.g., `AIRLINE` is Unavailable for the overwhelming majority of visible rows because flight-level association coverage is low, still show the column (each cell independently shows `Unavailable`) rather than removing it entirely, since removing it would be inconsistent from page load to page load as data coverage fluctuates; the per-cell `Unavailable` convention (Section 3.1) is the mechanism, not column removal.

Rows are clickable, navigating to the flight's detail panel/page.

Use compact status badges (Section 4.2 semantic colors) and readable numerical formatting (tabular figures, consistent unit suffixes — km/h, ft, etc., configurable to a single unit system rather than mixing metric/imperial inconsistently across the app).

Provide clear filter controls (same filter set/behavior as Section 15, applied to the tabular view instead of the map) and `Clear All`.

Support pagination or virtualized scrolling for the table (do not render an unbounded DOM list if hundreds of rows are possible) — a page size of 50 rows with standard pagination controls is a reasonable default.

---

# 28. FLIGHT DETAILS

Clicking an aircraft/flight immediately opens a side panel on desktop (no extra click to "expand" — the panel opens with the summary already populated).

The application may additionally provide a dedicated flight route/page (`/flights/{id}`) for deep linking (Section 61) — the side panel and the dedicated page render the same DTO/component, just in different layout containers.

Show, where available:

- Callsign / flight identifier.
- Airline.
- Origin.
- Destination.
- Departure time.
- Arrival time.
- Estimated arrival.
- Current position.
- Altitude.
- Speed.
- Heading.
- Aircraft information.
- Status.
- Distance travelled.
- Distance remaining.
- Weather.
- Last updated.
- Flight trail.
- Track Flight button.
- Conflict risk.
- AI delay prediction.

If unavailable:

> Unavailable

Suggested panel structure:

```text
FLIGHT HEADER
callsign / identifier
airline
status

ROUTE
origin → destination

CURRENT
position
altitude
speed
heading

PROGRESS
distance travelled
distance remaining
estimated arrival

WEATHER
relevant current conditions

AI DELAY PREDICTION
predicted delay
probability
category
why prediction?

CONFLICT RISK
risk
nearby aircraft
calculated values

ACTIONS
Track Flight / Untrack Flight
```

Each section collapses gracefully to just its header + "Unavailable" if none of its fields have data, rather than showing a section full of individually-Unavailable rows when the whole section is empty (reduces visual noise while still being honest — this is a presentation choice, not a data-fabrication one).


---

# 29. AIRPORTS

Provide:

1. Searchable airport directory (`/airports`).
2. Individual airport detail pages (`/airports/{id}`).

Search by:

- Airport name.
- IATA.
- ICAO.
- City.

Same search-behavior rules as Section 26 apply.

---

# 30. AIRPORT DETAILS

Show:

- Airport name.
- IATA code.
- ICAO code.
- Location.
- Country.
- Coordinates.
- Current weather.
- Live arrivals.
- Live departures.

Use tabs:

```text
ARRIVALS | DEPARTURES
```

Arrivals = aircraft whose reliably-known destination matches this airport and whose status is In Flight or recently Landed within the retention window; Departures = the analogous set for reliably-known origin. Both lists are inherently limited by the flight-information reliability constraints in Section 18.2 — if coverage is sparse, show fewer rows honestly rather than padding with guesses.

Use the same dense aviation-table design language as Flights (Section 27).

Provide:

> **View Live Traffic Around Airport**

This centers/zooms the map (navigates to `/map` with the view centered on the airport's coordinates at a zoom level tight enough to show local traffic, e.g. zoom 9–10, configurable).

---

# 31. WEATHER

There is **NO separate Weather page**. Weather is supporting data only.

Primary source: **Open-Meteo** (no API key required for the free tier, which satisfies the ₹0 budget constraint in Section 72 — verify current Open-Meteo terms of use before relying on this at scale).

Use weather for:

- Airport details (current conditions at the airport's coordinates).
- Flight details where relevant (weather at origin/destination and/or current position, as applicable to what's being shown).
- ML prediction inputs where applicable (Section 33's optional weather fields).

Potential fields (map from Open-Meteo's current-weather response):

- Temperature.
- Weather condition (Open-Meteo's WMO weather code, translated to a short human-readable label via a small static lookup table maintained in the backend).
- Wind speed/direction.
- Humidity.
- Visibility.
- Pressure.
- Cloud cover.
- Precipitation.

Only show available fields (Open-Meteo's free endpoint may not return every field above depending on the exact API called — use the `current` parameter set that includes as many of these as the free tier supports, and treat any field it doesn't return as Unavailable).

Cache weather responses (e.g. per-airport, TTL ~10–15 minutes, since weather doesn't change as fast as aircraft position) to avoid redundant calls for the same airport across many concurrent users — this is a Spring-side cache (e.g. Caffeine in-memory cache, or a Redis key `weather:{airportId}` with TTL), not a per-request live call.

If unavailable:

> Weather: Unavailable

Do not turn AeroAI into a weather application — no forecasts, no multi-day outlook, no weather maps/radar overlays; current conditions only, in service of flight/airport context.

---

# 32. ML DELAY PREDICTION

The ML service and artifacts already exist. Do not retrain the models during normal application implementation.

Expected files:

```text
ml/models/final_delay_regression_model.pkl
ml/models/final_delay_classification_model.pkl
ml/models/classification_threshold.pkl
```

## 32.1 Regression model

Final regression model: **XGBoost**

Purpose: Predict `arrival_delay_minutes`.

Reported selected-model evaluation from project experimentation:

```text
MAE  ≈ 20.358 minutes
RMSE ≈ 32.343 minutes
R²   ≈ 0.259
```

These are evaluation results, not guaranteed live accuracy — surface them only in developer-facing documentation/README, not as a live-updating metric in the product UI (the UI's "why this prediction?" explanation, Section 34.1, may reference them qualitatively but should not imply they update per-prediction).

## 32.2 Classification model

Final classification model: **Random Forest**

Purpose: Predict probability that arrival delay exceeds 15 minutes.

Classification threshold:

```text
0.45
```

(i.e., `delay_over_15_minutes = delay_probability >= 0.45`)

Reported evaluation:

```text
Accuracy  ≈ 0.718
Precision ≈ 0.545
Recall    ≈ 0.701
F1        ≈ 0.613
AUC       ≈ 0.783
```

These are evaluation results, not guarantees.

## 32.3 Category

Category comes from the regression output; do not create a third model.

```text
0–15 min   → Low
16–45 min  → Moderate
46+ min    → High
```

Normalize/clamp impossible negative displayed predictions to a sensible non-negative value (`max(0, predicted_delay_minutes)`) before categorization and before display, since a regression model can technically output a small negative number that has no physical meaning as "negative delay."

---

# 33. ML API CONTRACT

FastAPI endpoint:

```text
POST /predict
Content-Type: application/json
```

Input (exact field names and types the FastAPI Pydantic model must accept):

```json
{
  "from_airport": "string (IATA or ICAO code)",
  "to_airport": "string (IATA or ICAO code)",
  "airline": "string",
  "distance": "number (km)",
  "passenger_load_factor": "number | null (optional, 0.0-1.0)",
  "airline_rating": "number | null (optional)",
  "airport_rating": "number | null (optional)",
  "market_share": "number | null (optional, 0.0-1.0)",
  "scheduled_date": "string (YYYY-MM-DD)",
  "scheduled_departure": "string (HH:MM, 24h)",
  "scheduled_arrival": "string (HH:MM, 24h)",
  "windspeed": "number | null (optional)",
  "weather_description": "string | null (optional)",
  "precipitation": "number | null (optional)",
  "humidity": "number | null (optional)",
  "visibility": "number | null (optional)",
  "pressure": "number | null (optional)",
  "cloudcover": "number | null (optional)"
}
```

Required fields that are missing/null from the Spring Boot caller must cause FastAPI to return a `422 Unprocessable Entity` (standard FastAPI/Pydantic validation error) rather than a guessed default; the *optional* fields marked above may be omitted/null and the model must handle their absence via the same imputation/handling strategy used at training time (documented in the ML artifacts' own training notes — do not invent a new imputation strategy at inference time that wasn't used at training time, since that would silently change model behavior).

The FastAPI service derives features including:

```text
Day
Month
DayOfWeek
IsWeekend
DepartureHour
DepartureMinute
ArrivalHour
ArrivalMinute
DepartureHourSin
DepartureHourCos
DayOfWeekSin
DayOfWeekCos
DeparturePeriod
```

computed deterministically from `scheduled_date` + `scheduled_departure`/`scheduled_arrival` (standard cyclical encoding: `sin(2π·hour/24)`, `cos(2π·hour/24)`, `sin(2π·dow/7)`, `cos(2π·dow/7)`; `DeparturePeriod` a bucketed label such as Morning/Afternoon/Evening/Night from `DepartureHour`).

Do not duplicate model feature engineering incorrectly in the frontend or in Spring Boot — feature derivation happens exactly once, inside FastAPI, immediately before calling the model's `.predict()`.

## 33.1 Output

Expected output:

```json
{
  "predicted_delay_minutes": 42.07,
  "delay_probability": 0.5587,
  "delay_over_15_minutes": true,
  "category": "Moderate"
}
```

`predicted_delay_minutes` is the clamped-non-negative regression output (Section 32.3). `delay_probability` is the classification model's raw probability for the positive class (delay > 15 min). `delay_over_15_minutes` is the boolean threshold application (`>= 0.45`). `category` is derived per Section 32.3's bands from `predicted_delay_minutes`.

## 33.2 ML responsibility

FastAPI handles only:

- Model loading.
- ML input validation.
- Feature preparation.
- Regression.
- Classification.
- Category derivation.
- Prediction response.

It must not handle:

- OpenSky.
- Open-Meteo.
- PostgreSQL.
- Redis.
- Authentication.
- Users.
- Tracking.
- Alerts.
- WebSockets.

FastAPI should expose a simple unauthenticated `GET /health` endpoint (returns `{"status": "ok", "models_loaded": true}`) purely for Spring Boot's own health-check/circuit-breaker logic (Section 51) — this is infrastructure, not a scope violation of "ML-only."

---

# 34. ML UX

Flight details should show a compact prediction:

```text
AI Delay Prediction
42 min
Moderate
55.9% probability of >15 min delay
```

Also provide an expanded prediction section containing:

- Predicted delay.
- Probability.
- Category.
- Honest short explanation.
- Decision-support/AI label.

Do not invent a separate confidence score beyond `delay_probability` — there is exactly one probability value in the contract (Section 33.1), and the UI must not present a second, fabricated "confidence %" alongside it.

Do not make the product look like an AI chatbot (no chat bubble UI, no "Ask AI" framing, no conversational tone in the prediction copy — it reads like an instrument readout, consistent with Section 4.2's aviation-software direction).

## 34.1 Why prediction

Use an expandable section such as:

> Why this prediction?

It may explain that the model uses feature groups including:

- Scheduled departure/arrival.
- Route.
- Airline characteristics.
- Distance.
- Available airport/airline features.
- Weather-related features.

Do not claim a specific feature caused a prediction unless the implementation actually performs appropriate explainability analysis (e.g. SHAP values) — if no explainability computation is implemented, this section stays at the general feature-group level shown above and must not present a fabricated per-feature attribution.

## 34.2 Prediction availability

If required information is missing (the flight lacks reliable enough data to populate the required `POST /predict` fields per Section 33):

> AI Delay Prediction: Unavailable

---

# 35. ML REFRESH/CACHE

Do not call FastAPI every 10–15 seconds because the aircraft position changed — position has no bearing on the regression/classification inputs in Section 33 (none of those inputs are live position/speed/altitude).

Cache prediction results in Redis (`prediction:{flightOrAircraftId}`, Section 20) with TTL `PREDICTION_CACHE_TTL_SECONDS` (default 900s).

Recalculate when meaningful prediction inputs change (e.g. the flight's associated schedule/route/weather-at-departure-or-arrival changes materially) or when the cache TTL expires — whichever comes first.

Live aircraft position and ML prediction have different cadences: position updates every ~15s via the live pipeline; predictions update on a ~15-minute cadence or on meaningful-input-change, via the prediction pipeline. Keep these two update loops implemented as clearly separate code paths so it's obvious in review that one never accidentally triggers the other.

---

# 36. CONFLICT-RISK DETECTION

This is a basic mathematical/research decision-support feature. It is NOT TCAS, official collision avoidance, certified ATC, or an official safety warning — this must be stated adjacent to every conflict-risk UI element (Section 37) and in the disclaimer (Rule 16, Section 89).

## 36.1 Required inputs

Use a combination of:

- Horizontal geographic distance.
- Altitude separation.
- Heading.
- Relative velocity.
- Rate of closure.
- Predicted near-future separation.
- Time to closest approach where feasible.

Do not implement conflict detection as only a simple distance threshold.

## 36.2 Recommended mathematical approach

Full worked formulas (these are the concrete instantiation of the original spec's numbered steps 1–8):

**Step 1 — Horizontal separation (haversine).** Use the same haversine formula as Section 25.2 between aircraft A and B's current positions to get `horizontal_distance_km`.

**Step 2 — Vertical separation.** `altitude_diff_m = abs(altitude_A - altitude_B)` (convert units consistently; OpenSky altitude is in meters).

**Step 3 — Local tangent-plane velocity representation.** Convert each aircraft's `(speed, heading)` into a local East-North (EN) velocity vector so relative motion can be computed with ordinary vector subtraction rather than spherical trigonometry at every step:

```text
v_east  = speed * sin(heading_radians)
v_north = speed * cos(heading_radians)
```

(heading measured clockwise from true north, standard aviation convention, converted to radians for the trig functions).

**Step 4 — Relative position and velocity.** Convert the horizontal offset between A and B into approximate local Cartesian meters using an equirectangular approximation valid over the short distances relevant to a conflict check (tens of km):

```text
Δlat_m = (lat_B - lat_A) * 111320                                   -- meters per degree latitude, ~constant
Δlon_m = (lon_B - lon_A) * 111320 * cos(avg_lat_radians)             -- meters per degree longitude, latitude-corrected

relative_position = (Δlon_m, Δlat_m)          -- (east, north) meters, B relative to A
relative_velocity  = (v_east_B - v_east_A, v_north_B - v_north_A)   -- m/s, (east, north)
```

**Step 5 — Time to closest approach (TCPA).** Standard relative-motion result, constrained to a reasonable future horizon (`CONFLICT_PREDICTION_HORIZON_SECONDS`, default 300s / 5 min — beyond this horizon a "conflict" prediction is too speculative to be useful):

```text
tcpa = - (relative_position · relative_velocity) / |relative_velocity|²

  where "·" is the 2D dot product and |relative_velocity|² is the squared magnitude.
  If |relative_velocity| ≈ 0 (both aircraft moving identically / on parallel tracks
  at the same speed), TCPA is undefined — treat current separation as the
  ongoing separation (no convergence) rather than dividing by ~zero.

Clamp: if tcpa < 0 (closest approach was in the past — diverging now) or
tcpa > CONFLICT_PREDICTION_HORIZON_SECONDS, do not treat this pair as
converging within the relevant window; use current separation only.
```

**Step 6 — Distance at closest approach (DCPA).**

```text
position_at_tcpa = relative_position + relative_velocity * tcpa     -- meters, (east, north)
dcpa_horizontal_m = |position_at_tcpa|                               -- 2D magnitude
```

Vertical separation at TCPA can be approximated using each aircraft's current vertical rate projected forward by `tcpa` seconds (`altitude_A_at_tcpa = altitude_A + vertical_rate_A * tcpa`, similarly for B), then `altitude_diff_at_tcpa_m = abs(...)`. If vertical rate is unavailable for either aircraft, hold altitude constant (`vertical_rate = 0`) for that aircraft's projection rather than treating the whole calculation as Unavailable — this is still a meaningfully cautious estimate.

**Step 7 — Risk score/level.** Combine current separation, predicted (TCPA/DCPA) separation, altitude difference, closure rate, and time-to-closest-approach into a configurable risk score. A concrete, config-driven scoring approach:

```text
closure_rate_ms = -(d/dt of horizontal_distance) ≈
    (relative_position · relative_velocity) / |relative_position|     -- negative = closing, positive = opening

risk_level = HIGH   if dcpa_horizontal_m <= CONFLICT_HIGH_HORIZONTAL_M
                     AND altitude_diff_at_tcpa_m <= CONFLICT_HIGH_VERTICAL_M
                     AND 0 <= tcpa <= CONFLICT_PREDICTION_HORIZON_SECONDS

           MEDIUM if dcpa_horizontal_m <= CONFLICT_MEDIUM_HORIZONTAL_M
                     AND altitude_diff_at_tcpa_m <= CONFLICT_MEDIUM_VERTICAL_M
                     AND 0 <= tcpa <= CONFLICT_PREDICTION_HORIZON_SECONDS

           LOW    otherwise (including "currently close but diverging", i.e.
                     closure_rate_ms >= 0, or tcpa outside the horizon)
```

with default configurable thresholds (Section 94):

```text
CONFLICT_HIGH_HORIZONTAL_M     = 3000     (3 km)
CONFLICT_HIGH_VERTICAL_M       = 150
CONFLICT_MEDIUM_HORIZONTAL_M   = 9000     (9 km)
CONFLICT_MEDIUM_VERTICAL_M     = 300
CONFLICT_PREDICTION_HORIZON_SECONDS = 300
```

These pinned defaults are illustrative starting points for a *research/decision-support* feature, deliberately more conservative (larger) than real ATC separation minima, and must be presented as such (Rule 16) — they are not, and must never be described as, official separation standards.

**Step 8 — Spatial candidate filtering.** Before running Steps 1–7 pairwise, restrict candidate pairs using a coarse spatial index (e.g. a geohash/grid bucket of ~0.5°×0.5° per cell, or a simple k-d tree over current lat/lon) so only aircraft within a generous candidate radius (e.g. `CONFLICT_CANDIDATE_RADIUS_KM`, default 50km) of each other are ever passed into the full pairwise math. This avoids naive global O(N²) comparisons when many aircraft are present (Section 38).

## 36.3 Risk levels

Use exactly:

```text
LOW
MEDIUM
HIGH
```

The exact thresholds are configurable per Section 94's config table. Do not claim that the thresholds are official aviation separation minima.

### LOW

Aircraft sufficiently separated with no meaningful near-term convergence (per the formula in 36.2 Step 7).

### MEDIUM

Aircraft are close and/or converging enough to warrant research/decision-support attention.

### HIGH

Aircraft have small predicted separation and meaningful convergence within the prediction horizon.


---

# 37. CONFLICT UI

On the map:

- Highlight involved aircraft (e.g. an amber/red ring around each marker in the pair, matching the risk-level color from Section 4.2).
- Use a subtle connecting indicator (a thin dashed line between the two markers, same semantic color, low opacity so it doesn't dominate the map).
- Avoid global map clutter — only render conflict highlighting for pairs currently at MEDIUM or HIGH risk; LOW-risk pairs are not visually indicated on the map at all (they're implicitly "everything else").
- Clicking involved aircraft opens relevant details (the flight details panel, scrolled/expanded to the Conflict Risk section).

Flight details may show:

```text
Conflict Risk: MEDIUM
Potential conflict with: ABC123
Horizontal distance: ...
Altitude difference: ...
Closing/convergence: ...
Closest approach estimate: ...
Time to closest approach: ...
Last calculated: ...
```

mapping directly to the values computed in Section 36.2 (`horizontal_distance_km`, `altitude_diff_m`, `closure_rate_ms`, `dcpa_horizontal_m`/`altitude_diff_at_tcpa_m`, `tcpa`, and the calculation's own timestamp).

Unavailable calculations display `Unavailable` (e.g. if one aircraft in a would-be pair has stale/incomplete state).

---

# 38. CONFLICT COMPUTATION EFFICIENCY

Run conflict calculations once per ingestion cycle on normalized live updates (i.e., as a step in the pipeline in Section 55), not on frontend renders and not per-API-request.

Use spatial bucketing/grid/geospatial indexing (Section 36.2 Step 8) or another reasonable neighbor-selection strategy to reduce unnecessary pair comparisons.

Avoid naive global O(N²) comparisons when many aircraft are present — with the India-focused bounding box (Section 18.4) aircraft counts are expected to be in the low hundreds at most, so a simple grid-bucket prefilter followed by pairwise math on same-bucket-or-adjacent-bucket candidates is sufficient; no specialized geospatial database (e.g. PostGIS) is required for this scope, though it remains an acceptable future optimization if traffic volume grows.

Only compare plausible candidates based on geographic/altitude proximity (the `CONFLICT_CANDIDATE_RADIUS_KM` prefilter) and a reasonable prediction horizon (`CONFLICT_PREDICTION_HORIZON_SECONDS`).

---

# 39. AUTHENTICATION

Use only: **Email + password**

No OAuth.

Required:

- Register.
- Login.
- Logout.
- Secure password hashing (BCrypt, Section 8).
- Authenticated token/session handling: stateless JWT (Section 8's `jjwt` library), issued on login/register, sent by the client as `Authorization: Bearer <token>` on every authenticated request. Access token TTL default `JWT_EXPIRY_MINUTES = 60` (configurable, Section 94). No refresh-token flow is required for this scope's complexity level — on expiry, the frontend simply prompts re-login; this is an acceptable simplification consistent with "no complex profile management."
- Basic validation (email format via a standard regex/validator; password minimum length 8 characters, at least one letter and one number — pin this as the concrete rule rather than leaving "basic validation" undefined).

## 39.1 Register

Fields:

- Email.
- Password.
- Confirm password if appropriate (recommended: yes, client-side check that both fields match before submit, in addition to server-side validation of the single password value).

Validate email and password per the rules above. Return `409 Conflict` (via the standard error envelope, Section 46.1) if the email is already registered — do not leak whether an email exists via a different, inconsistent error shape elsewhere (e.g. login) beyond the intentionally generic message in 39.2.

## 39.2 Login

Fields:

- Email.
- Password.

Show clear but safe errors: a single generic message such as "Invalid email or password" for both "no such user" and "wrong password" cases, so the endpoint does not reveal account existence (a standard, low-effort security practice appropriate to this scope).

## 39.3 Account

Show:

- Email.
- Account creation date.
- Number of tracked flights.
- Basic account information.
- Logout.

No complex profile management (no password reset flow, no profile photo, no email change — all explicitly out of scope per the "no complex profile management" instruction; if password reset is desired later, that is a scope change requiring an update to this document).

---

# 40. USER DATA SECURITY

Authenticated users must only access their own:

- Tracked flights.
- Alerts.
- Account information.

Do not trust a user-supplied user ID to authorize access. Determine authenticated identity on the backend from the validated JWT's subject claim (Spring Security's `Authentication` principal), and scope every tracking/alert/account query by that identity server-side — never accept a `userId` path/query/body parameter as the authorization boundary for these endpoints (Section 46's tracked-flights/alerts endpoints operate implicitly on "the current user," e.g. `GET /api/me/flights`, not `GET /api/users/{id}/flights`).

Passwords must never be logged or stored plaintext — verify no logging statement (including debug-level) ever includes the raw request body of the register/login endpoints.

---

# 41. TRACK FLIGHT

Viewing flights is public. Tracking requires login.

Button:

```text
＋ Track Flight
```

After tracking:

```text
✓ Tracking
```

Untracking:

```text
Untrack Flight
```

Tracking must:

1. Persist the relationship to the authenticated user (`tracked_flights` row, Section 22.2).
2. Make the flight appear in My Flights.
3. Enable meaningful alerts (Section 43).
4. Continue showing current live information when available (the tracked-flight row is a relationship, not a data snapshot — display always joins back to current live/derived state by `aircraft_identifier`).

If an unauthenticated user clicks Track:

> Create an account to track flights.

Provide Login/Register actions inline in that prompt (a modal or inline panel — do not silently redirect without explanation).

---

# 42. MY FLIGHTS

My Flights is the personalized tracked-flight dashboard (`/my-flights`, requires auth — Section 61).

Each tracked flight should show:

- Callsign.
- Origin → destination.
- Current status.
- Delay prediction.
- Delay category.
- Last update.
- View Flight.
- Untrack.

Current information should come from current live state, not a stale tracking-time snapshot (join `tracked_flights.aircraft_identifier` against current Redis/derived state at render/API time).

If a tracked aircraft temporarily disappears from OpenSky, do not delete the tracking relationship. Show:

> Data temporarily unavailable

If no flights are tracked:

> No tracked flights yet.

Provide:

> Explore Live Traffic

(linking to `/map`).

---

# 43. BROWSER/IN-APP ALERTS

Alerts are a personalization layer for tracked flights, not a general notification-management system.

V1 supports: **Browser/in-app notifications only.** No email notifications. "Browser notification" means the standard Web Notifications API (`Notification.requestPermission()` / `new Notification(...)`), triggered client-side when a new unread alert arrives while the tab is open (via the same WebSocket/polling channel that also updates the in-app bell) — this requires no third-party push service and satisfies the ₹0 budget constraint; it does not work when the tab/browser is fully closed, which is an acceptable v1 limitation (do not attempt to add a service-worker-based push notification system, which would require a push provider and is out of scope per Section 2.3).

## 43.1 Alert triggers

Meaningful events may include:

- Flight status changes (Section 24 transitions, e.g. In Flight → Landed).
- Delay category changes (Section 32.3 category, e.g. Low → Moderate).
- Significant predicted-delay changes (e.g. `predicted_delay_minutes` change exceeding a configurable threshold, `ALERT_DELAY_CHANGE_THRESHOLD_MINUTES`, default 15, even without a category change).
- Flight data unavailable for an extended period (the tracked aircraft has been absent from live state beyond the stale-removal threshold, Section 57).
- Meaningful conflict-risk changes (Section 43.2's LOW/MEDIUM/HIGH transition rule, applied to any pair involving a tracked aircraft).

Do not alert for every position update.

## 43.2 Deduplication

Example:

```text
LOW → LOW
```

No alert.

```text
LOW → MODERATE
```

Alert.

```text
MODERATE → HIGH
```

Alert.

Avoid repeated identical notifications within a short configurable window (`ALERT_DEDUP_WINDOW_SECONDS`, default 300s) — track the last-alerted state per `(tracked_flight_id, alert_type)` and suppress a re-fire of the same type/state combination within that window even if the underlying trigger condition re-evaluates true on each cycle.

## 43.3 Notification UI

Logged-in header:

```text
🔔 2
```

(badge count = unread alert count for the current user, fetched via `GET /api/alerts` or a lightweight unread-count endpoint, and kept live via the same WebSocket connection or a short poll interval, e.g. every 30s, if a dedicated alert WebSocket channel is not implemented — either is acceptable as long as the count doesn't require a full page refresh to update).

Click opens a compact notification panel.

Each notification contains:

- Flight.
- Event.
- Short message.
- Timestamp.
- Read/unread state.

Clicking opens the relevant flight and marks the notification read (`PATCH /api/alerts/{id}/read`).

Provide:

> Mark all as read

(`PATCH /api/alerts/read-all`, scoped to the current authenticated user only per Section 40).

No separate large notification-management page.


---

# 44. ANALYTICS

Analytics (`/analytics`, public) supports exactly two primary ranges and may optionally be scoped to a selected city/airspace:

- **Today** (from local midnight of the current day, in the server's configured reporting timezone — pin `ANALYTICS_TIMEZONE`, default `Asia/Kolkata` given the India focus, so "Today" has an unambiguous boundary — through now).
- **Last 7 Days** (rolling 7×24h window ending now, matching `HISTORY_RETENTION_DAYS`, Section 23, so the range never claims data the system doesn't actually retain).

Where an airspace is selected, live traffic KPIs and historical analytics should use that airspace as the filter. A clearly labeled `All active airspaces` view may be retained for system-level health/aggregate statistics, but the product should not imply that AeroAI has complete global traffic coverage.

Do not build a huge BI platform — no custom date-range picker, no drill-down/export, no saved views.

## 44.1 Live traffic statistics

Include where data supports it:

- Total aircraft currently visible.
- Aircraft by altitude range (e.g. banded: 0–10,000ft, 10,000–30,000ft, 30,000ft+, Unavailable).
- Aircraft by status (Scheduled/In Flight/Landed/Unknown counts).
- Aircraft by region (coarse buckets, e.g. by the same bounding-box quadrants used for candidate filtering, or simply "within India" vs "surrounding region" if a finer breakdown isn't reliably derivable).
- Arrivals/departures activity (counts over the selected range, from `flight_history`/status-transition records, not live-only state).

## 44.2 Flight statistics

Include:

- Flights monitored.
- Active flights.
- Completed flights.
- Average predicted delay.
- Low / Moderate / High predicted-delay distribution.

Do not call predicted delay actual delay — every label in this section must read "predicted" or "AI-predicted," never "delay" unqualified, per Section 3.2's AI-data badge convention.

## 44.3 Conflict statistics

Include:

- Potential conflicts.
- Low/Medium/High risk distribution.
- Recent conflict-risk events.

Clearly label as AeroAI calculated indicators (Section 3.2's "Calculated" badge convention, plus the Rule 16 disclaimer).

## 44.4 System health

Include:

- Last OpenSky update (timestamp + freshness label per Section 17's rules).
- Data freshness.
- API/data availability (OpenSky/Open-Meteo/FastAPI up-or-degraded status, derived from the same failure-tracking used in Section 51 — a simple traffic-light per external dependency is sufficient, no historical uptime graph required).
- Number of tracked flights (aggregate count across all users — this is a system stat, not a per-user list, so it does not violate Section 40's per-user data isolation).

## 44.5 Visualization

Use:

- KPI cards (numeric headline stats).
- Simple line charts (Recharts `LineChart`, e.g. traffic-over-time for the selected range).
- Bar charts (Recharts `BarChart`, e.g. delay-category distribution).
- Donut/pie charts only where useful (Recharts `PieChart`, e.g. status distribution) — do not force a pie chart where a bar chart communicates the same distribution more legibly (more than ~5 categories should generally be a bar chart, not a pie chart).
- Tables (for the "recent conflict-risk events" list, etc.).

No decorative charts with no information value.

---

# 45. TIME HANDLING

Store timestamps in UTC internally where practical (PostgreSQL `TIMESTAMPTZ` columns as specified in Section 22 already do this correctly regardless of session timezone; Redis-stored timestamps should be Unix epoch seconds, which are inherently timezone-free).

Convert for display appropriately: the frontend renders timestamps in the user's browser-local timezone by default (via standard `Intl.DateTimeFormat` / `date-fns`), except for the Analytics "Today" boundary, which is fixed to `ANALYTICS_TIMEZONE` (Section 44) server-side regardless of the viewing user's browser timezone, since "Today" needs one authoritative boundary shared by all users of a public page.

Be explicit about timezone where ambiguity exists (e.g. always render full ISO-8601 with an explicit offset, or a clearly labeled "IST"/local-timezone suffix, rather than a bare "14:32" with no timezone context, anywhere the value could otherwise be ambiguous — flight departure/arrival times in particular).

Do not mix server time, browser local time, and UTC without conversion — every timestamp crossing the API boundary (Section 46) is UTC ISO-8601 (`2026-09-05T09:15:30Z`) on the wire; conversion to a display timezone happens only at the final render step in the frontend.

---

# 46. API CONTRACTS

Use REST for ordinary requests. All endpoints are prefixed `/api`.

Conceptual endpoints:

```text
POST   /api/auth/register
POST   /api/auth/login
POST   /api/auth/logout

GET    /api/airspaces
GET    /api/airspaces/{cityId}

GET    /api/flights?airspaceId={cityId}
GET    /api/flights/{id}
GET    /api/flights/search
GET    /api/flights/{id}/trail?minutes={5|15|30|60}

GET    /api/airports
GET    /api/airports/{id}
GET    /api/airports/search

GET    /api/me
GET    /api/me/flights

POST   /api/tracked-flights
DELETE /api/tracked-flights/{id}

GET    /api/alerts
PATCH  /api/alerts/{id}/read
PATCH  /api/alerts/read-all

GET    /api/analytics?range={today|7d}
```

Exact endpoint naming may be refined during implementation, but responsibilities must remain equivalent.

Use DTOs rather than exposing persistence entities directly (Section 48).

## 46.1 Error envelope (binding wire format)

Every non-2xx JSON error response uses this exact shape, produced by a single centralized `@ControllerAdvice`/`@ExceptionHandler` in the `common` package (Section 11), so the frontend has exactly one error shape to parse:

```json
{
  "timestamp": "2026-09-05T09:15:30Z",
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Flight not found.",
  "path": "/api/flights/abc123"
}
```

`error` is a short, stable machine-readable code (`VALIDATION_ERROR`, `UNAUTHORIZED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR`, `UPSTREAM_UNAVAILABLE`) the frontend can branch on without parsing `message` text. `message` is a short human-readable string safe to show to end users (never a raw exception message or stack trace — Section 50 "Errors").

---

# 47. WEBSOCKET CONTRACT

Use a dedicated WebSocket/STOMP topic (`/topic/aircraft`, Section 19) for live traffic.

Messages should be normalized AeroAI data rather than raw OpenSky responses.

A delta message uses exactly this shape:

```json
{
  "timestamp": "2026-09-05T09:15:45Z",
  "added": [
    {
      "icao24": "a1b2c3",
      "callsign": "AIC101",
      "latitude": 19.0912,
      "longitude": 72.8656,
      "altitude": 10972.8,
      "speed": 245.3,
      "heading": 128.4,
      "verticalRate": -1.2,
      "onGround": false,
      "status": "IN_FLIGHT",
      "lastUpdate": "2026-09-05T09:15:44Z"
    }
  ],
  "updated": [ /* same shape as added */ ],
  "removed": [ "b4c5d6" ]
}
```

`added`/`updated` entries use the full per-aircraft DTO shape shown above (the "relevant changed fields only" optimization mentioned in Section 19 is an acceptable alternative implementation, but if adopted, the schema must clearly distinguish a partial-update entry from a full entry, e.g. via a `"partial": true` flag, so the frontend merge logic is unambiguous — the simplest compliant default is to always send the full entry). `removed` is a flat array of ICAO24 strings for aircraft that dropped out of live state (Section 57).

Each aircraft record has a stable identifier (`icao24`) and only includes fields relevant to the current application (no raw/unused OpenSky fields passed through verbatim).

Exact serialization (e.g. field naming convention, camelCase vs snake_case) can be refined, but the semantic contract above must remain, and Section 48's TypeScript types must mirror whichever casing is chosen.

---

# 48. DTO REQUIREMENTS

Create purpose-built DTOs for:

- Airspace/city list.
- Airspace/city details.
- Flight list.
- Flight details.
- Airport.
- Weather.
- Prediction.
- Conflict risk.
- User/account.
- My Flights.
- Alerts.
- Analytics.
- WebSocket aircraft updates.

Do not expose JPA entities directly — every controller method returns/accepts a DTO class in the relevant domain package's `dto` sub-package, mapped to/from entities via a small mapper (a hand-written mapper method or MapStruct, either acceptable) so persistence-layer changes don't silently change the API contract.

Mirror every DTO with a matching TypeScript type in `frontend/src/types/`.

---

# 49. ENVIRONMENT VARIABLES

Never hardcode secrets.

Potential configuration (full reference table with defaults is in Section 94):

```text
DATABASE_URL
DATABASE_USERNAME
DATABASE_PASSWORD

REDIS_HOST
REDIS_PORT
REDIS_PASSWORD

OPENSKY_CREDENTIALS_FILE
OPENSKY_CLIENT_ID
OPENSKY_CLIENT_SECRET
OPENSKY_OAUTH_TOKEN_URL

ML_SERVICE_URL

JWT_SECRET
JWT_EXPIRY_MINUTES

OPENSKY_POLL_INTERVAL_SECONDS
HISTORY_RETENTION_DAYS
TRAIL_RETENTION_MINUTES
AIRSPACE_DEFAULT_CITY_ID
AIRSPACE_MAX_ACTIVE_CITY_COUNT
AIRSPACE_REQUEST_CACHE_TTL_SECONDS
```

For local development, `OPENSKY_CREDENTIALS_FILE` may point to the existing external credentials file (for example `C:\Coding\AATMP\opensky_api_creds\credentials.json`). Do not copy, rename, embed, or commit that file into the project. For deployment, prefer secret/environment injection via `OPENSKY_CLIENT_ID` and `OPENSKY_CLIENT_SECRET`. The application must also run correctly at a reduced rate limit when authenticated credentials are unset.

Frontend configuration must contain only values safe to expose publicly (e.g. `VITE_API_BASE_URL`, `VITE_WS_URL`, `VITE_MAP_TILE_URL`) — never a database/Redis/JWT secret, since anything in a Vite `import.meta.env.VITE_*` variable is bundled into the public JS.

---

# 50. SECURITY

Implement baseline production-quality security appropriate to the project.

## Passwords

- Secure password hashing (BCrypt, strength 10).
- Never plaintext.
- Never log passwords.

## Authentication

Protect personalized endpoints (`/api/me/**`, `/api/tracked-flights/**`, `/api/alerts/**`) with a Spring Security filter chain requiring a valid JWT; all other `/api/**` endpoints listed in Section 46 remain public per Section 2.1.

## Authorization

Authenticated users can only access their own tracked flights, alerts, and account data (Section 40).

## Validation

Validate:

- Email.
- Password.
- IDs (numeric/UUID format as applicable, existence checked before use).
- Search parameters (length limits, e.g. max 100 chars, to prevent abuse).
- Filter values (must match an allowed enum/known value set, reject unknown values with `VALIDATION_ERROR` rather than silently ignoring them).
- Request bodies (Bean Validation `@Valid`/`@NotNull`/`@Size` annotations on all DTOs accepting user input).

## CORS

Configure intentionally: allow only the known frontend origin(s) (configurable via an env var, e.g. `CORS_ALLOWED_ORIGINS`), not a wildcard `*`, especially once cookies/credentials are involved.

## Secrets

Never expose credentials/secrets in frontend bundles or API responses.

## SQL

Use JPA/parameterized queries. Never concatenate untrusted user input into SQL (this rules out building search queries via string concatenation even for the `ILIKE` partial-match search in Section 26 — use JPA `@Query` with bound parameters or the Criteria API).

## Errors

Do not return:

- Stack traces.
- Internal filesystem paths.
- Database credentials.
- Secrets.
- Sensitive internal implementation details.

Use the consistent safe API error envelope from Section 46.1 everywhere (log the full exception server-side; return only the sanitized envelope to the client).

---

# 51. ERROR HANDLING / GRACEFUL DEGRADATION

Every external service can fail. Implement each with a short HTTP client timeout (e.g. 5s connect / 10s read for OpenSky and Open-Meteo, 3s connect / 8s read for the internal FastAPI call) so one slow dependency never hangs an entire request thread pool.

## OpenSky fails

- Keep application alive.
- Preserve last valid state temporarily (Section 18.5).
- Mark stale.
- Retry.
- Eventually show unavailable.

## Open-Meteo fails

- Weather unavailable.
- Flight tracking remains usable (the flight-details response still returns fully, with `weather: null`/Unavailable, rather than the whole request failing).

## FastAPI fails

- Flight details still load.
- Prediction says unavailable (catch the client exception in the `prediction` package's client wrapper, return an "unavailable" prediction DTO rather than propagating a 5xx up to the flight-details response).

## Redis fails

- Degrade gracefully: reads fall back to "no current live data" (Unavailable) rather than throwing; the scheduled ingestion job logs the failure and retries next cycle rather than crashing the JVM.
- Persistent data remains in PostgreSQL.
- Recover automatically where possible (Lettuce's default reconnect behavior is sufficient; no custom reconnection logic required beyond ensuring calling code doesn't crash on a `RedisConnectionException`).

## PostgreSQL fails

- Return meaningful errors (`UPSTREAM_UNAVAILABLE` or `INTERNAL_ERROR` via Section 46.1, not a raw stack trace).
- Do not crash the entire application (a PostgreSQL outage should not take down endpoints that don't need it, e.g. the live map/WebSocket path, which depends on Redis, not PostgreSQL, for its hot path).

## WebSocket fails

- Show reconnecting state (Section 19.1).
- Automatically reconnect.


---

# 52. FRONTEND LOADING/EMPTY/ERROR STATES

Every major screen must have explicit states, implemented as a small shared set of components in `components/common/` (`<LoadingState>`, `<EmptyState>`, `<ErrorState>`) reused everywhere rather than ad hoc per-page markup.

## Loading

Examples:

```text
Loading live traffic…
Loading flight…
Loading airport…
```

Use subtle skeletons/spinners (skeleton preferred for list/table views so layout doesn't jump; spinner acceptable for small inline elements).

## Empty

```text
No aircraft found for the current filters.
No matching flights or airports found.
No tracked flights yet.
```

## Weather unavailable

```text
Weather: Unavailable
```

## Prediction unavailable

```text
AI Delay Prediction: Unavailable
```

## Live data delayed

```text
LIVE DATA DELAYED
Last update: ...
```

## WebSocket disconnected

```text
Connection interrupted — reconnecting…
```

Do not show blank broken screens — every data-fetching component must handle all three of loading/empty/error explicitly (verified in Section 76 tests) rather than relying on a fallback that happens to render nothing.

---

# 53. PERFORMANCE

## Frontend

- Do not rerender the whole map for each update — key Leaflet markers by `icao24` and update only the changed marker's position/rotation/label (`marker.setLatLng()`/custom rotation update) rather than clearing and re-adding all markers per WebSocket message.
- Update only changed aircraft (the `updated` array from Section 47's delta message already scopes this).
- Efficiently manage Leaflet markers (a `Map<icao24, LeafletMarker>` kept in a ref/store, not re-created each render).
- Efficiently manage trails (append the new point to the existing polyline's coordinate array rather than rebuilding the whole polyline object).
- Avoid unnecessary global state updates (aircraft position state should live in a store/ref that the map component subscribes to directly, not force a re-render of unrelated components like the header or filter panel).
- Lazy-load heavier pages/components where useful (route-based code splitting via `React.lazy`/dynamic `import()` for Analytics, Airports, and Account pages, at minimum).
- Debounce search (Section 26: ~250–300ms).
- Avoid duplicate API calls (React Query's built-in caching/dedup satisfies this for REST reads; ensure the WebSocket subscription is established exactly once per mounted Map page, not once per marker/component).

## Backend

- Redis for current state.
- PostgreSQL for persistence/history.
- Batch external processing where appropriate (a single OpenSky poll serves all connected clients; a single conflict-detection pass per ingestion cycle serves all pairs).
- Cache expensive/repeated operations (weather per-airport cache, Section 31; prediction cache, Section 35).
- Centralize external API calls (Section 18/31 — one scheduled poller each, never per-request).

## ML

Cache predictions (Section 35).

## Database

Add indexes for common query patterns (Section 54).

---

# 54. DATABASE INDEXES

Create suitable indexes for:

- User email (unique index, already implied by the `UNIQUE` constraint in Section 22.1).
- Tracked-flight user ID (`(user_id, active)` composite, Section 22.2).
- Aircraft/ICAO24 (`flight_history.aircraft_identifier`, part of the composite below).
- Callsign (only if callsign-based history lookups are a real query pattern; otherwise skip — "no random indexes" applies here too).
- Airport IATA (unique, Section 22.7).
- Airport ICAO (unique, Section 22.7).
- Timestamps (`flight_history."timestamp"` standalone, for retention cleanup).
- Flight-history aircraft + timestamp (`(aircraft_identifier, "timestamp")` composite, Section 22.3).
- Alert user + read state + created_at (`(user_id, read, created_at DESC)` composite, Section 22.5).

Do not create random indexes without a query reason — every index above maps to a named query pattern already described elsewhere in this document.

---

# 55. DATA INGESTION PIPELINE

The centralized ingestion process should conceptually be:

```text
FETCH
 ↓
VALIDATE
 ↓
NORMALIZE
 ↓
DEDUPLICATE
 ↓
CALCULATE DERIVED STATE
 ↓
REDIS CURRENT STATE
 ↓
POSTGRES HISTORY
 ↓
CONFLICT ENGINE
 ↓
PREDICTION CACHE WHEN NEEDED
 ↓
WEBSOCKET DELTA
```

implemented as a single `@Scheduled` method (or a short internal pipeline of injected collaborator services called in this order from that one scheduled entry point) in the `opensky` package, running once per `OPENSKY_POLL_INTERVAL_SECONDS`.

Do not perform expensive work repeatedly from each frontend request — everything above happens exactly once per ingestion cycle regardless of how many browsers are connected.

---

# 56. INGESTION VALIDATION

Validate:

- Latitude range (`-90..90`).
- Longitude range (`-180..180`).
- Timestamp (not absurdly in the future/past — reject if `last_contact` is more than a small tolerance, e.g. 120s, ahead of server time, or if it's older than the current stale threshold on first ingestion of that record).
- Stable aircraft identifier (`icao24` non-null, matches the expected 6-hex-character format).
- Numerical fields (altitude/speed/heading/vertical_rate are finite numbers, not NaN/Infinity — a corrupted upstream payload should be caught here, not propagated).
- Impossible values (e.g. `speed` above ~1500 km/h per the sanity ceiling in Section 14, `heading` outside `0..360`).
- Duplicate observations (Section 14's dedup rule).

Handle malformed external data without crashing the pipeline — a single invalid record is dropped/logged and the rest of the batch continues processing; one bad row from OpenSky must never abort the whole ingestion cycle.

---

# 57. AIRCRAFT STALE/REMOVAL LOGIC

If an aircraft disappears from the latest OpenSky response, do not immediately assume it landed.

Use a configurable stale threshold (`AIRCRAFT_STALE_THRESHOLD_SECONDS`, default `UNAVAILABLE_THRESHOLD_SECONDS` from Section 17, i.e. 90s — reuse the same constant unless a distinct value is genuinely needed).

After sufficient evidence (the aircraft's Redis `aircraft:{icao24}` key naturally expires per its TTL, Section 20, without being refreshed by a subsequent ingestion cycle):

- Remove from live map (a `removed` entry is emitted in the next WebSocket delta, Section 47).
- Preserve historical data (PostgreSQL `flight_history` rows are untouched — this only affects the live/current-state view).
- Update status if justified (Section 24's Landed criteria, which require corroborating on-ground evidence, not mere absence).

Temporary missing observations must not create false landed events — absence alone never sets status to Landed; only the explicit on-ground-state-persisted-across-cycles rule in Section 24 does.

---

# 58. PERSISTENCE VS LIVE STATE

Use this strict division:

### Redis

Fast-changing/current:

- Current aircraft.
- Recent trail.
- Cached prediction.
- Short-lived aggregates.

### PostgreSQL

Persistent:

- Users.
- Tracking relationships.
- Alerts.
- Airports.
- Flight history.
- Relevant prediction records.
- Analytics source data.

Do not duplicate everything without a reason — e.g. current aircraft state does not need a PostgreSQL mirror beyond the periodic `flight_history` write already specified in Section 55; there is no separate "current state" table in PostgreSQL.

---

# 59. AIRPORT DATA

Airport reference data (Section 22.7) should be maintained separately from volatile aircraft state — it is a static/slow-changing reference table, seeded once (e.g. via a Flyway migration or a one-time import script from a free/open airport-data source such as OurAirports' public dataset, which is public-domain/CC0 and satisfies the ₹0 budget constraint) rather than derived from live OpenSky data.

At minimum support:

- Name.
- IATA.
- ICAO.
- City/location.
- Country.
- Coordinates.

Do not invent airport metadata — any airport not present in the seeded reference dataset is simply not searchable/displayable; do not synthesize a plausible-looking airport record.

If an airport cannot be confidently associated with a live flight, do not fabricate the association (Section 18.2).

---

# 60. FLIGHT SEARCH/FILTER CONSISTENCY

Search and filters should be powered by normalized backend data — the same `flight`/`airport` service layer backs the global search (Section 26), the Flights page filters (Section 27), and the Map filters (Section 15); none of these implement their own independent matching logic client-side.

Do not create separate contradictory filtering implementations in different frontend pages.

The same concepts should behave consistently in:

- Global search.
- Flights page.
- Map filters.
- Airport search.

Search should prefer exact matches and support partial matching (Section 26's rules apply uniformly).

---

# 61. DEEP LINKS / ROUTES

Use stable routes similar to:

```text
/
/map
/flights
/flights/{id}
/airports
/airports/{id}
/analytics
/my-flights
/account
/login
/register
```

The Map page should preserve the selected city/airspace in the URL (e.g. `/map?airspace={cityId}`) so a selected airspace can be bookmarked/shared and survives browser refresh.

A flight/airport page must survive browser refresh — `/flights/{id}` and `/airports/{id}` fetch their data fresh from the backend on mount using the route param, not from in-memory navigation state passed via `<Link state={...}>`.

Do not make critical pages dependent solely on temporary in-memory state. `/my-flights` and `/account` additionally require a valid session on refresh; if the JWT is missing/expired, redirect to `/login` with a return-to path so the user lands back where they intended after logging in.

---

# 62. MAP -> FLIGHT -> DETAILS FLOW

The intended user flow is:

```text
Map
 ↓
Click aircraft
 ↓
Side panel opens immediately
 ↓
View flight information
 ↓
Track Flight if desired
```

The side panel should not require a second click just to see basic details (Section 28).

---

# 63. AIRPORT -> TRAFFIC FLOW

The intended flow is:

```text
Airports
 ↓
Search airport
 ↓
Airport details
 ↓
View arrivals/departures
 ↓
View current weather
 ↓
View Live Traffic Around Airport
 ↓
Map centered on airport
```

---

# 64. USER -> TRACKING FLOW

```text
Public flight view
 ↓
Track Flight
 ↓
If logged out → Login/Register
 ↓
If logged in → save tracked relationship
 ↓
My Flights
 ↓
Meaningful changes
 ↓
Browser/in-app alert
```

---

# 65. DATA FRESHNESS RULES

Every live-data screen should communicate freshness (Section 17's LIVE / DATA DELAYED / LIVE DATA UNAVAILABLE indicator, present on the Map and reflected as a smaller inline note anywhere else live-derived values are shown, e.g. a flight's "Last updated" field).

The backend should maintain timestamps for external updates per active airspace (the ingestion job's own "last successful fetch" timestamp, stored e.g. in `airspace:{airspaceId}:meta` plus an optional system-wide last-success timestamp, exposed via the current-state and analytics endpoints).

The frontend should calculate/display relative freshness from trusted timestamps (never let the frontend independently guess "how long ago" from something other than the backend-provided timestamp compared against the client's own clock, formatted with a ticking display per Section 17).

If data exceeds the stale threshold:

- Stop labeling it LIVE.
- Display delayed/unavailable status.

---

# 66. ALERT STATE MANAGEMENT

Persist alert state:

```text
created → unread → read
```

Marking all read must affect only the authenticated user's notifications (`PATCH /api/alerts/read-all` scoped by the JWT-derived user ID, per Section 40 — never a global operation).

Use state transitions to avoid duplicate alerts (Section 43.2's dedup-window mechanism, tracked per `(tracked_flight_id, alert_type)`).

---

# 67. CLEANUP JOBS

Implement scheduled cleanup (e.g. a nightly `@Scheduled(cron = "...")` job, or several smaller jobs) for:

- Flight history older than 7 days (`DELETE FROM flight_history WHERE "timestamp" < now() - interval '{HISTORY_RETENTION_DAYS} days'`).
- Expired trail cache (self-cleaning via Redis ZSET trim on write and the aircraft-state TTL, Section 20 — no separate job needed for this one).
- Stale aircraft cache (self-cleaning via TTL, Section 20 — no separate job needed).
- Old predictions no longer required (a reasonable retention on the `predictions` table, e.g. also 7 days by default, `PREDICTION_RETENTION_DAYS`, distinct from the short-lived Redis prediction *cache*).
- Old alerts according to a reasonable bounded retention policy (e.g. delete/archive read alerts older than 30 days, `ALERT_RETENTION_DAYS`, configurable).

Do not permit indefinite data growth — every table that receives ongoing writes (`flight_history`, `predictions`, `alerts`) has a defined, enforced retention policy above.

---

# 68. ANALYTICS DATA CORRECTNESS

Analytics must be based on AeroAI's stored/calculated data.

Do not pretend OpenSky directly supplies historical analytics — every Analytics number is computed from PostgreSQL (`flight_history`, `predictions`, conflict-event records) or from the live Redis snapshot for "current" stats, never a claim about what OpenSky itself reports historically.

For Today:

- Use the current day's time window (Section 44's `ANALYTICS_TIMEZONE`-anchored midnight-to-now boundary).

For Last 7 Days:

- Use the defined 7-day retention window (Section 23).

Do not show analytics for periods for which the application does not actually retain data — there is no "Last 30 Days" option, since the system does not retain 30 days of history.

---

# 69. LOCAL DEVELOPMENT

The complete system should be runnable locally with:

```text
React
Spring Boot
FastAPI
PostgreSQL
Redis
```

Docker Compose should be provided for infrastructure/local development where practical (a `docker-compose.yml` at the repo root bringing up `postgres`, `redis`, `ml`, and `backend`, with `frontend` run via `npm run dev` locally for fast iteration, or also containerized for a full one-command `docker compose up` — either is acceptable, but the full-containerized option should exist for anyone who doesn't want to install Node/Java/Python locally).

Document, in a root `README.md`:

- Runtime requirements (Section 8's pinned versions).
- Environment setup (copy `.env.example` to `.env`, fill in required values from Section 94).
- Service startup (per-service dev commands: `npm run dev`, `mvn spring-boot:run` or the packaged jar, `uvicorn` command from Section 70).
- Docker startup (`docker compose up -d postgres redis`, `docker compose up --build` for everything).
- Health checks (`GET /actuator/health` for Spring Boot via Spring Boot Actuator, `GET /health` for FastAPI).
- ML startup (Section 70).

---

# 70. ML LOCAL STARTUP

The existing ML service should support:

```text
python -m uvicorn ml.api.main:app --reload --port 8000
```

from the project root with the current model paths.

Do not break this structure — the path-resolution rule in Section 12 (paths relative to `main.py`, not CWD) is exactly what keeps this invocation working regardless of whether it's run from the repo root, from inside `ml/`, or from inside a container with a different working directory.

---

# 71. DOCKER

Support a local multi-service environment conceptually:

```text
frontend
backend
ml
postgres
redis
```

Container networking must use service names where appropriate (`ML_SERVICE_URL=http://ml:8000` inside the Docker network, not `http://localhost:8000`, from the `backend` container's perspective; `DATABASE_URL` pointing at `postgres`, `REDIS_HOST=redis`, etc.).

Do not hardcode `localhost` assumptions that break inside containers — every inter-service URL is env-var-driven per Section 49, with dev-local defaults (`localhost`) overridden by Docker Compose's own environment block to the service-name form above.

---

# 72. ₹0 BUDGET

This is a hard requirement.

The application must be designed to run/deploy using free/open-source software and free tiers where available.

Do not require:

- Paid API.
- Paid AI service.
- Paid map provider.
- Paid database.
- Paid Redis.
- Paid email service.
- Paid analytics platform.
- Paid hosting.

If a provider is selected later, verify its current free-tier terms before deployment (free tiers change over time — re-check OpenSky, Open-Meteo, and any chosen hosting provider's current terms at deployment time, not just at spec-writing time).

Do not embed provider-specific billing assumptions into application logic (e.g. don't write code that only works because a specific free-tier host happens to allow long-lived WebSocket connections — keep the WebSocket/polling fallback path in Section 19 robust enough to degrade to polling if a given free host doesn't support persistent WebSocket connections well).

---

# 73. MAP TILES

Leaflet is the rendering library.

Use an appropriate free tile source — **OpenStreetMap standard tiles** (`https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png`) is the default, zero-cost, no-API-key option consistent with the ₹0 budget; a dark-styled free alternative (e.g. CARTO's free "dark matter" basemap tiles, which pair naturally with Section 4.2's dark theme) is an acceptable and encouraged substitution if its usage policy is verified to allow this project's expected traffic — pick one and pin it as `VITE_MAP_TILE_URL`.

Respect:

- Attribution (required attribution string rendered in the map's attribution control — do not remove it).
- Usage policy (check the chosen provider's current tile-usage policy for acceptable request volume/caching requirements).
- Rate limits.
- Appropriate caching rules (rely on the browser's normal HTTP caching of tile image responses; do not add a custom tile-proxy/cache layer unless the chosen provider's policy requires one).

Do not assume public tile servers provide unlimited high-volume use.

Make the tile URL configurable if practical (`VITE_MAP_TILE_URL` env var, Section 49).


---

# 74. TESTING STRATEGY

Do not write hundreds of meaningless tests.

Focus on critical paths, integration points, failure cases, and security. Use JUnit 5 + Spring Boot Test (`@SpringBootTest`, `@WebMvcTest`, Testcontainers for PostgreSQL/Redis integration tests) on the backend, and Vitest + React Testing Library on the frontend — both free/open-source, consistent with Section 72.

---

# 75. BACKEND TESTS

Test:

- Registration.
- Login.
- Logout.
- Authentication failure.
- Protected endpoints.
- Flight retrieval.
- Airspace/city retrieval.
- City-scoped live traffic.
- Search.
- Filtering.
- Airport retrieval.
- Airport search.
- Tracking.
- Untracking.
- My Flights.
- Alerts.
- Read/unread.
- Prediction integration.
- ML failure.
- Conflict calculations (unit-test the Section 36.2 math directly against known input/output pairs computed independently, e.g. two aircraft on a known collision course vs. two on diverging tracks).
- Redis behavior.
- PostgreSQL persistence.
- OpenSky failure.
- Open-Meteo failure.
- Retention cleanup.

---

# 76. FRONTEND TESTS

Test:

- Homepage.
- Map rendering.
- City/airspace selector.
- City search.
- Selected-airspace viewport.
- Nearby-airport highlighting.
- Aircraft markers.
- Marker updates.
- Heading rotation.
- Callsign visibility.
- Trails.
- Trail duration selector.
- Filters.
- Flight search.
- Flight table.
- Flight side panel.
- Airport search.
- Airport detail.
- Arrivals/departures.
- Login.
- Register.
- My Flights.
- Track/untrack.
- Alerts.
- Analytics.
- Loading states.
- Empty states.
- Error states.
- Responsive behavior (verify no horizontal overflow at 360/390/414px per Section 7).

---

# 77. INTEGRATION TESTS

At minimum verify:

```text
OpenSky
   ↓
Spring Boot
   ↓
Redis
   ↓
WebSocket
   ↓
React Map
```

And:

```text
Flight data
   ↓
Spring Boot
   ↓
FastAPI
   ↓
Prediction
   ↓
Flight details
```

Also verify:

```text
User
 ↓
Track Flight
 ↓
PostgreSQL
 ↓
My Flights
 ↓
Alert
```

Use Testcontainers to spin up real PostgreSQL/Redis instances for these tests rather than mocking the databases entirely, so the integration is genuinely exercised.

---

# 78. SECURITY TESTS

Verify:

- Passwords are hashed.
- Unauthenticated users cannot access personalized endpoints.
- Users cannot access other users' data (attempt to fetch/modify user B's tracked flight while authenticated as user A; expect `403`/`404`, never success).
- Secrets are not returned.
- SQL injection is prevented.
- Invalid request data is rejected.
- CORS is intentional.
- Error responses do not expose stack traces (assert the Section 46.1 envelope shape on a deliberately-triggered 500).

---

# 79. ACCESSIBILITY BASICS

Implement reasonable accessibility:

- Semantic buttons/links (`<button>`/`<a>`, not `<div onClick>`).
- Keyboard-accessible controls (all map controls, filters, and forms operable via Tab/Enter/Space).
- Labels for inputs (`<label>` or `aria-label` on every form field, including search).
- Accessible contrast (verify the pinned palette in Section 4.2 meets WCAG AA for text-on-background combinations).
- Meaningful alt text where imagery is informational (the logo, any illustrative icons that convey information beyond decoration).
- Do not use color alone to communicate critical state (risk levels and status badges always pair color with a text label, e.g. "HIGH" not just a red dot).
- Visible focus states (do not remove `outline` without providing an equivalent visible focus style).

The map itself may require special interaction handling (Leaflet's own keyboard support is limited), but surrounding controls (zoom buttons, filter panel, search) must remain accessible.

---

# 80. ACCEPTANCE CRITERIA — PUBLIC

The project is not complete unless:

- Homepage works without login.
- Live map works without login.
- A large searchable major-city catalogue is available.
- Selecting a city centers the map on its configured airspace.
- Selected-city nearby airports are highlighted.
- Flights can be searched.
- Flight details can be viewed.
- Airports can be searched/viewed.
- Airport arrivals/departures work.
- Relevant weather displays.
- Delay prediction displays when possible.
- Conflict risk displays when calculable.

---

# 81. ACCEPTANCE CRITERIA — ACCOUNT

- Registration works.
- Login works.
- Logout works.
- Tracking requires authentication.
- My Flights works.
- Untracking works.
- Browser/in-app alerts work.
- Read/unread state works.
- Users cannot access another user's data.

---

# 82. ACCEPTANCE CRITERIA — REAL TIME

- Selected city airspace reaches the backend scheduler.
- OpenSky reaches backend using the selected city's bounding box.
- Redis contains current state scoped to the airspace.
- WebSocket updates reach browser.
- Aircraft update without full page refresh.
- Aircraft animate when reliable.
- Trails update.
- Stale data is identified.
- WebSocket reconnects.

---

# 83. ACCEPTANCE CRITERIA — INTELLIGENCE

- FastAPI loads all three model artifacts.
- `/predict` works.
- Spring Boot can call FastAPI.
- Correct ML response reaches flight details.
- Prediction caching works.
- Conflict calculations work.
- Risk levels display correctly.
- AI/calculated data is not represented as official aviation data.

---

# 84. ACCEPTANCE CRITERIA — PERSISTENCE

- Users persist.
- Tracking relationships persist.
- Relevant flight history persists.
- Alerts persist.
- Airports persist.
- Prediction records persist where appropriate.
- Seven-day cleanup works.

---

# 85. ACCEPTANCE CRITERIA — UI

- Dark aviation/radar theme.
- Black/cyan-blue/dark-green direction.
- Full-screen map.
- No map-style toggle.
- Clean/minimal UI.
- Subtle animations.
- Desktop-first.
- Mobile still functional.
- No major control overlap.
- Loading states exist.
- Empty states exist.
- Error states exist.

---

# 86. ACCEPTANCE CRITERIA — BUDGET

- Local system works using free/open-source software.
- No paid API is required.
- No paid AI API is required.
- No paid map service is required.
- No paid notification service is required.
- Deployment architecture remains free-tier-compatible.

---

# 87. IMPLEMENTATION ORDER

Implement progressively in this order.

## Phase 1 — Foundation

1. Create project structure.
2. Configure React + TypeScript + Vite.
3. Configure Tailwind.
4. Configure Leaflet.
5. Configure Spring Boot.
6. Configure PostgreSQL.
7. Configure Redis.
8. Configure FastAPI ML service.
9. Configure environment variables.
10. Create Docker Compose/local infrastructure.

## Phase 2 — Authentication

11. User entity.
12. Registration.
13. Login.
14. Logout.
15. Secure password hashing.
16. Authentication session/token.
17. Account page.
18. Protected endpoints.

## Phase 3 — OpenSky

19. Airspace/city reference data.
20. OpenSky client.
21. Scheduled polling.
22. Response normalization.
23. Validation.
24. Current-state management.
25. Redis caching.
26. PostgreSQL historical storage.
27. City-scoped ingestion scheduling.
28. Data freshness.
29. Stale-data handling.

## Phase 4 — Real Time

28. WebSocket endpoint.
29. Current-state REST endpoint.
30. Delta generation.
31. Added/updated/removed aircraft messages.
32. React WebSocket client.
33. Reconnection.
34. Live indicator.

## Phase 5 — Map

35. Full-screen Leaflet map.
36. Major-city/airspace selector.
37. City-airspace reference data and API.
38. Selected-airspace viewport and nearby-airport highlighting.
39. Aircraft icons.
40. Heading rotation.
41. Callsign labels.
42. Smooth interpolation.
43. Trail collection.
44. Trail rendering.
45. Trail-duration selector.
46. Map filters.
47. Map controls.
48. Locate-me.
49. Fullscreen.

## Phase 6 — Flights

48. Flights endpoint.
49. Search.
50. Filters.
51. Dense flight table.
52. Flight detail DTO.
53. Flight side panel.
54. Derived calculations.
55. Distance travelled.
56. Distance remaining.
57. ETA.
58. Last updated.

## Phase 7 — Airports

59. Airport data.
60. Airport search.
61. Airport directory.
62. Airport detail.
63. Arrivals.
64. Departures.
65. Airport weather.
66. Traffic-around-airport action.

## Phase 8 — Weather

67. Open-Meteo integration.
68. Weather normalization.
69. Airport weather.
70. Flight-relevant weather.
71. Weather failure handling.

Do not create a Weather page.

## Phase 9 — ML

72. Verify model artifact paths.
73. Verify FastAPI health.
74. Verify `/predict`.
75. Spring Boot ML client.
76. Prediction input mapping.
77. Prediction response mapping.
78. Prediction caching.
79. Flight prediction UI.
80. Why-prediction section.
81. Prediction unavailable state.

## Phase 10 — Conflict

82. Spatial candidate detection.
83. Geographic distance.
84. Altitude separation.
85. Velocity/heading calculation.
86. Relative movement.
87. Rate of closure.
88. Closest approach estimate.
89. Risk classification.
90. Conflict state/cache.
91. Map visualization.
92. Flight conflict panel.

## Phase 11 — Personalization

93. Track Flight.
94. Untrack.
95. My Flights.
96. Account.
97. Alert generation.
98. Alert deduplication.
99. Notification panel.
100. Read/unread.
101. Mark all read.

## Phase 12 — Analytics

102. Live statistics.
103. Today statistics.
104. Seven-day statistics.
105. Traffic charts.
106. Delay charts.
107. Conflict charts.
108. Data-health metrics.

## Phase 13 — Polish

109. Loading states.
110. Empty states.
111. Error states.
112. Responsive behavior.
113. Performance optimization.
114. Accessibility basics.
115. Security review.
116. Retention cleanup.
117. Tests.
118. Documentation.
119. Final production build.
120. End-to-end verification.

---

# 88. PRIORITY ORDER IF TIME/COMPLEXITY IS LIMITED

## Priority 1 — Must work

- React frontend.
- Spring Boot.
- OpenSky.
- Redis.
- PostgreSQL.
- Leaflet.
- Major-city airspace selector.
- Live city-scoped map.
- WebSockets.
- Flight search/details.
- Authentication.
- My Flights.
- ML prediction.

## Priority 2 — Important

- Airports.
- Open-Meteo.
- Trails.
- Conflict risk.
- Browser alerts.
- Analytics.

## Priority 3 — Polish

- Advanced animation.
- Fine-grained transitions.
- Extra chart customization.
- Minor convenience features.

Never sacrifice data correctness for visual polish.

---

# 89. ANTIGRAVITY/ZED RULES

## Rule 1 — This document is the source of truth

Follow this implementation specification. The implementation repository must contain exactly one active implementation specification named `IMPLEMENT.md`; do not treat older duplicate copies such as `IMPLEMENT(1).md`, `IMPLEMENT(2).md`, `IMPLEMENT(3).md`, or other pasted drafts as authoritative.

Follow this implementation specification.

If something is not required, do not add it merely because it is easy.

## Rule 2 — Do not expand scope

Do not reintroduce removed SRS features (Section 2.3).

## Rule 3 — Never fabricate data

Unavailable data must remain unavailable (Section 3.1).

## Rule 4 — Frontend only talks to Spring Boot

Never bypass the backend (Section 9).

## Rule 5 — FastAPI remains ML-only

Do not turn it into a second general backend (Section 33.2).

## Rule 6 — Preserve existing ML artifacts

Do not retrain/replace the final models during application implementation unless explicitly instructed (Section 32).

## Rule 7 — Separate live refresh from ML refresh

Do not call ML every time a plane moves (Section 35).

## Rule 8 — Use Redis for live state

Do not overload PostgreSQL with every frontend refresh (Section 58).

## Rule 9 — City selection is reference-data driven

Do not hardcode a handful of city buttons in the frontend. Major cities and their authoritative airspace bounds come from the backend reference dataset. Adding a supported city must be a data/seed change, not a React component rewrite.

## Rule 10 — Centralize external API usage

Do not make one OpenSky/Open-Meteo request per user (Sections 18.3, 31).

## Rule 11 — Graceful degradation

Any external dependency can fail. Handle failure without crashing unrelated functionality (Section 51).

## Rule 12 — Avoid unnecessary microservices

One Spring Boot modular monolith + separate FastAPI ML service (Section 9).

## Rule 13 — Avoid overengineering

Prefer simple, understandable solutions.

## Rule 14 — Keep code maintainable

Use clear names, focused services, DTOs, reusable components, centralized API clients, and configuration-driven thresholds (every numeric threshold introduced in this document — Sections 17, 24, 36, 43, 57, 67 — is a named config value, Section 94, not a magic number embedded in code).

## Rule 15 — Do not redesign the UI

Preserve:

- Dark aviation/radar theme.
- Black/cyan-blue/dark-green palette.
- Full-screen map.
- Clean/minimal UI.
- Subtle animations.
- Desktop-first responsive behavior.

## Rule 16 — Aviation disclaimer accuracy

Always make it clear where necessary that:

- AI delay predictions are estimates.
- ETA is an AeroAI calculation where applicable.
- Conflict risk is research/decision-support only.
- Live data may be stale/unavailable.
- AeroAI is not an official ATC/TCAS/safety system.

(Section 1's footer/About text and Section 3.2's per-value badges are the concrete implementation of this rule — do not treat it as satisfied by a single one-time splash screen a user can dismiss and never see again.)

---

# 90. FINAL USER FLOWS

## Flow 0 — Choose a city airspace

```text
Open AeroAI
  ↓
Choose Airspace
  ↓
Search/select major city
  ↓
Backend loads city + authoritative airspace bounds
  ↓
Map centers/zooms to selected city
  ↓
Nearby airports highlighted
  ↓
Live aircraft for selected airspace
```

The city catalogue is large and data-driven. Selecting a city never requires a direct OpenSky call from the browser.

## Flow A — Anonymous visitor

```text
Open AeroAI
 ↓
Homepage
 ↓
Explore Live Traffic
 ↓
Map
 ↓
Click aircraft
 ↓
Flight details side panel
 ↓
View prediction/conflict/weather
```

No login required.

## Flow B — Search flight

```text
Global search
 ↓
Search callsign/flight identifier
 ↓
Result
 ↓
Flight details
```

## Flow C — Search airport

```text
Global search
 ↓
Airport result
 ↓
Airport details
 ↓
ARRIVALS / DEPARTURES
 ↓
Weather
 ↓
View Live Traffic Around Airport
```

## Flow D — Track

```text
Flight details
 ↓
Track Flight
 ↓
If logged out: Login/Register
 ↓
Save tracking relationship
 ↓
My Flights
 ↓
Meaningful update
 ↓
Browser/in-app alert
```

## Flow E — Live traffic

```text
OpenSky
 ↓
Spring Boot
 ↓
Validate
 ↓
Normalize
 ↓
Redis
 ↓
PostgreSQL history
 ↓
Derived calculations
 ↓
WebSocket delta
 ↓
Leaflet
```

---

# 91. FINAL SYSTEM DATA FLOW

## Live traffic

```text
OpenSky
 ↓
Spring Boot
 ↓
Validate
 ↓
Normalize
 ↓
Redis
 ↓
PostgreSQL history
 ↓
Derived calculations
 ↓
WebSocket delta
 ↓
React + Leaflet
```

## Weather

```text
Open-Meteo
 ↓
Spring Boot
 ↓
Normalize/cache
 ↓
Airport/flight context
 ↓
ML input where applicable
```

## Delay prediction

```text
Flight + relevant weather/features
 ↓
Spring Boot
 ↓
FastAPI
 ↓
XGBoost regression
Random Forest classification
 ↓
Prediction
 ↓
Spring Boot
 ↓
Cache
 ↓
Flight details
```

## Conflict risk

```text
Current aircraft states
 ↓
Spatial candidate selection
 ↓
Geographic + altitude calculations
 ↓
Relative velocity
 ↓
Closest approach
 ↓
Risk level
 ↓
Map + flight details
```

## Tracking

```text
User
 ↓
Track Flight
 ↓
Spring Boot
 ↓
PostgreSQL
 ↓
My Flights
 ↓
Meaningful state changes
 ↓
Browser/in-app alert
```


---

# 92. FINAL DEFINITION OF DONE

AeroAI is complete only when a user can:

1. Open AeroAI without an account.
2. See the dark aviation-themed homepage.
3. Explore live traffic.
4. Choose a major city/airspace from a large searchable catalogue.
5. Open the full-screen map for that city.
6. See the selected city's airspace and nearby airports.
7. See live aircraft where OpenSky data is available.
8. See aircraft icons with heading.
9. See callsign labels intelligently based on zoom.
10. Watch aircraft update without a page refresh.
11. See actual collected trails.
12. Select trail duration.
13. Filter traffic.
14. Search flights.
15. Open flight details.
16. See current flight information.
17. See distance travelled where calculable.
18. See distance remaining where calculable.
19. See AeroAI ETA where calculable.
20. See relevant weather.
21. See AI delay prediction where inputs allow it.
22. See conflict risk where calculable.
23. Search airports.
24. Open airport details.
25. View arrivals/departures.
26. View airport weather.
27. Jump to airport traffic.
28. Register.
29. Login.
30. Track a flight.
31. See it in My Flights.
32. Receive meaningful browser/in-app alerts.
33. Mark alerts read.
34. View Today analytics.
35. View Last 7 Days analytics.
36. Experience graceful handling of external-data failures.
37. Use the site comfortably on desktop.
38. Open the site on a phone and use its core functionality.
39. Never encounter fabricated aviation data.
40. Never see stale data incorrectly labeled as live.
41. Never require a paid API/service to run the core product.
---

# 93. FINAL IMPLEMENTATION DIRECTIVE

Build AeroAI as a **coherent, polished, technically credible educational/research platform**.

The final product should feel like:

> **Modern aviation monitoring software with AI-based decision support.**

It must not feel like:

> **A generic AI dashboard with an airplane image.**

The map is the heart of the product.

Flight intelligence is the purpose.

AI delay prediction and conflict-risk detection are decision-support features.

Authentication exists only for personalization.

Weather exists only as supporting data.

Redis exists for fast live state.

PostgreSQL exists for persistent data and limited history.

Spring Boot is the central backend.

FastAPI is the isolated ML service.

React is the user-facing application.

OpenSky is the primary live-aircraft source.

Open-Meteo is supporting weather data.

Everything must remain compatible with the strict **₹0/free/open-source constraint**.

**Implement the system end-to-end, verify every critical integration, test failure cases, maintain accurate data freshness, preserve the existing ML artifacts, and do not fabricate data or expand the product scope.**

---

# 94. CONFIGURATION REFERENCE (ALL TUNABLE VALUES, ONE TABLE)

Every threshold mentioned anywhere in this document is a named, overridable configuration value. This table is the single place to look them up; do not let a second, drifting copy of these defaults exist elsewhere in code comments.

```text
NAME                                    DEFAULT       SECTION(S)
OPENSKY_POLL_INTERVAL_SECONDS           15            13.5, 18.3, 55
OPENSKY_BOUNDING_BOX (lamin,lomin,      5,60,38,100   18.4
  lamax,lomax)
LIVE_THRESHOLD_SECONDS                  20            17
UNAVAILABLE_THRESHOLD_SECONDS           90            17, 57
AIRCRAFT_STALE_THRESHOLD_SECONDS        90            57 (defaults to
                                                       UNAVAILABLE_THRESHOLD_SECONDS)
TRAIL_RETENTION_MINUTES                 60            14, 20
HISTORY_RETENTION_DAYS                  7             23, 67
PREDICTION_CACHE_TTL_SECONDS            900           20, 35
PREDICTION_RETENTION_DAYS               7             67
ALERT_RETENTION_DAYS                    30            67
ALERT_DELAY_CHANGE_THRESHOLD_MINUTES    15            43.1
ALERT_DEDUP_WINDOW_SECONDS              300           43.2
CONFLICT_HIGH_HORIZONTAL_M              3000          36.2
CONFLICT_HIGH_VERTICAL_M                150           36.2
CONFLICT_MEDIUM_HORIZONTAL_M            9000          36.2
CONFLICT_MEDIUM_VERTICAL_M              300           36.2
CONFLICT_PREDICTION_HORIZON_SECONDS     300           36.2, 38
CONFLICT_CANDIDATE_RADIUS_KM            50            36.2, 38
JWT_EXPIRY_MINUTES                      60            39
BCRYPT_STRENGTH                         10            8, 39, 50
ANALYTICS_TIMEZONE                      Asia/Kolkata  44, 45, 68
MAP_DEFAULT_CENTER (lat,lon)            22.0, 79.0    13.2
MAP_DEFAULT_ZOOM                        5             13.2
WEATHER_CACHE_TTL_MINUTES               15            31
AIRSPACE_DEFAULT_CITY_ID                mumbai        13.2, 94
AIRSPACE_MAX_ACTIVE_CITY_COUNT          1             18.4, 94
AIRSPACE_REQUEST_CACHE_TTL_SECONDS      120           18.4, 94
```

All of the above are environment variables or Spring `application.yml` properties (mirrored into FastAPI's own config where the value is relevant there, e.g. nothing above is ML-relevant so FastAPI needs none of this table) — never a hardcoded literal buried in a service class.

---

# 95. GLOSSARY

- **ICAO24** — the unique 24-bit hexadecimal address broadcast by an aircraft's transponder; the stable identifier used throughout this system for "which aircraft is this," independent of callsign (which can be reused/changed per flight).
- **Callsign** — the flight identifier broadcast by the aircraft (e.g. `AIC101`), used for display and search, but not treated as a permanently stable identifier the way ICAO24 is.
- **State vector** — OpenSky's term for one aircraft's current reported telemetry (position, altitude, speed, heading, etc.) at a point in time.
- **TCPA** — Time to Closest Point of Approach: the number of seconds in the future at which two moving objects' separation will be smallest, assuming both continue on their current velocity.
- **DCPA** — Distance at Closest Point of Approach: the separation distance predicted to occur at the TCPA.
- **Great-circle / haversine distance** — the shortest distance between two points on a sphere (used here as a good-enough approximation of Earth for the distances involved), as opposed to a straight-line-through-the-Earth or flat-map distance.
- **Delta message** — a WebSocket payload describing only what changed (added/updated/removed) since the last message, as opposed to re-sending the full current state every time.
- **Modular monolith** — a single deployable backend application internally organized into clearly separated modules/packages (by domain, e.g. `flight`, `airport`, `auth`), as opposed to either (a) one undifferentiated codebase with no internal boundaries, or (b) splitting each module into its own independently deployed microservice.
- **Decision-support** — describing information that helps a person reason about a situation, as distinct from an authoritative instruction or control system; core to how AeroAI's AI predictions and conflict-risk calculations must be framed throughout the product (Rule 16).
- **Airspace** — the configured geographic monitoring region associated with a supported major city; in AeroAI v1 this is represented by an explicit latitude/longitude bounding box used for live OpenSky queries.
- **Airspace ID / City ID** — the stable backend identifier for a supported major-city airspace, used in REST query parameters, WebSocket topics, Redis keys, and URL state.
- **Selected airspace** — the city airspace currently chosen by the user and therefore the authoritative scope for the Map page's live traffic view.
