# Testing Guide

This document explains how to run tests, generate coverage reports, and view them locally.

---

## Backend (Java / Spring Boot)

### Prerequisites
- JDK 17+
- Maven 3.8+ (or use the wrapper `./mvnw`)

### Run Unit Tests Only
```bash
cd insurance-claim-system
mvn clean test
```

### Run All Tests (Unit + Integration) & Generate JaCoCo Reports
```bash
cd insurance-claim-system
mvn clean verify jacoco:report
```

Reports are generated at:
- **Unit test coverage**: `target/site/jacoco/index.html`
- **Integration test coverage**: `target/site/jacoco-it/index.html`

### View Coverage Reports in Codespace

Since Codespace doesn't open HTML files directly in a browser, use the included HTTP server:

```bash
cd insurance-claim-system
node serve-jacoco.js
```

Then forward port **8081** via the Codespace Ports panel and open it in your browser.

The server automatically serves `target/site/jacoco/index.html` and all linked resources.

### Coverage Configuration

JaCoCo is configured in `pom.xml` with these exclusions:
- `com/insurance/claim/dto/**` — simple data carriers, excluded
- `com/insurance/claim/config/**` — OpenAPI config, excluded
- `com/insurance/claim/*Application.class` — entry point, excluded

Entities, services, controllers, repositories, validators, and exception handlers are all **included** in coverage metrics.

---

## Frontend (React / TypeScript)

### Prerequisites
- Node.js 20+
- npm 10+

### Install Dependencies
```bash
cd insurance-frontend
npm install
```

### Run Tests (Watch Mode)
```bash
npm test
```

### Run Tests Once (CI Mode) with Coverage
```bash
npm run test:coverage
```

Coverage report is generated at `coverage/index.html`.

### Open Vitest UI
```bash
npm run test:ui
```

### Test Files
| File | What it covers |
|------|----------------|
| `src/test/errors.test.ts` | `ApiError` class, `getErrorMessage`, `mapHttpErrorToMessage` |
| `src/test/authStore.test.ts` | Zustand auth store: login, logout, loadFromStorage, role checks |
| `src/test/ProtectedRoute.test.tsx` | Route protection, role-based redirect logic |
| `src/test/LoginPage.test.tsx` | Login UI rendering, role selection, navigation |

---

## Running Everything Together

### Backend + Frontend (combined)
```bash
# From repository root
cd insurance-claim-system && mvn clean verify jacoco:report && cd ..
cd insurance-frontend && npm run test:coverage && cd ..
```

---

## CI/CD (GitHub Actions)

The workflow `.github/workflows/test-coverage.yml` runs on every push and PR to `main`/`ui`:

1. **Backend job**: Runs `mvn clean verify jacoco:report` and enforces **≥80% instruction coverage** (fails CI if not met)
2. **Frontend job**: Runs `npm run test:coverage` with threshold checks in `vitest.config.ts`
3. **Coverage comment**: Posts a coverage summary table as a PR comment

### Artifacts
After each run, coverage reports are uploaded as artifacts:
- `jacoco-report` — Full JaCoCo HTML report (14 days retention)
- `frontend-coverage` — Vitest coverage HTML report (14 days retention)

---

## Coverage Targets

| Layer | Target | Current Exclusions |
|-------|--------|-------------------|
| Backend | ≥80% instruction coverage | DTOs, application entry, OpenAPI config |
| Frontend | ≥80% statements/branches/functions/lines | `main.tsx`, type declaration files |

---

## Test Structure

### Backend Test Files
```
src/test/java/com/insurance/claim/
├── controller/
│   ├── ClaimControllerIT.java         — Integration tests for ClaimController
│   ├── ClaimHistoryControllerIT.java  — Integration tests for ClaimHistoryController
│   └── PolicyControllerIT.java        — Integration tests for PolicyController
├── exception/
│   └── GlobalExceptionHandlerTest.java — Unit tests for all exception handlers
├── repository/
│   ├── ClaimRepositoryTest.java       — @DataJpaTest for ClaimRepository
│   └── PolicyRepositoryTest.java      — @DataJpaTest for PolicyRepository
├── service/
│   ├── ClaimServiceTest.java          — Unit tests for ClaimServiceImpl
│   └── PolicyServiceTest.java         — Unit tests for PolicyServiceImpl
└── validator/
    ├── IncidentDateValidatorTest.java  — Unit tests for IncidentDateValidator
    └── PolicyNumberValidatorTest.java  — Unit tests for PolicyNumberValidator
```

### Frontend Test Files
```
src/test/
├── setup.ts                   — Test environment setup (@testing-library/jest-dom)
├── errors.test.ts             — API error utility tests
├── authStore.test.ts          — Auth store unit tests
├── ProtectedRoute.test.tsx    — Route protection component tests
└── LoginPage.test.tsx         — Login page UI/behaviour tests
```

---

## Troubleshooting

**Backend tests fail with H2 connection errors**
- Ensure `src/test/resources/application-test.yml` is present with H2 datasource config
- Run `mvn clean` to clear cached build artifacts

**`@DataJpaTest` fails with schema errors**
- The tests use `ddl-auto: create-drop` — tables are created from entity annotations
- No manual schema setup is required for repository tests

**Frontend tests fail with import errors**
- Run `npm install` to ensure all test dependencies are installed
- Ensure `vitest.config.ts` is at the root of `insurance-frontend/`

**Vitest cannot find `@testing-library/jest-dom` matchers**
- Verify `src/test/setup.ts` imports `@testing-library/jest-dom`
- Verify `vitest.config.ts` lists `./src/test/setup.ts` in `setupFiles`
