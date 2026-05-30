# Project Context

## Vision

NexusXVA is a Java-based XVA and derivatives risk platform intended to demonstrate professional backend engineering and quantitative finance knowledge in a public portfolio repository.

The platform is inspired by systems used by investment banks, quantitative risk teams, XVA desks, and financial software vendors. It is not intended to be a commercial trading or risk system. It should be realistic enough to show strong engineering judgment while remaining small enough to build, test, document, and understand as an open-source learning project.

## Product Goal

The long-term product should allow a user to:

1. Create portfolios.
2. Add derivative instruments.
3. Define or load market data.
4. Price instruments.
5. Run Monte Carlo simulations.
6. Compute exposure profiles.
7. Calculate XVA metrics.
8. View risk and simulation results in dashboards.

## Initial XVA Focus

The first version focuses on:

- European options.
- Black-Scholes pricing.
- Greeks.
- Monte Carlo simulation.
- Expected Exposure.
- Potential Future Exposure.
- Simplified CVA.

The initial implementation should be explainable and testable. It is acceptable to use simplified financial assumptions when they are documented clearly.

## Technology Stack

Backend:

- Java 21 or newer.
- Spring Boot.
- Modular monolith architecture.
- PostgreSQL.
- JUnit.
- Mockito.
- Testcontainers.

Frontend:

- Next.js.
- TypeScript.
- Dashboard-oriented UI.
- Charts for exposure, pricing, and simulation results.

Infrastructure:

- Docker.
- Docker Compose.
- GitHub Actions.
- Future Kubernetes support after the monolith is mature.

Observability:

- Structured logging.
- Basic metrics.
- Execution timing.
- Simulation duration tracking.
- Clear error reporting.

## Development Philosophy

The project should prefer:

- Clean architecture over framework-driven design.
- Domain clarity over technical cleverness.
- Modular monolith before microservices.
- Correctness before performance.
- Measurement before optimization.
- Focused vertical slices over broad scaffolding.
- Strong tests for financial invariants.
- Explicit tradeoff documentation.

The backend is the heart of the project. The frontend exists to visualize backend results and support demonstration workflows. Backend correctness, testability, architecture, and documentation matter more than visual complexity.

## Repository Standard

This repository should read like a professional open-source project:

- Clear README and architecture documentation.
- Small, understandable milestones.
- Meaningful automated tests.
- Repeatable local development setup.
- AI-agent workflows documented in `.codex`.
- Architecture decisions recorded when choices matter.
- Examples and sample data where helpful.

## AI-Assisted Development Model

Future Codex sessions should begin by reading `.codex/context/project.md`, `.codex/context/architecture.md`, and the relevant agent, workflow, skill, or template file.

The Planner Agent is the primary agent. It designs before implementation. Programmer, Tester, Optimizer, and Reviewer agents operate from Planner specifications and project conventions.

