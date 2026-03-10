# Plan: Insurance Claim Submission System - Spring Boot Backend

## TL;DR
Build a production-grade Spring Boot backend for insurance claim submissions with layered architecture (Controller → Service → Repository → Model). The system validates policies, checks coverage limits, prevents duplicates, and tracks claim states. Deployable in <5 minutes with Maven, PostgreSQL, Docker, and comprehensive tests.

## Phases

### Phase 1: Project Setup & Database Schema
1. Initialize Maven project structure with Spring Boot 3.2+ and all required dependencies (Spring Web, Data JPA, Lombok, Validation, OpenAPI 3.0)
2. Create PostgreSQL database schema with 4 primary tables:
   - `policies` (policy_id, policy_number, customer_id, status, effective_date, expiry_date, coverage_limit)
   - `policy_coverages` (coverage_id, policy_id, claim_type, limit_amount, is_active)
   - `claims` (claim_id, policy_id, claim_type, claim_amount, incident_date, description, status, created_at, updated_at)
   - `claim_history` (history_id, claim_id, status, timestamp, reviewer_notes)
3. Create basic SQL init script (`schema.sql`) for local PostgreSQL setup

### Phase 2: Entity Models & DTOs
1. Create JPA entities with `@Entity` and Lombok annotations:
   - `Policy` entity (bidirectional relationship with PolicyCoverage)
   - `PolicyCoverage` entity (enumeration for ClaimType)
   - `Claim` entity (bidirectional relationship with ClaimHistory)
   - `ClaimHistory` entity
2. Create request/response DTOs:
   - `ClaimSubmissionRequest` (policy_number, claim_type, claim_amount, incident_date, description)
   - `ClaimResponse` (claim_id, policy_id, status, created_at, updated_at)
   - `PolicyResponse` (policy_id, policy_number, status, coverage_limits)
   - `ClaimReviewRequest` (action: APPROVE/REJECT, reviewer_notes)
3. Create enums: `ClaimStatus` (SUBMITTED, IN_REVIEW, APPROVED, REJECTED), `ClaimType`, `PolicyStatus`

### Phase 3: Validation Layer
1. Create custom validators using `@Validator` and `ConstraintValidator`:
   - `@ValidPolicyNumber` - format validation (e.g., POL-XXXXX)
   - `@ValidIncidentDate` - date cannot be future, must be within policy term
   - `@ValidClaimAmount` - positive, non-zero
2. Use standard `@NotNull`, `@NotBlank`, `@Positive` annotations in DTOs
3. Create `ValidationConfig` class with global `@ControllerAdvice` for centralized exception handling

### Phase 4: Repository Layer
1. Create repository interfaces extending `JpaRepository`:
   - `PolicyRepository` - findByPolicyNumber(), findByCustomerId()
   - `PolicyCoverageRepository` - findByPolicyIdAndClaimType()
   - `ClaimRepository` - findByPolicyIdAndClaimTypeAndIncidentDate() for duplicate detection
   - `ClaimHistoryRepository` - findByClaimIdOrderByTimestampDesc()
2. Implement custom query methods with `@Query` for complex searches

### Phase 5: Service Layer
1. Create service interfaces and implementations:
   - `PolicyService` - validatePolicy(), getPolicy()
   - `ClaimService` - submitClaim(), approveClaim(), rejectClaim(), getClaimStatus(), searchClaims()
   - `ValidationService` - checkDuplicateClaim(), verifyCoverage(), validateAmount()
2. Implement core business logic:
   - Claim submission: validate policy → check coverage → check duplicates → save with SUBMITTED status
   - Duplicate detection: query claims by policy_id, claim_type, incident_date with 24-hour window
   - Coverage verification: fetch PolicyCoverage, compare requested amount vs limit
3. Add structured logging with `@Slf4j` (Lombok)

### Phase 6: Controller Layer
1. Create REST controllers with `/api/v1` path base:
   - `ClaimController` (POST /api/v1/claims, GET /api/v1/claims/{id}, PATCH /api/v1/claims/{id}/review)
   - `PolicyController` (GET /api/v1/policies/{policyNumber})
   - `ClaimHistoryController` (GET /api/v1/claims/{id}/history)
2. Add OpenAPI annotations (@Operation, @ApiResponse, @ApiParam) to all endpoints
3. Implement proper HTTP status codes: 201 for creation, 200 for success, 400 for validation, 404 for not found, 409 for duplicate

### Phase 7: Exception Handling
1. Create custom exceptions:
   - `PolicyNotFoundException`
   - `DuplicateClaimException`
   - `CoverageExceededException`
   - `InvalidClaimTypeException`
   - `PolicyInactiveException`
2. Create `GlobalExceptionHandler` with `@ControllerAdvice`:
   - Map exceptions to structured error responses with `status`, `message`, `timestamp`, `details`
   - Handle validation errors (MethodArgumentNotValidException)
   - Log all exceptions

### Phase 8: Testing
1. Unit tests (JUnit 5) for services:
   - `ClaimServiceTest` - test submitClaim with valid/invalid policies, duplicate detection, coverage limits
   - `ValidationServiceTest` - test duplicate detection logic, coverage verification
   - `PolicyServiceTest` - test policy lookup and validation
2. Integration tests with `@SpringBootTest`:
   - Test full claim submission flow end-to-end
   - Test endpoints with MockMvc
   - Use embedded H2 database or testcontainers for PostgreSQL
3. Test fixtures: create test data builders for policies, claims

### Phase 9: Configuration & Documentation
1. Create `application.yml` with profiles:
   - default (dev): PostgreSQL localhost:5432, logging DEBUG
   - test: embedded H2
   - production: external DB config
2. Add OpenAPI bean configuration for Swagger UI at `/swagger-ui.html`
3. Create `pom.xml` with all dependencies organized by category
4. Document API with OpenAPI descriptions in controller methods

### Phase 10: Docker & Deployment
1. Create `Dockerfile` for multi-stage build:
   - Build stage: Maven build on JDK 17
   - Runtime stage: JDK 17 slim, expose port 8080
2. Create `docker-compose.yml`:
   - Service: Spring Boot app (port 8080)
   - Service: PostgreSQL (port 5432, volume for data persistence)
   - Health checks and environment variables
3. Include `.dockerignore` for clean builds

## Relevant Files (To Create)

### Project Structure
```
insurance-claim-system/
├── pom.xml
├── Dockerfile
├── docker-compose.yml
├── .dockerignore
├── src/
│   ├── main/
│   │   ├── java/com/insurance/claim/
│   │   │   ├── ClaimSubmissionSystemApplication.java
│   │   │   ├── config/ (OpenAPI, validation configs)
│   │   │   ├── controller/ (REST endpoints)
│   │   │   ├── service/ (business logic)
│   │   │   ├── repository/ (data access)
│   │   │   ├── entity/ (JPA models)
│   │   │   ├── dto/ (request/response)
│   │   │   ├── validator/ (custom validators)
│   │   │   ├── exception/ (custom exceptions)
│   │   │   └── util/ (logging, error responses)
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-test.yml
│   │       ├── application-prod.yml
│   │       └── schema.sql
│   └── test/
│       ├── java/com/insurance/claim/
│       │   ├── service/
│       │   ├── validator/
│       │   └── controller/
│       └── resources/
│           ├── application-test.yml
│           └── test-data.sql
```

### Key Files to Implement
- `pom.xml` — Maven dependencies (Spring Boot 3.2, PostgreSQL, Lombok, JUnit 5, OpenAPI 3.0)
- `ClaimSubmissionSystemApplication.java` — Spring Boot entry point
- `Claim.java`, `Policy.java`, `PolicyCoverage.java`, `ClaimHistory.java` — JPA entities
- `ClaimSubmissionRequest.java`, `ClaimResponse.java`, `PolicyResponse.java` — DTOs
- `ClaimService.java` + implementation — core business logic
- `ClaimController.java`, `PolicyController.java` — REST endpoints with OpenAPI
- `ClaimRepository.java`, `PolicyRepository.java`, etc. — data access
- `ClaimValidator.java`, `DuplicateClaimValidator.java` — custom validators
- `GlobalExceptionHandler.java` — centralized error handling
- `application.yml` — configuration with profiles
- `Dockerfile`, `docker-compose.yml` — containerization
- `ClaimServiceTest.java`, `ClaimControllerIT.java`, etc. — tests

## Implementation Dependencies & Parallelization

**Sequential Steps:**
1. Phase 1 (Project Setup) → Phase 2 (Entities & DTOs) → Phase 3 (Validators)
2. Phases 4, 5, 6 can run **in parallel** (Repository, Service, Controller are independent until integration)
3. Phase 7, 8, 9, 10 follow in sequence after development

**Blocking Dependencies:**
- Exception handling (Phase 7) should be in place before full testing (Phase 8)
- Configuration (Phase 9) should be finalized before Docker (Phase 10)
- Controllers must complete before integration tests

## Verification

1. **Build & Compile**: `mvn clean package` completes without errors
2. **Database**: PostgreSQL runs, schema created, initial data populated
3. **Unit Tests**: `mvn test` — all service and validator tests pass (>80% coverage)
4. **Integration Tests**: `mvn verify` — end-to-end claim submission flows pass
5. **API Verification** (manual with Docker):
   - Docker Compose up → services healthy
   - POST /api/v1/claims with valid payload → 201 response with claim_id
   - GET /api/v1/claims/{id} → 200 with full claim details
   - POST duplicate claim → 409 with clear error message
   - POST claim exceeding coverage → 400 with validation error
   - PUT /api/v1/claims/{id}/review (officer endpoint) → updates status to APPROVED/REJECTED
6. **OpenAPI Documentation** accessible at http://localhost:8080/swagger-ui.html
7. **Docker Deployment**: `docker-compose up` → system fully functional in <2 minutes

## Decisions & Scope

**Included:**
- Full layered architecture with clean separation
- Policy and coverage validation
- Duplicate claim prevention with efficient database queries
- Claim state management and history tracking
- Comprehensive error handling with custom exceptions
- Both unit and integration tests
- OpenAPI/Swagger documentation
- Docker containerization for rapid deployment
- Structured logging with SLF4J/Logback

**Excluded (Out of Scope):**
- Authentication/Authorization (assume all endpoints are accessible for hackathon demo)
- User management (pre-populated policies and customers in database)
- Payment processing or third-party integrations
- Email notifications
- Database migrations (using simple schema.sql for hackathon speed)
- Advanced reporting or analytics
- Frontend UI (backend only)

**Assumptions:**
- PostgreSQL available or Docker for quick deployment
- Policy and customer data pre-seeded in database via schema.sql and seed data
- No complex authorization required (all endpoints public for demo)
- Duplicate detection uses simple equality match (same policy_id, claim_type, incident_date within 24 hours)

## Further Considerations

1. **Duplicate Detection Window** — Currently set to 24 hours. Should this be configurable or fixed?
   - Recommendation: Fixed 24 hours for hackathon simplicity, easily adjustable in code.

2. **Claim Amount Rounding** — Should claim amounts be BigDecimal or Double?
   - Recommendation: Use BigDecimal for financial accuracy; already included in implementation.

3. **Initial Sample Data** — Should the schema include pre-populated policies and customers?
   - Recommendation: Yes, include 3-5 sample policies in schema.sql for easy demo/testing.

---

**Status**: Ready for implementation.
