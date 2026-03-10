# Insurance Claim Submission System

A production-grade Spring Boot backend for insurance claim submissions with layered architecture, policy validation, duplicate detection, and comprehensive error handling.

---

## Features

- **Claim Submission** — submit claims with full policy and coverage validation
- **Duplicate Detection** — blocks duplicate claims (same policy, type, incident date within 24 hours)
- **Coverage Limit Enforcement** — rejects claims exceeding per-type coverage limits
- **Claim State Management** — track status transitions (SUBMITTED → IN_REVIEW → APPROVED/REJECTED)
- **Claim History** — full audit trail of every status change
- **OpenAPI/Swagger UI** — interactive API documentation at `/swagger-ui.html`
- **Docker Compose** — one-command deployment with PostgreSQL
- **Comprehensive Tests** — 13 unit tests + 8 integration tests (H2 in-memory)

---

## Technology Stack

| Component | Technology |
|---|---|
| Language | Java 17 |
| Framework | Spring Boot 3.2 |
| Database | PostgreSQL 16 |
| ORM | Spring Data JPA / Hibernate |
| Validation | Jakarta Bean Validation + custom validators |
| Documentation | SpringDoc OpenAPI 3.0 (Swagger UI) |
| Build | Maven 3 |
| Containerization | Docker / Docker Compose |
| Test DB | H2 (in-memory) |
| Testing | JUnit 5, Mockito, MockMvc |

---

## Project Structure

```
insurance-claim-system/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
└── src/
    ├── main/
    │   ├── java/com/insurance/claim/
    │   │   ├── ClaimSubmissionSystemApplication.java
    │   │   ├── config/          # OpenAPI bean configuration
    │   │   ├── controller/      # REST endpoints
    │   │   ├── service/         # Business logic
    │   │   ├── repository/      # Data access (JPA)
    │   │   ├── entity/          # JPA entities + enums
    │   │   ├── dto/             # Request / response DTOs
    │   │   ├── validator/       # Custom constraint validators
    │   │   ├── exception/       # Custom exceptions + GlobalExceptionHandler
    │   │   └── util/            # ErrorResponse model
    │   └── resources/
    │       ├── application.yml        # Default (dev) profile — PostgreSQL
    │       ├── application-test.yml   # Test profile — H2
    │       ├── application-prod.yml   # Production profile
    │       └── schema.sql             # DDL + sample seed data
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

- Java 17+
- Maven 3.8+
- Docker & Docker Compose (for containerized run)

### Option 1 — Docker Compose (Recommended)

```bash
cd insurance-claim-system
docker-compose up --build
```

Services start in <2 minutes:
- **App**: http://localhost:8080
- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **PostgreSQL**: localhost:5432

To stop:

```bash
docker-compose down
```

To stop and remove data volumes:

```bash
docker-compose down -v
```

### Option 2 — Local Maven (requires PostgreSQL)

1. Start a local PostgreSQL instance and create the database:

```sql
CREATE DATABASE insurance_claims;
CREATE USER claims_user WITH PASSWORD 'claims_pass';
GRANT ALL PRIVILEGES ON DATABASE insurance_claims TO claims_user;
```

2. Run the schema:

```bash
psql -U claims_user -d insurance_claims -f insurance-claim-system/src/main/resources/schema.sql
```

3. Build and run:

```bash
cd insurance-claim-system
mvn spring-boot:run
```

Override DB connection via environment variables if needed:

```bash
DB_USERNAME=myuser DB_PASSWORD=mypass mvn spring-boot:run
```

---

## Running Tests

### Unit Tests Only

```bash
cd insurance-claim-system
mvn test
```

### All Tests (unit + integration)

```bash
cd insurance-claim-system
mvn verify
```

Integration tests run against an in-memory H2 database — no external services required.

---

## Configuration

Key properties in `src/main/resources/application.yml`:

| Property | Default | Description |
|---|---|---|
| `DB_USERNAME` | `claims_user` | PostgreSQL username |
| `DB_PASSWORD` | `claims_pass` | PostgreSQL password |
| `server.port` | `8080` | HTTP port |
| Spring profile | `default` | Use `prod` in production |

Set active profile via environment variable:

```bash
SPRING_PROFILES_ACTIVE=prod java -jar target/claim-submission-system-1.0.0-SNAPSHOT.jar
```

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

The `schema.sql` includes 5 pre-seeded policies for testing:

| Policy Number | Status | Coverage Limit | Claim Types Covered |
|---|---|---|---|
| `POL-10001` | ACTIVE | $100,000 | MEDICAL ($50k), DENTAL ($10k), AUTO ($40k) |
| `POL-10002` | ACTIVE | $75,000 | MEDICAL ($40k), VISION ($5k) |
| `POL-10003` | ACTIVE | $50,000 | MEDICAL ($30k), DENTAL ($8k) |
| `POL-10004` | INACTIVE | $80,000 | MEDICAL ($50k) |
| `POL-10005` | ACTIVE | $200,000 | MEDICAL ($100k), LIFE ($100k) |
