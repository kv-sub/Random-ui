# Insurance Claim Submission System

A full-stack insurance claim management platform with a Spring Boot REST API, React frontend, PostgreSQL database, and a Synthetic Data Generation Agent — all wired together via Docker Compose with profile-based deployment modes.

---

## Features

- **React Frontend** — role-based UI (Customer / Admin) for submitting, tracking, and reviewing claims
- **Claim Submission** — submit claims with full policy and coverage validation
- **Duplicate Detection** — blocks duplicate claims (same policy, type, incident date within 24 hours)
- **Coverage Limit Enforcement** — rejects claims exceeding per-type coverage limits
- **Claim State Management** — track status transitions (SUBMITTED → IN_REVIEW → APPROVED/REJECTED)
- **Claim History** — full audit trail of every status change
- **OpenAPI/Swagger UI** — interactive API documentation at `/swagger-ui.html`
- **Docker Compose** — two profiles: `prod` (api + db + frontend) and `test` (api + db + frontend + synthetic agent)
- **Spring Profile Routing** — both modes use the `prod` Spring profile (public schema, seed data); `synthetic` profile available as an opt-in advanced mode
- **Comprehensive Tests** — unit + integration tests (H2 in-memory)
- **Synthetic Data Agent** — Streamlit UI to discover a live schema, generate a Faker-based data plan via LLM, and bulk-load synthetic rows into the `synthetic` schema

---

## Technology Stack

| Component | Technology |
|---|---|
| Frontend | React 19 · TypeScript · Vite 7 · Tailwind CSS 4 |
| State / Data | TanStack Query 5 · Zustand 5 · React Hook Form 7 · Zod 4 |
| HTTP Client | Axios 1 |
| Backend Language | Java 17 |
| Backend Framework | Spring Boot 3.2 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation + custom validators |
| Documentation | SpringDoc OpenAPI 3.0 (Swagger UI) |
| Build | Maven 3 |
| Containerization | Docker / Docker Compose (profile-based) |
| Test DB | H2 (in-memory, unit/integration tests only) |
| Testing | JUnit 5, Mockito, MockMvc, Vitest |
| Synthetic Agent | Python 3.12 · Streamlit · Faker · psycopg2 · Groq LLM |

---

## Project Structure

```
Random-ui/                              ← workspace root
├── main.py                             # Synthetic Data Agent (Streamlit app)
├── synthetic-agent/
│   ├── Dockerfile                      # Python 3.12-slim image for the agent
│   └── requirements.txt
├── insurance-frontend/
│   ├── Dockerfile                      # Node 20-alpine image (Vite dev server)
│   ├── vite.config.ts                  # Proxy: BACKEND_URL → /api/v1
│   ├── src/
│   │   ├── App.tsx                     # Router + auth guard setup
│   │   ├── pages/                      # LoginPage, Home, SubmitClaim, TrackClaim, Admin
│   │   ├── components/                 # Layout, ProtectedRoute, form widgets
│   │   ├── api/client.ts               # Axios instance + typed API calls
│   │   ├── stores/authStore.ts         # Zustand role-based auth store
│   │   └── types/                      # Shared TypeScript types
│   └── package.json
└── insurance-claim-system/
    ├── pom.xml
    ├── Dockerfile
    ├── docker-compose.yml              # prod + test profiles
    ├── .env.prod                       # APP_ENV=prod
    ├── .env.test                       # APP_ENV=synthetic
    ├── .dockerignore
    └── src/
        ├── main/
        │   ├── java/com/insurance/claim/
        │   │   ├── ClaimSubmissionSystemApplication.java
        │   │   ├── config/             # OpenAPI bean configuration
        │   │   ├── controller/         # REST endpoints
        │   │   ├── service/            # Business logic
        │   │   ├── repository/         # Data access (JPA)
        │   │   ├── entity/             # JPA entities + enums
        │   │   ├── dto/                # Request / response DTOs
        │   │   ├── validator/          # Custom constraint validators
        │   │   ├── exception/          # Custom exceptions + GlobalExceptionHandler
        │   │   └── util/               # ErrorResponse model
        │   └── resources/
        │       ├── application.yml          # Default (dev) — PostgreSQL localhost
        │       ├── application-prod.yml     # prod profile — public schema, seed data
        │       ├── application-synthetic.yml # synthetic profile — synthetic schema, LLM data
        │       ├── application-test.yml     # test profile — H2 in-memory (Maven tests only)
        │       └── schema.sql               # DDL + sample seed data
        └── test/
            ├── java/com/insurance/claim/
            │   ├── controller/  ClaimControllerIT.java
            │   ├── service/     ClaimServiceTest.java, PolicyServiceTest.java
            │   └── validator/   PolicyNumberValidatorTest.java
            └── resources/
                ├── application-test.yml
                └── test-data.sql
```

---

## Spring Profiles

| Profile | Activated by | Database schema | Data source | Used in |
|---|---|---|---|
|---|
| `default` | no env file | `public` (localhost) | seed data | local Maven dev |
| `prod` | `APP_ENV=prod` | `public` | seed data | Docker **prod** stack |
| `synthetic` | `APP_ENV=synthetic` | `synthetic` | LLM-generated data | Docker **test** stack |
| `test` | `@ActiveProfiles("test")` in JUnit | H2 in-memory | test fixtures | Maven unit/IT tests only |

> **Important:** The `test` Spring profile is **only** used by JUnit/Maven tests. It is never set in Docker. The Docker `--profile test` flag is a Docker Compose concept that adds `synthetic-agent` — it is independent of the Spring profile.
>
> The `synthetic` Spring profile is used by the Docker test stack (`APP_ENV=synthetic`) and requires the Synthetic Data Agent to have been run at least once to populate the `synthetic` schema. The agent now generates domain-valid data (correct enum values, FK relationships) so the frontend can map all rows correctly.

---

## API Endpoints

| Method | Path | Description | Status |
|---|---|---|---|
| `POST` | `/api/v1/claims` | Submit a new claim | 201 / 400 / 404 / 409 |
| `GET` | `/api/v1/claims/{id}` | Get claim status | 200 / 404 |
| `PATCH` | `/api/v1/claims/{id}/review` | Approve or reject a claim | 200 / 400 / 404 |
| `GET` | `/api/v1/claims/policy/{policyId}` | List claims for a policy | 200 |
| `GET` | `/api/v1/claims/{id}/history` | Claim status history | 200 / 404 |
| `GET` | `/api/v1/policies/{policyNumber}` | Get policy details | 200 / 404 |

### Sample Request — Submit a Claim

```bash
curl -X POST http://localhost:8080/api/v1/claims \
  -H "Content-Type: application/json" \
  -d '{
    "policyNumber": "POL-10001",
    "claimType": "MEDICAL",
    "claimAmount": 5000.00,
    "incidentDate": "2026-03-05",
    "description": "Hospital visit following an accident"
  }'
```

### Sample Response

```json
{
  "claimId": 1,
  "policyId": 1,
  "policyNumber": "POL-10001",
  "claimType": "MEDICAL",
  "claimAmount": 5000.00,
  "incidentDate": "2026-03-05",
  "description": "Hospital visit following an accident",
  "status": "SUBMITTED",
  "createdAt": "2026-03-10T08:00:00",
  "updatedAt": "2026-03-10T08:00:00"
}
```

### Review a Claim

```bash
curl -X PATCH http://localhost:8080/api/v1/claims/1/review \
  -H "Content-Type: application/json" \
  -d '{
    "action": "APPROVE",
    "reviewerNotes": "All documents verified."
  }'
```

---

## Getting Started

### Prerequisites

- Docker & Docker Compose
- Java 17+ and Maven 3.8+ (only for running tests locally without Docker)
- Node.js 18+ (only for running the frontend locally without Docker)

---

### Option 1 — Production Stack (api + db + frontend)

Starts the Spring Boot API, PostgreSQL, and the React frontend. Uses the `prod` Spring profile — serves seed data from the `public` schema.

```bash
cd insurance-claim-system
docker compose --env-file .env.prod up --build
```

Or with defaults (same result):

```bash
cd insurance-claim-system
docker compose up --build
```

**Services started:**

| Service | URL |
|---|---|
| React Frontend | http://localhost:5173 |
| Spring Boot API | http://localhost:8080 |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| PostgreSQL | localhost:5432 |

---

### Option 2 — Test / Dev Stack (api + db + frontend + synthetic agent)

Same as production but also starts the Synthetic Data Agent on port 8501. Uses `APP_ENV=synthetic` so the Spring Boot API serves data from the `synthetic` schema (LLM-generated rows). The Synthetic Data Agent must be run at least once to populate the schema.

```bash
cd insurance-claim-system
docker compose --env-file .env.test --profile test up --build
```

**Services started:**

| Service | URL | Purpose |
|---|---|---|
| React Frontend | http://localhost:5173 | Claims portal UI |
| Spring Boot API | http://localhost:8080 | REST API (public schema, seed data) |
| Swagger UI | http://localhost:8080/swagger-ui.html | Interactive API docs |
| Synthetic Data Agent | http://localhost:8501 | Generate & load test data into synthetic schema |
| PostgreSQL | localhost:5432 | Shared database |

---

### Option 3 — Local Development (without Docker)

#### Backend

1. Start a local PostgreSQL instance:

```sql
CREATE DATABASE insurance_claims;
CREATE USER claims_user WITH PASSWORD 'claims_pass';
GRANT ALL PRIVILEGES ON DATABASE insurance_claims TO claims_user;
```

2. Run the schema:

```bash
psql -U claims_user -d insurance_claims -f insurance-claim-system/src/main/resources/schema.sql
```

3. Start Spring Boot (uses `application.yml` default profile — `localhost:5432`):

```bash
cd insurance-claim-system
mvn spring-boot:run
```

#### Frontend

```bash
cd insurance-frontend
npm install
npm run dev
```

Opens at **http://localhost:5173**. The Vite dev server proxies `/api/v1` to `http://localhost:8080` automatically — no CORS configuration needed.

#### Synthetic Data Agent

```bash
pip install -r synthetic-agent/requirements.txt
streamlit run main.py
```

Opens at **http://localhost:8501**.

---

## Frontend

The React frontend provides a role-based portal for interacting with the claims system.

### Roles

| Role | Access |
|---|---|
| **Customer** | Submit claims, track claim status |
| **Admin** | View all claims, approve/reject, view policies |

Login is simulated — on the login page choose **Customer** or **Admin** to enter the portal. No credentials are required; the role is stored in `localStorage`.

### Frontend local development

```bash
cd insurance-frontend
npm install
npm run dev          # http://localhost:5173
npm run test         # Vitest unit tests
npm run test:coverage  # Coverage report → coverage/
npm run build        # Production bundle → dist/
```

The Vite proxy target can be overridden via the `BACKEND_URL` environment variable (used automatically by Docker):

```bash
BACKEND_URL=http://my-api:8080 npm run dev
```

---

## Synthetic Data Agent

The Synthetic Data Agent (`main.py`) generates realistic test data by reading a live PostgreSQL schema, calling an LLM (Groq `llama-3.3-70b-versatile` by default) for a generation plan, and bulk-inserting Faker-generated rows into the `synthetic` schema. Designed for development and QA only — never runs in the production profile.

### Agent workflow

| Step | Button | What happens |
|---|---|---|
| **1 — Discover Schema** | **Discover** | Reads `information_schema` for tables and column types in the source schema (`public` by default). Click **Inspect** first to browse available schemas. |
| **2 — LLM Spec** | **Get Spec** | Sends the schema to the LLM to receive a per-column generation plan (strategies, ranges, Faker providers). Falls back to a built-in domain-aware rule-based plan if the LLM is unreachable. The fallback automatically assigns correct enum values (`ACTIVE`/`INACTIVE`/`SUBMITTED` etc.), sequential IDs for primary keys, and constrained FK ranges for referential integrity. |
| **3 — Generate Data** | **Generate** | Uses Faker to produce rows — text, numeric ranges, dates, UUIDs, booleans, JSON, categorical choices, bothify templates, and sequential counters — according to the spec. |
| **4 — Load to Database** | **Load Now** | Creates the `synthetic` schema (if absent), drops and re-creates each target table, then bulk-inserts all rows using `execute_values`. |

### Sidebar settings

| Setting | Default (Docker) | Description |
|---|---|---|
| Host | `db` (Docker) / `localhost` (local) | PostgreSQL host |
| Port | `5432` | PostgreSQL port |
| Database | `insurance_claims` | Target database |
| User / Password | `claims_user` / `claims_pass` | DB credentials |
| Source schema | `public` | Schema to introspect |
| Target schema | `synthetic` | Schema to write generated data into |
| Base URL | `https://api.groq.com/openai/v1` | LLM API endpoint |
| Model | `llama-3.3-70b-versatile` | LLM model name |
| Verify SSL | ✅ enabled | Disable only behind a corporate TLS proxy |
| Default rows per table | `200` | Rows per table when LLM spec does not specify |
| Faker locale | `en_IN` | Locale used by Faker providers |
| Seed | `12345` | Random seed for reproducible generation |
| **Demo Mode** | off | Simulates all DB and LLM calls — safe for offline demos |

### Querying generated data

```sql
-- List generated tables
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'synthetic';

-- Sample generated claims
SELECT * FROM synthetic.claims LIMIT 10;
```

To regenerate fresh data, run through all 4 steps again — each load drops and re-creates the target tables.

### Generation strategies

Each column in the spec receives a `strategy`. The supported strategies are:

| Strategy | Use for | Key extra fields |
|---|---|---|
| `faker` | Free-form text (names, sentences, emails) | `provider` — any Faker provider name |
| `categorical` | Enum/status columns with fixed valid values | `choices` — list of allowed values |
| `numeric` | Integers and decimals | `min`, `max`, `decimals` |
| `date` | Date-only columns | `date_start`, `date_end` (`YYYY-MM-DD`) |
| `timestamp` | Datetime columns | `ts_start`, `ts_end` (`YYYY-MM-DD`) |
| `uuid` | UUID primary/foreign keys | — |
| `boolean` | Boolean flags | — |
| `json` / `bytes` | JSON/JSONB or binary columns | — |
| `sequential` | **Primary key columns** — auto-incrementing IDs (1, 2, 3 …) | `start` — starting value (default: 1) |
| `template` | **Formatted strings** e.g. policy numbers | `template` — [bothify pattern](https://faker.readthedocs.io/en/master/providers/faker.providers.misc.html#faker.providers.misc.Provider.bothify) (`#` = digit, `?` = letter) |

**Critical rules for referential integrity:**
- Use `sequential` + `start: 1` for the primary key column of each table.
- Use `numeric` with `min: 1, max: <parent_row_count>` for foreign key columns. This ensures every FK value maps to a valid parent row.
- Use `categorical` with the **exact** enum values the JPA entities expect (e.g. `ACTIVE`, `SUBMITTED`, `MEDICAL`). Inventing arbitrary words causes 500 errors when Spring maps the rows.
- Use `template` with a bothify pattern for any column that must follow a specific format (e.g. `"POL-#####"` for policy numbers).

---

## Running Tests

### Backend — Unit Tests Only

```bash
cd insurance-claim-system
mvn test
```

### Backend — All Tests (unit + integration)

```bash
cd insurance-claim-system
mvn verify
```

Integration tests use the `test` Spring profile (H2 in-memory) — no external services required.

### Frontend — Unit Tests

```bash
cd insurance-frontend
npm run test          # watch mode
npm run test:coverage # single run with coverage report
```

---

## Configuration Reference

### Backend — `application.yml` key properties

| Property | Default | Description |
|---|---|---|
| `DB_USERNAME` | `claims_user` | PostgreSQL username |
| `DB_PASSWORD` | `claims_pass` | PostgreSQL password |
| `server.port` | `8080` | HTTP port |

Set active Spring profile:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/claim-submission-system-1.0.0-SNAPSHOT.jar
```

### Frontend — `vite.config.ts` proxy

The Vite dev server proxies all `/api/v1` requests to `BACKEND_URL` (default `http://localhost:8080`). When running in Docker, this is automatically set to `http://app:8080` via the `BACKEND_URL` environment variable injected by Docker Compose.

---

## Supported Claim Types

`MEDICAL`, `DENTAL`, `VISION`, `LIFE`, `AUTO`, `HOME`, `DISABILITY`

## Claim Status Flow

```
SUBMITTED → IN_REVIEW → APPROVED
                      → REJECTED
```

## Policy Number Format

Policy numbers must match the pattern **`POL-XXXXX`** (e.g., `POL-10001`, `POL-ABCDE`).

---

## Error Responses

All errors return a structured JSON body:

```json
{
  "status": 409,
  "error": "Duplicate Claim",
  "message": "Duplicate claim detected: policy=POL-10001, type=MEDICAL, incident_date=2026-03-05. A similar claim was submitted within the last 24 hours.",
  "timestamp": "2026-03-10T08:00:00",
  "details": null
}
```

| HTTP Status | Scenario |
|---|---|
| `201` | Claim submitted successfully |
| `400` | Validation failure, coverage exceeded, policy inactive |
| `404` | Policy or claim not found |
| `409` | Duplicate claim detected |
| `500` | Unexpected server error |

---

## Seeded Sample Policies

The `schema.sql` includes 5 pre-seeded policies available in the `prod` stack (and as the source schema for the synthetic agent):

| Policy Number | Status | Coverage Limit | Claim Types Covered |
|---|---|---|---|
| `POL-10001` | ACTIVE | $100,000 | MEDICAL ($50k), DENTAL ($10k), VISION ($5k) |
| `POL-10002` | ACTIVE | $75,000 | MEDICAL ($40k), DENTAL ($8k) |
| `POL-10003` | ACTIVE | $50,000 | MEDICAL ($30k), AUTO ($20k) |
| `POL-10004` | INACTIVE | $80,000 | MEDICAL ($50k) |
| `POL-10005` | ACTIVE | $200,000 | MEDICAL ($100k) |
