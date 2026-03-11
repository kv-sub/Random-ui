# Architecture Diagrams
## Mini Digital Insurance Claim System

---

| Field | Value |
|---|---|
| **Project** | Mini Digital Insurance Claim System |
| **Document Type** | Architecture Diagrams |
| **Version** | v1.0 |
| **Date** | 2026-03-11 |
| **Author** | Architect Agent |

---

## 1. System Context Diagram

Shows the system in relation to its human actors and external systems.

```mermaid
C4Context
    title System Context — Mini Digital Insurance Claim System

    Person(customer, "Customer", "An insurance policy holder who submits and monitors claims via the web portal")
    Person(officer, "Claims Officer / Admin", "Internal staff member who reviews, approves, or rejects submitted claims")

    System_Boundary(system, "Mini Digital Insurance Claim System") {
        System(portal, "Insurance Claims Portal", "Provides claim submission, tracking, and review functionality")
    }

    Rel(customer, portal, "Submits claims, views history & status", "HTTPS / Browser")
    Rel(officer, portal, "Reviews claims, approves/rejects", "HTTPS / Browser")

    UpdateLayoutConfig($c4ShapeInRow="2", $c4BoundaryInRow="1")
```

### Key Points
- **Customer** interacts exclusively through the React SPA
- **Claims Officer** uses the same SPA but navigates to officer-specific routes (e.g. `/officer/claims`)
- No external system integrations exist in the hackathon scope — policy seed data is loaded internally at startup
- The portal boundary encompasses both the frontend SPA and backend API as a single logical system

---

## 2. Container Diagram

Shows the major deployable units (containers) and how they communicate.

```mermaid
C4Container
    title Container Diagram — Mini Digital Insurance Claim System

    Person(customer, "Customer")
    Person(officer, "Claims Officer")

    Container_Boundary(fe, "Frontend Container  [Docker: nginx:alpine]") {
        Container(spa, "React SPA", "React 18 · TypeScript · Vite", "Renders claim submission form, claim history, officer review queue. Built to static assets served by Nginx.")
    }

    Container_Boundary(be, "Backend Container  [Docker: eclipse-temurin:17-jre]") {
        Container(api, "Claims REST API", "Java 17 · Spring Boot 3.x", "Handles all business logic: claim submission, validation, review workflow, audit recording")
        Container(swagger, "Swagger UI / OpenAPI", "springdoc-openapi 2.x", "Auto-generated interactive API docs served at /swagger-ui.html")
    }

    Container_Boundary(data, "Data Layer") {
        ContainerDb(postgres, "PostgreSQL 15", "Relational DB [Docker: postgres:15-alpine]", "Production: stores policies, claims, audit trail. Port 5432.")
        ContainerDb(h2, "H2 In-Memory DB", "Embedded JVM [Dev/Test only]", "Zero-setup DB for local development and CI test runs. No container required.")
    }

    Rel(customer, spa, "Uses", "HTTPS :3000")
    Rel(officer, spa, "Uses", "HTTPS :3000")
    Rel(spa, api, "REST JSON", "HTTP :8080")
    Rel(api, swagger, "Serves docs from", "Internal")
    Rel(api, postgres, "Read / Write [prod profile]", "JDBC :5432")
    Rel(api, h2, "Read / Write [dev/test profile]", "In-process JDBC")
```

### Container Responsibilities

| Container | Technology | Port | Primary Responsibility |
|---|---|---|---|
| React SPA | React 18 + TypeScript + Vite → Nginx | 3000 | User interface for all actors |
| Claims REST API | Spring Boot 3.x JAR | 8080 | All business logic and data access |
| Swagger UI | springdoc-openapi (embedded) | 8080 | API documentation and manual testing |
| PostgreSQL | postgres:15-alpine | 5432 | Durable production data store |
| H2 | In-process (dev/test) | In-JVM | Fast test-time database |

---

## 3. Component Diagram

Shows the internal layered structure of the Spring Boot backend application.

```mermaid
C4Component
    title Component Diagram — Spring Boot API Internal Architecture

    Container_Boundary(api_boundary, "Claims REST API  [Spring Boot 3.x]") {

        Component(claim_ctrl, "ClaimController", "Spring @RestController", "POST /api/claims — submit claim\nGET /api/claims — list customer claims\nGET /api/claims/{id} — claim detail\nGET /api/claims/{id}/status — status check")

        Component(officer_ctrl, "OfficerController", "Spring @RestController", "GET /api/officer/claims — pending queue\nGET /api/officer/claims/{id} — detail view\nPUT /api/officer/claims/{id}/review — approve/reject")

        Component(policy_ctrl, "PolicyController", "Spring @RestController", "GET /api/policies — list policies (seeded data)\nGET /api/policies/{number} — lookup by number")

        Component(claim_svc, "ClaimService", "Spring @Service", "Orchestrates claim submission flow:\n• Policy existence check\n• Claim type validation\n• Coverage limit check\n• Duplicate detection\n• Claim persistence + audit creation")

        Component(review_svc, "ReviewService", "Spring @Service", "Orchestrates claim review flow:\n• Status transition validation\n• UNDER_REVIEW on detail view\n• APPROVED / REJECTED on decision\n• Audit event recording")

        Component(policy_svc, "PolicyService", "Spring @Service", "Policy lookup and validation logic\nWraps PolicyRepository queries")

        Component(audit_svc, "AuditService", "Spring @Service", "Creates ClaimAudit records on every status change.\nNever modifies existing audit rows.")

        Component(claim_repo, "ClaimRepository", "Spring Data JPA", "JPA queries for Claim entity:\nfindByPolicyId, findByStatus,\nfindByPolicyIdAndClaimTypeAndStatusNot")

        Component(policy_repo, "PolicyRepository", "Spring Data JPA", "JPA queries for Policy entity:\nfindByPolicyNumber, findByStatus")

        Component(audit_repo, "AuditRepository", "Spring Data JPA", "JPA queries for ClaimAudit entity:\nfindByClaimIdOrderByTimestampAsc")

        Component(exc_handler, "GlobalExceptionHandler", "Spring @ControllerAdvice", "Catches all unhandled exceptions.\nMaps to consistent RFC-7807 Problem JSON error response.")

        Component(domain, "Domain / Entities", "JPA @Entity classes", "Policy, Claim, ClaimAudit\nEnums: ClaimType, ClaimStatus, PolicyStatus")

        Component(dtos, "DTOs", "Java Records / POJOs", "ClaimRequest, ClaimResponse, ReviewRequest,\nPolicyResponse, ErrorResponse, AuditEntry")
    }

    ContainerDb(db, "Database", "H2 / PostgreSQL", "")

    Rel(claim_ctrl, claim_svc, "Calls")
    Rel(officer_ctrl, review_svc, "Calls")
    Rel(officer_ctrl, claim_svc, "Calls (read)")
    Rel(policy_ctrl, policy_svc, "Calls")

    Rel(claim_svc, policy_svc, "Uses (validate policy)")
    Rel(claim_svc, audit_svc, "Uses (record SUBMITTED event)")
    Rel(claim_svc, claim_repo, "Uses")
    Rel(review_svc, claim_repo, "Uses")
    Rel(review_svc, audit_svc, "Uses (record review events)")
    Rel(policy_svc, policy_repo, "Uses")
    Rel(audit_svc, audit_repo, "Uses")

    Rel(claim_repo, db, "JDBC / JPA")
    Rel(policy_repo, db, "JDBC / JPA")
    Rel(audit_repo, db, "JDBC / JPA")

    Rel(claim_ctrl, exc_handler, "Exceptions propagate to")
    Rel(officer_ctrl, exc_handler, "Exceptions propagate to")

    Rel(claim_ctrl, dtos, "Uses (request/response)")
    Rel(officer_ctrl, dtos, "Uses (request/response)")
    Rel(claim_svc, domain, "Uses (entities)")
    Rel(review_svc, domain, "Uses (entities)")
```

### Layer Descriptions

| Layer | Classes | Pattern |
|---|---|---|
| **Controller** | `ClaimController`, `OfficerController`, `PolicyController` | Spring `@RestController` — HTTP routing, request validation (`@Valid`), response mapping |
| **Service** | `ClaimService`, `ReviewService`, `PolicyService`, `AuditService` | Spring `@Service` — business rules, orchestration, transaction boundaries (`@Transactional`) |
| **Repository** | `ClaimRepository`, `PolicyRepository`, `AuditRepository` | Spring Data JPA `JpaRepository` — query abstraction, no SQL boilerplate |
| **Domain** | `Policy`, `Claim`, `ClaimAudit` + enums | JPA `@Entity` — persistence mapping, column constraints |
| **DTO** | `ClaimRequest`, `ClaimResponse`, `ReviewRequest`, `AuditEntry`, `ErrorResponse` | Java records / POJOs — API contract, decoupled from JPA layer |
| **Cross-cutting** | `GlobalExceptionHandler` | `@ControllerAdvice` — intercepts all exceptions, returns consistent error JSON |

---

## 4. Deployment Diagram

Shows how containers are deployed using Docker Compose on a single host.

```mermaid
C4Deployment
    title Deployment Diagram — Docker Compose on a Single Docker Host

    Deployment_Node(host, "Docker Host", "Linux / Docker Engine 24+  (developer laptop or CI runner)") {

        Deployment_Node(net, "Docker Bridge Network: claims-net", "Internal container-to-container communication") {

            Deployment_Node(fe_node, "frontend  [Container]", "Image: nginx:alpine — built from ./frontend/Dockerfile") {
                Container(spa_deploy, "React SPA (static assets)", "Vite build output served by Nginx on container port 80", "Responds to browser requests. Proxies /api/* to backend container.")
            }

            Deployment_Node(be_node, "backend  [Container]", "Image: eclipse-temurin:17-jre — built from ./backend/Dockerfile") {
                Container(api_deploy, "Spring Boot JAR", "java -jar claims-api.jar  — container port 8080", "Handles REST requests from SPA. Connects to postgres over claims-net.")
                Container(swagger_deploy, "Swagger UI", "Served by Spring Boot at /swagger-ui.html", "Bundled within the same Spring Boot process.")
            }

            Deployment_Node(db_node, "postgres  [Container]", "Image: postgres:15-alpine") {
                ContainerDb(pg_deploy, "PostgreSQL 15", "Listens on container port 5432", "Stores all application data. Backed by named volume pgdata.")
            }
        }

        Deployment_Node(volumes, "Docker Named Volumes", "") {
            Container(pgdata, "pgdata", "Docker volume", "Persists PostgreSQL data across container restarts.")
        }
    }

    Rel(spa_deploy, api_deploy, "REST API calls", "HTTP  host:8080 → container:8080")
    Rel(api_deploy, pg_deploy, "Database queries", "JDBC  hostname: postgres  port: 5432")
    Rel(pg_deploy, pgdata, "Persists data to", "Volume mount: /var/lib/postgresql/data")
```

### Port Mapping Summary

| Service | Host Port | Container Port | Access From |
|---|---|---|---|
| `frontend` | `3000` | `80` | Browser → `http://localhost:3000` |
| `backend` | `8080` | `8080` | Browser (Swagger) → `http://localhost:8080/swagger-ui.html`; frontend SPA internally |
| `postgres` | `5432` | `5432` | DBA tools / local debugging only; not exposed in production |

### Docker Compose Quick Reference

```yaml
# Abbreviated — see docker-compose.yml for full definition
services:
  frontend:
    build: ./frontend
    ports: ["3000:80"]
    depends_on: [backend]
    networks: [claims-net]

  backend:
    build: ./backend
    ports: ["8080:8080"]
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/claimsdb
      SPRING_PROFILES_ACTIVE: prod
    depends_on: [postgres]
    networks: [claims-net]

  postgres:
    image: postgres:15-alpine
    ports: ["5432:5432"]
    environment:
      POSTGRES_DB: claimsdb
      POSTGRES_USER: claims_user
      POSTGRES_PASSWORD: claims_pass
    volumes: [pgdata:/var/lib/postgresql/data]
    networks: [claims-net]

volumes:
  pgdata:

networks:
  claims-net:
    driver: bridge
```

---

*End of Architecture Diagrams — Mini Digital Insurance Claim System v1.0*
