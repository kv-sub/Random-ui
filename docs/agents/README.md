# Agent System — Usage Guide
## Role-Based Custom Agents for Agile SDLC

**Version:** 1.1  
**Date:** March 2026

---

## What Are These Agents?

This repository includes a set of **project-agnostic, role-based custom agents** defined as `.prompt.md` files in `.github/prompts/`. Each agent represents a specialised role in the agile Software Development Lifecycle (SDLC) and works for **any software project and any technology stack**.

| Agent | Role | When to Use |
|---|---|---|
| 🎯 Orchestrator | Workflow coordinator | Start any project or phase |
| 📋 Product Owner | Requirements & user stories | Phase 1: Gather requirements |
| 🏛️ Architect | HLD & architecture | Phase 2: Design the system |
| 🔧 System Designer | LLD & API contracts | Phase 3: Detail the design |
| 📅 Sprint Planner | Sprint plans & backlog | Phase 4: Plan delivery |
| 💻 Developer | Code implementation | Phase 5: Build the system |
| 🧪 Tester | Gherkin tests & QA | Phase 6: Test the system |
| 🚀 DevOps | Docker & CI/CD | Phase 7: Deploy the system |

**Technology stack is never pre-assumed.** The Architect Agent presents options and the user chooses. All downstream agents (Developer, Tester, DevOps) adapt their templates to the chosen stack.

---

## Rebuilding the Insurance Claim Submission System From Scratch

The SDLC documents for the Insurance Claim Submission System already exist in `docs/`. You can use the Developer Agent to rebuild the entire implementation from scratch:

### Step 1: Start the Developer Agent for Sprint 1

```
@workspace #file:.github/prompts/agent-developer.prompt.md

Re-develop from scratch starting from Sprint 1.

Use these approved documents:
- docs/HLD.md (architecture + tech stack)
- docs/LLD.md (detailed design + DDL)
- docs/sprint-plan/sprint-plan.md (sprint tasks)
- docs/user-stories/user-stories.md (acceptance criteria)
- docs/api/openapi.yaml (API contract)
```

### Step 2: Validate Sprint 1 with the Tester Agent

```
@workspace #file:.github/prompts/agent-tester.prompt.md

Write Gherkin scenarios and step definitions for Sprint 1 stories.
Reference: docs/user-stories/user-stories.md and docs/LLD.md
```

### Step 3: Continue Sprint by Sprint

Repeat Step 1 and Step 2 for each sprint, referencing the sprint plan for scope.

### Step 4: Set Up Infrastructure

```
@workspace #file:.github/prompts/agent-devops.prompt.md

Set up Docker and CI/CD for this project.
Reference: docs/HLD.md (deployment section and tech stack)
```

---

## Starting a Brand-New Project

Open GitHub Copilot Chat and use the Orchestrator Agent:

```
@workspace #file:.github/prompts/agent-orchestrator.prompt.md

Start a new project: [describe your project in 2-3 sentences]
```

The Orchestrator will guide you through all phases, presenting technology options at each decision point and asking for your approval before proceeding.

---

## Running Individual Agents

You can invoke any agent directly for specific tasks:

```
# Requirements phase
@workspace #file:.github/prompts/agent-product-owner.prompt.md
Gather requirements for: [project description]

# Architecture phase
@workspace #file:.github/prompts/agent-architect.prompt.md
Design architecture for: [project name]
Reference the user stories in docs/user-stories/user-stories.md

# Detailed design phase
@workspace #file:.github/prompts/agent-system-designer.prompt.md
Create LLD for: [project name]
Reference docs/HLD.md for architecture and tech stack context

# Sprint planning
@workspace #file:.github/prompts/agent-sprint-planner.prompt.md
Create sprint plan for: [project name]
Team: [1 backend, 1 frontend, 1 QA — or your actual team]

# Development
@workspace #file:.github/prompts/agent-developer.prompt.md
Implement Sprint [N]
Reference: docs/HLD.md, docs/LLD.md, docs/sprint-plan/sprint-plan.md

# Testing
@workspace #file:.github/prompts/agent-tester.prompt.md
Write Gherkin scenarios for all stories in docs/user-stories/user-stories.md

# DevOps
@workspace #file:.github/prompts/agent-devops.prompt.md
Set up Docker and CI/CD for this project
Reference: docs/HLD.md for tech stack and service layout
```

---

## Making Changes to an Existing Project

When a stakeholder requests a change:

### Step 1: Update Requirements
```
@workspace #file:.github/prompts/agent-product-owner.prompt.md
Add user story: [describe new feature]
Update acceptance criteria for: US-XX
```

### Step 2: Update Architecture (if needed)
```
@workspace #file:.github/prompts/agent-architect.prompt.md
Update HLD with: [describe architecture change]
```

### Step 3: Update Design (if needed)
```
@workspace #file:.github/prompts/agent-system-designer.prompt.md
Update LLD: add [new component/endpoint/table]
```

### Step 4: Re-plan Sprints
```
@workspace #file:.github/prompts/agent-sprint-planner.prompt.md
Re-plan remaining backlog to include: US-XX
```

### Step 5: Implement the Change
```
@workspace #file:.github/prompts/agent-developer.prompt.md
Implement story US-XX: [title]
```

### Step 6: Write Tests
```
@workspace #file:.github/prompts/agent-tester.prompt.md
Write Gherkin scenarios for: US-XX
```

---

## Document Versioning

Every agent automatically:
1. Adds a **Document History** entry to any document it modifies
2. Increments the version number appropriately
3. Updates `CHANGELOG.md`
4. Updates `docs/PROJECT_MASTER_RECORD.md`

**Version scheme:**
- `1.0` → Initial creation
- `1.1, 1.2, ...` → Incremental additions
- `2.0` → Major restructure

---

## Agent Approval Gates

The Orchestrator enforces approval gates between phases. You **must** explicitly approve each phase before the next one starts:

```
Phase 1 → Phase 2: "Do you approve the user stories? [yes/no/modify]"
Phase 2 → Phase 3: "Do you approve the HLD? [yes/no/modify]"
Phase 3 → Phase 4: "Do you approve the LLD? [yes/no/modify]"
Phase 4 → Phase 5: "Do you approve the sprint plan? [yes/no/modify]"
Phase 5 → Phase 6: "Sprint N complete. Review and approve before testing?"
Phase 6 → Phase 7: "Tests passing. Ready to set up deployment?"
```

---

## Tips

1. **Always start with the Orchestrator** for new projects — it manages dependencies between agents.
2. **Reference documents explicitly** when invoking agents — they need context from previous phases.
3. **Approve before advancing** — never skip an approval gate; it ensures quality.
4. **Use agents for changes too** — don't manually edit docs; let the agents version them properly.
5. **Agents are interactive** — they will ask you questions. Answer them to get the best output.
6. **Tech stack is your choice** — the Architect Agent will present options. You decide; agents adapt.

---

## Troubleshooting

| Issue | Solution |
|---|---|
| Agent output doesn't match the project | Provide the relevant docs as context using `#file:` |
| Agent skipped requirements | Re-invoke with explicit reference to the prerequisite documents |
| Documents are out of sync | Use the Orchestrator to identify which phase caused the divergence |
| Need to undo a change | Revert the document to a previous version using Git, then re-invoke the agent |
| Agent uses wrong tech stack | Ensure `docs/HLD.md` Technology Stack section is complete and approved before invoking Developer/Tester/DevOps agents |
