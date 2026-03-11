# Project Master Record

**Project:** Mini Digital Insurance Claim System
**Version:** 1.0
**Status:** In Progress
**Last Updated:** 2026-03-11

## Project Overview

A digital insurance claim submission and management system enabling customers to submit claims online, with automated policy validation, coverage verification, duplicate detection, and claims officer review/approval workflows.

## Tech Stack

| Layer | Technology |
|---|---|
| Backend | Java 17+, Spring Boot, RESTful API, OpenAPI / Swagger |
| Database | H2 (dev/test), PostgreSQL (production) |
| Frontend | React |
| Testing | JUnit (≥80% code coverage) |
| Deployment | Docker |
| Quality | Centralized exception handling, structured logging |

## Stakeholders / User Roles

| Role | Description |
|---|---|
| Customer | Submits and tracks insurance claims |
| Claims Officer / Admin | Reviews, approves/rejects claims; manages system |

## Key Business Rules

1. **Policy Verification** — Claims are only accepted for active policies of a valid claim type.
2. **Coverage Evaluation** — Claim amounts must not exceed the policy coverage limit.
3. **Duplicate Claim Prevention** — No duplicate claims allowed for the same policy/event.
4. **User Feedback & Error Messaging** — Clear, actionable error messages for all validation failures.

## Quality Requirements

- JUnit test coverage ≥ 80%
- Centralized exception handling
- Structured logging throughout
- Dockerised deployment

---

## Decisions Log

| # | Decision | Made By | Date | Impact |
|---|---|---|---|---|
| 1 | Tech stack: Java 17 + Spring Boot + React + PostgreSQL | Stakeholder | 2026-03-11 | All phases |
| 2 | H2 for dev/test, PostgreSQL for production | Stakeholder | 2026-03-11 | Architecture, DevOps |
| 3 | Claims Officer role doubles as System Admin | Stakeholder | 2026-03-11 | Requirements, Design |
| 4 | Hackathon scope: remove auth & doc upload | Stakeholder | 2026-03-11 | Requirements, Sprint Planning |

## Document Versions

| Document | Current Version | Last Updated |
|---|---|---|
| PROJECT_MASTER_RECORD.md | 1.0 | 2026-03-11 |
| docs/user-stories/user-stories.md | v1.1 | 2026-03-11 |
| docs/user-stories/user-stories-jira.csv | v1.0 | 2026-03-11 |
| docs/HLD.md | v1.0 | 2026-03-11 |
| docs/architecture/01-architecture-diagram.md | v1.0 | 2026-03-11 |
| docs/architecture/02-service-decomposition.md | v1.0 | 2026-03-11 |
| docs/LLD.md | — | — |
| docs/data-model/ | — | — |
| docs/api/ | — | — |
| docs/sprint-plan/ | — | — |
| docs/gherkin/ | — | — |
| TESTING.md | — | — |

## Open Issues

| # | Issue | Owner | Status |
|---|---|---|---|

## Phase Status

| Phase | Status | Approved By | Date |
|---|---|---|---|
| 1. Requirements | Approved | Stakeholder | 2026-03-11 |
| 2. Architecture | In Progress | — | — |
| 3. Detailed Design | Not Started | — | — |
| 4. Sprint Planning | Not Started | — | — |
| 5. Development | Not Started | — | — |
| 6. Testing | Not Started | — | — |
| 7. DevOps | Not Started | — | — |
