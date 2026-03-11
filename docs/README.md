# Insurance Claim Submission System — Documentation Index

This folder contains the complete Software Development Life Cycle (SDLC) documentation for the **Insurance Claim Submission System**, maintained continuously across 10 sprints to provide a traceable, auditable record of design decisions, architecture, and delivery.

---

## Document Map

| # | Document | Type | Description |
|---|---|---|---|
| 1 | [HLD.md](HLD.md) | Design | High Level Design — system overview, architectural decisions, service interactions, non-functional requirements |
| 2 | [LLD.md](LLD.md) | Design | Low Level Design — detailed class design, DB schema, sequence flows, error handling strategy |
| 3 | [architecture/01-architecture-diagram.md](architecture/01-architecture-diagram.md) | Architecture | System architecture diagrams (Mermaid) |
| 4 | [architecture/02-service-decomposition.md](architecture/02-service-decomposition.md) | Architecture | Service decomposition — responsibilities, dependencies, boundaries |
| 5 | [data-model/03-er-diagram.md](data-model/03-er-diagram.md) | Data | Entity-Relationship diagram (Mermaid) |
| 6 | [data-model/04-database-model.md](data-model/04-database-model.md) | Data | Full relational database model — DDL, constraints, indices |
| 7 | [api/05-openapi.yaml](api/05-openapi.yaml) | API | OpenAPI 3.0 specification — all endpoints, request/response schemas, error codes |
| 8 | [user-stories/06-user-stories.md](user-stories/06-user-stories.md) | Agile | User stories with acceptance criteria, epics, and priority |
| 9 | [user-stories/user-stories-jira.csv](user-stories/user-stories-jira.csv) | Agile | **Jira-importable CSV** — user stories ready for backlog import |
| 10 | [gherkin/07-gherkin-scenarios.md](gherkin/07-gherkin-scenarios.md) | Testing | Full Gherkin BDD feature files for all user stories |
| 11 | [gherkin/gherkin-scenarios-jira.csv](gherkin/gherkin-scenarios-jira.csv) | Testing | **Jira-importable CSV** — Gherkin test scenarios as Jira sub-tasks |
| 12 | [synthetic-data/08-synthetic-data-plan.md](synthetic-data/08-synthetic-data-plan.md) | Testing | Synthetic test data generation plan — seed scripts, test personas, boundary cases |
| 13 | [sprint-plan/09-sprint-plan.md](sprint-plan/09-sprint-plan.md) | Agile | Sprint task breakdown — 10 sprints, story points, DoD, velocity |
| 14 | [`synthetic-agent/`](../synthetic-agent/) | Agent | Synthetic Data Generation Agent — Python/Streamlit, Dockerfile, requirements (Sprint 10) |

---

## System at a Glance

**System Name:** Insurance Claim Submission System  
**Backend:** Java 21 · Spring Boot 3 · PostgreSQL · JPA/Hibernate  
**Frontend:** React 19 · TypeScript · Vite · TanStack Query · Zustand · Tailwind CSS  
**Synthetic Data Agent:** Python 3.12 · Streamlit · Faker · psycopg2 · LLM (gpt-4.1 via AICafe)  
**API Style:** REST · JSON · OpenAPI 3.0 (Swagger UI at `/swagger-ui.html`)  
**Auth Model:** Role-based (CUSTOMER / ADMIN) — demo implementation, no JWT in current scope  
**Deployment:** Docker · Docker Compose with env-based profile routing (`prod` / `test`)

---

## How to Use These Docs

1. **New team member onboarding** → Start with [HLD.md](HLD.md), then [architecture/01-architecture-diagram.md](architecture/01-architecture-diagram.md)
2. **Backend developer** → [LLD.md](LLD.md) + [data-model/04-database-model.md](data-model/04-database-model.md) + [api/05-openapi.yaml](api/05-openapi.yaml)
3. **QA / tester** → [gherkin/07-gherkin-scenarios.md](gherkin/07-gherkin-scenarios.md) + [synthetic-data/08-synthetic-data-plan.md](synthetic-data/08-synthetic-data-plan.md)
4. **Product Owner / Scrum Master** → [user-stories/06-user-stories.md](user-stories/06-user-stories.md) + [sprint-plan/09-sprint-plan.md](sprint-plan/09-sprint-plan.md)
5. **Synthetic Data Agent** → See `../synthetic-agent/` for Dockerfile + `../main.py` for the agent source. Run the test stack with `docker compose --env-file .env.test --profile test up --build` from `insurance-claim-system/`.
6. **Jira import** → Use the two CSV files in `user-stories/` and `gherkin/`
