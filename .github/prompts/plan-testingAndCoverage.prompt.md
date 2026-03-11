# Plan: 90% Test Coverage with Jacoco Reports in Codespace

**TL;DR:** Add missing backend tests (services, controllers, exceptions), set up frontend testing with critical path coverage, configure partial entity inclusion, create a simple HTTP server to view Jacoco reports in Codespace, and add GitHub Actions for automated coverage enforcement.

---

## Steps

### **Phase 1: Backend Test Enhancement** (Parallel work)

1. **Expand ClaimServiceTest** — Add missing test cases for `ClaimServiceImpl`:
   - Edge cases (null inputs, boundary conditions)
   - Exception scenarios (CoverageExceededException, DuplicateClaimException)
   - Happy path variations
   - Target: Move from ~8 to ~15-20 test cases

2. **Expand PolicyServiceTest** — Complete coverage of `PolicyServiceImpl`:
   - Policy retrieval success/not-found scenarios
   - Response DTO mapping tests
   - Null/empty handling

3. **Enhance PolicyNumberValidatorTest** — Add more format variations:
   - Case sensitivity
   - Special characters
   - Boundary lengths

4. **Create ClaimControllerIT improvements** — Enhance integration tests:
   - Test all HTTP endpoints (@PostMapping, @GetMapping)
   - Verify response status codes and payloads
   - Test error handling paths

5. **Create new test files** (*parallel with above*):
   - `PolicyControllerIT.java` — Integration tests for PolicyController
   - `ClaimHistoryControllerIT.java` — Integration tests for ClaimHistoryController
   - `GlobalExceptionHandlerTest.java` — Test exception handling and error responses
   - `IncidentDateValidatorTest.java` — Validator unit tests
   - `ClaimRepositoryTest.java` — Spring Data JPA repository tests (using H2 test DB)
   - `PolicyRepositoryTest.java` — Policy repository tests

### **Phase 2: Jacoco Configuration Update**

6. **Modify pom.xml Jacoco exclusions** — Change from:
   ```
   <exclude>com/insurance/claim/dto/**</exclude>
   <exclude>com/insurance/claim/entity/**</exclude>
   ```
   To:
   ```
   <exclude>com/insurance/claim/dto/**</exclude>
   <!-- Keep entities INCLUDED for coverage -->
   ```
   *(DTOs remain excluded as they're simple getters/setters; entities now included)*

7. **Enable tests in Docker build** — Update `insurance-claim-system/Dockerfile`:
   - Change `mvn -B clean package -DskipTests` → `mvn -B clean package` (enable tests)
   - Optional: Add coverage threshold check before build success

### **Phase 3: Frontend Testing Setup**

8. **Install testing dependencies** in `insurance-frontend/package.json`:
   - Add: `vitest`, `@vitest/ui`, `@testing-library/react`, `@testing-library/user-event`, `jsdom`
   - Add devDependencies for type support

9. **Create frontend test framework configuration**:
   - `vitest.config.ts` — Test runner configuration with coverage settings
   - `vite.config.ts` update — Ensure test environment isolation

10. **Create critical path tests** (*implement tests for*):
    - `ClaimSubmissionForm.test.tsx` — Form validation, submission, error handling
    - `LoginPage.test.tsx` — Authentication flow, token storage
    - `ProtectedRoute.test.tsx` — Route protection logic, redirect behavior
    - `useAuth hook tests` (if exists) — Auth state and token management
    - `API client tests` — HTTP error handling, request/response mapping
    - Key UI components: `Button.test.tsx`, `Modal.test.tsx`, `Card.test.tsx`
    - Admin routes protection tests
    - Target: Cover critical auth/claim submission workflows

### **Phase 4: Jacoco Report Viewing Solution**

11. **Create HTTP server script** — Add `insurance-claim-system/serve-jacoco.js`:
    - Simple Node.js HTTP server (no external deps needed)
    - Serves jacoco reports from `target/site/jacoco/`
    - Listens on port 8081 (Codespace-forwarded port)
    - Includes directory listing for easy navigation

12. **Create .devcontainer Codespace config** (optional):
    - Pre-install Node.js (if not already available)
    - Add port forwarding config for port 8081

### **Phase 5: CI/CD Integration**

13. **Create GitHub Actions workflow** — Add `.github/workflows/test-coverage.yml`:
    - Trigger: On PR and push to main/ui
    - Steps:
      1. Checkout code
      2. Setup JDK 17 (match local Java version)
      3. Setup Node.js for frontend
      4. Run `mvn clean test jacoco:report` (backend)
      5. Run `npm test -- --coverage` (frontend)
      6. Parse and aggregate coverage
      7. Enforce minimum 90% threshold
      8. Comment PR with coverage report summary
      9. Fail CI if threshold not met

14. **Create Coverage Badge** — Add to `README.md`:
    - Link to coverage report
    - Display current coverage percentage

### **Phase 6: Documentation & Local Workflow**

15. **Create test execution guide** — Add `TESTING.md`:
    - Backend: `mvn clean test jacoco:report` → view in `target/site/jacoco/`
    - Frontend: `npm test` → view in terminal
    - Complete coverage: `npm run test:full` (combined script)
    - Serve reports locally: `node insurance-claim-system/serve-jacoco.js`

---

## Relevant Files

**Backend Changes:**
- `insurance-claim-system/pom.xml` — Update Jacoco exclusions, verify dependencies
- `insurance-claim-system/src/test/java/com/insurance/claim/service/ClaimServiceTest.java` — Expand with more test cases
- `insurance-claim-system/src/test/java/com/insurance/claim/service/PolicyServiceTest.java` — Add complete coverage
- **New files**: `*ControllerIT.java`, `*RepositoryTest.java`, exception handler tests

**Frontend Changes:**
- `insurance-frontend/package.json` — Add testing dependencies
- **Create**: `vitest.config.ts`, `vite.config.ts` update, test files for critical paths

**Tooling:**
- **Create**: `insurance-claim-system/serve-jacoco.js` — HTTP server for reports
- **Create**: `.github/workflows/test-coverage.yml` — CI/CD workflow
- **Create**: `TESTING.md` — Testing documentation
- **Update**: `README.md` — Add testing/coverage info and badges

---

## Verification

1. **Backend Coverage Test**:
   - Run `mvn clean test jacoco:report` 
   - Verify `target/site/jacoco/index.html` shows ≥80% coverage for included packages
   - Confirm DTOs excluded, entities included

2. **Frontend Coverage Test**:
   - Run `npm test -- --coverage`
   - Verify coverage report for critical components ≥80%
   - Test form submission, auth flows, protected routes

3. **Local Report Serving**:
   - Run `node insurance-claim-system/serve-jacoco.js`
   - Open browser to `localhost:8081` (or Codespace port forward)
   - Verify HTML reports are accessible and interactive

4. **Docker Build Test**:
   - Build image: `docker build .` (from insurance-claim-system/)
   - Verify tests run during build (no `-DskipTests`)
   - Confirm build fails if coverage < 90%

5. **CI/CD Test**:
   - Create test PR
   - Verify GitHub Actions runs all tests
   - Confirm PR comment shows coverage summary
   - Verify CI blocks merge if coverage < 90%

---

## Decisions

- **Partial Entity Inclusion**: Entities now counted in coverage (they contain business logic via validation annotations and relationships), DTOs remain excluded (simple data carriers)
- **Frontend Testing Priority**: Focus on critical paths (auth, claim submission, navigation) rather than exhaustive component testing—this optimizes time investment for 90% coverage
- **Codespace Report Viewing**: Simple HTTP server (minimal dependencies) rather than full dashboard, enables immediate use without complex tooling
- **CI/CD Coverage Threshold**: Set to enforce ≥80% to block PRs that reduce coverage

---

## Further Considerations

1. **Java Version Alignment**: Confirm your local JDK version matches CI (likely 17+). Verify in GitHub Actions workflow.
2. **Frontend Type Coverage**: TypeScript/React frontend may require specific coverage plugins—Vitest's built-in coverage should work, but you may need `@vitest/coverage-v8` or `@vitest/coverage-istanbul`.
3. **Test Data Management**: Current `test-data.sql` and H2 in-memory DB are good; verify schema.sql has all required entities for integration tests.

---

## Codebase Context Summary

### Backend Structure (insurance-claim-system)
- **Services**: ClaimService, PolicyService, ClaimHistoryService (with interfaces)
- **Controllers**: ClaimController, PolicyController, ClaimHistoryController, RootController
- **Repositories**: ClaimRepository, PolicyRepository, ClaimHistoryRepository, PolicyCoverageRepository
- **Validators**: PolicyNumberValidator, IncidentDateValidator (with custom annotations)
- **Entities**: Policy, Claim, PolicyCoverage, ClaimHistory, Status enums (PolicyStatus, ClaimStatus, ClaimType)
- **DTOs**: ClaimSubmissionRequest, ClaimResponse, PolicyResponse, ClaimHistoryResponse
- **Exception Handling**: GlobalExceptionHandler, 7 custom exceptions
- **Current Test Files**: ClaimServiceTest, PolicyServiceTest, PolicyNumberValidatorTest, ClaimControllerIT

### Frontend Structure (insurance-frontend)
- **Pages**: Home, LoginPage, SubmitClaimPage, TrackClaimPage, AdminPoliciesPage, AdminRoutes
- **Components**: 
  - Forms: ClaimSubmissionForm
  - Layout: Navbar
  - UI: Badge, Card, Button, Modal
  - Auth: ProtectedRoute
- **Current Tests**: NONE - requires framework setup

### Current Gaps
- Missing service/controller/exception handler tests for 90% coverage
- No frontend testing framework or tests
- Tests skipped in Docker build
- Entities/DTOs excluded from coverage metrics
- No CI/CD coverage enforcement
- No accessible way to view reports in Codespace
