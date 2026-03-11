# Project Master Record
## Insurance Claim Submission System

**Project:** Insurance Claim Submission System  
**Version:** 1.0  
**Status:** Complete (Sprint 10 Done)  
**Last Updated:** March 2026  
**Maintained By:** Orchestrator Agent

---

## Document History

| Version | Date | Changes |
|---|---|---|
| 1.0 | 2026-03-11 | Initial PMR — reverse engineered from completed project; agent system established |

---

## Project Summary

An end-to-end insurance claim submission system consisting of:
- **React frontend** (React 19, TypeScript, Vite, Tailwind CSS)
- **Spring Boot REST API** (Java 21, Spring Boot 3, PostgreSQL 16)
- **Synthetic Data Generation Agent** (Python, Streamlit, Faker, OpenAI-compatible LLM)
- **Full test suite** (JUnit 5, Cucumber BDD, Playwright E2E)
- **CI/CD** (GitHub Actions, Docker Compose, multi-environment profiles)

---

## Decisions Log

| # | Decision | Made By | Date | Impact |
|---|---|---|---|---|
| 1 | Use Spring Boot 3 + Java 21 for backend | Architect | 2026-01-05 | All backend implementation |
| 2 | Use React 19 + TypeScript + Vite for frontend | Architect | 2026-01-05 | All frontend implementation |
| 3 | Use PostgreSQL 16 as primary database | Architect | 2026-01-05 | DB schema, JPA config |
| 4 | Use TanStack Query for server state management | Architect | 2026-01-05 | Frontend API layer |
| 5 | Use Zustand for client state management | Architect | 2026-01-05 | Frontend auth store |
| 6 | Role-based access: CUSTOMER and ADMIN | PO | 2026-01-05 | Auth, protected routes |
| 7 | Claim duplicate detection: 24-hour window | PO | 2026-02-16 | ClaimService business logic |
| 8 | Policy number format: `POL-` + 5 alphanumeric | System Designer | 2026-01-05 | Validation, regex |
| 9 | Add Synthetic Data Agent (test-only) | PO | 2026-05-11 | New service, test Docker profile |
| 10 | Synthetic agent excluded from prod profile | DevOps | 2026-05-11 | docker-compose.prod.yml |

---

## Document Versions

| Document | Current Version | Last Updated | Agent |
|---|---|---|---|
| `docs/HLD.md` | 1.3 | 2026-05-11 | Architect |
| `docs/LLD.md` | 1.4 | 2026-05-11 | System Designer |
| `docs/user-stories/06-user-stories.md` | 1.2 | 2026-03-30 | Product Owner |
| `docs/gherkin/07-gherkin-scenarios.md` | 1.3 | 2026-03-30 | Tester |
| `docs/sprint-plan/09-sprint-plan.md` | 1.3 | 2026-05-11 | Sprint Planner |
| `docs/architecture/01-architecture-diagram.md` | 1.2 | 2026-05-11 | Architect |
| `docs/architecture/02-service-decomposition.md` | 1.2 | 2026-05-11 | System Designer |
| `docs/data-model/03-er-diagram.md` | 1.0 | 2026-01-05 | System Designer |
| `docs/data-model/04-database-model.md` | 1.0 | 2026-01-05 | System Designer |
| `docs/api/05-openapi.yaml` | 1.0 | 2026-01-05 | System Designer |
| `CHANGELOG.md` | — | 2026-05-11 | DevOps / PO |
| `TESTING.md` | — | 2026-05-11 | Tester / DevOps |
| `README.md` | — | 2026-05-11 | DevOps |
| `docs/agents/00-agent-interaction-model.md` | 1.0 | 2026-03-11 | Orchestrator |

---

## Open Issues

| # | Issue | Owner | Status |
|---|---|---|---|
| — | No open issues | — | — |

---

## Phase Status

| Phase | Status | Approved By | Date |
|---|---|---|---|
| 1. Requirements (Product Owner) | ✅ Complete | Product Owner | 2026-03-30 |
| 2. Architecture (Architect) | ✅ Complete | Architect | 2026-05-11 |
| 3. Detailed Design (System Designer) | ✅ Complete | System Designer | 2026-05-11 |
| 4. Sprint Planning (Sprint Planner) | ✅ Complete | Sprint Planner | 2026-05-11 |
| 5. Development (Developer) | ✅ Complete | Developer | 2026-05-22 |
| 6. Testing (Tester) | ✅ Complete | Tester | 2026-05-22 |
| 7. DevOps (DevOps) | ✅ Complete | DevOps | 2026-05-22 |

---

## Sprint Velocity

| Sprint | Theme | Planned Points | Actual Points | Completion |
|---|---|---|---|---|
| Sprint 1 | Auth & Navigation | 13 | 13 | 100% |
| Sprint 2 | Policy Management | 10 | 10 | 100% |
| Sprint 3 | Claim Submission | 18 | 18 | 100% |
| Sprint 4 | Duplicate Prevention + Tracking | 13 | 13 | 100% |
| Sprint 5 | History + Admin List | 11 | 11 | 100% |
| Sprint 6 | Admin Adjudication | 13 | 13 | 100% |
| Sprint 7 | Audit Trail + Error Handling | 10 | 10 | 100% |
| Sprint 8 | Integration + Load Testing | 8 | 8 | 100% |
| Sprint 9 | Production Hardening + Docs | 5 | 5 | 100% |
| Sprint 10 | Synthetic Data Agent | 13 | 13 | 100% |

---

## Key Metrics

| Metric | Value |
|---|---|
| Total User Stories | 13 |
| Total Sprints | 10 |
| Total Story Points Delivered | 114 |
| Backend Test Coverage | ≥80% (JaCoCo) |
| BDD Acceptance Scenarios | 50+ |
| API Endpoints | 7 |
| Database Tables | 5 (4 core + 1 synthetic) |

---

## Next Steps (For Re-development)

To re-develop this project using the agent system:

```
1. @orchestrator Re-develop project: Insurance Claim Submission System
2. Review and approve all existing docs (or update them first)
3. @developer Implement Sprint 1 from docs
4. Continue sprint by sprint with @developer
5. @tester Validate each sprint
6. @devops Configure deployment
```

See `docs/agents/README.md` for detailed instructions.
