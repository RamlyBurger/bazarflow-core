# BazarFlow Core

BazarFlow Core is an internal SaaS MVP for chilled and frozen food distributors. The practical business problem is simple: many small distributors still coordinate orders, stock, expiry dates, pricing, and delivery through Excel sheets, WhatsApp messages, and manual checks. Once SKUs, batches, outlets, and delivery windows grow, that workflow becomes hard to audit and easy to break.

This project turns that realistic operations problem into a production-style Java system. It demonstrates Java 21, Spring Boot, Spring Modulith, PostgreSQL, OAuth2 security, idempotent order submission, expiry-aware inventory reservation, event-driven workflows, and a React operations dashboard.

Interview positioning: this can be explained as an MVP for a chilled-food wholesale operation that had outgrown spreadsheets. The backend focuses on the business-critical paths: order intake, inventory reservation, batch expiry, pricing, delivery planning, auditability, and operations reporting.

Technically, it is also a focused Java 21 and Spring Modulith practice project. The main backend emphasis is module boundaries, idempotent order commands, inventory consistency, security, audit integrity, and testable domain logic. The frontend is an operations dashboard designed for staff who need to scan risk and act quickly.

## Current Status

Phase 0 bootstrap is in progress:

- Spring Boot 3.5.14 backend scaffolded with Java 21, Spring Modulith 1.4.11, Spring Security, Flyway, PostgreSQL, Kafka, and Springdoc dependencies
- Modulith module packages created for `common`, `identity`, `partner`, `catalog`, `inventory`, `pricing`, `ordering`, `fulfillment`, `audit`, and `reporting`
- `GET /api/platform/status` exposes the current module map
- React 19.2 and Vite 8.0 operations console scaffolded with TanStack Query, Recharts, and Lucide icons
- Docker Compose, Taskfile, ADRs, HTTP examples, and GitHub Actions CI added

## Tech Stack

- Java 21
- Spring Boot 3.5.14
- Spring Modulith 1.4.11
- PostgreSQL 18.3
- Keycloak 26.6.1
- Kafka
- React 19.2
- Vite 8.0
- TypeScript

## Quickstart

Prerequisites:

- JDK 21
- Docker Desktop
- Node.js 24 or another Vite 8 compatible Node release
- Task, optional

Run the backend tests:

```bash
cd server
./mvnw test
```

Run the frontend checks:

```bash
cd ops-console
npm install
npm run lint
npm run build
```

Start local infrastructure:

```bash
docker compose --env-file .env.example up -d
```

Start the API:

```bash
cd server
./mvnw spring-boot:run
```

Start the ops console:

```bash
cd ops-console
npm run dev
```

Useful URLs:

- API health: `http://localhost:8080/actuator/health`
- Platform status: `http://localhost:8080/api/platform/status`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Ops console: `http://localhost:5173`

## Git Workflow

Implementation changes should be committed in small, verified steps and pushed to GitHub for backup. Check `git status --short --branch` before work, verify with tests or builds, commit with a clear message, then push.
