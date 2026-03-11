# Agent System — Usage Guide
## Role-Based Custom Agents for Agile SDLC

**Version:** 1.0  
**Date:** March 2026

---

## What Are These Agents?

This repository includes a set of **role-based custom agents** defined as `.prompt.md` files in `.github/prompts/`. Each agent represents a specialised role in the agile Software Development Lifecycle (SDLC):

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

---

## Quick Start

### Starting a New Project

Open GitHub Copilot Chat and use the Orchestrator Agent:

```
@workspace #file:.github/prompts/agent-orchestrator.prompt.md

Start a new project: [describe your project in 2-3 sentences]
```

The Orchestrator will guide you through all phases, asking for your approval at each step.

---

### Running Individual Agents

You can invoke any agent directly for specific tasks:

```
# Requirements phase
@workspace #file:.github/prompts/agent-product-owner.prompt.md
Gather requirements for: [project description]

# Architecture phase
@workspace #file:.github/prompts/agent-architect.prompt.md
Design architecture for: [project name]
Reference the user stories in docs/user-stories/06-user-stories.md

# Detailed design phase
@workspace #file:.github/prompts/agent-system-designer.prompt.md
Create LLD for: [project name]
Reference docs/HLD.md for architecture context

# Sprint planning
@workspace #file:.github/prompts/agent-sprint-planner.prompt.md
Create sprint plan for: [project name]
Team: 1 backend, 1 frontend, 1 QA

# Development
@workspace #file:.github/prompts/agent-developer.prompt.md
Implement Sprint 1

# Testing
@workspace #file:.github/prompts/agent-tester.prompt.md
Write Gherkin scenarios for all stories in docs/user-stories/06-user-stories.md

# DevOps
@workspace #file:.github/prompts/agent-devops.prompt.md
Set up Docker and CI/CD for this project
```

---

## Re-developing the Insurance Claim System

To rebuild this project from scratch using the agents:

### Step 1: Verify Documents Are Up-to-Date
```
@workspace #file:.github/prompts/agent-orchestrator.prompt.md
Show project status for: Insurance Claim Submission System
```

### Step 2: Start Developer Agent from Sprint 1
```
@workspace #file:.github/prompts/agent-developer.prompt.md
Re-develop from scratch, starting from Sprint 1.
Reference:
- docs/HLD.md
- docs/LLD.md
- docs/sprint-plan/09-sprint-plan.md
- docs/user-stories/06-user-stories.md
- docs/api/05-openapi.yaml
```

### Step 3: Validate with Tester Agent
```
@workspace #file:.github/prompts/agent-tester.prompt.md
Validate Sprint 1 implementation against user stories in docs/user-stories/06-user-stories.md
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

---

## Troubleshooting

| Issue | Solution |
|---|---|
| Agent output doesn't match the project | Provide the relevant docs as context using `#file:` |
| Agent skipped requirements | Re-invoke with explicit reference to the prerequisite documents |
| Documents are out of sync | Use the Orchestrator to identify which phase caused the divergence |
| Need to undo a change | Revert the document to a previous version using Git, then re-invoke the agent |
