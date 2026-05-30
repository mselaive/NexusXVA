# NexusXVA Codex Operating System

This directory is the AI-assisted development guide for NexusXVA.

Future Codex sessions should treat `.codex` as the project memory and operating manual. Before planning, coding, testing, reviewing, or optimizing, read the relevant files here.

## Start Here

For general project context:

1. `context/project.md`
2. `context/architecture.md`
3. `context/conventions.md`
4. `context/roadmap.md`

For agent behavior:

- `agents/planner.md`
- `agents/programmer.md`
- `agents/tester.md`
- `agents/reviewer.md`
- `agents/optimizer.md`

The Planner Agent is primary. Non-trivial work should start with a Planner specification before code is written.

## How To Use The Agents

The files in `agents/` are role definitions for Codex. They are not application code, and they do not automatically become selectable GitHub agents unless the Codex or GitHub environment explicitly supports registering custom agents from repository files.

Use them by asking Codex to adopt one of the roles and read the relevant project context.

Planner example:

```text
Act as the Planner Agent defined in .codex/agents/planner.md.
Read .codex/context/project.md, .codex/context/architecture.md,
.codex/context/conventions.md, and .codex/context/roadmap.md.

Create the MVP feature specification using .codex/templates/feature-spec.md.
Do not write production code.
```

Reviewer example:

```text
Act as the Reviewer Agent defined in .codex/agents/reviewer.md.
Review the Planner specification for architecture risks, missing requirements,
overengineering, module boundaries, and test gaps.
Do not implement code.
```

Programmer example:

```text
Act as the Programmer Agent defined in .codex/agents/programmer.md.
Implement only the approved Planner specification.
Follow .codex/context/conventions.md and the relevant skill files.
Report files changed, tests added, assumptions, and how to run verification.
```

Tester example:

```text
Act as the Tester Agent defined in .codex/agents/tester.md.
Review the implemented feature against the Planner specification.
Add or recommend tests for financial correctness, edge cases, API behavior,
persistence behavior, and regression safety.
```

Optimizer example:

```text
Act as the Optimizer Agent defined in .codex/agents/optimizer.md.
Review the existing implementation for MVP-appropriate performance improvements.
Do not introduce complex infrastructure without measurement and justification.
```

Recommended sequence:

1. Ask the Planner Agent for a specification.
2. Ask the Reviewer Agent to review the specification.
3. Ask the Planner Agent to revise the specification if needed.
4. Ask the Programmer Agent to implement the approved specification.
5. Ask the Tester Agent to validate or improve tests.
6. Ask the Optimizer Agent for performance review only after working code exists.
7. Ask the Reviewer Agent for final merge review.

If GitHub or Codex shows selectable agents in the UI, copy the matching file from `agents/` into that agent's instructions, or tell the selected agent to read the file before starting.

## Directory Map

- `context/`: project vision, architecture, roadmap, and conventions.
- `agents/`: role definitions for specialized Codex agents.
- `skills/`: reusable domain and engineering knowledge.
- `workflows/`: repeatable development processes.
- `templates/`: structured templates for specs, APIs, ADRs, and test plans.

## Default Workflow

Use this flow for non-trivial work:

1. Planner creates the feature specification.
2. Reviewer validates the design.
3. Planner revises if needed.
4. Programmer implements.
5. Tester validates.
6. Optimizer reviews only after working code exists.
7. Reviewer gives final merge verdict.

## Project North Star

NexusXVA should demonstrate professional Java backend engineering, clean architecture, quantitative finance understanding, strong tests, and clear documentation.

Build the platform through small, realistic vertical slices. Avoid premature distributed architecture. Prefer correctness, clarity, and maintainability before optimization.
